import React, { useEffect, useState } from 'react';
import { Box, VStack } from '@chakra-ui/react';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import HeaderCard from '../components/common/HeaderCard';
import imageUrls from '../imageUrls';
import CircuitCard from '../components/circuit/CircuitCard';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import config from '../config';
import Leagues from '../components/league/Leagues';

const { showCircuitsFeature } = config;

function WarpScores() {
  const [circuits, setCircuits] = useState([]);
  const { authenticationReady, userPermissions } = useAuth0WithUserPermissions();
  const [leagues, setLeagues] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showCircuits, setShowCircuits] = useState(false);
  const [error, setError] = useState(undefined);

  const fetchCircuits = () => {
    setLoading(true);
    WarpScoresApiService.circuits()
      .then((data) => {
        setCircuits(data);
      })
      .then(() => setLoading(false))
      .catch((reason) => {
        setError({ type: 'error', message: reason.toLocaleString() });
      });
  };

  const fetchLeagues = () => {
    setLoading(true);
    WarpScoresApiService.leagues()
      .then((data) => {
        setLeagues(data);
      })
      .then(() => setLoading(false))
      .catch((reason) => {
        setError({ type: 'error', message: reason.toLocaleString() });
      });
  };

  useEffect(() => {
    if (showCircuits) {
      fetchCircuits();
    } else {
      fetchLeagues();
    }
  }, [showCircuits]);

  useEffect(() => {
    setShowCircuits(showCircuitsFeature && authenticationReady && userPermissions.writeLeagueAdmin);
  }, [authenticationReady, userPermissions]);

  return (
    <VStack align="left">
      <Box>
        <Navigation currentPage="home" />
      </Box>
      <>
        <HeaderCard
          mainImageSrc={imageUrls.warpscoresLogoPng('medium')}
          heading="Warp-Scores"
          subHeading="Welcome to warp-scores, a Spike-like facade to BB3 data provided by Cyanide's BB3-API."
        />
        <Box>
          <LoadingOrErrorWrapper loading={loading} error={error}>
            {showCircuits ? (
              circuits.map((currCircuit) => <CircuitCard mb={2} circuit={currCircuit} key={currCircuit.id} />)
            ) : (
              <Leagues leagues={leagues} />
            )}
          </LoadingOrErrorWrapper>
        </Box>
      </>
    </VStack>
  );
}

export default WarpScores;
