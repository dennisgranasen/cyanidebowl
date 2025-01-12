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
  if (contest.concede) color = 'red';

  const scoreA = contest.match?.teams ? contest.match.teams[0].score : contest.opponents[0].score;
  const scoreB = contest.match?.teams ? contest.match.teams[1].score : contest.opponents[1].score;
  const scoreText = `${scoreA} - ${scoreB}`;

  return (
    <Center w="100%" color={color}>
      <VStack>
        {matchPlayed && <Heading size={size}>{scoreText}</Heading>}
        {!matchPlayed && <MatchStatusIcon status={contest.status} live={contest.live} boxSize={boxSize} />}
      </VStack>
    </Center>
  );
}

export default ScoreOrIcon;
