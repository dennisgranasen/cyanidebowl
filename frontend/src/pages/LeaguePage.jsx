import React, { useEffect, useState } from 'react';
import { Box, Stack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import Competitions from '../components/competition/Competitions';
import RoundRobinAndWissenLeague from '../components/league/RoundRobinAndWissenLeague';
import imageUrls from '../imageUrls';
import HeaderCard from '../components/common/HeaderCard';
import LiveContests from '../components/contest/LiveContests';
import LatestMatches from '../components/contest/LatestMatches';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import LeagueInfo from '../components/league/LeagueInfo';

const transformCountsToObject = (statusCounts) => {
  if (!statusCounts || !Array.isArray(statusCounts)) return {};
  
  return statusCounts.reduce((acc, item) => {
    acc[item.status] = item.count;
    return acc;
  }, {});
};


function LeaguePage() {
  const {leagueId} = useParams();
  const [competitions, setCompetitions] = useState([]);
  const [league, setLeague] = useState();
  const [competitionCountByStatus, setCompetitionCountByStatus] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(undefined);
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

  useEffect(() => {
    WarpScoresApiService.leagues(leagueId)
      .then((data) => {
        setLeague(data);
      })
      .then(() => setLoading(false))
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString()}) )
  }, []);

  useEffect(() => {
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
  return (
    <Stack>
      <Box>
        <Navigation currentPage="league" league={[league?.id.key, league?.name]} />
      </Box>
      {league && (
        <HeaderCard heading={league.name} detailsHeading="League details" mainImageSrc={imageUrls.logo(league.logo,league?.id?.opus)}>
          <LeagueInfo league={league} competitionCountByStatus={competitionCountByStatus}/>
        </HeaderCard>
      )}
      <LoadingOrErrorWrapper loading={loading} error={error}>        
        {league?.id?.opus === 1 || leagueId.startsWith("1_") ? (
          <RoundRobinAndWissenLeague 
            key={league?.id.value} 
            league={league} 
            leagueLoading={loading} 
          />
        ) : (
          <Competitions competitions={competitions} league={league} />
        )}
      </LoadingOrErrorWrapper>
      {activeCompetitionsIncludeRoundRobinOrWissenOrKnockoutTournaments && <LiveContests league={league} />}
      <LatestMatches league={league} />
    </Stack>
  );
}

export default LeaguePage;