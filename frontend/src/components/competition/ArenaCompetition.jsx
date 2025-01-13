import { Card, CardBody, Heading, Image, SimpleGrid, Text } from '@chakra-ui/react';
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import LiveContests from '../contest/LiveContests';
import LatestContests from '../contest/LatestContests';
import WarpScoresApiService from '../../WarpScoresApiService';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';
import ImageUrls from '../../ImageUrls';
import prettyPrint from '../../util/PrettyPrint';
import useFetchArenaInfo from '../../hooks/useFetchArenaInfo';

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
      <Image maxH="120px" src={ImageUrls.race(race)} />
      <CardBody p={2}>
        <Heading>{prettyPrint(race)}</Heading>
        <LoadingOrErrorWrapper loading={arenaInfoLoading} error={error}>
          <Text>
            {arenaInfo?.coaches} Coaches, {arenaInfo?.teams} Teams
          </Text>
          <Text>
            Matches: {arenaInfo?.matches}, {arenaInfo?.wins} wins, {arenaInfo?.losses} losses
          </Text>
          <Text>
            Runs: {arenaInfo?.activeRuns} active, {arenaInfo?.completedRuns} completed, {arenaInfo?.failedRuns} failed
          </Text>
        </LoadingOrErrorWrapper>
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

function ArenaCompetition({ competition }) {
  const [arenaRacesLoading, setArenaRacesLoading] = useState(false);
  const [arenaRaces, setArenaRaces] = useState([]);
  const [error, setError] = useState(null);

  const fetchArenaRaces = () => {
    setArenaRacesLoading(true);
    WarpScoresApiService.arenaInfos(competition.uuid)
      .then(setArenaRaces)
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setArenaRacesLoading(false));
  };

  useEffect(() => {
    if (competition) {
      fetchArenaRaces(competition);
    }
  }, [competition]);

  return (
    <>
      <Heading size="md">Arena Teams</Heading>
      <LoadingOrErrorWrapper loading={arenaRacesLoading} error={error}>
        <ArenaInfos races={arenaRaces} competitionUuid={competition.uuid} />
      </LoadingOrErrorWrapper>
      <LiveContests competition={competition} limit={6} />
      <LatestContests competition={competition} limit={9} />
    </>
  );
}

export default ArenaCompetition;
