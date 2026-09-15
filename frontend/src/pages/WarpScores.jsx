import React, { useEffect, useState } from 'react';
import { Box, VStack } from '@chakra-ui/react';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import HeaderCard from '../components/common/HeaderCard';
import imageUrls from '../imageUrls';
import Leagues from '../components/league/Leagues';
import LeagueSystems from '../components/league/LeagueSystems';
import ArticleFeed from '../components/community/ArticleFeed';
import { useIntl } from 'react-intl';
import { useSearchParams } from 'react-router-dom';

function WarpScores() {
  const intl = useIntl();
  const [searchParams] = useSearchParams();
  const requestedLeagueSystemId = searchParams.get('leagueSystem');
  const [leagueSystems, setLeagueSystems] = useState([]);
  const [selectedLeagueSystem, setSelectedLeagueSystem] = useState(null);
  const [selectedSeasonId, setSelectedSeasonId] = useState(null);
  const [leagues, setLeagues] = useState([]);
  const [competitionCountsByStatus, setCompetitionCountsByStatus] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(undefined);

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
      setLeagueSystems(systems);
      if (systems.length === 0) {
        setSelectedLeagueSystem(null);
        await fetchLeagues();
      } else {
        const initial = systems.find((system) => system.id === requestedLeagueSystemId) || systems.find((system) => system.primary) || systems[0];
        const overview = await WarpScoresApiService.leagueSystemOverview(initial.id, searchParams.get('season'));
        setSelectedLeagueSystem(overview);
        const ordered = [...(overview.seasons || [])].sort((a, b) => (b.sequence ?? b.number ?? 0) - (a.sequence ?? a.number ?? 0));
        setSelectedSeasonId(searchParams.get('season') || (ordered.find(s => (s.phases || []).some(p => (p.stages || []).some(st => st.matches?.length))) || ordered[0])?.id);
      }
    } catch (reason) {
      setError({ type: 'error', message: reason.toLocaleString() });
    } finally {
      setLoading(false);
    }
  };

  const selectLeagueSystem = async (leagueSystemId, seasonId) => {
    setLoading(true);
    setError(undefined);
    try {
      const overview = await WarpScoresApiService.leagueSystemOverview(leagueSystemId, seasonId);
      setSelectedLeagueSystem(overview);
      const ordered = [...(overview.seasons || [])].sort((a, b) => (b.sequence ?? b.number ?? 0) - (a.sequence ?? a.number ?? 0));
      setSelectedSeasonId(seasonId || (ordered.find(s => (s.phases || []).some(p => (p.stages || []).some(st => st.matches?.length))) || ordered[0])?.id);
    } catch (reason) {
      setError({ type: 'error', message: reason.toLocaleString() });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHomeData();
  }, [requestedLeagueSystemId, searchParams.get('season')]);

  return (
    <VStack align="stretch" w="full">
      <Box>
        <Navigation
          currentPage="home"
          leagueSystems={leagueSystems}
          selectedLeagueSystemId={selectedLeagueSystem?.id}
          selectedSeasonId={selectedSeasonId}
          onSelectLeagueSystem={selectLeagueSystem}
        />
      </Box>
      <>
        <HeaderCard
          mainImageSrc={imageUrls.blaskscoreLogoPng('medium')}
          heading="BlaskScore"
          subHeading={intl.formatMessage({ id: 'home.tagline' })}
        />
        <ArticleFeed leagueSystemId={selectedLeagueSystem?.id} seasonId={selectedSeasonId} limit={6} />
        <Box>
          <LoadingOrErrorWrapper loading={loading} error={error}>
            {leagueSystems.length > 0 ? (
              <LeagueSystems selectedSeasonId={selectedSeasonId} summaries={leagueSystems} leagueSystem={selectedLeagueSystem} onSelectSeason={(seasonId) => selectLeagueSystem(selectedLeagueSystem.id, seasonId)} />
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
