import { useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';
import logger from '../util/logger';


export default function useFetchCompetition() {
  const [competition, setCompetition] = useState([]);
  const [league, setLeague] = useState(null);
  const [competitionLoading, setCompetitionLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchCompetition = (entityId) => {
    if (!entityId) {
      setError({ type: 'error', message: 'entityId is required' });
      return;
    }
    if (entityId === 'undefined' || entityId === 'null') {
      setError({ type: 'error', message: 'Invalid entityId' });
      return;
    }
    console.log('entityid type is: ', entityId.type);
    var leagueId, competitionId;
    if (entityId.type === 'composite') {
      leagueId = entityId.parts[0];
      competitionId = entityId.parts[1];
    }
    else if (entityId.type === 'simple') {
      leagueId = entityId.value
      competitionId = null;
    }
    else {
      setError({ type: 'error', message: 'Invalid entityId type' });
      return;
    }
    const opus = entityId.opus ? parseInt(entityId.opus, 10) : undefined;
    setCompetitionLoading(true);
    WarpScoresApiService.leagues(leagueId, opus).then((league) => {
      if (!league) {
        setError({ type: 'error', message: `League ${leagueId} not found` });
        setCompetitionLoading(false);
        return;
      }
      setLeague(league);
      logger.info(`Fetched league ${leagueId} with opus ${opus}:`, league);
            //logger.info(`Loading competition ${leagueId} Comp ${competitionId}:`);
      if (competitionId) {
        WarpScoresApiService.leagueCompetitions(leagueId, opus)
          .then((data) => {
            if (!data || !data.length) {
              logger.warn(`No competitions found for league ${leagueId} with opus ${opus}`);
              setCompetition([]);
              return;
            }
            const competitionData = data.find((comp) => comp.competitionId === competitionId);
            if (!competitionData) {
              logger.warn(`Competition ${competitionId} not found in league ${leagueId}`);
              setCompetition([]);
              return;
            } 
            logger.info(`Competition data found: `, competitionData);
            setCompetition(competitionData);
          })
          .catch((reason) => 
            {
              setError({ type: 'error', message: reason.toLocaleString() });          
            })
          .finally(() => setCompetitionLoading(false));
      }
      else {
        setCompetitionLoading(false);
      }
    
    })
    .catch((reason) => {
      setError({ type: 'error', message: reason.toLocaleString() });
      setCompetitionLoading(false);
    });
//    } // <-- Add this closing brace for fetchCompetition
  };

  return {
    fetchCompetition,
    league,
    competition,
    competitionLoading,
    error,
  };
}
