import React, { useEffect, useState } from 'react';
import {
  Alert, AlertIcon, Badge, Box, Button, FormControl, FormLabel,
  HStack, Menu, MenuButton, MenuItem, MenuList, Text, Textarea, VStack,
} from '@chakra-ui/react';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';
import EditorialCommunityApi from '../../EditorialCommunityApi';
import NuffleDiceGlyph, { ratingGlyphs } from '../NuffleDiceGlyph';

const RatingGlyph = ({ value, fontSize = '1.35rem' }) => (
  <NuffleDiceGlyph
    glyph={ratingGlyphs(value)}
    label={`Rating ${value > 0 ? `+${value}` : value}`}
    fontSize={fontSize}
  />
);

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
                  <HStack fontSize="sm" spacing={2}>
                    <Text>Publikt snitt:</Text>
                    <RatingGlyph value={Math.max(-3, Math.min(3, Math.round(item.average)))}/>
                    <Text>{item.average.toFixed(1)} ({item.count})</Text>
                  </HStack>
                )}
              </Box>

              {isAuthenticated && player.participated && (
                <Menu>
                  <MenuButton as={Button} size="sm" variant="outline" w="190px">
                    Ditt betyg
                  </MenuButton>
                  <MenuList minW="190px">
                    {[-3,-2,-1,0,1,2,3].map((n) => (
                      <MenuItem key={n} onClick={() => rate(player.playerId, n)}>
                        <HStack justify="space-between" w="full">
                          <RatingGlyph value={n}/>
                          <Text fontSize="sm">{n > 0 ? `+${n}` : n}</Text>
                        </HStack>
                      </MenuItem>
                    ))}
                  </MenuList>
                </Menu>
              )}
            </HStack>

            {agentRatings.length > 0 && (
              <VStack mt={2} align="stretch" spacing={1}>
                {agentRatings.map((rating) => (
                  <HStack key={rating.id} align="start">
                    <Badge minW="90px">{rating.reporterId}</Badge>
                    <Box fontWeight="700">
                      <RatingGlyph value={rating.rating}/>
                    </Box>
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
