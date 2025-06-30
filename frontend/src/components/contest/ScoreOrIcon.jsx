import { Center, Heading, VStack } from '@chakra-ui/react';
import React from 'react';
import MatchStatusIcon from './MatchStatusIcon';

function ScoreOrIcon({ contestOrMatch, size, boxSize }) {
  let matchPlayed = false;
  let matchValidated = false;
  let color = null;

  switch (contestOrMatch.status) {
    case 'played':
    case 'Validated':
      matchPlayed = true;
      matchValidated = true;
      break;
    case 'InProgress':
      matchPlayed =
        (contestOrMatch.matchDate || contestOrMatch.finished) &&
        contestOrMatch.winner &&
        contestOrMatch.status === 'InProgress' &&
        !contestOrMatch.live;
      break;
    default:
      matchPlayed = contestOrMatch.matchId && true;
      matchValidated = contestOrMatch.matchId && true;
      break;
  }

  if (matchPlayed && !contestOrMatch.live && !matchValidated) color = 'grey';
  if (contestOrMatch.adminResult) color = 'orange';
  if (contestOrMatch.concede) color = 'red';

  const teams = contestOrMatch.contestId ? contestOrMatch.opponents : contestOrMatch.teams;
  console.log('ScoreOrIcon', { contestOrMatch, teams, matchPlayed, matchValidated, color });
  const scoreA = teams[0].score;
  const scoreB = teams[1].score;
  const scoreText = `${scoreA} - ${scoreB}`;

  return (
    <Center w="100%" color={color}>
      <VStack>
        {matchPlayed && <Heading size={size}>{scoreText}</Heading>}
        {!matchPlayed && (
          <MatchStatusIcon status={contestOrMatch.status} live={contestOrMatch.live} boxSize={boxSize} />
        )}
      </VStack>
    </Center>
  );
}

export default ScoreOrIcon;
