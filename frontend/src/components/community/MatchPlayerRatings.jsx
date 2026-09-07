import React, { useEffect, useState } from 'react';
import { Badge, Box, HStack, Select, Text, VStack } from '@chakra-ui/react';
import { useAuth0 } from '@auth0/auth0-react';
import EditorialCommunityApi from '../../EditorialCommunityApi';

function MatchPlayerRatings({ matchId }) {
  const { isAuthenticated, getAccessTokenSilently } = useAuth0();
  const [players, setPlayers] = useState([]);
  const [summary, setSummary] = useState([]);

  const load = async () => {
    const [p, s] = await Promise.all([
      EditorialCommunityApi.matchPlayers(matchId),
      EditorialCommunityApi.matchRatings(matchId),
    ]);
    setPlayers(p);
    setSummary(s);
  };
  useEffect(() => { load(); }, [matchId]);

  const byPlayer = Object.fromEntries(summary.map((s) => [s.playerId, s]));
  const rate = async (playerId, score) => {
    await EditorialCommunityApi.ratePlayer(matchId, playerId, Number(score), getAccessTokenSilently);
    await load();
  };

  return (
    <VStack align="stretch">
      {players.map((player) => {
        const s = byPlayer[player.playerId];
        return (
          <HStack key={player.playerId} justify="space-between">
            <Box><Text>{player.playerName}</Text>
              {!player.participated && <Badge>MNG</Badge>}
              {s && <Text fontSize="sm">{s.average.toFixed(1)}/10 ({s.count})</Text>}
            </Box>
            {isAuthenticated && player.participated && (
              <Select size="sm" w="90px" placeholder="Betyg" onChange={(e) => rate(player.playerId, e.target.value)}>
                {[0,1,2,3,4,5,6,7,8,9,10].map((n) => <option key={n} value={n}>{n}</option>)}
              </Select>
            )}
          </HStack>
        );
      })}
    </VStack>
  );
}
export default MatchPlayerRatings;
