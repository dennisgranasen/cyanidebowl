import React, { useEffect, useState } from 'react';
import { Box, Stack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import Competitions from '../components/competition/Competitions';
import imageUrls from '../imageUrls';
import HeaderCard from '../components/common/HeaderCard';
import LiveContests from '../components/contest/LiveContests';
import LatestContests from '../components/contest/LatestContests';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import LeagueInfo from '../components/league/LeagueInfo';

function LeaguePage() {
  const { leagueUuid } = useParams();
  const [competitions, setCompetitions] = useState([]);
  const [league, setLeague] = useState();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(undefined);

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
    const fetchCompetitions = (leagueId) => {
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
    const leagueId = league && league !== null ? league.uuid : null;
    fetchCompetitions(leagueId);
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
        <Competitions competitions={competitions} />
        <LiveContests league={league} />
        <LatestContests league={league} />
      </LoadingOrErrorWrapper>
    </Stack>
  );
}

export default LeaguePage;
