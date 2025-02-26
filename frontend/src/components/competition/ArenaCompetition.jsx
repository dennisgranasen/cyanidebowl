import {
  Box,
  Card,
  CardBody,
  Center,
  Heading,
  Image,
  SimpleGrid,
  Text,
  VStack,
  Wrap,
  WrapItem,
} from '@chakra-ui/react';
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Icon } from '@chakra-ui/icons';
import { BiSolidTrophy, BiTrophy } from 'react-icons/bi';
import LiveContests from '../contest/LiveContests';
import LatestContests from '../contest/LatestContests';
import WarpScoresApiService from '../../WarpScoresApiService';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';
import imageUrls from '../../imageUrls';
import useFetchArenaInfo from '../../hooks/useFetchArenaInfo';
import config from '../../config';
import formatter from '../../util/formatter';
import Race from '../common/Race';
import WinRate from '../common/WinRate';

const { boxSize } = config;

const iconFor = (placement) => {
  if (placement >= 1 && placement <= 3) {
    return BiSolidTrophy;
  }
  return BiTrophy;
};

const ordinalIndicatorFor = (placement) => {
  switch (placement) {
    case 1:
      return 'st';
    case 2:
      return 'nd';
    case 3:
      return 'rd';
    default:
      return 'th';
  }
};

const ordinalFor = (placement) => {
  return (
    <Text as="sup" fontSize="md" position="relative" top="0">
      {ordinalIndicatorFor(placement)}
    </Text>
  );
};

const colorFor = (placement) => {
  switch (placement) {
    case 1:
      return 'gold';
    case 2:
      return 'silver';
    case 3:
      return 'darkgoldenrod';
    default:
      return null;
  }
};

function RaceAvatars({ races }) {
  races.sort();
  return (
    <Wrap>
      {races.map((race) => (
        <WrapItem as={Race} key={race} race={race} size="sm" boxSize={boxSize} asAvatar />
      ))}
    </Wrap>
  );
}

function ArenaInfoCard({ competitionUuid, race }) {
  const navigate = useNavigate();
  const goToArenaRace = () => {
    navigate(`/competition/${competitionUuid}/arena/${race}`);
  };
  const { fetchArenaInfo, arenaInfo, arenaInfoLoading, error } = useFetchArenaInfo();

  useEffect(() => {
    if (race) {
      fetchArenaInfo(competitionUuid, race);
    }
  }, [race]);

  return (
    <Card
      _hover={{ background: 'warpScoresHoverColor' }}
      cursor="pointer"
      onClick={goToArenaRace}
      direction="row"
      overflow="hidden"
      align="center"
    >
      <Image maxH="120px" src={imageUrls.race(race)} />
      <CardBody p={2} height="100%">
        <VStack align="left" height="100%">
          <Heading>
            <Race size="md" race={race} />
          </Heading>
          <LoadingOrErrorWrapper loading={arenaInfoLoading} error={error}>
            <Text>
              {arenaInfo?.coaches} Coaches, {arenaInfo?.teams} Teams
            </Text>
            <Text>
              Matches: {arenaInfo?.matches}, {arenaInfo?.wins} wins, {arenaInfo?.losses} losses
            </Text>
            <Text>
              Runs: {arenaInfo?.completedRuns} completed, {arenaInfo?.activeRuns} active, {arenaInfo?.failedRuns} failed
            </Text>
          </LoadingOrErrorWrapper>
        </VStack>
      </CardBody>
    </Card>
  );
}

function ArenaInfos({ races, competitionUuid }) {
  return (
    <SimpleGrid columns={{ lg: 3, sm: 1, md: 2 }} spacing="1.25rem">
      {races
        ?.sort((raceA, raceB) => raceA.localeCompare(raceB))
        .map((race) => (
          <ArenaInfoCard key={race} race={race} competitionUuid={competitionUuid} />
        ))}
    </SimpleGrid>
  );
}

function ArenaCoachCard({ competitionUuid, coach, placement }) {
  const navigate = useNavigate();
  const goToArenaCoach = () => {
    navigate(`/competition/${competitionUuid}/arena/coach/${coach.coachUuid}`);
  };

  return (
    <Card
      _hover={{ background: 'warpScoresHoverColor' }}
      cursor="pointer"
      onClick={goToArenaCoach}
      direction="row"
      overflow="hidden"
      align="center"
    >
      <Center width="100px">
        <VStack>
          <Icon boxSize="60px" as={iconFor(placement)} color={colorFor(placement)} />
          <Heading color={colorFor(placement)}>
            {placement}
            {ordinalFor(placement)}
          </Heading>
        </VStack>
      </Center>
      <CardBody p={2} height="100%">
        <VStack align="left">
          <Heading>{coach?.coachName}</Heading>
          <Text>
            <WinRate identifier="Win rate (overall)" winRate={coach.overallWinRate} />
          </Text>
          <Text>
            Completed teams: {coach ? coach.completedTeamsCount || 0 : '-'} (last on{' '}
            {formatter.formatAsDate(coach.lastCompletion, '-')})
          </Text>
          <Box>
            <RaceAvatars races={coach.completedRaces} />
          </Box>
        </VStack>
      </CardBody>
    </Card>
  );
}

function ArenaCoaches({ coaches, competitionUuid }) {
  return (
    <SimpleGrid columns={{ lg: 3, sm: 1, md: 2 }} spacing="1.25rem">
      {coaches?.map((coach, index) => (
        <ArenaCoachCard key={coach.id} placement={index + 1} coach={coach} competitionUuid={competitionUuid} />
      ))}
    </SimpleGrid>
  );
}

function ArenaCompetition({ competition }) {
  const [arenaRacesLoading, setArenaRacesLoading] = useState(false);
  const [arenaRaces, setArenaRaces] = useState([]);
  const [arenaRacesError, setArenaRacesError] = useState(null);
  const [arenaCoachesLoading, setArenaCoachesLoading] = useState(false);
  const [arenaCoaches, setArenaCoaches] = useState(null);
  const [arenaCoachesError, setArenaCoachesError] = useState(null);

  const fetchArenaRaces = async () => {
    setArenaRacesLoading(true);
    await WarpScoresApiService.arenaInfos(competition.uuid)
      .then(setArenaRaces)
      .catch((reason) => setArenaRacesError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setArenaRacesLoading(false));
  };

  const fetchArenaTopCoaches = async () => {
    setArenaCoachesLoading(true);
    await WarpScoresApiService.arenaTopCoaches(competition.uuid)
      .then(setArenaCoaches)
      .catch((reason) => setArenaCoachesError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setArenaCoachesLoading(false));
  };

  useEffect(() => {
    if (competition) {
      fetchArenaRaces(competition);
      fetchArenaTopCoaches(competition);
    }
  }, [competition]);

  return (
    <>
      <Heading size="md">Top Arena Coaches</Heading>
      <LoadingOrErrorWrapper loading={arenaCoachesLoading} error={arenaCoachesError}>
        <ArenaCoaches coaches={arenaCoaches} competitionUuid={competition.uuid} />
      </LoadingOrErrorWrapper>
      <Heading size="md">Arena Teams</Heading>
      <LoadingOrErrorWrapper loading={arenaRacesLoading} error={arenaRacesError}>
        <ArenaInfos races={arenaRaces} competitionUuid={competition.uuid} />
      </LoadingOrErrorWrapper>
      <LiveContests competition={competition} limit={6} />
      <LatestContests competition={competition} limit={9} />
    </>
  );
}

export default ArenaCompetition;
