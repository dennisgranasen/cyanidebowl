import { Heading, Spinner } from '@chakra-ui/react';
import React from 'react';
import Ranks from './Ranks';
import Contests from '../contest/Contests';

function RoundRobinCompetition({ ranks, contests, competition }) {
  return (
    <>
      <Heading size="md">Ranking</Heading>
      {ranks ? <Ranks ranks={ranks} /> : <Spinner />}
      <Heading size="md">Contests</Heading>
      {contests ? <Contests contests={contests} currentRound={competition.currentRound} /> : <Spinner />}
    </>
  );
}

export default RoundRobinCompetition;
