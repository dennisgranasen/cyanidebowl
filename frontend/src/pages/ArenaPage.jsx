import React, { useEffect, useState } from 'react';
import {
  Accordion,
  AccordionButton,
  AccordionItem,
  AccordionPanel,
  Box,
  Center,
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
import prettyPrint from '../util/PrettyPrint';
import useFetchArenaInfo from '../hooks/useFetchArenaInfo';
import InfoItem from '../components/common/InfoItem';
import WarpScoresApiService from '../WarpScoresApiService';
import config from '../config';
import DelayedIconTooltip from '../components/common/DelayedIconTooltip';
import Formatter from '../util/Formatter';
import comparators from '../util/Comparators';

const { boxSize } = config;

function ContestLabel({ teamUuid, contest }) {
  const opponent = contest.opponents.find((opp) => opp.id !== teamUuid);
  return (
    <VStack align="left">
      <HStack>
        <Image
          src={`${ImageUrls.logo(opponent.logo)}`}
          boxSize={boxSize}
          fallback={<QuestionOutlineIcon boxSize={boxSize} />}
        />
        <Heading>{opponent.name}</Heading>
      </HStack>
      <VStack align="left">
        <Box>Coach: {opponent.coachName}</Box>
        <Box>Race: {prettyPrint(opponent.race)}</Box>
        <Box>Played: {Formatter.formatAsDate(contest.matchDate)}</Box>
      </VStack>
    </VStack>
  );
}

function getLastContest(contests) {
  const sortedContests = [].concat(contests.sort(comparators.compareContestsByMatchOrContestUuidAsDatesAsc));
  return sortedContests.pop();
}

const teamByLastMatchDate = (team1, team2) => {
  const contest1 = getLastContest(team1.contests);
  const contest2 = getLastContest(team2.contests);
  return comparators.compareContestsByMatchOrContestUuidAsDatesAsc(contest1, contest2);
};

function Score({ contest, teamUuid }) {
  const team = contest.opponents.find((opp) => opp.id === teamUuid);
  const opponent = contest.opponents.find((opp) => opp.id !== teamUuid);
  const teamScore = team?.score ?? 0;
  const opponentScore = opponent?.score ?? 0;
  return (
    <Center height={boxSize} width={boxSize}>
      {teamScore}:{opponentScore}
    </Center>
  );
}

function ArenaContest({ contest, teamUuid }) {
  const win = teamUuid === contest?.winner?.team?.id;
  return (
    <DelayedIconTooltip label={<ContestLabel teamUuid={teamUuid} contest={contest} />}>
      <Box
        borderRadius="0.5rem"
        boxSize={boxSize}
        background={win ? 'green.500' : 'red.500'}
        fontFamily="EmbeddedBigStarRegular"
        height={boxSize}
      >
        <Score teamUuid={teamUuid} contest={contest} />
      </Box>
    </DelayedIconTooltip>
  );
}

function isWinner(teamUuid, contest) {
  return teamUuid === contest.winner?.team?.id;
}

const clusterContests = (teamUuid, contests) => {
  let wins = 0;
  let losses = 0;
  const clusteredContests = [];
  let currContests = [];
  contests.forEach((contest) => {
    currContests.push(contest);
    if (isWinner(teamUuid, contest)) {
      wins += 1;
    } else {
      losses += 1;
    }
    if (wins === 7 || losses === 2) {
      clusteredContests.push(currContests);
      wins = 0;
      losses = 0;
      currContests = [];
    }
  });
  return clusteredContests.sort((c1, c2) =>
    comparators.compareContestsByMatchOrContestUuidAsDatesAsc(getLastContest(c1), getLastContest(c2))
  );
};

function ArenaProgress({ contests, teamUuid }) {
  return (
    <HStack height={boxSize} spacing="0.2rem">
      {contests?.map((contest) => (
        <ArenaContest key={`arenaContest-${contest.contestUuid}`} teamUuid={teamUuid} contest={contest} />
      ))}
    </HStack>
  );
}

function TeamRow({ competitionUuid, arenaTeam }) {
  const navigate = useNavigate();
  const goToTeam = () => {
    navigate(`/competition/${competitionUuid}/team/${arenaTeam.teamUuid}`);
  };
  const goToCoach = () => {
    navigate(`/competition/${competitionUuid}/arena/coach/${arenaTeam.coachUuid}`);
  };
  const sortedContests = [].concat(arenaTeam?.contests ?? []);
  sortedContests.sort(comparators.compareContestsByMatchOrContestUuidAsDatesAsc);
  const clusteredSortedContests = clusterContests(arenaTeam.teamUuid, sortedContests);
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
      <Td>{Formatter.formatAsDate(arenaTeam.contests[0].matchDate, '-')}</Td>
      <Td>{Formatter.formatAsDate(arenaTeam.contests[arenaTeam.contests.length - 1].matchDate, '-')}</Td>
      <Td>{sortedContests.length}</Td>
      <Td>{clusteredSortedContests.length}</Td>
      <Td>
        <HStack spacing="0.8rem">
          {clusteredSortedContests.map((clusteredContests) => (
            <ArenaProgress
              key={`arenaProgress-${getLastContest(clusteredContests).contestUuid}`}
              teamUuid={arenaTeam.teamUuid}
              contests={clusteredContests}
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
        .then((teams) => teams.sort(teamByLastMatchDate))
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
