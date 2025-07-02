import { Heading } from '@chakra-ui/react';
import React, { useEffect } from 'react';
import Ranks from './Ranks';
import useFetchRanks from '../../hooks/useFetchRanksForLeague';
import useFetchTeams from '../../hooks/useFetchTeams';

function RoundRobinAndWissenLeague({ league, leagueLoading }) {
  const { fetchRanks, ranks, ranksLoading, error: ranksError } = useFetchRanks();
  const { fetchTeams, teams, teamsLoading, error: teamsError } = useFetchTeams();

  useEffect(() => {
    if (league) {
      console.log(league);
      fetchRanks(league);
      fetchTeams(league.id);
      console.log('Ranks:', ranks);
    }
  }, [league]);

  return (
    <>
      <Heading size="md">Ranking</Heading>
      <Ranks
        loading={leagueLoading || ranksLoading || teamsLoading}
        teams={teams}
        leagueId={league?.id}
        ranks={ranks}
        error={ranksError}
      />
    </>
  );
}

export default RoundRobinAndWissenLeague;
