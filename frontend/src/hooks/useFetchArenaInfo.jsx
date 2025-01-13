import { useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';

export default function useFetchArenaInfo() {
  const [arenaInfo, setArenaInfo] = useState([]);
  const [arenaInfoLoading, setArenaInfoLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchArenaInfo = (uuid, raceToFetch) => {
    setArenaInfoLoading(true);
    WarpScoresApiService.arenaInfos(uuid, raceToFetch)
      .then(setArenaInfo)
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setArenaInfoLoading(false));
  };

  return {
    fetchArenaInfo,
    arenaInfo,
    arenaInfoLoading,
    error,
  };
}
