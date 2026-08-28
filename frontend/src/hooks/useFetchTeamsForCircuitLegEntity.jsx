import { useState } from 'react';
import WarpScoresApiService from '../WarpScoresApiService';

export default function useFetchTeams() {
  const [teamsLoading, setTeamsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [teams, setTeams] = useState([]);

  const fetchTeams = (id, circuitLegId, circuitLegEntityId, limit) => {
    console.log('Fetching teams for circuit id:', id, "Circuit Leg ID:", circuitLegId, "Circuit Leg Entity ID:", circuitLegEntityId, "Limit:", limit);

    WarpScoresApiService.circuitLegEntityTeams(id, circuitLegId, circuitLegEntityId, limit)
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
