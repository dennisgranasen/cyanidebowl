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
import ImageUrls from '../ImageUrls';
import InfoArea from '../components/common/InfoArea';
import HeaderCard from '../components/common/HeaderCard';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import useFetchCompetition from '../hooks/useFetchCompetition';
import prettyPrint from '../util/PrettyPrint';
import WarpScoresApiService from '../WarpScoresApiService';
import config from '../config';
import DelayedIconTooltip from '../components/common/DelayedIconTooltip';
import Formatter from '../util/Formatter';
import formatter from '../util/Formatter';
import Navigation from '../components/misc/Navigation';
import InfoItem from '../components/common/InfoItem';
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
  const sortedContests = [].concat(contests);
  sortedContests.sort(comparators.compareContestsByMatchOrContestUuidAsDatesAsc);
  return sortedContests.pop();
}

const teamsByLastMatchDateDesc = (team1, team2) => {
  const contest1 = getLastContest(team1.contests);
  const contest2 = getLastContest(team2.contests);
  return comparators.compareContestsByMatchOrContestUuidAsDatesDesc(contest1, contest2);
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
  if (currContests.length > 0 && (wins > 0 || losses > 0)) clusteredContests.push(currContests);
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
  const sortedContests = [].concat(arenaTeam?.contests ?? []);
  sortedContests.sort((c1, c2) => {
    return comparators.compareContestsByMatchOrContestUuidAsDatesAsc(c1, c2);
  });
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
      <Td>{prettyPrint(arenaTeam.race)}</Td>
      <Td>{formatter.formatAsDate(sortedContests[0].matchDate)}</Td>
      <Td>{formatter.formatAsDate(sortedContests[sortedContests.length - 1].matchDate)}</Td>
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
            return comparison || teamsByLastMatchDateDesc(team1, team2);
          });
          setArenaCoachTeams(teams);
        })
        .catch((reason) => setArenaCoachTeamsError({ type: 'error', message: reason.toLocaleString() }))
        .finally(() => setArenaCoachTeamsLoading(false));
    };
    fetchArenaCoachTeams(competitionUuid, coachUuid);
  }, []);

  const coachName = arenaCoachTeams ? arenaCoachTeams[0].coachName : null;
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
                info={arenaCoachTeams?.map((arenaTeam) => arenaTeam.contests.length).reduce((a, b) => a + b, 0)}
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
