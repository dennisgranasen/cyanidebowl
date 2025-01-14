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
import Navigation from '../components/misc/Navigation';
import ImageUrls from '../ImageUrls';
import InfoArea from '../components/common/InfoArea';
import HeaderCard from '../components/common/HeaderCard';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import useFetchCompetition from '../hooks/useFetchCompetition';
import prettyPrint from '../util/prettyPrint';
import useFetchArenaInfo from '../hooks/useFetchArenaInfo';
import InfoItem from '../components/common/InfoItem';
import WarpScoresApiService from '../WarpScoresApiService';
import config from '../config';
import formatter from '../util/formatter';
import comparators from '../util/comparators';
import arenaHelpers from '../components/arena/arenaHelpers';
import ArenaProgress from '../components/arena/ArenaProgress';

const { boxSize } = config;

function TeamRow({ competitionUuid, arenaTeam }) {
  const navigate = useNavigate();
  const goToTeam = () => {
    navigate(`/competition/${competitionUuid}/team/${arenaTeam.teamUuid}`);
  };
  const goToCoach = () => {
    navigate(`/competition/${competitionUuid}/arena/coach/${arenaTeam.coachUuid}`);
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
      <Td
        _hover={{ textEmphasisColor: 'warpScoresHoverColor', textDecoration: 'underline' }}
        cursor="pointer"
        onClick={goToCoach}
      >
        {arenaTeam.coachName}
      </Td>
      <Td>{formatter.formatAsDate(arenaTeam.matches[0].finished, '-')}</Td>
      <Td>{formatter.formatAsDate(arenaTeam.matches[arenaTeam.matches.length - 1].finished, '-')}</Td>
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
                    <Th>Coach</Th>
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
                    <Th>Coach</Th>
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

function ArenaPage() {
  const { competitionUuid, race } = useParams();
  const { fetchCompetition, competition, competitionLoading, error: competitionError } = useFetchCompetition();
  const [arenaTeams, setArenaTeams] = useState(null);
  const [arenaTeamsLoading, setArenaTeamsLoading] = useState(false);
  const [arenaTeamsError, setArenaTeamsError] = useState(null);

  useEffect(() => {
    if (competitionUuid) fetchCompetition(competitionUuid);
  }, [competitionUuid]);

  const { fetchArenaInfo, arenaInfo, arenaInfoLoading, error: arenaError } = useFetchArenaInfo();

  useEffect(() => {
    if (race) {
      fetchArenaInfo(competitionUuid, race);
    }
  }, [race]);

  useEffect(() => {
    const fetchArenaTeams = (uuid, raceToFetch, runType) => {
      setArenaTeamsLoading(true);
      WarpScoresApiService.arenaTeams(uuid, raceToFetch, runType)
        .then((teams) => teams.sort(arenaHelpers.teamsByLastMatchDate))
        .then(setArenaTeams)
        .catch((reason) => setArenaTeamsError({ type: 'error', message: reason.toLocaleString() }))
        .finally(() => setArenaTeamsLoading(false));
    };
    if (race) {
      fetchArenaTeams(competitionUuid, race, 'completed');
    }
  }, [race]);

  return (
    <VStack align="left">
      <Box>
        <Navigation
          currentPage="race"
          league={competition ? [competition.leagueId, competition.leagueName] : []}
          competition={[competitionUuid, competition ? competition.name : '']}
          race={race}
        />
      </Box>
      <LoadingOrErrorWrapper loading={competitionLoading} error={competitionError}>
        <HeaderCard
          heading={`${competition?.name} - ${prettyPrint(race)}`}
          subHeading={<RouteLink to={`/${competition?.leagueId}`}>League: {competition?.leagueName}</RouteLink>}
          detailsHeading="Arena details"
          mainImageSrc={ImageUrls.logo(competition?.leagueLogo)}
          additionalImageSrc={ImageUrls.race(race)}
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
      <Heading>{prettyPrint(race)}</Heading>
      <Accordion variant="simple" allowMultiple defaultIndex={[0]}>
        <RunAccordionItem
          key="completed"
          label={`Completed runs (${arenaInfo?.completedRuns})`}
          loading={arenaTeamsLoading}
          error={arenaTeamsError}
          arenaTeams={arenaTeams}
          competitionUuid={competitionUuid}
        />
        <RunAccordionItem
          key="active"
          label={`Active runs (${arenaInfo?.activeRuns})`}
          loading={arenaInfoLoading}
          error={arenaError}
          competitionUuid={competitionUuid}
        />
        <RunAccordionItem
          key="failed"
          label={`Failed runs (${arenaInfo?.failedRuns})`}
          loading={arenaInfoLoading}
          error={arenaError}
          competitionUuid={competitionUuid}
        />
      </Accordion>
    </VStack>
  );
}

export default ArenaPage;
