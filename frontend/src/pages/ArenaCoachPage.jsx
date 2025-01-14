import React, { useEffect, useState } from 'react';
import {
  Accordion,
  AccordionButton,
  AccordionItem,
  AccordionPanel,
  Box,
  Heading,
  HStack,
  Image,
  Table,
  TableContainer,
  Tbody,
  Td,
  Tfoot,
  Th,
  Thead,
  Tr,
  VStack,
} from '@chakra-ui/react';
import { Link as RouteLink, useNavigate, useParams } from 'react-router-dom';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import ImageUrls from '../ImageUrls';
import InfoArea from '../components/common/InfoArea';
import HeaderCard from '../components/common/HeaderCard';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import useFetchCompetition from '../hooks/useFetchCompetition';
import prettyPrint from '../util/prettyPrint';
import WarpScoresApiService from '../WarpScoresApiService';
import config from '../config';
import formatter from '../util/formatter';
import Navigation from '../components/misc/Navigation';
import InfoItem from '../components/common/InfoItem';
import comparators from '../util/comparators';
import arenaHelpers from '../components/arena/arenaHelpers';
import ArenaProgress from '../components/arena/ArenaProgress';

const { boxSize } = config;

function TeamRow({ competitionUuid, arenaTeam }) {
  const navigate = useNavigate();
  const goToTeam = () => {
    navigate(`/competition/${competitionUuid}/team/${arenaTeam.teamUuid}`);
  };
  const sortedMatches = [].concat(arenaTeam?.matches ?? []);
  sortedMatches.sort(comparators.compareContestsByMatchOrContestUuidAsDatesAsc);
  const clusteredSortedMatches = arenaHelpers.clusterMatches(arenaTeam.teamUuid, sortedMatches);
  return (
    <Tr>
      <Td
        _hover={{ textEmphasisColor: 'warpScoresHoverColor', textDecoration: 'underline' }}
        cursor="pointer"
        onClick={goToTeam}
      >
        <HStack>
          <Image
            src={`${ImageUrls.logo(arenaTeam?.teamLogo)}`}
            boxSize={boxSize}
            fallback={<QuestionOutlineIcon boxSize={boxSize} />}
          />
          <Box>{arenaTeam.teamName}</Box>
        </HStack>
      </Td>
      <Td>{prettyPrint(arenaTeam.race)}</Td>
      <Td>{formatter.formatAsDate(sortedMatches[0].finished)}</Td>
      <Td>{formatter.formatAsDate(sortedMatches[sortedMatches.length - 1].finished)}</Td>
      <Td>{sortedMatches.length}</Td>
      <Td>{clusteredSortedMatches.length}</Td>
      <Td>
        <HStack spacing="0.8rem">
          {clusteredSortedMatches.map((clusteredMatches) => (
            <ArenaProgress
              key={`arenaProgress-${arenaHelpers.getLastMatch(clusteredMatches).matchId}`}
              teamUuid={arenaTeam.teamUuid}
              matches={clusteredMatches}
            />
          ))}
        </HStack>
      </Td>
    </Tr>
  );
}

function RunAccordionItem({ competitionUuid, label, loading, error, arenaTeams }) {
  return (
    <AccordionItem>
      <AccordionButton>
        <Heading size="md">{label}</Heading>
      </AccordionButton>
      <AccordionPanel>
        <LoadingOrErrorWrapper loading={loading} error={error}>
          {arenaTeams ? (
            <TableContainer>
              <Table variant="striped" size="sm">
                <Thead>
                  <Tr>
                    <Th>Team</Th>
                    <Th>Race</Th>
                    <Th>First game</Th>
                    <Th>Last game</Th>
                    <Th>Games played</Th>
                    <Th>Runs</Th>
                    <Th>Progress</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {arenaTeams.map((arenaTeam) => (
                    <TeamRow key={arenaTeam.teamUuid} competitionUuid={competitionUuid} arenaTeam={arenaTeam} />
                  ))}
                </Tbody>
                <Tfoot>
                  <Tr>
                    <Th>Team</Th>
                    <Th>Race</Th>
                    <Th>First game</Th>
                    <Th>Last game</Th>
                    <Th>Games played</Th>
                    <Th>Runs</Th>
                    <Th>Progress</Th>
                  </Tr>
                </Tfoot>
              </Table>
            </TableContainer>
          ) : (
            'Not yet available...'
          )}
        </LoadingOrErrorWrapper>
      </AccordionPanel>
    </AccordionItem>
  );
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
        .then((teams) => {
          teams.sort((team1, team2) => {
            const comparison = team1.race.localeCompare(team2.race);
            return comparison || arenaHelpers.teamsByLastMatchDate(team1, team2);
          });
          setArenaCoachTeams(teams);
        })
        .catch((reason) => setArenaCoachTeamsError({ type: 'error', message: reason.toLocaleString() }))
        .finally(() => setArenaCoachTeamsLoading(false));
    };
    fetchArenaCoachTeams(competitionUuid, coachUuid);
  }, []);

  const coachName = arenaCoachTeams ? arenaCoachTeams[0]?.coachName : null;
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
          mainImageSrc={ImageUrls.logo(competition?.leagueLogo)}
        >
          <LoadingOrErrorWrapper loading={arenaCoachTeamsLoading} error={arenaCoachTeamsError}>
            <InfoArea>
              <InfoItem
                key="playedRaces"
                label="Played races"
                info={new Set(arenaCoachTeams?.map((arenaTeam) => arenaTeam.race)).size}
              />
              <InfoItem
                key="playedMatches"
                label="Played matches"
                info={arenaCoachTeams?.map((arenaTeam) => arenaTeam.matches.length).reduce((a, b) => a + b, 0)}
              />
              <InfoItem key="completedRuns" label="Completed runs" info="-" />
              <InfoItem key="failedRuns" label="Failed runs" info="-" />
              <InfoItem key="activeRuns" label="Active runs" info="-" />
            </InfoArea>
          </LoadingOrErrorWrapper>
        </HeaderCard>
      </LoadingOrErrorWrapper>
      <Heading>Coach</Heading>
      <LoadingOrErrorWrapper loading={arenaCoachTeamsLoading} error={arenaCoachTeamsError}>
        <Accordion defaultIndex={[0]}>
          <RunAccordionItem
            competitionUuid={competitionUuid}
            label="Teams"
            loading={arenaCoachTeamsLoading}
            error={arenaCoachTeamsError}
            arenaTeams={arenaCoachTeams}
          />
        </Accordion>
      </LoadingOrErrorWrapper>
    </VStack>
  );
}

export default ArenaCoachPage;
