import React, { useEffect, useState } from 'react';
import {
  Avatar,
  Box,
  Button,
  Heading,
  HStack,
  NumberInput,
  NumberInputField,
  Spinner,
  Switch,
  Table,
  Tbody,
  Td,
  Text,
  Th,
  Thead,
  Tr,
} from '@chakra-ui/react';
import { useNavigate } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import AiReporterApi from '../AiReporterApi';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';

function AdminAiReportersPage() {
  const {
    authenticationReady,
    userPermissions,
    getAccessTokenSilently,
    getAccessTokenWithPopup,
  } = useAuth0WithUserPermissions();

  const navigate = useNavigate();
  const [reporters, setReporters] = useState(null);
  const [saving, setSaving] = useState({});
  const [error, setError] = useState(null);

  const auth = [getAccessTokenSilently, getAccessTokenWithPopup];

  useEffect(() => {
    if (!authenticationReady) return;
    if (!userPermissions.writeSiteAdmin) {
      navigate('/admin');
      return;
    }
    AiReporterApi.adminReporters(...auth).then(setReporters).catch(setError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticationReady, userPermissions.writeSiteAdmin]);

  const buildPayload = (reporter, patch) => {
    const runtime = reporter.runtime || {};
    return {
      enabledOverride: runtime.enabledOverride ?? reporter.enabled,
      reportsEnabledOverride: runtime.reportsEnabledOverride ?? reporter.reportsEnabled,
      interactionsEnabledOverride: runtime.interactionsEnabledOverride ?? reporter.interactionsEnabled,
      playerRatingsEnabledOverride: runtime.playerRatingsEnabledOverride ?? reporter.playerRatingsEnabled,
      writingWeightOverride: runtime.writingWeightOverride ?? reporter.writingWeight,
      commentProbabilityOverride: runtime.commentProbabilityOverride ?? null,
      reactionProbabilityOverride: runtime.reactionProbabilityOverride ?? null,
      replyProbabilityOverride: runtime.replyProbabilityOverride ?? null,
      ...patch,
    };
  };

  const save = async (reporter, patch) => {
    setSaving((current) => ({ ...current, [reporter.id]: true }));
    setError(null);
    try {
      const updated = await AiReporterApi.updateRuntime(
        reporter.id,
        buildPayload(reporter, patch),
        ...auth
      );
      setReporters((current) =>
        current.map((item) => item.id === reporter.id ? updated : item)
      );
    } catch (reason) {
      setError(reason);
    } finally {
      setSaving((current) => ({ ...current, [reporter.id]: false }));
    }
  };

  const reset = (reporter) => save(reporter, {
    enabledOverride: null,
    reportsEnabledOverride: null,
    interactionsEnabledOverride: null,
    playerRatingsEnabledOverride: null,
    writingWeightOverride: null,
    commentProbabilityOverride: null,
    reactionProbabilityOverride: null,
    replyProbabilityOverride: null,
  });

  return (
    <Box p={{ base: 3, md: 6 }}>
      <Navigation currentPage="admin" parentPage="admin" />
      <Heading mt={6}>AI Reporters</Heading>
      <Text mt={2} color="gray.400">Runtime overrides for all AI reporters.</Text>

      {error && <Text mt={4} color="red.300">{error.message || String(error)}</Text>}
      {!reporters && !error && <Spinner mt={8} />}

      {reporters && (
        <Box mt={6} borderWidth="1px" borderRadius="lg" overflow="auto" maxH="calc(100vh - 220px)">
          <Table size="sm">
            <Thead position="sticky" top={0} zIndex={1} bg="chakra-body-bg">
              <Tr>
                <Th minW="260px">Reporter</Th>
                <Th textAlign="center">Enabled</Th>
                <Th textAlign="center">Reports</Th>
                <Th textAlign="center">Interactions</Th>
                <Th textAlign="center">Ratings</Th>
                <Th minW="150px">Writing weight</Th>
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {reporters.map((reporter) => (
                <Tr key={reporter.id}>
                  <Td>
                    <HStack spacing={3}>
                      <Avatar
                        size="sm"
                        name={reporter.alias}
                        src={reporter.avatarImage || reporter.portraitImage || undefined}
                        flexShrink={0}
                      />
                      <Box minW={0}>
                        <Text fontWeight="700" noOfLines={1}>{reporter.alias}</Text>
                        {reporter.race && (
                          <Text fontSize="xs" color="gray.500" noOfLines={1}>
                            {reporter.race}
                          </Text>
                        )}
                      </Box>
                    </HStack>
                  </Td>
                  <Td textAlign="center">
                    <Switch
                      isChecked={reporter.enabled}
                      isDisabled={saving[reporter.id]}
                      onChange={(e) => save(reporter, { enabledOverride: e.target.checked })}
                    />
                  </Td>
                  <Td textAlign="center">
                    <Switch
                      isChecked={reporter.reportsEnabled}
                      isDisabled={!reporter.enabled || saving[reporter.id]}
                      onChange={(e) => save(reporter, { reportsEnabledOverride: e.target.checked })}
                    />
                  </Td>
                  <Td textAlign="center">
                    <Switch
                      isChecked={reporter.interactionsEnabled}
                      isDisabled={!reporter.enabled || saving[reporter.id]}
                      onChange={(e) => save(reporter, { interactionsEnabledOverride: e.target.checked })}
                    />
                  </Td>
                  <Td textAlign="center">
                    <Switch
                      isChecked={reporter.playerRatingsEnabled}
                      isDisabled={!reporter.enabled || saving[reporter.id]}
                      onChange={(e) => save(reporter, { playerRatingsEnabledOverride: e.target.checked })}
                    />
                  </Td>
                  <Td>
                    <NumberInput
                      size="sm"
                      min={0}
                      step={0.05}
                      value={reporter.writingWeight}
                      isDisabled={!reporter.enabled || saving[reporter.id]}
                      onChange={(_, value) => {
                        if (!Number.isFinite(value)) return;
                        setReporters((current) => current.map((item) =>
                          item.id === reporter.id ? { ...item, writingWeight: value } : item
                        ));
                      }}
                      onBlur={() => save(reporter, { writingWeightOverride: reporter.writingWeight })}
                    >
                      <NumberInputField />
                    </NumberInput>
                  </Td>
                  <Td textAlign="right">
                    <Button
                      size="xs"
                      variant="outline"
                      isLoading={saving[reporter.id]}
                      onClick={() => reset(reporter)}
                    >
                      Reset
                    </Button>
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </Box>
      )}
    </Box>
  );
}

export default AdminAiReportersPage;
