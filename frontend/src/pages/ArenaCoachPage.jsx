import React, { useEffect, useState } from 'react';
import { Accordion, Box, VStack } from '@chakra-ui/react';
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
  const activeTeamsCount = arenaCoachTeams ? arenaCoachTeams.active?.length ?? '0' : '-';
  const completedTeamsCount = arenaCoachTeams ? arenaCoachTeams.completed?.length ?? '0' : '-';
  const failedTeamsCount = arenaCoachTeams ? arenaCoachTeams.failed?.length ?? '0' : '-';
  const activeRacesCount = arenaCoachTeams
    ? new Set(arenaCoachTeams.active?.map((team) => team.race)).size ?? '0'
    : '-';
  const completedRacesCount = arenaCoachTeams
    ? new Set(arenaCoachTeams.completed?.map((team) => team.race)).size ?? '0'
    : '-';
  const failedRacesCount = arenaCoachTeams
    ? new Set(arenaCoachTeams.failed?.map((team) => team.race)).size ?? '0'
    : '-';
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
          heading={`Coach: ${coachName ? `${coachName}` : ''}`}
          subHeading={<RouteLink to={`/competition/${competitionUuid}`}>Competition: {competition?.name}</RouteLink>}
          detailsHeading="Arena Coach Details"
          mainImageSrc={competition?.logo ? imageUrls.logo(competition?.logo) : imageUrls.logo(competition?.leagueLogo)}
        >
          <LoadingOrErrorWrapper loading={arenaCoachTeamsLoading} error={arenaCoachTeamsError}>
            <InfoArea>
              <InfoItem key="playedRaces" label="Played races" info={getDistinctRaces(arenaCoachTeams)} />
              <InfoItem key="playedMatches" label="Played matches" info={getPlayedMatchesCount(arenaCoachTeams)} />
              <InfoItem
                key="completedRuns"
                label="Completed"
                info={`${completedTeamsCount} Teams, ${completedRacesCount} Races`}
              />
              <InfoItem key="failedRuns" label="Failed" info={`${failedTeamsCount} Teams, ${failedRacesCount} Races`} />
              <InfoItem key="activeRuns" label="Active" info={`${activeTeamsCount} Teams, ${activeRacesCount} Races`} />
            </InfoArea>
          </LoadingOrErrorWrapper>
        </HeaderCard>
      </LoadingOrErrorWrapper>
      <LoadingOrErrorWrapper loading={arenaCoachTeamsLoading} error={arenaCoachTeamsError}>
        <Accordion defaultIndex={[0]}>
          <ArenaRunAccordionItem
            key="completed"
            label={`Completed teams (${completedTeamsCount})`}
            competitionUuid={competitionUuid}
            loading={arenaCoachTeamsLoading}
            error={arenaCoachTeamsError}
            arenaTeams={arenaCoachTeams?.completed ?? []}
            coachOrRace="Race"
          />
          <ArenaRunAccordionItem
            key="active"
            label={`Active teams (${activeTeamsCount})`}
            competitionUuid={competitionUuid}
            loading={arenaCoachTeamsLoading}
            error={arenaCoachTeamsError}
            arenaTeams={arenaCoachTeams?.active ?? []}
            coachOrRace="Race"
          />
          <ArenaRunAccordionItem
            key="failed"
            label={`Failed teams (${failedTeamsCount})`}
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
