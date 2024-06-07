import { Center, Heading, VStack } from '@chakra-ui/react';
import React from 'react';
import MatchStatusIcon from './MatchStatusIcon';

function ScoreOrIcon({ contest, size, boxSize }) {
  let matchPlayed = false;
  let matchValidated = false;
  let color = null;

  switch (contest.status) {
    case 'played':
    case 'Validated':
      matchPlayed = true;
      matchValidated = true;
      break;
    case 'InProgress':
      matchPlayed = contest.matchDate && contest.winner && contest.status === 'InProgress' && !contest.live;
      break;
    default:
      break;
  }

  if (matchPlayed && !contest.live && !matchValidated) color = 'grey';
  if (contest.adminResult) color = 'orange';

  return (
    <Center w="100%" color={color}>
      <VStack>
        {matchPlayed && contest.match && (
          <Heading size={size}>{`${contest.match.teams[0].score} - ${contest.match.teams[1].score}`}</Heading>
        )}
        {((matchPlayed && !contest.match) || !matchPlayed) && (
          <MatchStatusIcon status={contest.status} live={contest.live} boxSize={boxSize} />
        )}
      </VStack>
    </Center>
  );
}

export default ScoreOrIcon;
