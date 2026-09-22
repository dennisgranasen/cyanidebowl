import React, { useEffect, useState } from 'react';
import {
  Avatar,
  Box,
  Button,
  Heading,
  HStack,
  NumberInput,
  NumberInputField,
  Select,
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
import { useIntl } from 'react-intl';

function AdminAiReportersPage() {
  const intl = useIntl();
  const {
    authenticationReady,
    userPermissions,
    getAccessTokenSilently,
    getAccessTokenWithPopup,
  } = useAuth0WithUserPermissions();

  const navigate = useNavigate();
  const [reporters, setReporters] = useState(null);
  const [settings, setSettings] = useState(null);
  const [savingSettings, setSavingSettings] = useState(false);
  const [saving, setSaving] = useState({});
  const [error, setError] = useState(null);

  const auth = [getAccessTokenSilently, getAccessTokenWithPopup];

  useEffect(() => {
    if (!authenticationReady) return;
    if (!userPermissions.writeSiteAdmin) {
      navigate('/admin');
      return;
    }
    Promise.all([
      AiReporterApi.adminReporters(...auth),
      AiReporterApi.adminSettings(...auth),
    ])
      .then(([loadedReporters, loadedSettings]) => {
        setReporters(loadedReporters);
        setSettings(loadedSettings);
      })
      .catch(setError);
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
      primaryLanguageOverride: runtime.primaryLanguageOverride ?? null,
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
    primaryLanguageOverride: null,
  });

  const saveDefaultLanguage = async (defaultLanguage) => {
    setSavingSettings(true);
    setError(null);
    try {
      const updated = await AiReporterApi.updateAdminSettings(
        { defaultLanguage },
        ...auth
      );
      setSettings(updated);
      const refreshed = await AiReporterApi.adminReporters(...auth);
      setReporters(refreshed);
    } catch (reason) {
      setError(reason);
    } finally {
      setSavingSettings(false);
    }
  };

  return (
    <Box p={{ base: 3, md: 6 }}>
      <Navigation currentPage="admin" parentPage="admin" />
      <Heading mt={6}>{intl.formatMessage({ id: 'aiAdmin.heading' })}</Heading>
      <Text mt={2} color="gray.400">{intl.formatMessage({ id: 'aiAdmin.help' })}</Text>

      {error && <Text mt={4} color="red.300">{error.message || String(error)}</Text>}

      {settings && (
        <Box mt={5} maxW="360px">
          <Text mb={1} fontSize="sm" fontWeight="700">{intl.formatMessage({ id: 'aiAdmin.defaultArticleLanguage' })}</Text>
          <Select
            size="sm"
            value={settings.defaultLanguage || 'sv'}
            isDisabled={savingSettings}
            onChange={(e) => saveDefaultLanguage(e.target.value)}
          >
            <option value="sv">Svenska (sv)</option>
            <option value="en">English (en)</option>
          </Select>
          <Text mt={1} fontSize="xs" color="gray.500">
            {intl.formatMessage({ id: 'aiAdmin.defaultLanguageHelp' })}
          </Text>
        </Box>
      )}

      {!reporters && !error && <Spinner mt={8} />}

      {reporters && (
        <Box mt={6} borderWidth="1px" borderRadius="lg" overflow="auto" maxH="calc(100vh - 220px)">
          <Table size="sm">
            <Thead position="sticky" top={0} zIndex={1} bg="chakra-body-bg">
              <Tr>
                <Th minW="260px">{intl.formatMessage({ id: 'aiAdmin.reporter' })}</Th>
                <Th textAlign="center">{intl.formatMessage({ id: 'aiAdmin.enabled' })}</Th>
                <Th textAlign="center">{intl.formatMessage({ id: 'aiAdmin.reports' })}</Th>
                <Th textAlign="center">{intl.formatMessage({ id: 'aiAdmin.interactions' })}</Th>
                <Th textAlign="center">{intl.formatMessage({ id: 'aiAdmin.ratings' })}</Th>
                <Th minW="175px">{intl.formatMessage({ id: 'aiAdmin.language' })}</Th>
                <Th minW="150px">{intl.formatMessage({ id: 'aiAdmin.writingWeight' })}</Th>
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
                    <Select
                      size="sm"
                      value={reporter.runtime?.primaryLanguageOverride || ''}
                      isDisabled={!reporter.enabled || saving[reporter.id]}
                      onChange={(e) => save(reporter, {
                        primaryLanguageOverride: e.target.value || null,
                      })}
                    >
                      <option value="">
                        {intl.formatMessage({ id: 'aiAdmin.inherit' }, { language: reporter.profileLanguage || settings?.defaultLanguage || 'sv' })}
                      </option>
                      <option value="sv">Svenska (sv)</option>
                      <option value="en">English (en)</option>
                    </Select>
                    <Text mt={1} fontSize="xs" color="gray.500">
                      {intl.formatMessage({ id: 'aiAdmin.effective' }, { language: reporter.primaryLanguage })}
                    </Text>
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
                      {intl.formatMessage({ id: 'aiAdmin.reset' })}
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
