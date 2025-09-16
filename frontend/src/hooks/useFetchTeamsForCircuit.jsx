import { useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';

export default function useFetchTeams() {
  const [teamsLoading, setTeamsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [teams, setTeams] = useState([]);

  const fetchTeams = (id, limit) => {
    console.log('Fetching teams for circuit id:', id, "Limit:", limit);

    WarpScoresApiService.circuitTeams(id, limit)
      .then((data) => {
        console.log("Fetched teams:", data);
        setTeams(data);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setTeamsLoading(false));
  };

  return {
    fetchTeams,
    teams,
    teamsLoading,
    error,
  };
}
