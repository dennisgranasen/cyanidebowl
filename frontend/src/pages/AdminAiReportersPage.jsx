import React, { useEffect, useState } from 'react';
import {
  Avatar,
  Badge,
  Box,
  Button,
  Card,
  CardBody,
  FormControl,
  FormLabel,
  Heading,
  HStack,
  NumberInput,
  NumberInputField,
  SimpleGrid,
  Spinner,
  Switch,
  Text,
  VStack,
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

  const load = () => {
    setError(null);
    AiReporterApi.adminReporters(...auth).then(setReporters).catch(setError);
  };

  useEffect(() => {
    if (!authenticationReady) return;
    if (!userPermissions.writeSiteAdmin) {
      navigate('/admin');
      return;
    }
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authenticationReady, userPermissions.writeSiteAdmin]);

  const save = async (reporter, patch) => {
    setSaving((current) => ({ ...current, [reporter.id]: true }));
    setError(null);
    try {
      const runtime = reporter.runtime || {};
      const payload = {
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
      const updated = await AiReporterApi.updateRuntime(reporter.id, payload, ...auth);
      setReporters((current) => current.map((item) => item.id === reporter.id ? updated : item));
    } catch (reason) {
      setError(reason);
    } finally {
      setSaving((current) => ({ ...current, [reporter.id]: false }));
    }
  };

  const reset = async (reporter) => {
    await save(reporter, {
      enabledOverride: null,
      reportsEnabledOverride: null,
      interactionsEnabledOverride: null,
      playerRatingsEnabledOverride: null,
      writingWeightOverride: null,
      commentProbabilityOverride: null,
      reactionProbabilityOverride: null,
      replyProbabilityOverride: null,
    });
  };

  return (
    <Box p={{ base: 3, md: 6 }}>
      <Navigation currentPage="admin" parentPage="admin" />
      <Heading mt={6}>AI Reporters</Heading>
      <Text mt={2} color="gray.400">
        Runtime settings override the Markdown defaults. Disabling an agent stops new reports,
        ratings and interactions but keeps historical content.
      </Text>

      {error && <Text mt={4} color="red.300">{error.message || String(error)}</Text>}
      {!reporters && !error && <Spinner mt={8} />}

      <SimpleGrid mt={6} columns={{ base: 1, xl: 2 }} spacing={4}>
        {(reporters || []).map((reporter) => (
          <Card key={reporter.id}>
            <CardBody>
              <HStack align="flex-start" spacing={4}>
                <Avatar name={reporter.alias} />
                <Box flex="1">
                  <Heading size="md">{reporter.alias}</Heading>
                  <HStack mt={1}>
                    {reporter.race && <Badge>{reporter.race}</Badge>}
                    <Badge colorScheme={reporter.enabled ? 'green' : 'gray'}>
                      {reporter.enabled ? 'Active' : 'Disabled'}
                    </Badge>
                  </HStack>
                  <Text mt={1} color="gray.400">{reporter.role}</Text>
                </Box>
              </HStack>

              <VStack align="stretch" spacing={3} mt={5}>
                <FormControl display="flex" alignItems="center">
                  <FormLabel mb="0" flex="1">Enabled</FormLabel>
                  <Switch
                    isChecked={reporter.enabled}
                    isDisabled={saving[reporter.id]}
                    onChange={(e) => save(reporter, { enabledOverride: e.target.checked })}
                  />
                </FormControl>

                <FormControl display="flex" alignItems="center">
                  <FormLabel mb="0" flex="1">Can write reports</FormLabel>
                  <Switch
                    isChecked={reporter.reportsEnabled}
                    isDisabled={!reporter.enabled || saving[reporter.id]}
                    onChange={(e) => save(reporter, { reportsEnabledOverride: e.target.checked })}
                  />
                </FormControl>

                <FormControl display="flex" alignItems="center">
                  <FormLabel mb="0" flex="1">Can interact</FormLabel>
                  <Switch
                    isChecked={reporter.interactionsEnabled}
                    isDisabled={!reporter.enabled || saving[reporter.id]}
                    onChange={(e) => save(reporter, { interactionsEnabledOverride: e.target.checked })}
                  />
                </FormControl>

                <FormControl display="flex" alignItems="center">
                  <FormLabel mb="0" flex="1">Rates players</FormLabel>
                  <Switch
                    isChecked={reporter.playerRatingsEnabled}
                    isDisabled={!reporter.enabled || saving[reporter.id]}
                    onChange={(e) => save(reporter, { playerRatingsEnabledOverride: e.target.checked })}
                  />
                </FormControl>

                <FormControl>
                  <FormLabel>Writing weight</FormLabel>
                  <NumberInput
                    min={0}
                    step={0.05}
                    value={reporter.writingWeight}
                    isDisabled={!reporter.enabled || saving[reporter.id]}
                    onChange={(_, value) => {
                      if (Number.isFinite(value)) {
                        setReporters((current) => current.map((item) =>
                          item.id === reporter.id ? { ...item, writingWeight: value } : item));
                      }
                    }}
                    onBlur={() => save(reporter, { writingWeightOverride: reporter.writingWeight })}
                  >
                    <NumberInputField />
                  </NumberInput>
                </FormControl>

                <Button
                  size="sm"
                  variant="outline"
                  isLoading={saving[reporter.id]}
                  onClick={() => reset(reporter)}
                >
                  Reset to profile defaults
                </Button>
              </VStack>
            </CardBody>
          </Card>
        ))}
      </SimpleGrid>
    </Box>
  );
}

export default AdminAiReportersPage;
