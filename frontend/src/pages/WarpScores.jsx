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
import LeagueSystems from '../components/league/LeagueSystems';

const { showCircuitsFeature } = config;

function WarpScores() {
  const [circuits, setCircuits] = useState([]);
  const { authenticationReady, userPermissions } = useAuth0WithUserPermissions();
  const [leagueSystems, setLeagueSystems] = useState([]);
  const [leagues, setLeagues] = useState([]);
  const [competitionCountsByStatus, setCompetitionCountsByStatus] = useState({});
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

  const fetchLeagues = async () => {
    const data = await WarpScoresApiService.leagues();
    setLeagues(data);
    if (data.length > 0) {
      const counts = await WarpScoresApiService.competitionCountByStatus(data);
      setCompetitionCountsByStatus(counts);
    }
  };

  const fetchHomeData = async () => {
    setLoading(true);
    try {
      const systems = await WarpScoresApiService.publicLeagueSystems();
        if (systems.length === 0) {
          setLeagueSystems([]);
        await fetchLeagues();
        } else {
          const overviews = await Promise.all(systems.map((system) => WarpScoresApiService.leagueSystemOverview(system.id)));
          setLeagueSystems(overviews);
      }
    } catch (reason) {
      setError({ type: 'error', message: reason.toLocaleString() });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (showCircuits) {
      fetchCircuits();
    } else {
      fetchHomeData();
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
          mainImageSrc={imageUrls.blaskscoreLogoPng('medium')}
          heading="BlaskScore"
          subHeading="Blödareblaskans omutliga(?) resultatförmedlingstjänst"
        />
        <Box>
          <LoadingOrErrorWrapper loading={loading} error={error}>
            {showCircuits ? (
              circuits.map((currCircuit) => <CircuitCard mb={2} circuit={currCircuit} key={currCircuit.id} />)
            ) : leagueSystems.length > 0 ? (
              <LeagueSystems leagueSystems={leagueSystems} />
            ) : (
              <Leagues leagues={leagues} competitionCountByStatusPerLeague={competitionCountsByStatus} />
            )}
          </LoadingOrErrorWrapper>
        </Box>
      </>
    </VStack>
  );
}

export default WarpScores;
