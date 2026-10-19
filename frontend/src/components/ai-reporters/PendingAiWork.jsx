import React, { useEffect, useState } from 'react';
import { Box, Button, Heading, Text, Select, Input, HStack, Link, Table, Thead, Tbody, Tr, Th, Td } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import AiReporterApi from '../../AiReporterApi';

const PAGE_SIZE = 10;

export default function PendingAiWork({ getAccessTokenSilently, getAccessTokenWithPopup, onInspectJob }) {
  const [snapshot, setSnapshot] = useState(null);
  const [error, setError] = useState(null);
  const [service, setService] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(1);
  const [savingId, setSavingId] = useState('');
  const [actionError, setActionError] = useState('');
  const [targetOptions, setTargetOptions] = useState({});
  const [selectedTargets, setSelectedTargets] = useState({});
  useEffect(() => {
    let active = true, timer;
    const refresh = async () => {
      try {
        const data = await AiReporterApi.pendingWork(getAccessTokenSilently, getAccessTokenWithPopup);
        if (active) { setSnapshot(data); setError(null); }
      } catch (e) { if (active) setError(e); }
      finally { if (active) timer = setTimeout(refresh, 10000); }
    };
    refresh();
    return () => { active = false; clearTimeout(timer); };
  }, [getAccessTokenSilently, getAccessTokenWithPopup]);
  useEffect(() => {
    let active = true;
    Promise.all(['FAN_PROFILE', 'PLAYER_RATING'].map(async kind => [
      kind,
      await AiReporterApi.pendingWorkTargets(kind, getAccessTokenSilently, getAccessTokenWithPopup),
    ])).then(entries => {
      if (active) setTargetOptions(Object.fromEntries(entries));
    }).catch(reason => {
      if (active) setActionError(reason?.response?.data?.message || reason?.message || 'Could not load AI targets.');
    });
    return () => { active = false; };
  }, [getAccessTokenSilently, getAccessTokenWithPopup]);
  const jobs = snapshot?.jobs || [];
  const visible = jobs.filter(j => (!service || j.service === service) && `${j.subject} ${j.kind} ${j.detail}`.toLocaleLowerCase().includes(query.toLocaleLowerCase()));
  const pageCount = Math.max(1, Math.ceil(visible.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount);
  const pageJobs = visible.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
  const queuePath = job => {
    const separator = job.id.indexOf(':');
    if (separator < 0) return null;
    const kind = job.id.slice(0, separator);
    const id = encodeURIComponent(job.id.slice(separator + 1));
    if (kind === 'fans') return `pending/fan-profiles/${id}`;
    if (kind === 'rating') return `pending/player-ratings/${id}`;
    if (kind === 'fan-rating') return `pending/fan-player-ratings/${id}`;
    return null;
  };
  const applyAction = async (job, action) => {
    const path = queuePath(job);
    if (!path) return;
    setSavingId(job.id);
    setActionError('');
    try {
      await action(path);
      setSnapshot(await AiReporterApi.pendingWork(getAccessTokenSilently, getAccessTokenWithPopup));
    } catch (reason) {
      setActionError(reason?.response?.data?.message || reason?.message || 'Could not update the queued work.');
    } finally {
      setSavingId('');
    }
  };
  const changePriority = job => {
    const max = job.id.startsWith('autonomous:') ? 1000 : 100;
    const raw = window.prompt(`New priority for ${job.subject} (0–${max}):`, String(job.priority ?? 50));
    if (raw === null) return;
    const priority = Number(raw);
    if (!Number.isInteger(priority) || priority < 0 || priority > max) {
      window.alert(`Priority must be a whole number from 0 to ${max}.`);
      return;
    }
    if (!window.confirm(`Change priority for ${job.subject} to ${priority}?`)) return;
    applyAction(job, path => AiReporterApi.reprioritizeAiQueueJob(
      path, priority, getAccessTokenSilently, getAccessTokenWithPopup));
  };
  const removeJob = job => {
    if (!window.confirm(`Remove ${job.subject} from its queue?`)) return;
    applyAction(job, path => AiReporterApi.deleteAiQueueJob(
      path, getAccessTokenSilently, getAccessTokenWithPopup));
  };
  const moveJob = job => {
    const path = queuePath(job);
    if (!path) return;
    const targetId = selectedTargets[job.id] ?? job.targetOverride ?? '';
    const move = confirmCopilotQuota => AiReporterApi.movePendingWork(
      path, targetId, confirmCopilotQuota, getAccessTokenSilently, getAccessTokenWithPopup);
    applyAction(job, async () => {
      try {
        return await move(false);
      } catch (reason) {
        const warning = reason?.response?.data;
        if (reason?.response?.status !== 409
            || warning?.code !== 'COPILOT_QUOTA_CONFIRMATION_REQUIRED') throw reason;
        if (!window.confirm(warning.message)) return;
        return move(true);
      }
    });
  };
  return <Box borderWidth="1px" borderRadius="lg" p={4}>
    <Heading size="md">Fan profile and player-rating tasks</Heading>
    <Text mt={2} fontSize="sm">
      This table lists fan-profile and player-rating tasks before model execution. Autonomous, article, editorial-image, and community-media jobs have dedicated queues elsewhere on this page. Provider calls and their queue controls are shown in the provider view.
    </Text>
    <Text mt={2} fontSize="sm">Refreshes every 10 seconds. {snapshot ? `Last update: ${new Date(snapshot.capturedAt).toLocaleString()}. ${jobs.length} unfinished tasks.` : 'Loading…'}</Text>
    {error && <Text color="red.300">Could not refresh. Displayed data may be out of date: {error.message}</Text>}
    {actionError && <Text color="red.300">{actionError}</Text>}
    <HStack my={3} flexWrap="wrap">
      <Input aria-label="Search tasks by team, match or type" placeholder="Search team, match or task type" value={query} onChange={e => { setQuery(e.target.value); setPage(1); }} />
      <Select aria-label="Service" value={service} onChange={e => { setService(e.target.value); setPage(1); }}>
        <option value="">All services</option>
        {[...new Set(jobs.map(j => j.service))].sort().map(s => <option key={s}>{s}</option>)}
      </Select>
    </HStack>
    <HStack justify="space-between" py={2} fontSize="sm">
      <Text color="gray.500">{visible.length ? `${(currentPage - 1) * PAGE_SIZE + 1}–${Math.min(currentPage * PAGE_SIZE, visible.length)} of ${visible.length}` : '0 tasks'}</Text>
      <HStack>
        <Button size="xs" isDisabled={currentPage <= 1} onClick={() => setPage(currentPage - 1)}>Previous</Button>
        <Text>{currentPage} / {pageCount}</Text>
        <Button size="xs" isDisabled={currentPage >= pageCount} onClick={() => setPage(currentPage + 1)}>Next</Button>
      </HStack>
    </HStack>
    <Box overflowX="auto" maxH="600px" overflowY="auto">
      <Table size="sm"><Thead><Tr><Th>Task</Th><Th>Team / member / match</Th><Th>Service</Th><Th>Status</Th><Th isNumeric>Priority</Th><Th>Remaining</Th><Th>Progress</Th><Th>Storage</Th><Th>Eligible from</Th><Th>Last error</Th><Th>Actions</Th></Tr></Thead>
        <Tbody>{pageJobs.map(j => <Tr key={j.id}>
          <Td>{j.kind}</Td><Td>{j.subjectUrl ? <Link as={RouteLink} to={j.subjectUrl}>{j.subject}</Link> : j.subject}</Td>
          <Td>{j.service}</Td><Td>{j.status}</Td><Td isNumeric>{j.priority ?? '—'}</Td><Td>{j.remaining ?? 'Unknown'}</Td><Td>{j.detail}</Td>
          <Td>{j.persistent ? 'Saved' : 'Memory only'}</Td><Td>{j.nextAttemptAt ? new Date(j.nextAttemptAt).toLocaleString() : '—'}</Td><Td>{j.error || '—'}</Td>
          <Td><HStack>
            <Button size="xs" onClick={() => onInspectJob?.(j.id.split(':', 1)[0], j.id.slice(j.id.indexOf(':') + 1))}>
              Input
            </Button>
            {['FAN_PROFILE', 'PLAYER_RATING'].includes(j.kind) && <>
              <Select size="xs" width="180px" aria-label={`Destination for ${j.subject}`}
                value={selectedTargets[j.id] ?? j.targetOverride ?? ''}
                isDisabled={j.status === 'RUNNING' || savingId === j.id}
                onChange={event => setSelectedTargets(old => ({ ...old, [j.id]: event.target.value }))}>
                <option value="">Default routing</option>
                {j.targetOverride && !(targetOptions[j.kind] || []).some(target => target.targetId === j.targetOverride)
                  && <option value={j.targetOverride}>{j.targetOverride} · unavailable</option>}
                {(targetOptions[j.kind] || []).map(target => <option key={target.targetId} value={target.targetId}>
                  {target.targetId} · {target.provider}
                </option>)}
              </Select>
              <Button size="xs" isDisabled={j.status === 'RUNNING' || savingId === j.id
                || (selectedTargets[j.id] ?? j.targetOverride ?? '') === (j.targetOverride ?? '')
                || !targetOptions[j.kind]?.length}
                onClick={() => moveJob(j)}>Move</Button>
            </>}
            <Button size="xs" isDisabled={j.status === 'RUNNING' || savingId === j.id || !queuePath(j)}
              onClick={() => changePriority(j)}>Priority</Button>
            <Button size="xs" colorScheme="red" variant="outline"
              isDisabled={j.status === 'RUNNING' || savingId === j.id || !queuePath(j)}
              onClick={() => removeJob(j)}>Remove</Button>
          </HStack></Td>
        </Tr>)}</Tbody>
      </Table>
    </Box>
    {snapshot && !visible.length && <Text mt={3}>No matching unfinished tasks.</Text>}
    <Text mt={2} fontSize="xs">Rows are sorted by service and priority. Priority and removal are available for queued items; running work cannot be reordered or removed. For fan profiles, Remaining means new profiles still to generate; reactivations and deactivations are shown separately. Eligible from is the earliest possible attempt, not a promised start time.</Text>
  </Box>;
}
