import { HStack } from '@chakra-ui/react';
import React from 'react';
import ArenaMatch from './ArenaMatch';
import config from '../../config';

const { boxSize } = config;

function ArenaProgress({ matches, teamId }) {
  return (
    <HStack height={boxSize} spacing="0.2rem">
      {matches?.map((match) => (
        <ArenaMatch key={`arenaMatch-${match.matchId}`} teamId={teamId} match={match} />
      ))}
    </HStack>
  );
}

export default ArenaProgress;
