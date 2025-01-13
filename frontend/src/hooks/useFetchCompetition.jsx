import { useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';

export default function useFetchCompetition() {
  const [competition, setCompetition] = useState([]);
  const [competitionLoading, setCompetitionLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchCompetition = (competitionUuid) => {
    setCompetitionLoading(true);
    WarpScoresApiService.competition(competitionUuid)
      .then((data) => {
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
