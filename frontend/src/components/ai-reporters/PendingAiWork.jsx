import React, { useEffect, useState } from 'react';
import { Box, Heading, Text, Select, Input, HStack, Link, Table, Thead, Tbody, Tr, Th, Td } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import AiReporterApi from '../../AiReporterApi';

export default function PendingAiWork({ getAccessTokenSilently, getAccessTokenWithPopup }) {
  const [snapshot, setSnapshot] = useState(null);
  const [error, setError] = useState(null);
  const [service, setService] = useState('');
  const [query, setQuery] = useState('');
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
  const jobs = snapshot?.jobs || [];
  const visible = jobs.filter(j => (!service || j.service === service) && `${j.subject} ${j.kind} ${j.detail}`.toLocaleLowerCase().includes(query.toLocaleLowerCase()));
  return <Box borderWidth="1px" borderRadius="lg" p={4}>
    <Heading size="md">Queued content and analysis tasks</Heading>
    <Text mt={2} fontSize="sm">Saved work is listed here before it reaches a model. Service execution queues below show individual model calls for the same work; do not add those counts together. Direct requests appear only when they enter an execution queue.</Text>
    <Text mt={2} fontSize="sm">Refreshes every 10 seconds. {snapshot ? `Last update: ${new Date(snapshot.capturedAt).toLocaleString()}. ${jobs.length} unfinished tasks.` : 'Loading…'}</Text>
    {error && <Text color="red.300">Could not refresh. Displayed data may be out of date: {error.message}</Text>}
    <HStack my={3} flexWrap="wrap">
      <Input aria-label="Search tasks by team, match or type" placeholder="Search team, match or task type" value={query} onChange={e => setQuery(e.target.value)} />
      <Select aria-label="Service" value={service} onChange={e => setService(e.target.value)}>
        <option value="">All services</option>
        {[...new Set(jobs.map(j => j.service))].sort().map(s => <option key={s}>{s}</option>)}
      </Select>
    </HStack>
    <Box overflowX="auto" maxH="600px" overflowY="auto">
      <Table size="sm"><Thead><Tr><Th>Task</Th><Th>Team / member / match</Th><Th>Service</Th><Th>Status</Th><Th>Remaining</Th><Th>Progress</Th><Th>Storage</Th><Th>Eligible from</Th><Th>Last error</Th></Tr></Thead>
        <Tbody>{visible.map(j => <Tr key={j.id}>
          <Td>{j.kind}</Td><Td>{j.subjectUrl ? <Link as={RouteLink} to={j.subjectUrl}>{j.subject}</Link> : j.subject}</Td>
          <Td>{j.service}</Td><Td>{j.status}</Td><Td>{j.remaining ?? 'Unknown'}</Td><Td>{j.detail}</Td>
          <Td>{j.persistent ? 'Saved' : 'Memory only'}</Td><Td>{j.nextAttemptAt ? new Date(j.nextAttemptAt).toLocaleString() : '—'}</Td><Td>{j.error || '—'}</Td>
        </Tr>)}</Tbody>
      </Table>
    </Box>
    {snapshot && !visible.length && <Text mt={3}>No matching unfinished tasks.</Text>}
    <Text mt={2} fontSize="xs">For fan profiles, Remaining means new profiles still to generate. Reactivations and deactivations are shown separately. Eligible from is the earliest possible attempt, not a promised start time.</Text>
  </Box>;
}
