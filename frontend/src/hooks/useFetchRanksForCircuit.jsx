import { useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';

export default function useFetchRanks() {
  const [ranksLoading, setRanksLoading] = useState(true);
  const [ranks, setRanks] = useState([]);
  const [error, setError] = useState(null);

  const fetchRanks = (circuit, limit) => {
    setRanksLoading(true);
    if (!circuit) return;
    console.log('Fetching ranks for circuit:', circuit.circuitId, ". Limit: ",limit);
    WarpScoresApiService.circuitRanks(circuit.circuitId, limit)
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
