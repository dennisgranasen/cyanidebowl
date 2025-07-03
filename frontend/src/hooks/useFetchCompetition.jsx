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
    setCompetitionLoading(true);
    WarpScoresApiService.leagues(entityId).then((league) => {
      if (!league) {
        setError({ type: 'error', message: `League ${leagueId} not found` });
        setCompetitionLoading(false);
        return;
      }
      setLeague(league);
      logger.info(`Fetched league ${leagueId}`, league);
            //logger.info(`Loading competition ${leagueId} Comp ${competitionId}:`);
      if (competitionId) {
        WarpScoresApiService.leagueCompetitions(entityId)
          .then((data) => {
            if (!data || !data.length) {
              logger.warn(`No competitions found for league ${entityId}`);
              setCompetition([]);
              return;
            }
            console.log(`Fetched competitions for league ${leagueId}:`, data);
            const competitionData = data.find((comp) => comp.competitionId === entityId.value || comp.competitionId === competitionId);
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
