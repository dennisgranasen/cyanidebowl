import { Heading } from '@chakra-ui/react';
import React from 'react';
import Ranks from './Ranks';
import Contests from '../contest/Contests';

function RoundRobinCompetition({ ranks, contests, competition, ranksLoading, contestsLoading, competitionLoading }) {
  return (
    <>
      <Heading size="md">Ranking</Heading>
      <Ranks loading={ranksLoading} ranks={ranks} />
      <Heading size="md">Contests</Heading>
      <Contests
        contestsLoading={contestsLoading}
        contests={contests}
        competitionLoading={competitionLoading}
        competition={competition}
      />
    </>
  );
}

export default RoundRobinCompetition;
