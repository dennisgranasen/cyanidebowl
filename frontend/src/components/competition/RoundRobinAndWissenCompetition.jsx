import { Heading } from '@chakra-ui/react';
import React, { useEffect } from 'react';
import Ranks from './Ranks';
import TabbedContests from '../contest/TabbedContests';
import useFetchContests from '../../hooks/useFetchContests';
import useFetchRanks from '../../hooks/useFetchRanks';

function RoundRobinAndWissenCompetition({ competition, competitionLoading }) {
  const { fetchContests, contests, contestsLoading, error: contestError } = useFetchContests();
  const { fetchRanks, ranks, ranksLoading, error: ranksError } = useFetchRanks();

  useEffect(() => {
    if (competition) {
      fetchRanks(competition);
      fetchContests(competition);
    }
  }, [competition]);
  console.log('RoundRobinAndWissenCompetition', competition, competitionLoading, contests, contestsLoading, contestError, ranks, ranksLoading, ranksError);
  return (
    <>
      <Heading size="md">Ranking</Heading>
      <Ranks
        loading={competitionLoading || ranksLoading}
        competitionId={competition?.id}
        ranks={ranks}
        error={ranksError}
      />
      <Heading size="md">Contests</Heading>
      <TabbedContests
        contests={contests}
        currentRound={competition?.currentRound}
        loading={competitionLoading || contestsLoading}
        error={contestError}
      />
    </>
  );
}

export default RoundRobinAndWissenCompetition;
