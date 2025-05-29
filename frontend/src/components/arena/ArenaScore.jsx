import { Center } from '@chakra-ui/react';
import React from 'react';
import config from '../../config';

const { boxSize } = config;

function ArenaScore({ match, teamUuid }) {
  const team = match.teams.find((t) => t.id === teamUuid);
  const opponent = match.teams.find((t) => t.id !== teamUuid);
  const teamScore = team?.score ?? 0;
  const opponentScore = opponent?.score ?? 0;
  return (
    <Center height={boxSize} width={boxSize}>
      {teamScore}:{opponentScore}
    </Center>
  );
}

export default ArenaScore;
