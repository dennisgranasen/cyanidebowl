import React, { useEffect, useState } from 'react';
import { Box, Stack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import Competitions from '../components/competition/Competitions';
import RoundRobinAndWissenLeague from '../components/league/RoundRobinAndWissenLeague';
import imageUrls from '../imageUrls';
import HeaderCard from '../components/common/HeaderCard';
import CircuitLeg from '../components/circuit/CircuitLegInfo';
import LiveContests from '../components/contest/LiveContests';

import Standings from '../components/common/Standings';
import LatestMatches from '../components/contest/LatestMatches';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import LeagueInfo from '../components/league/LeagueInfo';
import useFetchRanks from '../hooks/useFetchRanksForCircuitLegEntity';
//import useFetchTeams from '../hooks/useFetchTeamsForCircuitLegEntity';

const transformCountsToObject = (statusCounts) => {
  if (!statusCounts || !Array.isArray(statusCounts)) return {};
  
  return statusCounts.reduce((acc, item) => {
    acc[item.status] = item.count;
    return acc;
  }, {});
};


function CircuitLegEntityPage() {
  const {circuitId, legId, entityId} = useParams();  
  //const [competitions, setCompetitions] = useState([]);
  const [entity, setEntity] = useState();
  const [circuitLeg, setCircuitLeg] = useState();
  const [circuit, setCircuit] = useState();
  const [competitionCountByStatus, setCompetitionCountByStatus] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(undefined);
  const pageType = "circuitLegEntity";



  const { fetchRanks, ranks, ranksLoading, error: ranksError } = useFetchRanks();
  //const { fetchTeams, teams, teamsLoading, error: teamsError } = useFetchTeams();
  
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
        console.log("Looking for entityId:", entityId);
        const ent = leg?.entities.find(e => e.entityId.key === entityId);
        setEntity(ent);
        console.log("Found entity:", ent);

        fetchRanks(data, legId, entityId);
        console.log('Ranks:', ranks);
        //fetchTeams(circuitId, legId, entityId);
        //console.log('Teams:', teams);

      })
      .then(() => setLoading(false))
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString()}) )
  }, []);

/*useEffect(() => {
    if (!league) return;
    console.log("Fetching CC counts for league:", league.id);
    WarpScoresApiService.competitionCountByStatus([league.id])
      .then((counts) =>  {
          if (counts.length > 0) {            
            setCompetitionCountByStatus(transformCountsToObject(counts[0].statusCounts));
            console.log("Competition counts by status:", counts); 
          }
        })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
  }, [league]);
*/
/*
  useEffect(() => {
    const fetchCompetitions = async (leagueId) => {
      setError(undefined);
      setCompetitions([]);
      setLoading(true);
      if (leagueId === null || leagueId.length === 0) {
        setLoading(false);
        setError({ type: 'info', message: 'No League selected.' });
        return;
      }
      WarpScoresApiService.leagueCompetitions(leagueId)
        .then(setCompetitions)
        .then(() => setLoading(false))
        .catch((reason) => {
          setError({ type: 'error', message: reason.toLocaleString() });
        });
    };
    if (league)
      fetchCompetitions(leagueId);
  }, [league]);
  */
  return (
    <Stack>
      <Box>
        <Navigation currentPage={pageType} circuitId={circuitId} circuitLeg={[legId, circuitLeg?.label]} circuitLegEntity={[entity?.entityId, entity?.label]} />
      </Box>
      {circuitLeg && (
        <HeaderCard heading={circuitLeg.name} detailsHeading="CircuitLeg details">
          {
          //<CircuitLegInfo circuitLeg={circuitLeg} />
          }
        </HeaderCard>
      )}
      <LoadingOrErrorWrapper loading={loading} error={error}>        
        {circuitLeg?
        <>
          <Standings ranks={ranks} loading={loading} /*teams={teams}*/ error={error} /> 
          {
            //<LatestMatches type={pageType} id={`${circuitId}-${circuitLeg.circuitLegId}`} data={circuitLeg} /> 
          }
        </>
          : (error ? null : <Box>No Circuit Leg found with ID {legId}.</Box>)
        }
      </LoadingOrErrorWrapper>
      {// activeCompetitionsIncludeRoundRobinOrWissenOrKnockoutTournaments && <LiveContests league={league} />
      }
    </Stack>
  );
}

export default CircuitLegEntityPage;