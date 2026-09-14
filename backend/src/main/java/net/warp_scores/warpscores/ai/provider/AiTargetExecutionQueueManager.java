package net.warp_scores.warpscores.ai.provider;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.provider.trace.AiGenerationTraceStore;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

@Service
@RequiredArgsConstructor
public class AiTargetExecutionQueueManager {
    private final AiProviderProperties properties;
    private final LlmProviderRegistry registry;
    private final AiGenerationTraceStore traceStore;
    private final AiGenerationAdmissionService admission;
    private final Map<String, TargetQueue> queues = new ConcurrentHashMap<>();
    private final Map<String, QuotaState> quotas = new ConcurrentHashMap<>();

    @PostConstruct void initialize(){properties.getTargets().forEach((id,cfg)->{if(cfg.getModality()==AiProviderProperties.Modality.TEXT) queues.computeIfAbsent(id,x->new TargetQueue(id,cfg));});}

    public CanonicalLlmResponse execute(LlmProviderRouter.ModelTarget target,String agentId,CanonicalLlmRequest request,int priority){
        if(target.targetId()==null) return direct(target,agentId,request);
        AiProviderProperties.TargetConfig cfg=properties.getTargets().get(target.targetId());
        if(cfg==null||cfg.getModality()!=AiProviderProperties.Modality.TEXT) throw new IllegalStateException("Unknown/non-text AI target: "+target.targetId());
        TargetQueue q=queues.computeIfAbsent(target.targetId(),x->new TargetQueue(target.targetId(),cfg));
        QueuedCall call=new QueuedCall(UUID.randomUUID().toString(),priority,target,agentId,request); q.enqueue(call);
        try{return call.result.join();}catch(CompletionException e){Throwable c=e.getCause();if(c instanceof RuntimeException r)throw r;throw e;}
    }

    public List<QueueSnapshot> snapshots(){return queues.values().stream().sorted(Comparator.comparing(q->q.targetId)).map(TargetQueue::snapshot).toList();}
    public boolean reprioritize(String target,String job,int priority){validatePriority(priority);TargetQueue q=queues.get(target);return q!=null&&q.reprioritize(job,priority);}
    public boolean remove(String target,String job){TargetQueue q=queues.get(target);return q!=null&&q.remove(job);}
    public int clear(String target){TargetQueue q=queues.get(target);return q==null?0:q.clear();}
    @PreDestroy void stop(){queues.values().forEach(TargetQueue::stop);}

    private CanonicalLlmResponse direct(LlmProviderRouter.ModelTarget t,String agent,CanonicalLlmRequest r){admission.acquire(agent,r);try{return registry.require(t.providerId()).generate(r);}finally{admission.release();}}

    private final class TargetQueue {
        final String targetId; final AiProviderProperties.TargetConfig config; final Object monitor=new Object(); final List<QueuedCall> pending=new ArrayList<>(); final List<Thread> workers=new ArrayList<>();
        final AtomicInteger running=new AtomicInteger(); final AtomicLong succeeded=new AtomicLong(); final AtomicLong failed=new AtomicLong();
        volatile boolean stopping; volatile String lastError; volatile Integer lastStatusCode; volatile Instant lastErrorAt;
        TargetQueue(String id,AiProviderProperties.TargetConfig cfg){targetId=id;config=cfg;for(int i=0;i<Math.max(1,cfg.getQueue().getConcurrency());i++){Thread t=new Thread(this::loop,"ai-target-"+id+"-"+(i+1));t.setDaemon(true);workers.add(t);t.start();}}
        void enqueue(QueuedCall c){synchronized(monitor){pending.add(c);monitor.notifyAll();}}
        void loop(){while(!stopping&&!Thread.currentThread().isInterrupted()){try{QueuedCall c=take();if(c!=null)run(c);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}}}
        QueuedCall take() throws InterruptedException {synchronized(monitor){while(!stopping){Instant now=Instant.now();Instant blocked=quota(group()).blockedUntil();QueuedCall best=pending.stream().filter(c->!c.result.isDone()).filter(c->c.availableAt==null||!c.availableAt.isAfter(now)).filter(c->blocked==null||!blocked.isAfter(now)).max(Comparator.comparingInt((QueuedCall c)->c.priority).thenComparing(c->c.createdAt,Comparator.reverseOrder())).orElse(null);if(best!=null){pending.remove(best);best.status=JobStatus.RUNNING;return best;}Instant wake=blocked;for(QueuedCall c:pending)if(c.availableAt!=null&&(wake==null||c.availableAt.isBefore(wake)))wake=c.availableAt;if(wake==null)monitor.wait();else monitor.wait(Math.max(1L,Duration.between(now,wake).toMillis()));}return null;}}
        void run(QueuedCall c){running.incrementAndGet();boolean admitted=false;long start=System.nanoTime();try{try{admission.acquire(c.agentId,c.request);admitted=true;}catch(AiGenerationAdmissionService.AdmissionDeniedException d){if(d.reason()==AiGenerationAdmissionService.DenialReason.CONCURRENCY_LIMIT){c.status=JobStatus.QUEUED;c.availableAt=Instant.now().plusMillis(500);enqueue(c);return;}throw d;}LlmProvider p=registry.require(c.target.providerId());if(!p.isConfigured())throw new IllegalStateException("AI provider is not configured: "+c.target.providerId());c.attempts++;CanonicalLlmResponse response=p.generate(c.request);traceStore.recordSuccess(c.agentId,c.target.providerId(),c.request,response,elapsed(start));c.status=JobStatus.SUCCEEDED;succeeded.incrementAndGet();c.result.complete(response);}catch(LlmProviderException e){traceStore.recordFailure(c.agentId,c.target.providerId(),c.request,e,elapsed(start));remember(e);if(e.kind()==LlmProviderException.Kind.RATE_LIMIT&&c.attempts<Math.max(1,config.getQueue().getMaxAttempts())){Instant retry=e.retryAt();if(retry==null||!retry.isAfter(Instant.now()))retry=Instant.now().plus(backoff(c.attempts));quota(group()).blockUntil(retry,e.getMessage());c.status=JobStatus.RETRY_WAIT;c.availableAt=retry;c.lastError=shortError(e);enqueue(c);return;}c.status=JobStatus.FAILED;c.lastError=shortError(e);failed.incrementAndGet();c.result.completeExceptionally(e);}catch(RuntimeException e){traceStore.recordUnexpectedFailure(c.agentId,c.target.providerId(),c.request,e,elapsed(start));lastError=shortError(e);lastErrorAt=Instant.now();c.status=JobStatus.FAILED;c.lastError=shortError(e);failed.incrementAndGet();c.result.completeExceptionally(e);}finally{if(admitted)admission.release();running.decrementAndGet();}}
        boolean reprioritize(String id,int p){synchronized(monitor){QueuedCall c=pending.stream().filter(x->x.id.equals(id)).findFirst().orElse(null);if(c==null)return false;c.priority=p;monitor.notifyAll();return true;}}
        boolean remove(String id){synchronized(monitor){QueuedCall c=pending.stream().filter(x->x.id.equals(id)).findFirst().orElse(null);if(c==null)return false;pending.remove(c);c.status=JobStatus.CANCELLED;c.result.completeExceptionally(new CancellationException("Removed from AI execution queue by admin"));monitor.notifyAll();return true;}}
        int clear(){synchronized(monitor){List<QueuedCall> copy=new ArrayList<>(pending);pending.clear();copy.forEach(c->{c.status=JobStatus.CANCELLED;c.result.completeExceptionally(new CancellationException("AI execution queue cleared by admin"));});monitor.notifyAll();return copy.size();}}
        QueueSnapshot snapshot(){List<JobSnapshot> jobs;synchronized(monitor){jobs=pending.stream().sorted(Comparator.comparingInt((QueuedCall c)->c.priority).reversed().thenComparing(c->c.createdAt)).map(QueuedCall::snapshot).toList();}QuotaState q=quota(group());return new QueueSnapshot(targetId,config.getProvider(),config.getModel(),group(),Math.max(1,config.getQueue().getConcurrency()),jobs.size(),running.get(),jobs.stream().filter(j->j.status()==JobStatus.RETRY_WAIT).count(),succeeded.get(),failed.get(),q.blockedUntil(),q.reason(),lastError,lastStatusCode,lastErrorAt,jobs);}
        String group(){return config.getQuotaGroup()==null||config.getQuotaGroup().isBlank()?targetId:config.getQuotaGroup();}
        Duration backoff(int attempt){AiProviderProperties.QueueConfig q=config.getQueue();long mult=1L<<Math.min(20,Math.max(0,attempt-1));long raw=Math.min(q.getMaxBackoff().toMillis(),q.getBaseBackoff().toMillis()*mult);double j=Math.max(0,Math.min(1,q.getJitter()));double f=1+ThreadLocalRandom.current().nextDouble(-j,j);return Duration.ofMillis(Math.max(1,Math.round(raw*f)));}
        void remember(LlmProviderException e){lastError=shortError(e);lastStatusCode=e.statusCode();lastErrorAt=Instant.now();}
        void stop(){stopping=true;synchronized(monitor){monitor.notifyAll();}workers.forEach(Thread::interrupt);}
    }
    private QuotaState quota(String g){return quotas.computeIfAbsent(g,x->new QuotaState());}
    private static final class QuotaState {volatile Instant blockedUntil;volatile String reason;synchronized void blockUntil(Instant t,String r){if(t!=null&&(blockedUntil==null||t.isAfter(blockedUntil))){blockedUntil=t;reason=r;}}Instant blockedUntil(){if(blockedUntil!=null&&!blockedUntil.isAfter(Instant.now())){blockedUntil=null;reason=null;}return blockedUntil;}String reason(){blockedUntil();return reason;}}
    private static final class QueuedCall {final String id;volatile int priority;final LlmProviderRouter.ModelTarget target;final String agentId;final CanonicalLlmRequest request;final Instant createdAt=Instant.now();final CompletableFuture<CanonicalLlmResponse> result=new CompletableFuture<>();volatile JobStatus status=JobStatus.QUEUED;volatile int attempts;volatile Instant availableAt;volatile String lastError;QueuedCall(String id,int priority,LlmProviderRouter.ModelTarget target,String agentId,CanonicalLlmRequest request){validatePriority(priority);this.id=id;this.priority=priority;this.target=target;this.agentId=agentId;this.request=request;}JobSnapshot snapshot(){return new JobSnapshot(id,request.taskType()==null?null:request.taskType().name(),agentId,priority,status,attempts,createdAt,availableAt,lastError);}}
    public enum JobStatus {QUEUED,RUNNING,RETRY_WAIT,SUCCEEDED,FAILED,CANCELLED}
    public record JobSnapshot(String id,String task,String agentId,int priority,JobStatus status,int attempts,Instant createdAt,Instant nextAttemptAt,String lastError){}
    public record QueueSnapshot(String target,String provider,String model,String quotaGroup,int concurrency,int queued,int running,long retryWaiting,long succeeded,long failed,Instant blockedUntil,String throttleReason,String lastError,Integer lastStatusCode,Instant lastErrorAt,List<JobSnapshot> jobs){}
    private static long elapsed(long n){return Math.max(0,(System.nanoTime()-n)/1_000_000L);} private static String shortError(Throwable e){String m=e.getMessage();if(m==null||m.isBlank())m=e.getClass().getSimpleName();return m.length()<=700?m:m.substring(0,700);} private static void validatePriority(int p){if(p<0||p>100)throw new IllegalArgumentException("priority must be 0..100");}
}
