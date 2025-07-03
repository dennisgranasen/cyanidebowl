import { useState } from 'react';
import comparators from '../util/comparators';
import WarpScoresApiService from '../WarpScoresApiService';
import logger from '../util/logger';

export default function fetchContestsWithMatches() {
  const [contestsLoading, setContestsLoading] = useState(true);
  const [contests, setContests] = useState([]);
  const [error, setError] = useState(null);

  const fetchContestsWithMatches = (competition, limit = null) => {
    setContestsLoading(true);
    
    //const cid = competition.competitionId || competition.id;

    logger.info('Fetching contests with matches for competition: %o', competition.id);
    WarpScoresApiService.competitionContests(competition.id, limit)
      .then((data) => {
        console.log('Fetched contests:', data);
        WarpScoresApiService.competitionMatches(competition.id, limit)
          .then((matches) => {
            console.log('Fetched matches:', matches);
            // Merge contests with matches
            data.forEach((contest) => {
              contest.match = matches.filter(match => match.matchId === contest.matchUuid)[0] || null;              
            });
            console.log('Matchcontests:', data);

            //data.sort((x, y) => x.match.started - y.match.started);
          })
          .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }));

        setContests(data);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setContestsLoading(false));
  };

  return {
    fetchContestsWithMatches,
    contests,
    contestsLoading,
    error,
  };
}
