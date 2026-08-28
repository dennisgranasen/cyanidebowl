import { Heading } from '@chakra-ui/react';
import React, { useEffect } from 'react';
import Ranks from './Ranks';
import useFetchRanks from '../../hooks/useFetchRanksForCircuit';
import useFetchTeams from '../../hooks/useFetchTeamsForCircuit';

function Circuit({ circuit, circuitLoading }) {
  const { fetchRanks, ranks, ranksLoading, error: ranksError } = useFetchRanks();
  const { fetchTeams, teams, teamsLoading, error: teamsError } = useFetchTeams();

  useEffect(() => {    
    if (circuit) {    
      console.log(circuit);
      fetchRanks(circuit);
      fetchTeams(circuit.circuitId);
      console.log('Teams:', teams);
      console.log('Ranks:', ranks);
    }
  }, [circuit]);

  return (
    <>
      <Heading size="md">Ranking</Heading>
      <Ranks
        loading={circuitLoading || ranksLoading || teamsLoading}
        teams={teams}
        circuitId={circuit?.id}
        ranks={ranks}
        error={ranksError || teamsError}
      />
    </>
  );
}

export default Circuit;
