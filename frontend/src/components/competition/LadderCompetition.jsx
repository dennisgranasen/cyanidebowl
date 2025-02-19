import { Heading } from '@chakra-ui/react';
import React, { useEffect } from 'react';
import Ranks from './Ranks';
import useFetchRanks from '../../hooks/useFetchRanks';
import LiveContests from '../contest/LiveContests';
import LatestContests from '../contest/LatestContests';

function LadderCompetition({ competition, competitionLoading }) {
  const { fetchRanks, ranks, ranksLoading, error: ranksError } = useFetchRanks();

  useEffect(() => {
    if (competition) {
      fetchRanks(competition, 20);
    }
  }, [competition]);

  return (
    <>
      <Heading size="md">Ranking</Heading>
      <Ranks
        competitionUuid={competition?.uuid}
        loading={competitionLoading || ranksLoading}
        ranks={ranks}
        error={ranksError}
      />
      <LiveContests competition={competition} limit={6} />
      <LatestContests competition={competition} limit={9} />
    </>
  );
}

export default LadderCompetition;
