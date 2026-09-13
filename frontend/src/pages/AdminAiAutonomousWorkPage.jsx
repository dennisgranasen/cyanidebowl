import React, { useCallback, useEffect, useState } from 'react';
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
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';

const metric = (label, value) => (
  <Box borderWidth="1px" borderRadius="md" p={3}>
    <Text fontSize="xs" color="gray.500">{label}</Text>
    <Text fontSize="2xl" fontWeight="700">{value ?? 0}</Text>
  </Box>
);

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

  const load = useCallback(async () => {
    try {
      setError(null);
      const data = await AiReporterApi.autonomousWorkOverview(
        getAccessTokenSilently,
        getAccessTokenWithPopup
      );
      setOverview(data);
    } catch (reason) {
      setError(reason);
    }
  }, [getAccessTokenSilently, getAccessTokenWithPopup]);

  useEffect(() => {
    if (!authenticationReady) return;
    if (!userPermissions.writeSiteAdmin) {
      navigate('/admin');
      return;
    }
    load();
  }, [authenticationReady, userPermissions.writeSiteAdmin, navigate, load]);

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
        <Button variant="outline" onClick={load} isDisabled={!overview}>
          Refresh
        </Button>
      </HStack>

      {error && (
        <Text mt={4} color="red.300">{error.message || String(error)}</Text>
      )}

      {!overview && !error && <Spinner mt={8} />}

      {overview && (
        <VStack mt={6} spacing={6} align="stretch">
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
            </Text>
          </Box>

          <Box>
            <Heading size="md" mb={3}>Generation usage today (UTC)</Heading>
            <SimpleGrid columns={{ base: 2, md: 4 }} spacing={3}>
              {metric('Successful generations', usage.successfulGenerations)}
              {metric('Input tokens', usage.inputTokens)}
              {metric('Output tokens', usage.outputTokens)}
              {metric('In flight', usage.inFlight)}
            </SimpleGrid>
            {(usage.unknownInputTokenGenerations > 0 || usage.unknownOutputTokenGenerations > 0) && (
              <Text mt={2} fontSize="sm" color="yellow.300">
                Some successful generations have unknown token usage:
                {' '}input {usage.unknownInputTokenGenerations || 0},
                {' '}output {usage.unknownOutputTokenGenerations || 0}.
              </Text>
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
