import { useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';
import logger from '../util/logger';


export default function useFetchCompetition() {
  const [competition, setCompetition] = useState([]);
  const [competitionLoading, setCompetitionLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchCompetition = (leagueId, competitionId, opus) => {
    //logger.info(`Fetching competition ${leagueId} Comp ${competitionId}:`);
    setCompetitionLoading(true);
    //logger.info(`Loading competition ${leagueId} Comp ${competitionId}:`);
    WarpScoresApiService.leagueCompetitions(leagueId, opus)
      .then((data) => {
        //logger.info(`data: `, data);
        if (!data || !data.length) {
          //logger.warn(`No competitions found for league ${leagueId} with opus ${opus}`);
          setCompetition([]);
          return;
        }
        const competitionData = data.find((comp) => comp.uuid === competitionId);
        if (!competitionData) {
          //logger.warn(`Competition ${competitionId} not found in league ${leagueId}`);
          setCompetition([]);
          return;
        } 
        //logger.info(`Competition data found: `, competitionData);
        //logger.info(`Done fetching competition ${leagueId} Comp ${competitionId}:`);
        setCompetition(competitionData);
      })
      .catch((reason) => 
        {
          setError({ type: 'error', message: reason.toLocaleString() });          
        })
      .finally(() => setCompetitionLoading(false));
  };

  return {
    fetchCompetition,
    competition,
    competitionLoading,
    error,
  };
}
