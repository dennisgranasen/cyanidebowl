import React, { useEffect, useState } from 'react';
import { Box, Stack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import Competitions from '../components/competition/Competitions';
import imageUrls from '../imageUrls';
import HeaderCard from '../components/common/HeaderCard';
import LiveContests from '../components/contest/LiveContests';
import LatestMatches from '../components/contest/LatestMatches';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import LeagueInfo from '../components/league/LeagueInfo';

function LeaguePage() {
  const { leagueUuid } = useParams();
  const [competitions, setCompetitions] = useState([]);
  const [league, setLeague] = useState();
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
    const fetchLeague = () => {
      WarpScoresApiService.leagues(leagueUuid)
        .then((data) => {
          setLeague(data);
        })
        .then(() => setLoading(false))
        .catch((reason) => {
          setError({ type: 'error', message: reason.toLocaleString() });
        });
    };
    fetchLeague();
  }, []);

  useEffect(() => {
    const fetchCompetitions = async (leagueId, initialized) => {
      setError(undefined);
      setCompetitions([]);
      setLoading(true);
      if (leagueId === null || leagueId.length === 0) {
        setLoading(false);
        setError({ type: 'info', message: 'No League selected.' });
        return;
      }
      WarpScoresApiService.leagueCompetitions(leagueId, initialized)
        .then(setCompetitions)
        .then(() => setLoading(false))
        .catch((reason) => {
          setError({ type: 'error', message: reason.toLocaleString() });
        });
    };
    if (league) fetchCompetitions(league?.uuid).then(fetchCompetitions(league?.uuid, true));
  }, [league]);

  return (
    <Stack>
      <Box>
        <Navigation currentPage="league" league={[league?.uuid, league?.name]} />
      </Box>
      {league && (
        <HeaderCard heading={league.name} detailsHeading="League details" mainImageSrc={imageUrls.logo(league.logo)}>
          <LeagueInfo league={league} />
        </HeaderCard>
      )}
      <LoadingOrErrorWrapper loading={loading} error={error}>
        <Competitions competitions={competitions} league={league} />
      </LoadingOrErrorWrapper>
      {activeCompetitionsIncludeRoundRobinOrWissenOrKnockoutTournaments && <LiveContests league={league} />}
      <LatestMatches league={league} />
    </Stack>
  );
}

export default LeaguePage;
