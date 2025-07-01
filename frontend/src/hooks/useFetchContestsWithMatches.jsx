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

    logger.info('Fetching contests for competition: %o', competition.id);
    WarpScoresApiService.competitionContests(competition.id, limit)
      .then((data) => {
        console.debug('Fetched contests:', data);
        WarpScoresApiService.competitionMatches(competition.id, limit)
          .then((matches) => {
            console.debug('Fetched matches:', matches);
            // Merge contests with matches
            data.forEach((contest) => {
              contest.match = matches.filter(match => match.contestId === contest.id)[0] || null;              
            });
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
