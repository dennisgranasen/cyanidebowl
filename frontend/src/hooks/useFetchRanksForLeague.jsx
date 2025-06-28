import { useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';

export default function useFetchRanks() {
  const [ranksLoading, setRanksLoading] = useState(true);
  const [ranks, setRanks] = useState([]);
  const [error, setError] = useState(null);

  const fetchRanks = (league, limit) => {
    setRanksLoading(true);
    console.log('Fetching ranks for league:', league.id.value, league.id.opus, limit);
    WarpScoresApiService.leagueRanks(league.id.value, league.id.opus, limit)
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
