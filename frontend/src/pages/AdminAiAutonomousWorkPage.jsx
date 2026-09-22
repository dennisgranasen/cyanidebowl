import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Box,
  Button,
  Heading,
  HStack,
  SimpleGrid,
  Spinner,
  Switch,
  Table,
  Tbody,
  Td,
  Text,
  Th,
  Thead,
  Tr,
  VStack,
} from '@chakra-ui/react';
import { useNavigate } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import AiReporterApi from '../AiReporterApi';
import PendingAiWork from '../components/ai-reporters/PendingAiWork';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';

const metric = (label, value) => (
  <Box borderWidth="1px" borderRadius="md" p={3}>
    <Text fontSize="xs" color="gray.500">{label}</Text>
    <Text fontSize="2xl" fontWeight="700">{value ?? 0}</Text>
  </Box>
);

const duration = (seconds) => {
  if (seconds == null) return 'unknown';
  if (seconds < 60) return `${seconds}s`;
  if (seconds < 3600) return `${Math.ceil(seconds / 60)}m`;
  if (seconds < 86400) return `${(seconds / 3600).toFixed(1)}h`;
  return `${(seconds / 86400).toFixed(1)}d`;
};

const when = (value) => value ? new Date(value).toLocaleString() : '—';

const countdown = (value, nowMs) => {
  if (!value) return null;
  const remaining = Math.max(0, Math.ceil((new Date(value).getTime() - nowMs) / 1000));
  const hours = Math.floor(remaining / 3600);
  const minutes = Math.floor((remaining % 3600) / 60);
  const seconds = remaining % 60;
  if (hours > 0) return `${hours}h ${String(minutes).padStart(2, '0')}m ${String(seconds).padStart(2, '0')}s`;
  if (minutes > 0) return `${minutes}m ${String(seconds).padStart(2, '0')}s`;
  return `${seconds}s`;
};

function AdminAiAutonomousWorkPage() {
  const {
    authenticationReady,
    userPermissions,
    getAccessTokenSilently,
    getAccessTokenWithPopup,
  } = useAuth0WithUserPermissions();
  const navigate = useNavigate();
  const [overview, setOverview] = useState(null);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(false);
  const loadInProgress = useRef(false);
  const [nowMs, setNowMs] = useState(() => Date.now());

  const load = useCallback(async () => {
    if (loadInProgress.current) return;
    loadInProgress.current = true;
    setLoading(true);
    try {
      const data = await AiReporterApi.autonomousWorkOverview(
        getAccessTokenSilently,
        getAccessTokenWithPopup
      );
      setOverview(data);
      setError(null);
    } catch (reason) {
      setError(reason);
    } finally {
      loadInProgress.current = false;
      setLoading(false);
    }
  }, [getAccessTokenSilently, getAccessTokenWithPopup]);

  useEffect(() => {
    if (!authenticationReady) return;
    if (!userPermissions.writeSiteAdmin) {
      navigate('/admin');
      return;
    }
    let active = true, timer;
    const refresh = async () => {
      await load();
      if (active) timer = setTimeout(refresh, 10000);
    };
    refresh();
    return () => { active = false; clearTimeout(timer); };
  }, [authenticationReady, userPermissions.writeSiteAdmin, navigate, load]);

  useEffect(() => {
    const timer = setInterval(() => setNowMs(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  const setEnabled = async (enabled) => {
    setSaving(true);
    setError(null);
    try {
      const data = await AiReporterApi.setAutonomousWorkEnabled(
        enabled,
        getAccessTokenSilently,
        getAccessTokenWithPopup
      );
      setOverview(data);
    } catch (reason) {
      setError(reason);
    } finally {
      setSaving(false);
    }
  };

  const queue = overview?.queue || {};
  const usage = overview?.generationUsage || {};
  const failures = queue.recentFailures || [];
  const providerUsage = usage.providers || [];
  const executionQueues = overview?.executionQueues || [];
  const mediaQueue = overview?.mediaQueue || {};
  const autonomousJobs = queue.pendingJobs || [];
  const replayQueue = overview?.replayAnalysisQueue || {};
  const modelJobs = executionQueues.reduce((sum, qs) => sum + (qs.jobs?.length || 0), 0);
  const autonomousOutstanding = (queue.queued || 0) + (queue.running || 0) + (queue.retryWaiting || 0);
  const mediaOutstanding = (mediaQueue.queued || 0) + (mediaQueue.running || 0);

  const mutateQueue = async (action) => {
    setSaving(true); setError(null);
    try { setOverview(await action()); } catch (reason) { setError(reason); } finally { setSaving(false); }
  };
  const priorityPrompt = (label, current, action, max = 100) => {
    const raw = window.prompt(`Ny prioritet för ${label} (0–${max}):`, String(current ?? 50));
    if (raw === null) return;
    const priority = Number(raw);
    if (!Number.isInteger(priority) || priority < 0 || priority > max) { window.alert(`Prioritet måste vara ett heltal 0–${max}.`); return; }
    if (!window.confirm(`Ändra prioritet för ${label} till ${priority}?`)) return;
    mutateQueue(() => action(priority));
  };
  const removePrompt = (label, action) => { if (window.confirm(`Ta bort ${label} ur kön?`)) mutateQueue(action); };
  const clearPrompt = (label, count, action) => { if (count && window.confirm(`Rensa ${label}? ${count} väntande jobb tas bort. Körande jobb lämnas orörda.`)) mutateQueue(action); };
  const movePriority = (jobs, index, direction, valueOf, max, action) => {
    const neighbor = jobs[index + direction];
    if (!neighbor) return;
    const current = valueOf(jobs[index]) ?? 0;
    const adjacent = valueOf(neighbor) ?? 0;
    const next = direction < 0
      ? Math.min(max, Math.max(current + 1, adjacent + 1))
      : Math.max(0, Math.min(current - 1, adjacent - 1));
    mutateQueue(() => action(next));
  };

  return (
    <Box p={{ base: 3, md: 6 }}>
      <Navigation currentPage="admin" parentPage="admin" />
      <HStack mt={6} justify="space-between" align="start">
        <Box>
          <Heading>Autonomous AI work</Heading>
          <Text mt={2} color="gray.400">
            Inspect the durable autonomous queue and pause or resume scheduled AI work.
            Direct and editor-triggered generation is not affected by this switch.
          </Text>
        </Box>
        <Button variant="outline" onClick={load} isLoading={loading} isDisabled={!authenticationReady || !userPermissions.writeSiteAdmin}>
          Refresh
        </Button>
      </HStack>

      {error && (
        <Text mt={4} color="red.300">{error.message || String(error)}</Text>
      )}

      {!overview && !error && <Spinner mt={8} />}

      {overview && (
        <VStack mt={6} spacing={6} align="stretch">
          <SimpleGrid columns={{ base: 2, md: 4 }} spacing={3}>
            {metric('Autonomous backlog', autonomousOutstanding)}
            {metric('Model calls active/queued', modelJobs)}
            {metric('Media jobs', mediaOutstanding)}
            {metric('Replay analyses', (replayQueue.queued || 0) + (replayQueue.runningMatchId ? 1 : 0))}
          </SimpleGrid>
          <PendingAiWork getAccessTokenSilently={getAccessTokenSilently} getAccessTokenWithPopup={getAccessTokenWithPopup} />

          <Box borderWidth="1px" borderRadius="lg" p={4}>
            <Heading size="md">Deterministic replay analysis</Heading>
            <Text mt={1} fontSize="sm" color="gray.500">Parser v{replayQueue.parserVersion ?? '—'} · no LLM/provider quota. Jobs are created by new replay data, parser-version upgrades or an explicit reanalysis request.</Text>
            <SimpleGrid mt={3} columns={{ base: 2, md: 6 }} spacing={3}>
              {metric('Queued', replayQueue.queued)}
              {metric('Runnable', replayQueue.runnable)}
              {metric('Blocked', replayQueue.blocked)}
              {metric('Running', replayQueue.runningMatchId ? 1 : 0)}
              {metric('Completed 1h', replayQueue.completedLastHour)}
              {metric('Completed 24h', replayQueue.completedLast24Hours)}
            </SimpleGrid>
            <Text mt={2} fontSize="sm" color="gray.500">Running: {replayQueue.runningMatchId || 'none'} · next worker check: {when(replayQueue.nextWorkerCheckAt)} · estimated drain: {duration(replayQueue.estimatedClearSeconds)}</Text>
            {replayQueue.jobs?.length > 0 && <Box mt={3} overflowX="auto" maxH="360px" overflowY="auto"><Table size="sm"><Thead><Tr><Th>Match</Th><Th>Reason</Th><Th>Status</Th><Th>Parser</Th><Th>Requested</Th><Th>Error</Th></Tr></Thead><Tbody>{replayQueue.jobs.map((job) => <Tr key={job.matchId}><Td fontSize="xs">{job.matchId}</Td><Td>{job.reason}</Td><Td>{job.status}</Td><Td>{job.parserVersion ?? '—'} → {job.targetParserVersion}</Td><Td fontSize="xs">{job.requestedAt ? `${when(job.requestedAt)}${job.requestedBy ? ` · ${job.requestedBy}` : ''}` : '—'}</Td><Td fontSize="xs">{job.originalAvailable ? (job.error || '—') : 'Original replay file missing'}</Td></Tr>)}</Tbody></Table></Box>}
            {replayQueue.recentFailures?.length > 0 && <Box mt={3}><Text fontSize="sm" fontWeight="700">Recent parser failures</Text>{replayQueue.recentFailures.slice(0, 5).map((failure) => <Text key={`${failure.matchId}-${failure.analyzedAt}`} fontSize="xs" color="red.300">{failure.matchId} · attempted parser {failure.analysisAttemptVersion ?? '—'} · {when(failure.analyzedAt)} · {failure.error}</Text>)}</Box>}
          </Box>

          <Box borderWidth="1px" borderRadius="lg" p={4}>
            <HStack justify="space-between">
              <Box>
                <Heading size="sm">Autonomous execution</Heading>
                <Text mt={1} fontSize="sm" color="gray.500">
                  When disabled, queued work remains durable but the scheduler does not claim it.
                </Text>
              </Box>
              <Switch
                size="lg"
                isChecked={Boolean(overview.autonomousExecutionEnabled)}
                isDisabled={saving}
                onChange={(event) => setEnabled(event.target.checked)}
              />
            </HStack>
          </Box>

          <Box>
            <Heading size="md" mb={3}>Queue</Heading>
            <SimpleGrid columns={{ base: 2, md: 5 }} spacing={3}>
              {metric('Queued', queue.queued)}
              {metric('Running', queue.running)}
              {metric('Retry waiting', queue.retryWaiting)}
              {metric('Succeeded', queue.succeeded)}
              {metric('Failed', queue.failed)}
            </SimpleGrid>
            <Text mt={2} fontSize="sm" color="gray.500">
              Oldest pending: {queue.oldestPendingCreatedAt
                ? new Date(queue.oldestPendingCreatedAt).toLocaleString()
                : 'none'}
              {' · '}completed 1h/24h: {queue.completedLastHour ?? 0}/{queue.completedLast24Hours ?? 0}
              {' · '}next eligible: {when(queue.nextEligibleAt)}
              {' · '}estimated drain: {duration(queue.estimatedClearSeconds)}
            </Text>
          </Box>

          <Box>
            <HStack justify="space-between" mb={2}><Heading size="md">Autonomous queue jobs</Heading><Button size="xs" colorScheme="red" variant="outline" isDisabled={!autonomousJobs.length || saving} onClick={() => clearPrompt('autonomous queue', autonomousJobs.length, () => AiReporterApi.clearAiQueue('queue/jobs', getAccessTokenSilently, getAccessTokenWithPopup))}>Clear queue</Button></HStack>
            {autonomousJobs.length === 0 ? <Text color="gray.500">No pending jobs.</Text> : <Box borderWidth="1px" borderRadius="lg" overflowX="auto"><Table size="sm"><Thead><Tr><Th>Job</Th><Th>Kind</Th><Th>Agent</Th><Th isNumeric>Priority</Th><Th>Status</Th><Th>Actions</Th></Tr></Thead><Tbody>{autonomousJobs.map((job, index) => <Tr key={job.candidateKey}><Td fontSize="xs">{job.candidateKey}</Td><Td>{job.kind || job.handlerKey}</Td><Td>{job.actorId || '—'}</Td><Td isNumeric>{job.priorityRank}</Td><Td>{job.status}</Td><Td><HStack><Button size="xs" isDisabled={index===0||saving} onClick={() => movePriority(autonomousJobs,index,-1,j=>j.priorityRank,1000,(priority)=>AiReporterApi.reprioritizeAiQueueJob(`queue/jobs/${encodeURIComponent(job.candidateKey)}`,priority,getAccessTokenSilently,getAccessTokenWithPopup))}>↑</Button><Button size="xs" isDisabled={index===autonomousJobs.length-1||saving} onClick={() => movePriority(autonomousJobs,index,1,j=>j.priorityRank,1000,(priority)=>AiReporterApi.reprioritizeAiQueueJob(`queue/jobs/${encodeURIComponent(job.candidateKey)}`,priority,getAccessTokenSilently,getAccessTokenWithPopup))}>↓</Button><Button size="xs" onClick={() => priorityPrompt(job.candidateKey, job.priorityRank, (priority) => AiReporterApi.reprioritizeAiQueueJob(`queue/jobs/${encodeURIComponent(job.candidateKey)}`, priority, getAccessTokenSilently, getAccessTokenWithPopup), 1000)}>Priority</Button><Button size="xs" colorScheme="red" variant="outline" onClick={() => removePrompt(job.candidateKey, () => AiReporterApi.deleteAiQueueJob(`queue/jobs/${encodeURIComponent(job.candidateKey)}`, getAccessTokenSilently, getAccessTokenWithPopup))}>Remove</Button></HStack></Td></Tr>)}</Tbody></Table></Box>}
          </Box>

          <Box>
            <Heading size="md" mb={2}>AI quota queues</Heading>
            <VStack align="stretch" spacing={4}>{executionQueues.map((qs) => <Box key={qs.target} borderWidth="1px" borderRadius="lg" p={3}><HStack justify="space-between" align="start"><Box><Text fontWeight="700">{qs.quotaGroup || qs.target} · {qs.jobs?.length || 0} jobs</Text><Text fontSize="xs" color="gray.500">{qs.provider || "—"} · {qs.model || "—"}{qs.targets?.length ? ` · targets ${qs.targets.join(", ")}` : ""}</Text><Text fontSize="xs" color="gray.500">queued {qs.queued ?? 0} · running {qs.running ?? 0}/{qs.concurrency ?? 0} · retry {qs.retryWaiting ?? 0} · completed 1h/24h {qs.completedLastHour ?? 0}/{qs.completedLast24Hours ?? 0} · avg {qs.averageDurationMs == null ? '—' : `${Math.round(qs.averageDurationMs/1000)}s`} · ETA {duration(qs.estimatedClearSeconds)}</Text><Text fontSize="xs" color="gray.500">Resume/next eligible: {qs.jobs?.length ? (qs.resumeAt ? when(qs.resumeAt) : 'now') : '—'} · throughput history since {when(qs.historyStartedAt)}</Text>{qs.blockedUntil && <Text fontSize="sm" fontWeight="700" color="yellow.300">Throttled · retry in {countdown(qs.blockedUntil, nowMs)} <Text as="span" fontSize="xs" fontWeight="400">({when(qs.blockedUntil)}){qs.throttleReason ? ` · ${qs.throttleReason}` : ''}</Text></Text>}{qs.lastError && <Text fontSize="xs" color="red.300">Last error{qs.lastStatusCode ? ` HTTP ${qs.lastStatusCode}` : ''}: {qs.lastError}</Text>}</Box><Button size="xs" colorScheme="red" variant="outline" isDisabled={!qs.jobs?.length || saving} onClick={() => clearPrompt(`${qs.target} queue`, qs.jobs?.length || 0, () => AiReporterApi.clearAiQueue(`execution-queues/${encodeURIComponent(qs.target)}/jobs`, getAccessTokenSilently, getAccessTokenWithPopup))}>Clear queue</Button></HStack>{qs.jobs?.length > 0 && <Box mt={3} overflowX="auto"><Table size="sm"><Thead><Tr><Th>Task</Th><Th>Target</Th><Th>Model</Th><Th>Agent</Th><Th isNumeric>Priority</Th><Th>Status</Th><Th>Attempts</Th><Th>Next attempt</Th><Th>Actions</Th></Tr></Thead><Tbody>{qs.jobs.map((job, index) => <Tr key={job.id}><Td>{job.task || '—'}</Td><Td>{job.target || '—'}</Td><Td fontSize="xs">{job.model || '—'}</Td><Td>{job.agentId || '—'}</Td><Td isNumeric>{job.priority}</Td><Td>{job.status}</Td><Td>{job.attempts}</Td><Td>{job.nextAttemptAt ? new Date(job.nextAttemptAt).toLocaleString() : '—'}</Td><Td><HStack><Button size="xs" isDisabled={index===0||job.status==='RUNNING'||saving} onClick={() => movePriority(qs.jobs,index,-1,j=>j.priority,100,(priority)=>AiReporterApi.reprioritizeAiQueueJob(`execution-queues/${encodeURIComponent(qs.target)}/jobs/${encodeURIComponent(job.id)}`,priority,getAccessTokenSilently,getAccessTokenWithPopup))}>↑</Button><Button size="xs" isDisabled={index===qs.jobs.length-1||job.status==='RUNNING'||saving} onClick={() => movePriority(qs.jobs,index,1,j=>j.priority,100,(priority)=>AiReporterApi.reprioritizeAiQueueJob(`execution-queues/${encodeURIComponent(qs.target)}/jobs/${encodeURIComponent(job.id)}`,priority,getAccessTokenSilently,getAccessTokenWithPopup))}>↓</Button><Button size="xs" isDisabled={job.status==='RUNNING'} onClick={() => priorityPrompt(`${job.task}/${job.agentId}`, job.priority, (priority) => AiReporterApi.reprioritizeAiQueueJob(`execution-queues/${encodeURIComponent(qs.target)}/jobs/${encodeURIComponent(job.id)}`, priority, getAccessTokenSilently, getAccessTokenWithPopup))}>Priority</Button><Button size="xs" colorScheme="red" variant="outline" isDisabled={job.status==='RUNNING'} onClick={() => removePrompt(`${job.task}/${job.agentId}`, () => AiReporterApi.deleteAiQueueJob(`execution-queues/${encodeURIComponent(qs.target)}/jobs/${encodeURIComponent(job.id)}`, getAccessTokenSilently, getAccessTokenWithPopup))}>Remove</Button></HStack></Td></Tr>)}</Tbody></Table></Box>}</Box>)}</VStack>
          </Box>

          <Box>
            <HStack justify="space-between" mb={2}><Heading size="md">Community media queue</Heading><Button size="xs" colorScheme="red" variant="outline" isDisabled={!mediaQueue.jobs?.length || saving} onClick={() => clearPrompt('media queue', mediaQueue.jobs?.length || 0, () => AiReporterApi.clearAiQueue('media-queue/jobs', getAccessTokenSilently, getAccessTokenWithPopup))}>Clear queue</Button></HStack>
            <Text fontSize="sm" color="gray.500">queued {mediaQueue.queued ?? 0} · running {mediaQueue.running ?? 0} · succeeded {mediaQueue.succeeded ?? 0} · failed {mediaQueue.failed ?? 0} · completed 1h/24h {mediaQueue.completedLastHour ?? 0}/{mediaQueue.completedLast24Hours ?? 0} · resume {mediaQueue.providerConfigured === false ? 'provider not configured' : (mediaQueue.resumeAt ? when(mediaQueue.resumeAt) : 'now')} · ETA {duration(mediaQueue.estimatedClearSeconds)}</Text>
            {mediaQueue.blockedUntil && (
              <Box mt={2} borderWidth="1px" borderRadius="md" p={3}>
                <Text fontSize="xs" color="gray.500">Provider cooldown</Text>
                <Text fontSize="2xl" fontWeight="700" color="yellow.300">
                  {countdown(mediaQueue.blockedUntil, nowMs)}
                </Text>
                <Text fontSize="xs" color="gray.500">
                  Next provider attempt {when(mediaQueue.blockedUntil)}
                  {mediaQueue.blockReason ? ` · ${mediaQueue.blockReason}` : ''}
                </Text>
              </Box>
            )}
            {mediaQueue.jobs?.length > 0 && <Box mt={2} borderWidth="1px" borderRadius="lg" overflowX="auto"><Table size="sm"><Thead><Tr><Th>Character</Th><Th>Type</Th><Th>Status</Th><Th>Registered</Th><Th>Provider</Th><Th isNumeric>Priority</Th><Th>Attempts</Th><Th>Next attempt</Th><Th>Error</Th><Th>Actions</Th></Tr></Thead><Tbody>{mediaQueue.jobs.map((job, index) => <Tr key={job.id}><Td><Text>{job.fanName || 'Deleted / unknown character'}</Text><Text fontSize="xs" color="gray.500">{job.fanProfileId || job.id}</Text></Td><Td>{job.target}</Td><Td><Text color={job.status === 'RUNNING' ? 'blue.300' : 'yellow.300'}>{job.status}</Text></Td><Td>{job.createdAt ? new Date(job.createdAt).toLocaleString() : '—'}</Td><Td fontSize="xs">{job.provider || job.requestedProvider || 'Cloudflare (automatic)'}{job.model ? ` · ${job.model}` : ''}</Td><Td isNumeric>{job.priority ?? 0}</Td><Td>{job.attempts ?? 0}</Td><Td>{job.nextAttemptAt ? new Date(job.nextAttemptAt).toLocaleString() : '—'}</Td><Td fontSize="xs">{job.error || '—'}</Td><Td><HStack><Button size="xs" isDisabled={index===0||saving} onClick={() => movePriority(mediaQueue.jobs,index,-1,j=>j.priority??0,100,(priority)=>AiReporterApi.reprioritizeAiQueueJob(`media-queue/jobs/${encodeURIComponent(job.id)}`,priority,getAccessTokenSilently,getAccessTokenWithPopup))}>↑</Button><Button size="xs" isDisabled={index===mediaQueue.jobs.length-1||saving} onClick={() => movePriority(mediaQueue.jobs,index,1,j=>j.priority??0,100,(priority)=>AiReporterApi.reprioritizeAiQueueJob(`media-queue/jobs/${encodeURIComponent(job.id)}`,priority,getAccessTokenSilently,getAccessTokenWithPopup))}>↓</Button><Button size="xs" onClick={() => priorityPrompt(job.id, job.priority ?? 0, (priority) => AiReporterApi.reprioritizeAiQueueJob(`media-queue/jobs/${encodeURIComponent(job.id)}`, priority, getAccessTokenSilently, getAccessTokenWithPopup))}>Priority</Button><Button size="xs" colorScheme="red" variant="outline" onClick={() => removePrompt(job.id, () => AiReporterApi.deleteAiQueueJob(`media-queue/jobs/${encodeURIComponent(job.id)}`, getAccessTokenSilently, getAccessTokenWithPopup))}>Remove</Button></HStack></Td></Tr>)}</Tbody></Table></Box>}
            {mediaQueue.recentFailures?.length > 0 && <Box mt={4}><Heading size="sm" mb={2} color="red.300">Recent failed community-media jobs</Heading><Box borderWidth="1px" borderColor="red.700" borderRadius="lg" overflowX="auto"><Table size="sm"><Thead><Tr><Th>Character</Th><Th>Type</Th><Th>Registered</Th><Th>Failed</Th><Th>Attempts</Th><Th>Error</Th></Tr></Thead><Tbody>{mediaQueue.recentFailures.map(job => <Tr key={job.id}><Td><Text>{job.fanName || 'Deleted / unknown character'}</Text><Text fontSize="xs" color="gray.500">{job.fanProfileId || job.id}</Text></Td><Td>{job.target}</Td><Td>{job.createdAt ? new Date(job.createdAt).toLocaleString() : '—'}</Td><Td>{job.completedAt ? new Date(job.completedAt).toLocaleString() : '—'}</Td><Td>{job.attempts ?? 0}</Td><Td fontSize="xs" color="red.300">{job.error || 'No error message recorded'}</Td></Tr>)}</Tbody></Table></Box></Box>}
          </Box>

          <Box>
            <Heading size="md" mb={1}>Global AI safety budget today (UTC)</Heading>
            <Text mb={3} fontSize="sm" color="gray.500">
              Site-wide Cyanidebowl counters used by the generation admission gate.
              These are aggregate safety/budget counters across all providers, not
              Gemini, Groq, OpenRouter or other provider account quotas.
            </Text>
            <SimpleGrid columns={{ base: 2, md: 4 }} spacing={3}>
              {metric('Successful generations', usage.successfulGenerations)}
              {metric('Input tokens · all providers', usage.inputTokens)}
              {metric('Output tokens · all providers', usage.outputTokens)}
              {metric('In flight', usage.inFlight)}
            </SimpleGrid>
            {(usage.unknownInputTokenGenerations > 0 || usage.unknownOutputTokenGenerations > 0) && (
              <Text mt={2} fontSize="sm" color="yellow.300">
                Some successful generations are missing token telemetry:
                {' '}input {usage.unknownInputTokenGenerations || 0},
                {' '}output {usage.unknownOutputTokenGenerations || 0}.
              </Text>
            )}
          </Box>

          <Box>
            <Heading size="md" mb={1}>Usage by provider and model today (UTC)</Heading>
            <Text mb={3} fontSize="sm" color="gray.500">
              Operational telemetry from actual provider attempts. Token counts are
              measured on successful generations; Requests includes failed attempts.
              429s show observed rate-limit responses and are not a provider quota counter.
            </Text>
            {providerUsage.length === 0 ? (
              <Text color="gray.500">No provider usage recorded today.</Text>
            ) : (
              <Box borderWidth="1px" borderRadius="lg" overflowX="auto">
                <Table size="sm">
                  <Thead>
                    <Tr>
                      <Th>Provider</Th>
                      <Th>Model</Th>
                      <Th isNumeric>Requests</Th>
                      <Th isNumeric>Success</Th>
                      <Th isNumeric>Failed</Th>
                      <Th isNumeric>Input tokens</Th>
                      <Th isNumeric>Output tokens</Th>
                      <Th isNumeric>429s</Th>
                      <Th>Last 429</Th>
                    </Tr>
                  </Thead>
                  <Tbody>
                    {providerUsage.map((provider) => (
                      <Tr key={`${provider.providerId}:${provider.model}`}>
                        <Td fontWeight="600">{provider.providerId}</Td>
                        <Td>{provider.model}</Td>
                        <Td isNumeric>{provider.requests ?? 0}</Td>
                        <Td isNumeric>{provider.successfulGenerations ?? 0}</Td>
                        <Td isNumeric>{provider.failedGenerations ?? 0}</Td>
                        <Td isNumeric>
                          {provider.inputTokens ?? 0}
                          {provider.unknownInputTokenGenerations > 0
                            ? ` (+${provider.unknownInputTokenGenerations} unknown)` : ''}
                        </Td>
                        <Td isNumeric>
                          {provider.outputTokens ?? 0}
                          {provider.unknownOutputTokenGenerations > 0
                            ? ` (+${provider.unknownOutputTokenGenerations} unknown)` : ''}
                        </Td>
                        <Td isNumeric>{provider.rateLimitFailures ?? 0}</Td>
                        <Td whiteSpace="nowrap">
                          {provider.lastRateLimitAt
                            ? new Date(provider.lastRateLimitAt).toLocaleString()
                            : '—'}
                        </Td>
                      </Tr>
                    ))}
                  </Tbody>
                </Table>
              </Box>
            )}
          </Box>

          <Box>
            <Heading size="md" mb={3}>Recent terminal failures</Heading>
            {failures.length === 0 ? (
              <Text color="gray.500">No terminal failures.</Text>
            ) : (
              <Box borderWidth="1px" borderRadius="lg" overflowX="auto">
                <Table size="sm">
                  <Thead>
                    <Tr>
                      <Th>Candidate</Th>
                      <Th>Kind</Th>
                      <Th>Attempts</Th>
                      <Th>Error</Th>
                      <Th>Updated</Th>
                    </Tr>
                  </Thead>
                  <Tbody>
                    {failures.map((failure) => (
                      <Tr key={failure.candidateKey}>
                        <Td>{failure.candidateKey}</Td>
                        <Td>{failure.kind || failure.handlerKey || '—'}</Td>
                        <Td>{failure.attempts ?? '—'}/{failure.maxAttempts ?? '—'}</Td>
                        <Td>
                          <Text fontSize="xs">
                            {failure.lastErrorClass || 'Failure'}
                            {failure.lastErrorMessage ? `: ${failure.lastErrorMessage}` : ''}
                          </Text>
                        </Td>
                        <Td whiteSpace="nowrap">
                          {failure.updatedAt
                            ? new Date(failure.updatedAt).toLocaleString()
                            : '—'}
                        </Td>
                      </Tr>
                    ))}
                  </Tbody>
                </Table>
              </Box>
            )}
          </Box>
        </VStack>
      )}
    </Box>
  );
}

export default AdminAiAutonomousWorkPage;
