import React, { useEffect, useState } from 'react';
import { Accordion, Box, Heading, VStack } from '@chakra-ui/react';
import { Link as RouteLink, useParams } from 'react-router-dom';
import imageUrls from '../imageUrls';
import InfoArea from '../components/common/InfoArea';
import HeaderCard from '../components/common/HeaderCard';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import useFetchCompetition from '../hooks/useFetchCompetition';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import InfoItem from '../components/common/InfoItem';
import ArenaRunAccordionItem from '../components/arena/ArenaRunAccordionItem';

function getAllArenaTeams(arenaCoachTeamsByRunType) {
  if (!arenaCoachTeamsByRunType) return [];
  return Object.values(arenaCoachTeamsByRunType).reduce((acc, arenaTeams) => acc.concat(arenaTeams), []);
}

function getCoachNameFrom(arenaCoachTeamsByRunType) {
  const coachNames = getAllArenaTeams(arenaCoachTeamsByRunType).map((arenaTeam) => arenaTeam.coachName);
  return coachNames[0] || null;
}

function getDistinctRaces(arenaCoachTeamsByRunType) {
  const distinctRaces = new Set(getAllArenaTeams(arenaCoachTeamsByRunType).map((arenaTeam) => arenaTeam.race));
  return distinctRaces.size;
}

function getPlayedMatchesCount(arenaCoachTeamsByRunType) {
  return getAllArenaTeams(arenaCoachTeamsByRunType)
    .map((arenaTeam) => arenaTeam.matches.length)
    .reduce((acc, count) => acc + count, 0);
}

function ArenaCoachPage() {
  const { competitionUuid, coachUuid } = useParams();
  const { fetchCompetition, competition, competitionLoading, error: competitionError } = useFetchCompetition();
  const [arenaCoachTeams, setArenaCoachTeams] = useState(null);
  const [arenaCoachTeamsLoading, setArenaCoachTeamsLoading] = useState(false);
  const [arenaCoachTeamsError, setArenaCoachTeamsError] = useState(null);

  useEffect(() => {
    if (competitionUuid) fetchCompetition(competitionUuid);
  }, [competitionUuid]);

  useEffect(() => {
    const fetchArenaCoachTeams = (competitionId, coachId) => {
      setArenaCoachTeamsLoading(true);
      WarpScoresApiService.arenaCoachTeams(competitionId, coachId)
        .then((teamsByRunType) => {
          setArenaCoachTeams(teamsByRunType);
        })
        .catch((reason) => setArenaCoachTeamsError({ type: 'error', message: reason.toLocaleString() }))
        .finally(() => setArenaCoachTeamsLoading(false));
    };
    fetchArenaCoachTeams(competitionUuid, coachUuid);
  }, []);

  const coachName = getCoachNameFrom(arenaCoachTeams);
  const activeRunsCount = arenaCoachTeams ? arenaCoachTeams.active?.length ?? '0' : '-';
  const completedRunsCount = arenaCoachTeams ? arenaCoachTeams.completed?.length ?? '0' : '-';
  const failedRunsCount = arenaCoachTeams ? arenaCoachTeams.failed?.length ?? '0' : '-';
  return (
    <VStack align="left">
      <Box>
        <Navigation
          currentPage="coach"
          league={competition ? [competition.leagueId, competition.leagueName] : []}
          competition={[competitionUuid, competition ? competition.name : '']}
          coach={coachName}
        />
      </Box>
      <LoadingOrErrorWrapper loading={competitionLoading} error={competitionError}>
        <HeaderCard
          heading={`${competition?.name}${coachName ? ` - ${coachName}` : ''}`}
          subHeading={<RouteLink to={`/${competition?.leagueId}`}>League: {competition?.leagueName}</RouteLink>}
          detailsHeading="Arena Coach Details"
          mainImageSrc={competition?.logo ? imageUrls.logo(competition?.logo) : imageUrls.logo(competition?.leagueLogo)}
        >
          <LoadingOrErrorWrapper loading={arenaCoachTeamsLoading} error={arenaCoachTeamsError}>
            <InfoArea>
              <InfoItem key="playedRaces" label="Played races" info={getDistinctRaces(arenaCoachTeams)} />
              <InfoItem key="playedMatches" label="Played matches" info={getPlayedMatchesCount(arenaCoachTeams)} />
              <InfoItem key="completedRuns" label="Completed runs" info={completedRunsCount} />
              <InfoItem key="failedRuns" label="Failed runs" info={failedRunsCount} />
              <InfoItem key="activeRuns" label="Active runs" info={activeRunsCount} />
            </InfoArea>
          </LoadingOrErrorWrapper>
        </HeaderCard>
      </LoadingOrErrorWrapper>
      <Heading>Coach</Heading>
      <LoadingOrErrorWrapper loading={arenaCoachTeamsLoading} error={arenaCoachTeamsError}>
        <Accordion defaultIndex={[0]}>
          <ArenaRunAccordionItem
            key="completed"
            label={`Completed runs (${completedRunsCount})`}
            competitionUuid={competitionUuid}
            loading={arenaCoachTeamsLoading}
            error={arenaCoachTeamsError}
            arenaTeams={arenaCoachTeams?.completed ?? []}
            coachOrRace="Race"
          />
          <ArenaRunAccordionItem
            key="active"
            label={`Active runs (${activeRunsCount})`}
            competitionUuid={competitionUuid}
            loading={arenaCoachTeamsLoading}
            error={arenaCoachTeamsError}
            arenaTeams={arenaCoachTeams?.active ?? []}
            coachOrRace="Race"
          />
          <ArenaRunAccordionItem
            key="failed"
            label={`Failed runs (${failedRunsCount})`}
            competitionUuid={competitionUuid}
            loading={arenaCoachTeamsLoading}
            error={arenaCoachTeamsError}
            arenaTeams={arenaCoachTeams?.failed ?? []}
            coachOrRace="Race"
          />
        </Accordion>
      </LoadingOrErrorWrapper>
    </VStack>
  );
}

export default ArenaCoachPage;
