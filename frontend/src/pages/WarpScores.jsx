import React, { useEffect, useState } from 'react';
import { Box, Heading, Stack } from '@chakra-ui/react';
import WarpScoresApiService from '../WarpScoresApiService';
import config from '../config';
import Navigation from '../components/misc/Navigation';
import LeagueCard from '../components/league/LeagueCard';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';

function WarpScores() {
  const [leagues, setLeagues] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(undefined);

  useEffect(() => {
    const fetchLeagues = () => {
      setLoading(true);
      WarpScoresApiService.league()
        .then((data) => {
          setLeagues(data);
        })
        .then(() => setLoading(false))
        .catch((reason) => {
          setError(reason.toLocaleString(config.locale));
        });
    };
    fetchLeagues();
  }, []);

  return (
    <Stack>
      <Box>
        <Navigation currentPage="home" />
      </Box>
      <Box w="100%">
        <Heading>Warp-Scores</Heading>
        <Heading size="md">Leagues</Heading>
        <LoadingOrErrorWrapper loading={loading} error={error}>
          {leagues.map((currLeague) => (
            <LeagueCard mb={2} league={currLeague} key={currLeague.uuid} />
          ))}
        </LoadingOrErrorWrapper>
      </Box>
    </Stack>
  );
}

export default WarpScores;
