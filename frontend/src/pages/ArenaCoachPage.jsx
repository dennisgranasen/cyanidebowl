import React, { useEffect, useState } from 'react';
import {
  Accordion,
  AccordionButton,
  AccordionIcon,
  AccordionItem,
  AccordionPanel,
  Box,
  Card,
  CardBody,
  Center,
  Heading,
  SimpleGrid,
  Text,
  VStack,
} from '@chakra-ui/react';
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
import WinRate from '../components/common/WinRate';
import Race from '../components/common/Race';
import prettyPrint from '../util/prettyPrint';
import { identityUtils } from '../util/identityUtil';
import { useIntl } from 'react-intl';

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

function WinRateCard({ race, winRate }) {
  return (
    <Card direction="row" overflow="hidden" align="center">
      <Center width="80px">
        <Race asAvatar race={race} size="lg" />
      </Center>
      <CardBody p={2} height="100%">
        <VStack align="left">
          <Heading size="md">{prettyPrint(race)}</Heading>
          <Text>
            <WinRate identifier="" winRate={winRate} />
          </Text>
        </VStack>
      </CardBody>
    </Card>
  );
}

function ArenaCoachPage() {
  const intl = useIntl();
  const { competitionId, coachId } = useParams();
  const { fetchCompetition, competition, competitionLoading, error: competitionError } = useFetchCompetition();
  const [arenaCoach, setArenaCoach] = useState(null);
  const [arenaCoachTeams, setArenaCoachTeams] = useState(null);
  const [arenaCoachTeamsLoading, setArenaCoachTeamsLoading] = useState(false);
  const [arenaCoachTeamsError, setArenaCoachTeamsError] = useState(null);

  useEffect(() => {
    if (competitionId) fetchCompetition(competitionId);
  }, [competitionId]);

  useEffect(() => {
    const fetchArenaCoachTeams = (competitionId, coachId) => {
      setArenaCoachTeamsLoading(true);
      WarpScoresApiService.arenaCoachTeams(competitionId, coachId)
        .then((data) => {
          setArenaCoach(data.arenaCoach);
          setArenaCoachTeams(data.arenaTeams);
        })
        .catch((reason) => setArenaCoachTeamsError({ type: 'error', message: reason.toLocaleString() }))
        .finally(() => setArenaCoachTeamsLoading(false));
    };
    fetchArenaCoachTeams(competitionId, coachId);
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
          league={competition ? [competition.leagueId.key, competition.leagueName] : []}
          competition={[competitionId, competition ? competition.name : '']}
          coach={coachName}
        />
      </Box>
      <LoadingOrErrorWrapper loading={competitionLoading} error={competitionError}>
        <HeaderCard
          heading={intl.formatMessage({ id: 'team.coach' }, { name: coachName || '' })}
          subHeading={<RouteLink to={`/competition/${competitionId}`}>{intl.formatMessage({ id: 'arena.competition' }, { name: competition?.name })}</RouteLink>}
          detailsHeading={intl.formatMessage({ id: 'arena.coachDetails' })}
          mainImageSrc={competition?.logo ? imageUrls.logo(competition?.logo, competition?.id?.opus) : imageUrls.logo(competition?.leagueLogo, identityUtils.opus(competitionId))}
        >
          <LoadingOrErrorWrapper loading={arenaCoachTeamsLoading} error={arenaCoachTeamsError}>
            <InfoArea>
              <InfoItem key="playedRaces" label={intl.formatMessage({ id: 'arena.playedRaces' })} info={getDistinctRaces(arenaCoachTeams)} />
              <InfoItem key="playedMatches" label={intl.formatMessage({ id: 'arena.playedMatches' })} info={getPlayedMatchesCount(arenaCoachTeams)} />
              <InfoItem
                key="completedRuns"
                label={intl.formatMessage({ id: 'arena.completed' })}
                info={intl.formatMessage({ id: 'arena.teamsRaces' }, { teams: completedTeamsCount, races: completedRacesCount })}
              />
              <InfoItem key="failedRuns" label={intl.formatMessage({ id: 'arena.failed' })} info={intl.formatMessage({ id: 'arena.teamsRaces' }, { teams: failedTeamsCount, races: failedRacesCount })} />
              <InfoItem key="activeRuns" label={intl.formatMessage({ id: 'arena.active' })} info={intl.formatMessage({ id: 'arena.teamsRaces' }, { teams: activeTeamsCount, races: activeRacesCount })} />
              <InfoItem
                key="winRate"
                label={intl.formatMessage({ id: 'arena.winRateOverall' })}
                info={<WinRate winRate={arenaCoach?.overallWinRate} />}
              />
            </InfoArea>
          </LoadingOrErrorWrapper>
        </HeaderCard>
      </LoadingOrErrorWrapper>
      <LoadingOrErrorWrapper loading={arenaCoachTeamsLoading} error={arenaCoachTeamsError}>
        <Accordion defaultIndex={arenaCoach && arenaCoach.winRateByRace ? [0, 1] : [0]} allowMultiple>
          {arenaCoach && arenaCoach.winRateByRace && (
            <AccordionItem>
              <AccordionButton>
                <Box as="span" flex="1" textAlign="left">
                  <Heading size="md">{intl.formatMessage({ id: 'arena.winRates' })}</Heading>
                </Box>
                <AccordionIcon />
              </AccordionButton>
              <AccordionPanel>
                <SimpleGrid columns={{ lg: 5, md: 4, sm: 3, base: 2 }} spacing="1.25rem">
                  {Object.keys(arenaCoach.winRateByRace)
                    .toSorted((r1, r2) => r1.localeCompare(r2))
                    .map((race) => (
                      <WinRateCard key={race} race={race} winRate={arenaCoach.winRateByRace[race]} />
                    ))}
                </SimpleGrid>
              </AccordionPanel>
            </AccordionItem>
          )}
          <ArenaRunAccordionItem
            key="completed"
            label={intl.formatMessage({ id: 'arena.completedTeams' }, { count: completedTeamsCount })}
            competitionId={competitionId}
            loading={arenaCoachTeamsLoading}
            error={arenaCoachTeamsError}
            arenaTeams={arenaCoachTeams?.completed ?? []}
            coachOrRace="Race"
          />
          <ArenaRunAccordionItem
            key="active"
            label={intl.formatMessage({ id: 'arena.activeTeams' }, { count: activeTeamsCount })}
            competitionId={competitionId}
            loading={arenaCoachTeamsLoading}
            error={arenaCoachTeamsError}
            arenaTeams={arenaCoachTeams?.active ?? []}
            coachOrRace="Race"
          />
          <ArenaRunAccordionItem
            key="failed"
            label={intl.formatMessage({ id: 'arena.failedTeams' }, { count: failedTeamsCount })}
            competitionId={competitionId}
            loading={arenaCoachTeamsLoading}
            error={arenaCoachTeamsError}
            arenaTeams={arenaCoachTeams?.failed ?? []}
            coachOrRace="Race"
          />
        </Accordion>
      </LoadingOrErrorWrapper>
      ;
    </VStack>
  );
}

export default ArenaCoachPage;
