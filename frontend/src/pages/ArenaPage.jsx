import React, { useEffect, useState } from 'react';
import { Accordion, Box, VStack } from '@chakra-ui/react';
import { Link as RouteLink, useParams } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import imageUrls from '../imageUrls';
import InfoArea from '../components/common/InfoArea';
import HeaderCard from '../components/common/HeaderCard';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import useFetchCompetition from '../hooks/useFetchCompetition';
import prettyPrint from '../util/prettyPrint';
import useFetchArenaInfo from '../hooks/useFetchArenaInfo';
import InfoItem from '../components/common/InfoItem';
import WarpScoresApiService from '../WarpScoresApiService';
import ArenaRunAccordionItem from '../components/arena/ArenaRunAccordionItem';
import { use } from 'react';

function ArenaPage() {
  const { competitionId, race } = useParams();
  const { fetchCompetition, competition, competitionLoading, error: competitionError } = useFetchCompetition();
  const [arenaTeams, setArenaTeams] = useState(null);
  const [arenaTeamsLoading, setArenaTeamsLoading] = useState(false);
  const [arenaTeamsError, setArenaTeamsError] = useState(null);

  
  useEffect(() => {
    if (competitionId) fetchCompetition(competitionId);
  }, [competitionId]);

  const { fetchArenaInfo, arenaInfo, arenaInfoLoading, error: arenaError } = useFetchArenaInfo();

  useEffect(() => {
    if (race) {
      fetchArenaInfo(competitionId, race);
    }
  }, [race]);

  useEffect(() => {
    const fetchArenaTeams = (id, raceToFetch, runType) => {
      setArenaTeamsLoading(true);
      WarpScoresApiService.arenaTeams(id, raceToFetch, runType)
        .then(setArenaTeams)
        .catch((reason) => setArenaTeamsError({ type: 'error', message: reason.toLocaleString() }))
        .finally(() => setArenaTeamsLoading(false));
    };
    if (race) {
      fetchArenaTeams(competitionId, race, 'completed');
    }
  }, [race]);

  return (
    <VStack align="left">
      <Box>
        <Navigation
          currentPage="race"
          league={competition ? [competition.leagueId, competition.leagueName] : []}
          competition={[competitionId, competition ? competition.name : '']}
          race={race}
        />
      </Box>
      <LoadingOrErrorWrapper loading={competitionLoading} error={competitionError}>
        <HeaderCard
          heading={`${competition?.name} - ${prettyPrint(race)}`}
          subHeading={<RouteLink to={`/${competition?.leagueId}`}>League: {competition?.leagueName}</RouteLink>}
          detailsHeading="Arena details"
          mainImageSrc={competition?.logo ? imageUrls.logo(competition?.logo, competition?.id?.opus) : imageUrls.logo(competition?.leagueLogo, competition?.id?.opus)}
          additionalImageSrc={imageUrls.race(race, competition?.id?.opus)}
        >
          <LoadingOrErrorWrapper loading={arenaInfoLoading} error={arenaError}>
            <InfoArea>
              <InfoItem key="Coaches" label="Coaches" info={arenaInfo?.coaches} />
              <InfoItem key="Teams" label="Teams" info={arenaInfo?.teams} />
              <InfoItem key="Active" label="Active runs" info={arenaInfo?.activeRuns} />
              <InfoItem key="Completed" label="Completed runs" info={arenaInfo?.completedRuns} />
              <InfoItem key="Failed" label="Failed runs" info={arenaInfo?.failedRuns} />
            </InfoArea>
          </LoadingOrErrorWrapper>
        </HeaderCard>
      </LoadingOrErrorWrapper>
      <Accordion variant="simple" allowMultiple defaultIndex={[0]}>
        <ArenaRunAccordionItem
          key="completed"
          label={`Completed runs (${arenaInfo?.completedRuns})`}
          loading={arenaTeamsLoading}
          error={arenaTeamsError}
          arenaTeams={arenaTeams?.completed ?? null}
          competitionId={competitionId}
          coachOrRace="Coach"
        />
        <ArenaRunAccordionItem
          key="active"
          label={`Active runs (${arenaInfo?.activeRuns})`}
          loading={arenaInfoLoading}
          error={arenaError}
          competitionId={competitionId}
          arenaTeams={arenaTeams?.active ?? null}
          coachOrRace="Coach"
        />
        <ArenaRunAccordionItem
          key="failed"
          label={`Failed runs (${arenaInfo?.failedRuns})`}
          loading={arenaInfoLoading}
          error={arenaError}
          competitionId={competitionId}
          arenaTeams={arenaTeams?.failed ?? null}
          coachOrRace="Coach"
        />
      </Accordion>
    </VStack>
  );
}

export default ArenaPage;
