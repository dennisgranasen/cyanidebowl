import { Center } from '@chakra-ui/react';
import React from 'react';
import config from '../../config';

const { boxSize } = config;

function ArenaScore({ match, teamId }) {
  const team = match.teams.find((t) => t.id === teamId);
  const opponent = match.teams.find((t) => t.id !== teamId);
  const teamScore = team?.score ?? 0;
  const opponentScore = opponent?.score ?? 0;
  console.log("boxSize: " + boxSize);
  return (
    <Center height={boxSize} width={boxSize}>
      {teamScore}:{opponentScore}
    </Center>
  );
}

export default ArenaScore;
