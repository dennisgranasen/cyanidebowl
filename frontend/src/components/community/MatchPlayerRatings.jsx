import React, { useEffect, useState } from 'react';
import {
  Alert, AlertIcon, Badge, Box, Button, FormControl, FormLabel,
  HStack, Select, Text, Textarea, VStack,
} from '@chakra-ui/react';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';
import EditorialCommunityApi from '../../EditorialCommunityApi';

const scaleLabel = (value) => {
  const n = Number(value);
  if (n === -3) return '☠ ☠ ☠';
  if (n === -2) return '☠ ☠';
  if (n === -1) return '☠';
  if (n === 0) return '0';
  if (n === 1) return 'POW';
  if (n === 2) return 'POW POW';
  if (n === 3) return 'POW POW POW';
  return String(value);
};

function MatchPlayerRatings({ matchId }) {
  const { isAuthenticated, getAccessTokenSilently } = useAuth0WithUserPermissions();
  const [players, setPlayers] = useState([]);
  const [summary, setSummary] = useState([]);
  const [aiRatings, setAiRatings] = useState([]);
  const [caps, setCaps] = useState(null);
  const [instruction, setInstruction] = useState('');
  const [generating, setGenerating] = useState(false);
  const [message, setMessage] = useState('');

  const load = async () => {
    const [p, s, ai, capabilityData] = await Promise.all([
      EditorialCommunityApi.matchPlayers(matchId),
      EditorialCommunityApi.matchRatings(matchId),
      EditorialCommunityApi.aiPlayerRatings(matchId),
      isAuthenticated
        ? EditorialCommunityApi.matchArticleCapabilities(
            matchId, getAccessTokenSilently).catch(() => null)
        : Promise.resolve(null),
    ]);
    setPlayers(p || []);
    setSummary(s || []);
    setAiRatings(ai || []);
    setCaps(capabilityData);
  };

  useEffect(() => { load(); }, [matchId, isAuthenticated]);

  const byPlayer = Object.fromEntries(
    summary.map((item) => [item.playerId, item]));
  const aiByPlayer = aiRatings.reduce((result, rating) => {
    (result[rating.playerId] ||= []).push(rating);
    return result;
  }, {});

  const rate = async (playerId, score) => {
    if (score === '') return;
    await EditorialCommunityApi.ratePlayer(
      matchId, playerId, Number(score), getAccessTokenSilently);
    await load();
  };

  const generateAll = async () => {
    setGenerating(true);
    setMessage('');
    try {
      const result = await EditorialCommunityApi.generateAiPlayerRatings(
        matchId,
        {
          reporterIds: [],
          instruction: instruction.trim() || null,
          force: true,
        },
        getAccessTokenSilently
      );
      setMessage(
        `${result.reporterIds?.length || 0} AI-agenter betygsatte `
        + `${result.playerCount || 0} spelare.`
      );
      await load();
    } catch (error) {
      setMessage(
        error?.response?.data?.message
        || error?.message
        || 'Betygsättningen misslyckades.'
      );
    } finally {
      setGenerating(false);
    }
  };

  const canGenerateAi = Boolean(caps?.canReview || caps?.canDeleteAny);

  return (
    <VStack align="stretch" spacing={4}>
      {canGenerateAi && (
        <Box borderWidth="1px" borderRadius="md" p={3}>
          <Text fontWeight="700">AI-spelarbetyg</Text>
          <Text mt={1} fontSize="sm" color="gray.500">
            Alla aktiva betygsagenter får samma fakta och betygsätter alla
            spelare i ett anrop per agent. Skalan är tre skallar (-3) till
            tre POWs (+3).
          </Text>
          <FormControl mt={3}>
            <FormLabel fontSize="sm">
              Instruktion till agenterna (valfritt)
            </FormLabel>
            <Textarea
              value={instruction}
              onChange={(e) => setInstruction(e.target.value)}
              placeholder="T.ex. var extra hård mot onödiga turnovers, men håll er till replayfakta…"
            />
          </FormControl>
          <Button
            mt={3}
            colorScheme="orange"
            onClick={generateAll}
            isLoading={generating}
          >
            Betygsätt alla spelare med alla AI-agenter
          </Button>
          {message && (
            <Alert mt={3} status="info">
              <AlertIcon/>{message}
            </Alert>
          )}
        </Box>
      )}

      {players.map((player) => {
        const item = byPlayer[player.playerId];
        const agentRatings = aiByPlayer[player.playerId] || [];
        return (
          <Box key={player.playerId} borderBottomWidth="1px" pb={3}>
            <HStack justify="space-between" align="start">
              <Box>
                <Text fontWeight="600">{player.playerName}</Text>
                {!player.participated && <Badge>MNG</Badge>}
                {item && (
                  <Text fontSize="sm">
                    Publikt snitt: {item.average.toFixed(1)} ({item.count})
                  </Text>
                )}
              </Box>

              {isAuthenticated && player.participated && (
                <Select
                  size="sm"
                  w="190px"
                  placeholder="Ditt betyg"
                  onChange={(e) => rate(player.playerId, e.target.value)}
                >
                  {[-3,-2,-1,0,1,2,3].map((n) => (
                    <option key={n} value={n}>
                      {scaleLabel(n)} ({n > 0 ? `+${n}` : n})
                    </option>
                  ))}
                </Select>
              )}
            </HStack>

            {agentRatings.length > 0 && (
              <VStack mt={2} align="stretch" spacing={1}>
                {agentRatings.map((rating) => (
                  <HStack key={rating.id} align="start">
                    <Badge minW="90px">{rating.reporterId}</Badge>
                    <Text fontSize="sm" fontWeight="700">
                      {scaleLabel(rating.rating)}
                    </Text>
                    <Text fontSize="sm" color="gray.500">
                      {rating.verdict}
                    </Text>
                  </HStack>
                ))}
              </VStack>
            )}
          </Box>
        );
      })}
    </VStack>
  );
}

export default MatchPlayerRatings;
