import React, { useEffect, useState } from 'react';
import {
  Alert,
  AlertIcon,
  Box,
  Button,
  Checkbox,
  FormControl,
  FormLabel,
  Heading,
  HStack,
  NumberInput,
  NumberInputField,
  Select,
  Spinner,
  Switch,
  Text,
  VStack,
} from '@chakra-ui/react';
import WarpScoresApiService from '../../WarpScoresApiService';
import AiReporterApi from '../../AiReporterApi';

function AiMatchArticleSettings({ getAccessTokenSilently, getAccessTokenWithPopup }) {
  const [systems, setSystems] = useState([]);
  const [leagueSystemId, setLeagueSystemId] = useState('');
  const [policy, setPolicy] = useState(null);
  const [reporterIds, setReporterIds] = useState([]);
  const [articleCount, setArticleCount] = useState('1');
  const [autoAccept, setAutoAccept] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    let active = true;
    WarpScoresApiService.publicLeagueSystems()
      .then((data) => {
        if (!active) return;
        const available = data || [];
        setSystems(available);
        setLeagueSystemId((available.find((system) => system.primary) || available[0])?.id || '');
        setLoading(false);
      })
      .catch((reason) => {
        if (!active) return;
        setError(reason?.message || 'Could not load league systems.');
        setLoading(false);
      });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!leagueSystemId) return undefined;
    let active = true;
    setLoading(true);
    setError('');
    setSaved(false);
    setPolicy(null);
    AiReporterApi.leagueSystemInitiativePolicy(
      leagueSystemId, getAccessTokenSilently, getAccessTokenWithPopup
    )
      .then((data) => {
        if (!active) return;
        setPolicy(data);
        const staff = data.effective?.staff || {};
        const availableIds = (data.availableMatchArticleReporters || []).map((reporter) => reporter.id);
        setReporterIds(staff.matchArticleReporterIds == null
          ? availableIds
          : staff.matchArticleReporterIds.filter((id) => availableIds.includes(id)));
        setArticleCount(String(staff.matchArticlesPerMatch ?? 1));
        setAutoAccept(Boolean(staff.matchArticlesAutoAccept));
        setLoading(false);
      })
      .catch((reason) => {
        if (!active) return;
        setError(reason?.response?.data?.message || reason?.message || 'Could not load article settings.');
        setLoading(false);
      });
    return () => { active = false; };
  }, [leagueSystemId, getAccessTokenSilently, getAccessTokenWithPopup]);

  const save = async () => {
    const count = Number(articleCount);
    if (!Number.isInteger(count) || count < 1 || count > 10) {
      setError('Article count must be a whole number from 1 to 10.');
      return;
    }
    setSaving(true);
    setError('');
    setSaved(false);
    try {
      const updated = await AiReporterApi.updateLeagueSystemInitiativePolicy(
        leagueSystemId,
        {
          ...(policy.override || {}),
          staff: {
            ...(policy.override?.staff || {}),
            matchArticleReporterIds: reporterIds,
            matchArticlesPerMatch: count,
            matchArticlesAutoAccept: autoAccept,
          },
        },
        getAccessTokenSilently,
        getAccessTokenWithPopup
      );
      setPolicy(updated);
      setSaved(true);
    } catch (reason) {
      setError(reason?.response?.data?.message || reason?.message || 'Could not save article settings.');
    } finally {
      setSaving(false);
    }
  };

  const reset = async () => {
    setSaving(true);
    setError('');
    setSaved(false);
    try {
      const updated = await AiReporterApi.updateLeagueSystemInitiativePolicy(
        leagueSystemId,
        {
          ...(policy.override || {}),
          staff: {
            ...(policy.override?.staff || {}),
            matchArticleReporterIds: null,
            matchArticlesPerMatch: null,
            matchArticlesAutoAccept: null,
          },
        },
        getAccessTokenSilently,
        getAccessTokenWithPopup
      );
      setPolicy(updated);
      const staff = updated.effective?.staff || {};
      setReporterIds(staff.matchArticleReporterIds == null
        ? (updated.availableMatchArticleReporters || []).map((reporter) => reporter.id)
        : staff.matchArticleReporterIds);
      setArticleCount(String(staff.matchArticlesPerMatch ?? 1));
      setAutoAccept(Boolean(staff.matchArticlesAutoAccept));
      setSaved(true);
    } catch (reason) {
      setError(reason?.response?.data?.message || reason?.message || 'Could not reset article settings.');
    } finally {
      setSaving(false);
    }
  };

  const toggleReporter = (id, checked) => {
    setReporterIds((current) => checked
      ? [...new Set([...current, id])]
      : current.filter((value) => value !== id));
  };

  return (
    <Box mt={6} borderWidth="1px" borderRadius="md" p={4}>
      <Heading size="md">Automatic match articles</Heading>
      <Text mt={1} color="gray.500" fontSize="sm">
        Choose the AI reporters and publication policy for finalized matches with an analyzed replay.
      </Text>

      {error && <Alert status="error" mt={3}><AlertIcon />{error}</Alert>}
      {saved && <Alert status="success" mt={3}><AlertIcon />Settings saved.</Alert>}

      <FormControl mt={4} maxW="460px">
        <FormLabel>League system</FormLabel>
        <Select value={leagueSystemId} onChange={(event) => setLeagueSystemId(event.target.value)}
          isDisabled={loading || saving} placeholder="Select a league system">
          {systems.map((system) => (
            <option key={system.id} value={system.id}>{system.name || system.id}</option>
          ))}
        </Select>
      </FormControl>

      {loading && <HStack mt={4}><Spinner size="sm" /><Text>Loading settings…</Text></HStack>}
      {!loading && policy && (
        <VStack mt={4} align="stretch" spacing={4}>
          <FormControl maxW="220px">
            <FormLabel>Articles per match</FormLabel>
            <NumberInput min={1} max={10} precision={0} value={articleCount} onChange={setArticleCount}>
              <NumberInputField />
            </NumberInput>
          </FormControl>

          <Box>
            <Text mb={2} fontWeight="semibold">AI reporters</Text>
            {!policy.availableMatchArticleReporters?.length && (
              <Text color="gray.500" fontSize="sm">No enabled reporters currently have a configured article provider.</Text>
            )}
            <VStack align="start" spacing={2}>
              {(policy.availableMatchArticleReporters || []).map((reporter) => (
                <Checkbox key={reporter.id} isChecked={reporterIds.includes(reporter.id)}
                  isDisabled={saving} onChange={(event) => toggleReporter(reporter.id, event.target.checked)}>
                  {reporter.alias} <Text as="span" color="gray.500" fontSize="sm">({reporter.providerId} · {reporter.model})</Text>
                </Checkbox>
              ))}
            </VStack>
          </Box>

          <HStack justify="space-between" align="start" maxW="560px">
            <Box>
              <Text fontWeight="semibold">Auto-accept generated articles</Text>
              <Text fontSize="sm" color="gray.500">Accepted articles are published immediately. Off means they wait for editorial review.</Text>
            </Box>
            <Switch isChecked={autoAccept} isDisabled={saving}
              onChange={(event) => setAutoAccept(event.target.checked)} />
          </HStack>

          <HStack>
            <Button size="sm" colorScheme="blue" onClick={save} isLoading={saving} isDisabled={!leagueSystemId}>
              Save settings
            </Button>
            <Button size="sm" variant="outline" onClick={reset} isLoading={saving} isDisabled={!leagueSystemId}>
              Inherit defaults
            </Button>
          </HStack>
        </VStack>
      )}
    </Box>
  );
}

export default AiMatchArticleSettings;