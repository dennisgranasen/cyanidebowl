import React, { useEffect, useState } from 'react';
import {
  Alert,
  AlertIcon,
  Box,
  Button,
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

const MAX_BACKFILL_DAYS = 3650;

function AiFanMatchCommentSettings({
  getAccessTokenSilently,
  getAccessTokenWithPopup,
  onBackfillComplete,
}) {
  const [systems, setSystems] = useState([]);
  const [leagueSystemId, setLeagueSystemId] = useState('');
  const [policy, setPolicy] = useState(null);
  const [enabled, setEnabled] = useState(true);
  const [probability, setProbability] = useState('100');
  const [daysBack, setDaysBack] = useState('40');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [backfilling, setBackfilling] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState('');

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
    setResult('');
    setPolicy(null);
    AiReporterApi.leagueSystemInitiativePolicy(
      leagueSystemId, getAccessTokenSilently, getAccessTokenWithPopup
    )
      .then((data) => {
        if (!active) return;
        setPolicy(data);
        const fans = data.effective?.fans || {};
        setEnabled(Boolean(fans.ownTeamMatchCommentsEnabled));
        setProbability(String(Math.round((fans.ownTeamMatchCommentProbability ?? 1) * 100)));
        setLoading(false);
      })
      .catch((reason) => {
        if (!active) return;
        setError(reason?.response?.data?.message || reason?.message || 'Could not load fan comment settings.');
        setLoading(false);
      });
    return () => { active = false; };
  }, [leagueSystemId, getAccessTokenSilently, getAccessTokenWithPopup]);

  const update = async (fansPatch) => {
    if (!policy) return;
    setSaving(true);
    setError('');
    setResult('');
    try {
      const updated = await AiReporterApi.updateLeagueSystemInitiativePolicy(
        leagueSystemId,
        {
          ...(policy.override || {}),
          fans: {
            ...(policy.override?.fans || {}),
            ...fansPatch,
          },
        },
        getAccessTokenSilently,
        getAccessTokenWithPopup
      );
      setPolicy(updated);
      const fans = updated.effective?.fans || {};
      setEnabled(Boolean(fans.ownTeamMatchCommentsEnabled));
      setProbability(String(Math.round((fans.ownTeamMatchCommentProbability ?? 1) * 100)));
    } catch (reason) {
      setError(reason?.response?.data?.message || reason?.message || 'Could not save fan comment settings.');
    } finally {
      setSaving(false);
    }
  };

  const saveSettings = () => {
    const value = Number(probability);
    if (!Number.isFinite(value) || value < 0 || value > 100) {
      setError('Comment probability must be between 0 and 100.');
      return;
    }
    update({
      ownTeamMatchCommentsEnabled: enabled,
      ownTeamMatchCommentProbability: value / 100,
    });
  };

  const resetSettings = () => update({
    ownTeamMatchCommentsEnabled: null,
    ownTeamMatchCommentProbability: null,
  });

  const backfill = async () => {
    const days = Number(daysBack);
    if (!Number.isInteger(days) || days < 1 || days > MAX_BACKFILL_DAYS) {
      setError(`Lookback must be a whole number from 1 to ${MAX_BACKFILL_DAYS} days.`);
      return;
    }
    if (!window.confirm(`Queue fan comments for eligible finalized matches from the past ${days} days? Existing queued work will not be duplicated.`)) return;

    setBackfilling(true);
    setError('');
    setResult('');
    try {
      const backfillResult = await AiReporterApi.backfillFanMatchComments(
        leagueSystemId, days, getAccessTokenSilently, getAccessTokenWithPopup
      );
      setResult(
        `Checked ${backfillResult.matchesConsidered} finalized matches; queued ${backfillResult.candidatesQueued} new fan-comment jobs.`
      );
      await onBackfillComplete();
    } catch (reason) {
      setError(reason?.response?.data?.message || reason?.message || 'Could not queue historical match comments.');
    } finally {
      setBackfilling(false);
    }
  };

  return (
    <Box mt={6} borderWidth="1px" borderRadius="md" p={4}>
      <Heading size="md">Fan match comments</Heading>
      <Text mt={1} color="gray.500" fontSize="sm">
        Configure how often a fan may comment on a match involving their supported team.
      </Text>

      {error && <Alert status="error" mt={3}><AlertIcon />{error}</Alert>}
      {result && <Alert status="success" mt={3}><AlertIcon />{result}</Alert>}

      <FormControl mt={4} maxW="460px">
        <FormLabel>League system</FormLabel>
        <Select
          value={leagueSystemId}
          onChange={(event) => setLeagueSystemId(event.target.value)}
          isDisabled={loading || saving || backfilling}
          placeholder="Select a league system"
        >
          {systems.map((system) => (
            <option key={system.id} value={system.id}>{system.name || system.id}</option>
          ))}
        </Select>
      </FormControl>

      {loading && <HStack mt={4}><Spinner size="sm" /><Text>Loading settings…</Text></HStack>}
      {!loading && !systems.length && <Text mt={4} color="gray.500">No league systems are available.</Text>}
      {policy && !loading && (
        <VStack mt={4} align="stretch" spacing={4}>
          <HStack justify="space-between" align="start" maxW="460px">
            <Box>
              <Text fontWeight="semibold">Allow fan match comments</Text>
              <Text fontSize="sm" color="gray.500">Only fans whose supported team played are eligible.</Text>
            </Box>
            <Switch isChecked={enabled} onChange={(event) => setEnabled(event.target.checked)} />
          </HStack>
          <FormControl maxW="220px">
            <FormLabel>Chance per eligible match (%)</FormLabel>
            <NumberInput
              min={0}
              max={100}
              precision={0}
              value={probability}
              onChange={setProbability}
            >
              <NumberInputField />
            </NumberInput>
          </FormControl>
          <HStack>
            <Button size="sm" colorScheme="blue" onClick={saveSettings} isLoading={saving}>
              Save settings
            </Button>
            <Button size="sm" variant="outline" onClick={resetSettings} isLoading={saving}>
              Inherit defaults
            </Button>
          </HStack>

          <Box pt={3} borderTopWidth="1px">
            <Heading size="sm">Backfill older matches</Heading>
            <Text mt={1} fontSize="sm" color="gray.500">
              Queue one eligible fan-comment job per finalized match in the selected period. Existing jobs are skipped, and queued jobs use the currently saved policy.
            </Text>
            <HStack mt={3} align="end" flexWrap="wrap">
              <FormControl maxW="180px">
                <FormLabel>Look back (days)</FormLabel>
                <NumberInput
                  min={1}
                  max={MAX_BACKFILL_DAYS}
                  precision={0}
                  value={daysBack}
                  onChange={setDaysBack}
                >
                  <NumberInputField />
                </NumberInput>
              </FormControl>
              <Button
                colorScheme="teal"
                onClick={backfill}
                isLoading={backfilling}
                isDisabled={!leagueSystemId || loading}
              >
                Queue historical comments
              </Button>
            </HStack>
          </Box>
        </VStack>
      )}
    </Box>
  );
}

export default AiFanMatchCommentSettings;
