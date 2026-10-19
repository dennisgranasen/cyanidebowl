import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Alert,
  AlertIcon,
  Box,
  Button,
  ButtonGroup,
  Heading,
  HStack,
  Input,
  Select,
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
import AiJobInputDialog from '../components/ai-reporters/AiJobInputDialog';
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

const PAGE_SIZE = 10;

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
  const [queuePages, setQueuePages] = useState({});
  const [viewMode, setViewMode] = useState('service');
  const [inputDialogOpen, setInputDialogOpen] = useState(false);
  const [inputDetails, setInputDetails] = useState(null);
  const [inputJobRef, setInputJobRef] = useState(null);
  const [inputLoading, setInputLoading] = useState(false);
  const [inputError, setInputError] = useState('');
  const [copilotQuota, setCopilotQuota] = useState(null);
  const [communityImageProviders, setCommunityImageProviders] = useState([]);
  const [selectedMediaProviders, setSelectedMediaProviders] = useState({});
  const [copilotQuotaError, setCopilotQuotaError] = useState(null);
  const [quotaThresholdDraft, setQuotaThresholdDraft] = useState('20');
  const [quotaThresholdDirty, setQuotaThresholdDirty] = useState(false);
  const [quotaSaving, setQuotaSaving] = useState(false);
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

  const refreshCopilotQuota = useCallback(async () => {
    try {
      const data = await AiReporterApi.copilotQuota(
        getAccessTokenSilently,
        getAccessTokenWithPopup
      );
      setCopilotQuota(data);
      setCopilotQuotaError(null);
      if (!quotaThresholdDirty) setQuotaThresholdDraft(String(data.minimumRemainingPercent));
    } catch (reason) {
      setCopilotQuota(null);
      setCopilotQuotaError(reason?.response?.data?.detail
        || reason?.response?.data?.message
        || reason?.message
        || 'Could not load Copilot quota.');
    }
  }, [getAccessTokenSilently, getAccessTokenWithPopup, quotaThresholdDirty]);

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
    if (!authenticationReady || !userPermissions.writeSiteAdmin) return undefined;
    refreshCopilotQuota();
    const timer = setInterval(refreshCopilotQuota, 60000);
    return () => clearInterval(timer);
  }, [authenticationReady, userPermissions.writeSiteAdmin, refreshCopilotQuota]);

  useEffect(() => {
    if (!authenticationReady || !userPermissions.writeSiteAdmin) return undefined;
    let active = true;
    AiReporterApi.communityImageProviders(getAccessTokenSilently, getAccessTokenWithPopup)
      .then(providers => { if (active) setCommunityImageProviders(providers || []); })
      .catch(reason => {
        if (active) setError(reason?.response?.data?.message || reason?.message || 'Could not load image providers.');
      });
    return () => { active = false; };
  }, [authenticationReady, userPermissions.writeSiteAdmin, getAccessTokenSilently, getAccessTokenWithPopup]);

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

  const saveCopilotQuotaThreshold = async () => {
    const threshold = Number(quotaThresholdDraft);
    if (!Number.isInteger(threshold) || threshold < 0 || threshold > 100) {
      setCopilotQuotaError('Copilot quota threshold must be a whole percentage from 0 to 100.');
      return;
    }
    setQuotaSaving(true);
    setCopilotQuotaError(null);
    try {
      const saved = await AiReporterApi.setCopilotQuotaThreshold(
        threshold,
        getAccessTokenSilently,
        getAccessTokenWithPopup
      );
      setCopilotQuota(old => old ? { ...old, minimumRemainingPercent: saved.minimumRemainingPercent } : old);
      setQuotaThresholdDraft(String(saved.minimumRemainingPercent));
      setQuotaThresholdDirty(false);
    } catch (reason) {
      setCopilotQuotaError(reason?.response?.data?.detail
        || reason?.response?.data?.message
        || reason?.message
        || 'Could not save Copilot quota threshold.');
    } finally {
      setQuotaSaving(false);
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
  const modelJobs = executionQueues.reduce(
    (sum, qs) => sum + (qs.queued || 0) + (qs.running || 0) + (qs.retryWaiting || 0),
    0
  );
  const autonomousOutstanding = (queue.queued || 0) + (queue.running || 0) + (queue.retryWaiting || 0);
  const mediaOutstanding = (mediaQueue.queued || 0) + (mediaQueue.running || 0);
  const articleGenerationQueue = overview?.articleGenerationQueue || {};
  const articleGenerationJobs = articleGenerationQueue.jobs || [];
  const editorialImageJobs = overview?.editorialImageQueue || [];
  const providerSections = [...new Set([
    ...executionQueues.map(qs => qs.provider || 'Unknown provider'),
    ...providerUsage.map(provider => provider.providerId || 'Unknown provider'),
  ])].sort().map(provider => ({
    provider,
    queues: executionQueues.filter(qs => (qs.provider || 'Unknown provider') === provider),
    usage: providerUsage.filter(item => (item.providerId || 'Unknown provider') === provider),
  }));

  const paginate = (key, jobs) => {
    const pageCount = Math.max(1, Math.ceil(jobs.length / PAGE_SIZE));
    const page = Math.min(queuePages[key] || 1, pageCount);
    const start = (page - 1) * PAGE_SIZE;
    return { page, pageCount, start, jobs: jobs.slice(start, start + PAGE_SIZE) };
  };
  const pager = (key, jobs, page) => (
    <HStack justify="space-between" py={2} fontSize="sm">
      <Text color="gray.500">
        {jobs.length ? `${(page - 1) * PAGE_SIZE + 1}–${Math.min(page * PAGE_SIZE, jobs.length)} of ${jobs.length}` : '0 jobs'}
      </Text>
      <HStack>
        <Button size="xs" isDisabled={page <= 1} onClick={() => setQueuePages(old => ({ ...old, [key]: page - 1 }))}>Previous</Button>
        <Text>{page} / {Math.max(1, Math.ceil(jobs.length / PAGE_SIZE))}</Text>
        <Button size="xs" isDisabled={page >= Math.ceil(jobs.length / PAGE_SIZE)} onClick={() => setQueuePages(old => ({ ...old, [key]: page + 1 }))}>Next</Button>
      </HStack>
    </HStack>
  );

  const mutateQueue = async (action) => {
    setSaving(true); setError(null);
    try { setOverview(await action()); } catch (reason) { setError(reason); } finally { setSaving(false); }
  };
  const moveExecutionJobToCopilot = (queueTarget, jobId) => {
    const move = confirmCopilotQuota => AiReporterApi.moveAiExecutionJob(
      queueTarget,
      jobId,
      'copilot-gpt-6-luna',
      confirmCopilotQuota,
      getAccessTokenSilently,
      getAccessTokenWithPopup
    );
    mutateQueue(async () => {
      try {
        const data = await move(false);
        refreshCopilotQuota();
        return data;
      } catch (reason) {
        const warning = reason?.response?.data;
        if (reason?.response?.status !== 409
            || warning?.code !== 'COPILOT_QUOTA_CONFIRMATION_REQUIRED') throw reason;
        if (!window.confirm(warning.message)) return overview;
        const data = await move(true);
        refreshCopilotQuota();
        return data;
      }
    });
  };
  const inspectJobInput = async (type, id, queue) => {
    setInputDialogOpen(true);
    setInputJobRef({ type, id, queue });
    setInputLoading(true);
    setInputDetails(null);
    setInputError('');
    try {
      setInputDetails(await AiReporterApi.aiJobInput(
        type, id, queue, getAccessTokenSilently, getAccessTokenWithPopup
      ));
    } catch (reason) {
      setInputError(reason?.response?.data?.message || reason?.message || 'Could not load saved job input.');
    } finally {
      setInputLoading(false);
    }
  };
  const submitExternalResult = async content => {
    if (inputJobRef?.type !== 'provider-call' || !inputJobRef.queue) {
      throw new Error('External results can only be submitted for provider calls.');
    }
    await AiReporterApi.submitExternalAiResult(
      inputJobRef.queue, inputJobRef.id, content,
      getAccessTokenSilently, getAccessTokenWithPopup);
    setInputDialogOpen(false);
    await load();
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
  const autonomousPage = paginate('autonomous', autonomousJobs);
  const articleGenerationPage = paginate('article-generation', articleGenerationJobs);
  const editorialImagePage = paginate('editorial-images', editorialImageJobs);
  const mediaPage = paginate('community-media', mediaQueue.jobs || []);

  return (
    <Box p={{ base: 3, md: 6 }}>
      <Navigation currentPage="admin" parentPage="admin" />
      <HStack mt={6} justify="space-between" align="start">
        <Box>
          <Heading>AI work queues</Heading>
          <Text mt={2} color="gray.400">
            Inspect AI jobs either by the application service that requested them or by the AI provider executing them.
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
          <Box>
            <ButtonGroup isAttached variant="outline" mb={2}>
              <Button aria-pressed={viewMode === 'service'} colorScheme={viewMode === 'service' ? 'blue' : undefined}
                onClick={() => setViewMode('service')}>By application service</Button>
              <Button aria-pressed={viewMode === 'provider'} colorScheme={viewMode === 'provider' ? 'blue' : undefined}
                onClick={() => setViewMode('provider')}>By AI provider</Button>
            </ButtonGroup>
            <Text fontSize="sm" color="gray.500">
              {viewMode === 'service'
                ? 'Service view shows requested work (articles, images, profiles, and other workflows). A single request may create more than one provider call.'
                : 'Provider view shows individual model calls, retries, recorded token usage, and provider queue throttling. Vendor account quotas are not directly available here.'}
            </Text>
          </Box>
          {viewMode === 'service' && <>
            <PendingAiWork
              getAccessTokenSilently={getAccessTokenSilently}
              getAccessTokenWithPopup={getAccessTokenWithPopup}
              onInspectJob={(type, id) => inspectJobInput({
                fans: 'fan-profile',
                rating: 'player-rating',
                'fan-rating': 'fan-player-rating',
              }[type] || type, id)}
            />

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
            <HStack justify="space-between" mb={2}>
              <Heading size="md">Autonomous queue jobs</Heading>
              <Button size="xs" colorScheme="red" variant="outline" isDisabled={!autonomousJobs.length || saving}
                onClick={() => clearPrompt('autonomous queue', autonomousJobs.length,
                  () => AiReporterApi.clearAiQueue('queue/jobs', getAccessTokenSilently, getAccessTokenWithPopup))}>
                Clear queue
              </Button>
            </HStack>
            {pager('autonomous', autonomousJobs, autonomousPage.page)}
            <Box borderWidth="1px" borderRadius="lg" overflowX="auto" maxH="440px" overflowY="auto">
              <Table size="sm">
                <Thead><Tr><Th>Job</Th><Th>Kind</Th><Th>Agent</Th><Th isNumeric>Priority</Th><Th>Status</Th><Th>Actions</Th></Tr></Thead>
                <Tbody>{autonomousPage.jobs.map((job, index) => {
                  const position = autonomousPage.start + index;
                  return <Tr key={job.candidateKey}>
                    <Td fontSize="xs">{job.candidateKey}</Td><Td>{job.kind || job.handlerKey}</Td>
                    <Td>{job.actorId || '—'}</Td><Td isNumeric>{job.priorityRank}</Td><Td>{job.status}</Td>
                    <Td><HStack>
                      <Button size="xs" onClick={() => inspectJobInput('autonomous', job.candidateKey)}>Input</Button>
                      <Button size="xs" isDisabled={position === 0 || saving}
                        onClick={() => movePriority(autonomousJobs, position, -1, j => j.priorityRank, 1000,
                          priority => AiReporterApi.reprioritizeAiQueueJob(`queue/jobs/${encodeURIComponent(job.candidateKey)}`, priority, getAccessTokenSilently, getAccessTokenWithPopup))}>↑</Button>
                      <Button size="xs" isDisabled={position === autonomousJobs.length - 1 || saving}
                        onClick={() => movePriority(autonomousJobs, position, 1, j => j.priorityRank, 1000,
                          priority => AiReporterApi.reprioritizeAiQueueJob(`queue/jobs/${encodeURIComponent(job.candidateKey)}`, priority, getAccessTokenSilently, getAccessTokenWithPopup))}>↓</Button>
                      <Button size="xs" onClick={() => priorityPrompt(job.candidateKey, job.priorityRank,
                        priority => AiReporterApi.reprioritizeAiQueueJob(`queue/jobs/${encodeURIComponent(job.candidateKey)}`, priority, getAccessTokenSilently, getAccessTokenWithPopup), 1000)}>Priority</Button>
                      <Button size="xs" colorScheme="red" variant="outline" onClick={() => removePrompt(job.candidateKey,
                        () => AiReporterApi.deleteAiQueueJob(`queue/jobs/${encodeURIComponent(job.candidateKey)}`, getAccessTokenSilently, getAccessTokenWithPopup))}>Remove</Button>
                    </HStack></Td>
                  </Tr>;
                })}</Tbody>
              </Table>
            </Box>
          </Box>

          <Box borderWidth="1px" borderRadius="lg" p={3}>
            <HStack justify="space-between">
              <Box>
                <Heading size="md">Editorial article generation queue</Heading>
                <Text fontSize="xs" color="gray.500">
                  queued {articleGenerationQueue.queued ?? 0} · running {articleGenerationQueue.running ?? 0}
                  {' · '}completed {articleGenerationQueue.completed ?? 0} · failed {articleGenerationQueue.failed ?? 0}
                </Text>
              </Box>
            </HStack>
            {pager('article-generation', articleGenerationJobs, articleGenerationPage.page)}
            <Box borderWidth="1px" borderRadius="lg" overflowX="auto" maxH="440px" overflowY="auto">
              <Table size="sm">
                <Thead><Tr><Th>Request</Th><Th>Model override</Th><Th isNumeric>Priority</Th><Th>Status</Th><Th>Created</Th><Th>Expected start</Th><Th>Error</Th><Th>Actions</Th></Tr></Thead>
                <Tbody>{articleGenerationPage.jobs.map(job => (
                  <Tr key={job.id}>
                    <Td fontSize="xs">{job.id}</Td>
                    <Td fontSize="xs">{job.modelOverrideTargetId || 'Standard pipeline'}</Td>
                    <Td isNumeric>{job.priority}</Td>
                    <Td color={job.status === 'FAILED' ? 'red.300' : undefined}>{job.status}</Td>
                    <Td whiteSpace="nowrap">{when(job.createdAt)}</Td>
                    <Td whiteSpace="nowrap">{job.status === 'RUNNING' ? `running since ${when(job.startedAt)}` : when(job.estimatedStartAt)}</Td>
                    <Td fontSize="xs" color="red.300">{job.error || '—'}</Td>
                    <Td><HStack>
                      <Button size="xs" onClick={() => inspectJobInput('article-generation', job.id)}>Input</Button>
                      <Button size="xs" isDisabled={job.status !== 'QUEUED' || saving}
                        onClick={() => priorityPrompt(job.id, job.priority,
                          priority => AiReporterApi.reprioritizeAiQueueJob(`article-generation/jobs/${encodeURIComponent(job.id)}`, priority, getAccessTokenSilently, getAccessTokenWithPopup))}>Priority</Button>
                      <Button size="xs" colorScheme="red" variant="outline" isDisabled={job.status === 'RUNNING' || saving}
                        onClick={() => removePrompt(job.id,
                          () => AiReporterApi.deleteAiQueueJob(`article-generation/jobs/${encodeURIComponent(job.id)}`, getAccessTokenSilently, getAccessTokenWithPopup))}>Remove</Button>
                    </HStack></Td>
                  </Tr>
                ))}</Tbody>
              </Table>
            </Box>
          </Box>

          <Box borderWidth="1px" borderRadius="lg" p={3}>
            <HStack justify="space-between">
              <Box>
                <Heading size="md">Editorial image generation queue</Heading>
                <Text fontSize="xs" color="gray.500">
                  queued {editorialImageJobs.filter(job => job.status === 'DEVELOPING').length}
                  {' · '}running {editorialImageJobs.filter(job => job.status === 'RUNNING').length}
                  {' · '}failed {editorialImageJobs.filter(job => job.status === 'FAILED').length}
                </Text>
              </Box>
            </HStack>
            {pager('editorial-images', editorialImageJobs, editorialImagePage.page)}
            <Box borderWidth="1px" borderRadius="lg" overflowX="auto" maxH="440px" overflowY="auto">
              <Table size="sm">
                <Thead><Tr><Th>Request</Th><Th>Photographer</Th><Th isNumeric>Priority</Th><Th>Status</Th><Th>Created</Th><Th>Expected start</Th><Th>Attempts</Th><Th>Error</Th><Th>Actions</Th></Tr></Thead>
                <Tbody>{editorialImagePage.jobs.map((job, index) => {
                  const position = editorialImagePage.start + index;
                  const queuedBefore = editorialImageJobs.slice(0, position)
                    .filter(previous => previous.status === 'DEVELOPING').length;
                  const expectedStart = job.status === 'DEVELOPING'
                    ? new Date(nowMs + (editorialImageJobs.some(previous => previous.status === 'RUNNING') ? 60 : 0) * 1000
                      + queuedBefore * 60000).toLocaleString()
                    : job.status === 'RUNNING' ? `running since ${when(job.startedAt)}` : '—';
                  return <Tr key={job.id}>
                    <Td fontSize="xs">{job.id}</Td><Td>{job.photographerId || '—'}</Td>
                    <Td isNumeric>{job.priority}</Td>
                    <Td color={job.status === 'FAILED' ? 'red.300' : undefined}>{job.status}</Td>
                    <Td whiteSpace="nowrap">{when(job.createdAt)}</Td><Td whiteSpace="nowrap">{expectedStart}</Td>
                    <Td>{job.attempts}</Td><Td fontSize="xs" color="red.300">{job.error || '—'}</Td>
                    <Td><HStack>
                      <Button size="xs" onClick={() => inspectJobInput('editorial-image', job.id)}>Input</Button>
                      <Button size="xs" isDisabled={!['DEVELOPING', 'COMMISSIONED'].includes(job.status) || saving}
                        onClick={() => priorityPrompt(job.id, job.priority,
                          priority => AiReporterApi.reprioritizeAiQueueJob(`editorial-images/jobs/${encodeURIComponent(job.id)}`, priority, getAccessTokenSilently, getAccessTokenWithPopup))}>Priority</Button>
                      <Button size="xs" colorScheme="red" variant="outline" isDisabled={job.status === 'RUNNING' || saving}
                        onClick={() => removePrompt(job.id,
                          () => AiReporterApi.deleteAiQueueJob(`editorial-images/jobs/${encodeURIComponent(job.id)}`, getAccessTokenSilently, getAccessTokenWithPopup))}>Remove</Button>
                    </HStack></Td>
                  </Tr>;
                })}</Tbody>
              </Table>
            </Box>
          </Box>

          </>}
          {viewMode === 'provider' && <>
          <Box borderWidth="1px" borderRadius="lg" p={3}>
            <HStack justify="space-between" align="start" mb={2}>
              <Box>
                <Heading size="md">GitHub Copilot quota</Heading>
                <Text fontSize="sm" color="gray.500">
                  Account quota from the Copilot SDK. Token totals below are locally observed successful generations.
                </Text>
              </Box>
              <Button size="xs" onClick={refreshCopilotQuota}>Refresh quota</Button>
            </HStack>
            {copilotQuotaError && <Alert status="error" mb={3}><AlertIcon />{copilotQuotaError}</Alert>}
            {!copilotQuota && !copilotQuotaError && <Text color="gray.500">Loading Copilot quota…</Text>}
            {copilotQuota && !copilotQuota.configured && (
              <Alert status="info" mb={3}><AlertIcon />{copilotQuota.message || 'GitHub Copilot is not enabled.'}</Alert>
            )}
            {copilotQuota?.configured && !copilotQuota.premiumInteractions && (
              <Alert status="warning" mb={3}>
                <AlertIcon />The Copilot account did not report a premium-interaction quota. Manual Copilot jobs are blocked until the quota can be verified.
              </Alert>
            )}
            {copilotQuota?.premiumInteractions && <SimpleGrid columns={{ base: 1, md: 3 }} spacing={3} mb={3}>
              {metric('Premium interactions used', copilotQuota.premiumInteractions.usedRequests)}
              {metric('Entitlement', copilotQuota.premiumInteractions.unlimited
                ? 'Unlimited'
                : copilotQuota.premiumInteractions.entitlementRequests)}
              {metric('Remaining', copilotQuota.premiumInteractions.unlimited
                ? 'Unlimited'
                : copilotQuota.premiumInteractions.remainingPercentage == null
                  ? 'Unknown'
                  : `${copilotQuota.premiumInteractions.remainingPercentage.toFixed(1)}%`)}
            </SimpleGrid>}
            {copilotQuota?.premiumInteractions?.resetDate && (
              <Text fontSize="xs" color="gray.500" mb={3}>
                Copilot quota resets {when(copilotQuota.premiumInteractions.resetDate)}.
              </Text>
            )}
            <HStack align="end">
              <Box>
                <Text fontSize="sm" mb={1}>Require confirmation when remaining is at or below</Text>
                <Input
                  type="number"
                  min={0}
                  max={100}
                  width="120px"
                  value={quotaThresholdDraft}
                  onChange={event => {
                    setQuotaThresholdDraft(event.target.value);
                    setQuotaThresholdDirty(true);
                  }}
                  aria-label="Copilot quota confirmation threshold percentage"
                />
              </Box>
              <Text pb={2}>%</Text>
              <Button isLoading={quotaSaving} isDisabled={!quotaThresholdDirty} onClick={saveCopilotQuotaThreshold}>
                Save runtime setting
              </Button>
            </HStack>
            <Text mt={2} fontSize="xs" color="gray.500">
              Initial default comes from AI_COPILOT_MIN_REMAINING_PERCENT (20% if unset); this admin setting is saved for runtime use.
            </Text>
          </Box>
          <Box>
            <Heading size="md" mb={1}>Provider execution queues</Heading>
            <Text mb={3} fontSize="sm" color="gray.500">
              These rows are individual model calls, not service requests. A service job can create multiple calls through retries or fallbacks.
              Queue concurrency and throttling are application settings and observed provider responses, not the provider account's full quota.
            </Text>
            {executionQueues.length === 0 ? <Text color="gray.500">No configured AI quota queues.</Text> : (
              <VStack align="stretch" spacing={4}>
                {executionQueues.map(qs => {
                  const jobs = qs.jobs || [];
                  const key = `execution:${qs.quotaGroup || qs.target}`;
                  const page = paginate(key, jobs);
                  return <Box key={qs.target} borderWidth="1px" borderRadius="lg" p={3}>
                    <HStack justify="space-between" align="start">
                      <Box>
                        <Text fontWeight="700">{qs.provider || 'Unknown provider'} · quota group {qs.quotaGroup || qs.target}</Text>
                        <Text fontSize="xs" color="gray.500">{qs.provider || '—'} · {qs.model || '—'}{qs.targets?.length ? ` · targets ${qs.targets.join(', ')}` : ''}</Text>
                        <Text fontSize="xs" color="gray.500">Queued {qs.queued ?? 0} · running {qs.running ?? 0}/{qs.concurrency ?? 0} · retry waiting {qs.retryWaiting ?? 0} · retained failed {qs.failed ?? 0} · completed 1h/24h {qs.completedLastHour ?? 0}/{qs.completedLast24Hours ?? 0} · ETA {duration(qs.estimatedClearSeconds)}</Text>
                        {qs.blockedUntil && <Text fontSize="sm" fontWeight="700" color="yellow.300">Throttled · next attempt {when(qs.blockedUntil)}</Text>}
                        {qs.lastError && <Text fontSize="xs" color="red.300">Last error{qs.lastStatusCode ? ` HTTP ${qs.lastStatusCode}` : ''}: {qs.lastError}</Text>}
                      </Box>
                      <Button size="xs" colorScheme="red" variant="outline" isDisabled={!jobs.length || saving}
                        onClick={() => clearPrompt(`${qs.target} queue`, jobs.length,
                          () => AiReporterApi.clearAiQueue(`execution-queues/${encodeURIComponent(qs.target)}/jobs`, getAccessTokenSilently, getAccessTokenWithPopup))}>Clear queue</Button>
                    </HStack>
                    {pager(key, jobs, page.page)}
                    <Box borderWidth="1px" borderRadius="lg" overflowX="auto" maxH="440px" overflowY="auto">
                      <Table size="sm">
                        <Thead><Tr><Th>Task</Th><Th>Target</Th><Th>Model</Th><Th>Agent</Th><Th isNumeric>Priority</Th><Th>Status</Th><Th>Attempts</Th><Th>Next attempt</Th><Th>Actions</Th></Tr></Thead>
                        <Tbody>{page.jobs.map((job, index) => {
                          const position = page.start + index;
                          const path = `execution-queues/${encodeURIComponent(qs.target)}/jobs/${encodeURIComponent(job.id)}`;
                          return <Tr key={job.id}>
                            <Td>{job.task || '—'}</Td><Td>{job.target || '—'}</Td><Td fontSize="xs">{job.model || '—'}</Td>
                            <Td>{job.agentId || '—'}</Td><Td isNumeric>{job.priority}</Td>
                            <Td color={job.status === 'FAILED' ? 'red.300' : undefined}>{job.status}</Td>
                            <Td>{job.attempts}</Td><Td>{when(job.nextAttemptAt)}</Td>
                            <Td><HStack>
                              <Button size="xs" onClick={() => inspectJobInput('provider-call', job.id, qs.target)}>Input</Button>
                              {copilotQuota?.configured
                                && ['QUEUED', 'RETRY_WAIT'].includes(job.status)
                                && job.target !== 'copilot-gpt-6-luna'
                                && <Button size="xs" colorScheme="purple" isDisabled={saving}
                                  onClick={() => moveExecutionJobToCopilot(qs.target, job.id)}>
                                  Send to GPT-6 Luna
                                </Button>}
                              <Button size="xs" isDisabled={position === 0 || ['RUNNING', 'FAILED'].includes(job.status) || saving}
                                onClick={() => movePriority(jobs, position, -1, j => j.priority, 100,
                                  priority => AiReporterApi.reprioritizeAiQueueJob(path, priority, getAccessTokenSilently, getAccessTokenWithPopup))}>↑</Button>
                              <Button size="xs" isDisabled={position === jobs.length - 1 || ['RUNNING', 'FAILED'].includes(job.status) || saving}
                                onClick={() => movePriority(jobs, position, 1, j => j.priority, 100,
                                  priority => AiReporterApi.reprioritizeAiQueueJob(path, priority, getAccessTokenSilently, getAccessTokenWithPopup))}>↓</Button>
                              <Button size="xs" isDisabled={['RUNNING', 'FAILED'].includes(job.status)}
                                onClick={() => priorityPrompt(`${job.task}/${job.agentId}`, job.priority,
                                  priority => AiReporterApi.reprioritizeAiQueueJob(path, priority, getAccessTokenSilently, getAccessTokenWithPopup))}>Priority</Button>
                              <Button size="xs" colorScheme="red" variant="outline" isDisabled={job.status === 'RUNNING'}
                                onClick={() => removePrompt(`${job.task}/${job.agentId}`,
                                  () => AiReporterApi.deleteAiQueueJob(path, getAccessTokenSilently, getAccessTokenWithPopup))}>Remove</Button>
                            </HStack></Td>
                          </Tr>;
                        })}</Tbody>
                      </Table>
                    </Box>
                  </Box>;
                })}
              </VStack>
            )}
          </Box>
          </>}

          {viewMode === 'service' && <>
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
            {pager('community-media', mediaQueue.jobs || [], mediaPage.page)}
            <Box mt={2} borderWidth="1px" borderRadius="lg" overflowX="auto" maxH="440px" overflowY="auto">
              <Table size="sm">
                <Thead><Tr><Th>Character</Th><Th>Type</Th><Th>Status</Th><Th>Registered</Th><Th>Provider</Th><Th isNumeric>Priority</Th><Th>Attempts</Th><Th>Next attempt</Th><Th>Error</Th><Th>Actions</Th></Tr></Thead>
                <Tbody>{mediaPage.jobs.map((job, index) => {
                  const position = mediaPage.start + index;
                  const path = `media-queue/jobs/${encodeURIComponent(job.id)}`;
                  return <Tr key={job.id}>
                    <Td><Text>{job.fanName || 'Deleted / unknown character'}</Text><Text fontSize="xs" color="gray.500">{job.fanProfileId || job.id}</Text></Td>
                    <Td>{job.target}</Td><Td color={job.status === 'FAILED' ? 'red.300' : undefined}>{job.status}</Td>
                    <Td>{when(job.createdAt)}</Td><Td fontSize="xs">{job.provider || job.requestedProvider || 'Cloudflare (automatic)'}{job.model ? ` · ${job.model}` : ''}</Td>
                    <Td isNumeric>{job.priority ?? 0}</Td><Td>{job.attempts ?? 0}</Td><Td>{when(job.nextAttemptAt)}</Td><Td fontSize="xs" color="red.300">{job.error || '—'}</Td>
                    <Td><HStack>
                      <Button size="xs" onClick={() => inspectJobInput('community-media', job.id)}>Input</Button>
                      <Select size="xs" width="150px" aria-label={`Image provider for ${job.fanName || job.id}`}
                        value={selectedMediaProviders[job.id] ?? job.requestedProvider ?? ''}
                        isDisabled={saving || job.status === 'RUNNING'}
                        onChange={event => setSelectedMediaProviders(old => ({ ...old, [job.id]: event.target.value }))}>
                        <option value="">Select image provider</option>
                        {communityImageProviders.filter(provider => provider.configured).map(provider => (
                          <option key={provider.id} value={provider.id}>{provider.name}</option>
                        ))}
                      </Select>
                      <Button size="xs" isDisabled={saving || job.status === 'RUNNING'
                        || !(selectedMediaProviders[job.id] ?? job.requestedProvider)}
                        onClick={() => mutateQueue(() => AiReporterApi.moveCommunityMediaJob(
                          job.id,
                          selectedMediaProviders[job.id] ?? job.requestedProvider,
                          getAccessTokenSilently,
                          getAccessTokenWithPopup
                        ))}>
                        {job.status === 'FAILED' ? 'Retry / move' : 'Move'}
                      </Button>
                      <Button size="xs" isDisabled={position === 0 || ['RUNNING', 'FAILED'].includes(job.status) || saving}
                        onClick={() => movePriority(mediaQueue.jobs, position, -1, j => j.priority ?? 0, 100,
                          priority => AiReporterApi.reprioritizeAiQueueJob(path, priority, getAccessTokenSilently, getAccessTokenWithPopup))}>↑</Button>
                      <Button size="xs" isDisabled={position === mediaQueue.jobs.length - 1 || ['RUNNING', 'FAILED'].includes(job.status) || saving}
                        onClick={() => movePriority(mediaQueue.jobs, position, 1, j => j.priority ?? 0, 100,
                          priority => AiReporterApi.reprioritizeAiQueueJob(path, priority, getAccessTokenSilently, getAccessTokenWithPopup))}>↓</Button>
                      <Button size="xs" isDisabled={['RUNNING', 'FAILED'].includes(job.status)}
                        onClick={() => priorityPrompt(job.id, job.priority ?? 0,
                          priority => AiReporterApi.reprioritizeAiQueueJob(path, priority, getAccessTokenSilently, getAccessTokenWithPopup))}>Priority</Button>
                      <Button size="xs" colorScheme="red" variant="outline" isDisabled={job.status === 'RUNNING'}
                        onClick={() => removePrompt(job.id,
                          () => AiReporterApi.deleteAiQueueJob(path, getAccessTokenSilently, getAccessTokenWithPopup))}>Remove</Button>
                    </HStack></Td>
                  </Tr>;
                })}</Tbody>
              </Table>
            </Box>
              {mediaQueue.recentFailures?.length > 0 && <Box mt={4}><Heading size="sm" mb={2} color="red.300">Recent failed community-media jobs</Heading><Box borderWidth="1px" borderColor="red.700" borderRadius="lg" overflowX="auto"><Table size="sm"><Thead><Tr><Th>Character</Th><Th>Type</Th><Th>Registered</Th><Th>Failed</Th><Th>Attempts</Th><Th>Error</Th><Th>Provider / actions</Th></Tr></Thead><Tbody>{mediaQueue.recentFailures.map(job => <Tr key={job.id}><Td><Text>{job.fanName || 'Deleted / unknown character'}</Text><Text fontSize="xs" color="gray.500">{job.fanProfileId || job.id}</Text></Td><Td>{job.target}</Td><Td>{job.createdAt ? new Date(job.createdAt).toLocaleString() : '—'}</Td><Td>{job.completedAt ? new Date(job.completedAt).toLocaleString() : '—'}</Td><Td>{job.attempts ?? 0}</Td><Td fontSize="xs" color="red.300">{job.error || 'No error message recorded'}</Td><Td><HStack>
                <Button size="xs" onClick={() => inspectJobInput('community-media', job.id)}>Inspect</Button>
                <Select size="xs" width="150px" aria-label={`Image provider for ${job.fanName || job.id}`}
                  value={selectedMediaProviders[job.id] ?? job.requestedProvider ?? ''}
                  isDisabled={saving}
                  onChange={event => setSelectedMediaProviders(old => ({ ...old, [job.id]: event.target.value }))}>
                  <option value="">Select image provider</option>
                  {communityImageProviders.filter(provider => provider.configured).map(provider => (
                    <option key={provider.id} value={provider.id}>{provider.name}</option>
                  ))}
                </Select>
                <Button size="xs" isDisabled={saving || !(selectedMediaProviders[job.id] ?? job.requestedProvider)}
                  onClick={() => mutateQueue(() => AiReporterApi.moveCommunityMediaJob(
                    job.id,
                    selectedMediaProviders[job.id] ?? job.requestedProvider,
                    getAccessTokenSilently,
                    getAccessTokenWithPopup
                  ))}>Retry / move</Button>
              </HStack></Td></Tr>)}</Tbody></Table></Box></Box>}
          </Box>
          </>}

          {viewMode === 'provider' && <>
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
          </>}

          {viewMode === 'service' && <>
          <Box>
            <Heading size="md" mb={1}>Provider usage today (UTC)</Heading>
            <Text mb={3} fontSize="sm" color="gray.500">
              Token usage is recorded for successful generations; requests include failed attempts.
              Copilot's account quota is shown above; other provider quotas are not exposed here.
            </Text>
            {providerSections.length === 0 ? <Text color="gray.500">No provider usage recorded today.</Text> : (
              <VStack align="stretch" spacing={3}>
                {providerSections.map(section => {
                  const totals = section.usage.reduce((sum, item) => ({
                    requests: sum.requests + (item.requests ?? 0),
                    successful: sum.successful + (item.successfulGenerations ?? 0),
                    failed: sum.failed + (item.failedGenerations ?? 0),
                    inputTokens: sum.inputTokens + (item.inputTokens ?? 0),
                    outputTokens: sum.outputTokens + (item.outputTokens ?? 0),
                    rateLimits: sum.rateLimits + (item.rateLimitFailures ?? 0),
                  }), { requests: 0, successful: 0, failed: 0, inputTokens: 0, outputTokens: 0, rateLimits: 0 });
                  return <Box key={section.provider} borderWidth="1px" borderRadius="lg" p={3}>
                    <Heading size="sm" mb={2}>{section.provider}</Heading>
                    <SimpleGrid columns={{ base: 2, md: 5 }} spacing={2}>
                      {metric('Requests', totals.requests)}
                      {metric('Successful', totals.successful)}
                      {metric('Failed', totals.failed)}
                      {metric('Input tokens', totals.inputTokens)}
                      {metric('Output tokens', totals.outputTokens)}
                    </SimpleGrid>
                    <Text mt={2} mb={2} fontSize="xs" color="gray.500">
                      Observed rate-limit responses (HTTP 429): {totals.rateLimits}. Token totals exclude failed calls.
                    </Text>
                    {section.usage.length > 0 && <Box overflowX="auto">
                      <Table size="sm">
                        <Thead><Tr><Th>Model</Th><Th isNumeric>Requests</Th><Th isNumeric>Success</Th><Th isNumeric>Failed</Th><Th isNumeric>Input tokens</Th><Th isNumeric>Output tokens</Th><Th isNumeric>429s</Th><Th>Last 429</Th></Tr></Thead>
                        <Tbody>{section.usage.map(item => (
                          <Tr key={`${item.providerId}:${item.model}`}>
                            <Td>{item.model}</Td><Td isNumeric>{item.requests ?? 0}</Td>
                            <Td isNumeric>{item.successfulGenerations ?? 0}</Td><Td isNumeric>{item.failedGenerations ?? 0}</Td>
                            <Td isNumeric>{item.inputTokens ?? 0}{item.unknownInputTokenGenerations > 0 ? ` (+${item.unknownInputTokenGenerations} unknown)` : ''}</Td>
                            <Td isNumeric>{item.outputTokens ?? 0}{item.unknownOutputTokenGenerations > 0 ? ` (+${item.unknownOutputTokenGenerations} unknown)` : ''}</Td>
                            <Td isNumeric>{item.rateLimitFailures ?? 0}</Td>
                            <Td whiteSpace="nowrap">{item.lastRateLimitAt ? when(item.lastRateLimitAt) : '—'}</Td>
                          </Tr>
                        ))}</Tbody>
                      </Table>
                    </Box>}
                  </Box>;
                })}
              </VStack>
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
          </>}
          <AiJobInputDialog
            isOpen={inputDialogOpen}
            onClose={() => setInputDialogOpen(false)}
            details={inputDetails}
            loading={inputLoading}
            error={inputError}
            canSubmitResult={inputJobRef?.type === 'provider-call' && Boolean(inputDetails)}
            onSubmitResult={submitExternalResult}
          />
        </VStack>
      )}
    </Box>
  );
}

export default AdminAiAutonomousWorkPage;
