import { Heading } from '@chakra-ui/react';
import React, {useEffect} from 'react';
import Ranks from './Ranks';
import Contests from '../contest/Contests';
import logger from "../../util/Logger";

function RoundRobinCompetition({ ranks, contests, competition, ranksLoading, contestsLoading, competitionLoading }) {
    useEffect(() => {
        logger.debug("========== Ranks loading: %s, contestsLoading: %s, competitionLoading: %s, ", ranksLoading, contestsLoading, competitionLoading);
    }, [ranksLoading, contestsLoading, competitionLoading]);

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
