import React, { useEffect, useState } from 'react';
import { Box, Heading, VStack } from '@chakra-ui/react';
import WarpScoresApiService from '../WarpScoresApiService';
import config from '../config';
import Navigation from '../components/misc/Navigation';
import LeagueCard from '../components/league/LeagueCard';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import HeaderCard from '../components/common/HeaderCard';
import ImageUrls from '../ImageUrls';
import CircuitCard from '../components/circuit/CircuitCard';

function WarpScores() {
  const [circuits, setCircuits] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(undefined);

  useEffect(() => {
    const fetchCircuits = () => {
      setLoading(true);
      WarpScoresApiService.circuits()
        .then((data) => {
          setCircuits(data);
        })
        .then(() => setLoading(false))
        .catch((reason) => {
          setError({ type: 'error', message: reason.toLocaleString(config.locale) });
        });
    };
    fetchCircuits();
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
          <Heading size="md">Circuits</Heading>
          <LoadingOrErrorWrapper loading={loading} error={error}>
            {circuits.map((currCircuit) => (
              <CircuitCard mb={2} circuit={currCircuit} key={currCircuit.id} />
            ))}
          </LoadingOrErrorWrapper>
        </Box>
      </>
    </VStack>
  );
}

export default WarpScores;
