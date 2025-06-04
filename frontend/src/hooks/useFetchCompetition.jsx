import { useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';

export default function useFetchCompetition() {
  const [competition, setCompetition] = useState([]);
  const [competitionLoading, setCompetitionLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchCompetition = (competitionUuid) => {
    console.debug('Fetching competition with UUID:', competitionUuid);
    setCompetitionLoading(true);
    console.debug('Loading competition with UUID:', competitionUuid);
    WarpScoresApiService.competition(competitionUuid)
      .then((data) => {
        console.debug('Done fetching competition with UUID:', competitionUuid);
        setCompetition(data);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setCompetitionLoading(false));
  };

  return {
    fetchCompetition,
    competition,
    competitionLoading,
    error,
  };
}
