import { useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';

export default function useFetchRanks() {
  const [ranksLoading, setRanksLoading] = useState(true);
  const [ranks, setRanks] = useState([]);
  const [error, setError] = useState(null);

  const fetchRanks = (circuit, circuitLegId, entityId, limit) => {
    setRanksLoading(true);
    if (!circuit) return;
    console.log('Fetching ranks for circuit:', circuit.circuitId, "CircuitLegId: ", circuitLegId, "EntityId: ", entityId, ". Limit: ", limit); 
    WarpScoresApiService.circuitLegEntityRanks(circuit.circuitId, circuitLegId, entityId, limit)
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
