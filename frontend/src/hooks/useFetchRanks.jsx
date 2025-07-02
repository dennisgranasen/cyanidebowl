import { useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';

export default function useFetchRanks() {
  const [ranksLoading, setRanksLoading] = useState(true);
  const [ranks, setRanks] = useState([]);
  const [error, setError] = useState(null);

  const fetchRanks = (competition, limit) => {
    setRanksLoading(true);
    console.log('Fetching ranks for competition:', competition.id, limit);
    WarpScoresApiService.competitionRanks(competition.id, limit)
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
