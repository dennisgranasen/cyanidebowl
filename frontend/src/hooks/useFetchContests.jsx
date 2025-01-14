import { useState } from 'react';
import comparators from '../util/Comparators';
import WarpScoresApiService from '../WarpScoresApiService';
import logger from '../util/Logger';

export default function useFetchContests() {
  const [contestsLoading, setContestsLoading] = useState(true);
  const [contests, setContests] = useState([]);
  const [error, setError] = useState(null);

  const fetchContests = (competition, limit = null) => {
    setContestsLoading(true);
    logger.debug('Fetching contests for competition: %o', competition);
    WarpScoresApiService.competitionContests(competition.uuid, limit)
      .then((data) => {
        data.sort(comparators.compareContestsByMatchOrContestUuidAsDatesDesc);
        setContests(data);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setContestsLoading(false));
  };

  return {
    fetchContests,
    contests,
    contestsLoading,
    error,
  };
}
