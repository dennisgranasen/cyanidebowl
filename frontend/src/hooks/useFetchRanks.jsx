import { useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';

export default function useFetchRanks() {
  const [ranksLoading, setRanksLoading] = useState(true);
  const [ranks, setRanks] = useState([]);
  const [error, setError] = useState(null);

  const fetchRanks = (competition, limit) => {
    setRanksLoading(true);
    WarpScoresApiService.competitionRanks(competition.id.value, competition.id.opus, limit)
      .then((data) => {
        data.sort((rankA, rankB) => rankA.rank - rankB.rank);
        setRanks(data);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setRanksLoading(false));
  };

  return {
    fetchRanks,
    ranks,
    ranksLoading,
    error,
  };
}
