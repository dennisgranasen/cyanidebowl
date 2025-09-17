import React, { useEffect, useState } from 'react';
import { Box, Stack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import prettyPrint from '../util/prettyPrint';
import Competitions from '../components/competition/Competitions';
import RoundRobinAndWissenLeague from '../components/league/RoundRobinAndWissenLeague';
import imageUrls from '../imageUrls';
import HeaderCard from '../components/common/HeaderCard';
import CompetitionProgress from '../components/competition/CompetitionProgress';

import LiveContests from '../components/contest/LiveContests';
import InfoArea from '../components/common/InfoArea';
import InfoItem from '../components/common/InfoItem';
import formatter from '../util/formatter';


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
  const [competition, setCompetition] = useState();
  const [competitionCountByStatus, setCompetitionCountByStatus] = useState({});
  const [loading, setLoading] = useState(false);
  const [loadingCompetition, setLoadingCompetition] = useState(false);
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

      })
      .then(() => setLoading(false))
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString()}) )
  }, []);

  useEffect(() => {
    if (!entityId) return;
    console.log("Fetching competition for entity:", entityId);
    WarpScoresApiService.competition(entityId)
      .then((comp) => {
        setCompetition(comp);
        setLoadingCompetition(false);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }));
  
  }, [entityId]);


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
        <Navigation currentPage={pageType} 
          circuit={[circuitId, circuit?.circuitName || circuitId]}
          circuitLeg={[legId, circuitLeg?.label || legId]}
          circuitLegEntity={[entityId, (entity?.entityNames && entity.entityNames.join(' - ')) || entityId]} 
        />
      </Box>
      <LoadingOrErrorWrapper loading={loading || loadingCompetition} error={error}>

        {circuitLeg && (
          <HeaderCard heading={circuit.name} detailsHeading="CircuitLeg details">
            <InfoArea>
              {competition && (
                <>
                  <InfoItem key="Created" label="Created" info={formatter.formatAsDate(competition.dateCreated, '-')} />
                  <InfoItem key="Format" label="Format" info={prettyPrint(competition.format)} />
                  <InfoItem
                    key="Progress"
                    label="Progress"
                    info={
                      <CompetitionProgress
                        status={competition?.status}
                        format={competition?.format}
                        currentRound={competition?.currentRound}
                        totalRounds={competition?.totalRounds}
                        totalMatches={competition?.totalMatches}
                        playedMatches={competition?.playedMatches}
                        notValidatedMatches={competition?.notValidatedMatches}
                        liveMatches={competition?.liveMatches}
                        withPadding
                      />
                    }
                  />
                </>
              )}
              <InfoItem key="Teams" label="Teams" info={formatter.formatAsNumber(ranks.length)} />
              <InfoItem
                key="TimeSettings"
                label="Time settings"
                info={`Turn: ${formatter.formatAsNumber((competition?.turnDuration ?? 0) / 60)}m`}
                additionalInfo={`Bonus: ${formatter.formatAsNumber((competition?.timeBonusDuration ?? 0) / 60)}m`}
              />
            </InfoArea>
          </HeaderCard>
        )}
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



{/*
        <HeaderCard
          heading={competition?.name}
          subHeading={<RouteLink to={`/${competition?.leagueId}`}>League: {competition?.leagueName}</RouteLink>}
          detailsHeading="Competition details"
          mainImageSrc={competition?.logo ? imageUrls.logo(competition?.logo, competition?.id?.opus) : imageUrls.logo(competition?.leagueLogo, competition?.id?.opus)}
          additionalImageSrc={competition?.logo ? imageUrls.logo(competition?.leagueLogo, competition?.id?.opus) : null}
        >
          <InfoArea>
            <InfoItem key="Created" label="Created" info={formatter.formatAsDate(competition?.dateCreated)} />
            <InfoItem key="Format" label="Format" info={prettyPrint(competition?.format)} />
            <InfoItem
              key="Progress"
              label="Progress"
              info={
                <CompetitionProgress
                  status={competition?.status}
                  format={competition?.format}
                  currentRound={competition?.currentRound}
                  totalRounds={competition?.totalRounds}
                  totalMatches={competition?.totalMatches}
                  playedMatches={competition?.playedMatches}
                  notValidatedMatches={competition?.notValidatedMatches}
                  liveMatches={competition?.liveMatches}
                  withPadding
                />
              }
            />
            <InfoItem key="Teams" label="Teams" info={formatter.formatAsNumber(getTeamsFor(competition))} />
            <InfoItem
              key="TimeSettings"
              label="Time settings"
              info={`Turn: ${formatter.formatAsNumber((competition?.turnDuration ?? 0) / 60)}m`}
              additionalInfo={`Bonus: ${formatter.formatAsNumber((competition?.timeBonusDuration ?? 0) / 60)}m`}
            />
            {competition?.status === 'Finished' &&
              (!checkPermissions || (authenticationReady && userPermissions.writeLeagueAdmin)) && (
                <InfoItem
                  key="NafDataExport"
                  label="Export"
                  info={
                    <NafExportButton
                      authenticationReady={authenticationReady}
                      checkPermissions={checkPermissions}
                      isAuthenticated={isAuthenticated}
                      getAccessTokenSilently={getAccessTokenSilently}
                      getAccessTokenWithPopup={getAccessTokenWithPopup}
                      competitionId={competition?.id}
                    />
                  }
                />
              )}
          </InfoArea>
          <RouteLink to={`/competition/${competition?.id}/stats`}>
            <Button size="xs">Statistics</Button>
          </RouteLink>
        </HeaderCard>
*/}