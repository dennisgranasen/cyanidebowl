import React, { useEffect, useState } from 'react';
import { Box, Heading, VStack } from '@chakra-ui/react';
import WarpScoresApiService from '../WarpScoresApiService';
import config from '../config';
import Navigation from '../components/misc/Navigation';
import LeagueCard from '../components/league/LeagueCard';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import HeaderCard from '../components/common/HeaderCard';
import ImageUrls from '../ImageUrls';

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
    <VStack align="left">
      <Box>
        <Navigation currentPage="home" />
      </Box>
      <>
        <HeaderCard
          mainImageSrc={ImageUrls.warpscoresLogoPng('medium')}
          heading="Warp-Scores"
          subHeading="Welcome to warp-scores, a Spike-like facade to BB3 data provided by Cyanide's BB3-API."
        />
        <Box>
          <Heading size="md">Leagues</Heading>
          <LoadingOrErrorWrapper loading={loading} error={error}>
            {leagues.map((currLeague) => (
              <LeagueCard mb={2} league={currLeague} key={currLeague.uuid} />
            ))}
          </LoadingOrErrorWrapper>
        </Box>
      </>
    </VStack>
  );
}

export default WarpScores;
