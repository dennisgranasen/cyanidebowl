import React, { useEffect, useState } from 'react';
import { Box, Stack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import Competitions from '../components/competition/Competitions';
import RoundRobinAndWissenLeague from '../components/league/RoundRobinAndWissenLeague';
import imageUrls from '../imageUrls';
import HeaderCard from '../components/common/HeaderCard';
import CircuitLegEntities from '../components/circuit/CircuitLegEntities';
import LiveContests from '../components/contest/LiveContests';
import CircuitLegMatches from '../components/circuit/CircuitLegMatches';

import Standings from '../components/common/Standings';
import LatestMatches from '../components/contest/LatestMatches';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import LeagueInfo from '../components/league/LeagueInfo';
import useFetchRanks from '../hooks/useFetchRanksForCircuitLeg';
import useFetchTeams from '../hooks/useFetchTeamsForCircuitLeg';

const transformCountsToObject = (statusCounts) => {
  if (!statusCounts || !Array.isArray(statusCounts)) return {};
  
  return statusCounts.reduce((acc, item) => {
    acc[item.status] = item.count;
    return acc;
  }, {});
};


function CircuitLegPage() {
  const {circuitId, legId} = useParams();  
  const [circuitLeg, setCircuitLeg] = useState();
  const [circuit, setCircuit] = useState();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(undefined);
  const pageType = "circuitLeg";

  const { fetchRanks, ranks, ranksLoading, error: ranksError } = useFetchRanks();
  const { fetchTeams, teams, teamsLoading, error: teamsError } = useFetchTeams();
  
  /*
  const [
    activeCompetitionsIncludeRoundRobinOrWissenOrKnockoutTournaments,
    setActiveCompetitionsIncludeRoundRobinOrWissenOrKnockoutTournaments,
  ] = useState();

  useEffect(() => {
    const formats = competitions.map((competition) => competition.format);
    const includeRoundRobinOrWissenOrKnockoutTournaments =
      formats.includes('RoundRobin') || formats.includes('Wissen') || formats.includes('Knockout');
    setActiveCompetitionsIncludeRoundRobinOrWissenOrKnockoutTournaments(includeRoundRobinOrWissenOrKnockoutTournaments);
  }, [competitions]);
*/
  useEffect(() => {
    WarpScoresApiService.circuits(circuitId)
      .then((data) => {
        console.log("Fetched circuit:", data);
        setCircuit(data);
        const leg = data.circuitLegs.find((cl) => cl.circuitLegId.toString() === legId);
        setCircuitLeg(leg); 
        console.log("Fetched circuit leg:", leg);
        console.log("Found leg:", leg);
        fetchRanks(data, legId);
        fetchTeams(circuitId, legId);
      })
      .then(() => setLoading(false))
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString()}) )
  }, []);

  return (
    <Stack>
      <Box>
        <Navigation currentPage={pageType} 
          circuit={[circuitId, circuit?.circuitName || circuitId]}
          circuitLeg={[legId, circuitLeg?.label || legId]}
        />
      </Box>
      {circuitLeg && (
        <HeaderCard heading={circuitLeg.name} detailsHeading="CircuitLeg details">
          {
            <CircuitLegEntities circuitLeg={circuitLeg} expanded={true} circuitId={circuitId} />
          }
        </HeaderCard>
      )}
      <LoadingOrErrorWrapper loading={loading || ranksLoading || teamsLoading} error={error || ranksError || teamsError}>        
        {circuitLeg?
        <>
          <Standings ranks={ranks} loading={loading} teams={teams} error={error} /> 
          {
            <CircuitLegMatches circuitId={circuitId} circuitLeg={circuitLeg} />
            //<LatestMatches type={pageType} id={`${circuitId}-${circuitLeg.circuitLegId}`} data={circuitLeg} /> 
          }
        </>
          : (error ? null : <Box>No Circuit Leg found with ID {legId}.</Box>)
        }
      </LoadingOrErrorWrapper>
      {
      // activeCompetitionsIncludeRoundRobinOrWissenOrKnockoutTournaments && <LiveContests league={league} />
      }
    </Stack>
  );
}

export default CircuitLegPage;