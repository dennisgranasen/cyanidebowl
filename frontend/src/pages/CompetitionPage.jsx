import React, { useEffect, useState } from 'react';
import { Box, Card, CardBody, Flex, Heading, Image, Spinner, VStack } from '@chakra-ui/react';
import { Link as RouteLink, useParams } from 'react-router-dom';
import CyanideApiService from '../CyanideApiService';
import Navigation from '../components/Navigation';
import Formatter from '../util/Formatter';
import Contests from '../components/Contests';
import comparators from '../util/Comparators';
import ImageUrls from '../ImageUrls';
import Ranks from '../components/Ranks';
import prettyPrint from '../util/PrettyPrint';
import CompetitionProgress from '../components/CompetitionProgress';
import InfoArea from '../components/InfoArea';
import InfoItem from '../components/InfoItem';

function TeamPage() {
  const { competitionUuid } = useParams();
  const [competition, setCompetition] = useState();
  const [ranks, setRanks] = useState();
  const [contests, setContests] = useState();

  useEffect(() => {
    const fetchCompetition = () => {
      CyanideApiService.competition(competitionUuid).then((data) => {
        setCompetition(data);
      });
    };
    const fetchTeams = () => {
      CyanideApiService.competitionRanks(competitionUuid).then((data) => {
        data.sort((rankA, rankB) => rankA.rank - rankB.rank);
        setRanks(data);
      });
    };

    const fetchContests = () => {
      CyanideApiService.competitionContests(competitionUuid).then((data) => {
        data.sort((compA, compB) => comparators.compareAsDates(compA.matchDate, compB.matchDate));
        setContests(data);
      });
    };

    fetchCompetition();
    fetchTeams();
    fetchContests();
  }, []);

  return (
    <VStack align="left">
      <Box>
        <Navigation
          currentPage="competition"
          league={competition ? [competition.leagueId, competition.leagueName] : []}
          competition={[competitionUuid, competition ? competition.name : '']}
        />
      </Box>
      {competition ? (
        <>
          <Card direction="row">
            <Box>
              <Image objectFit="contain" maxW="140px" src={ImageUrls.logo(competition.leagueLogo)} />
            </Box>
            <CardBody>
              <Flex>
                <Box flex="1">
                  <Heading>{competition.name}</Heading>
                  <Box mb="10px">
                    <RouteLink to={`/${competition.leagueId}`}>League: {competition.leagueName}</RouteLink>
                  </Box>
                  <InfoArea
                    infoItems={[
                      <InfoItem key="Created" label="Created" info={Formatter.formatAsDate(competition.dateCreated)} />,
                      <InfoItem key="Format" label="Format" info={prettyPrint(competition.format)} />,
                      <InfoItem
                        key="Progress"
                        label="Progress"
                        info={
                          <CompetitionProgress
                            status={competition.status}
                            currentRound={competition.currentRound}
                            totalRounds={competition.totalRounds}
                            totalMatches={competition.totalMatches}
                            playedMatches={competition.playedMatches}
                          />
                        }
                      />,
                      <InfoItem key="Teams" label="Teams" info={Formatter.formatAsNumber(competition.teamsMax)} />,
                      <InfoItem
                        key="TimeSettings"
                        label="Time settings"
                        info={`Turn: ${Formatter.formatAsNumber(competition.turnDuration / 60)}m`}
                        additionalInfo={`Bonus: ${Formatter.formatAsNumber(competition.timeBonusDuration / 60)}m`}
                      />,
                    ]}
                  />
                </Box>
                <Box hideBelow="lg">
                  <Image objectFit="contain" maxW="140px" src={ImageUrls.logo(competition.logo)} fallback={null} />
                </Box>
              </Flex>
            </CardBody>
          </Card>
          <Heading size="md">Ranking</Heading>
          {ranks ? <Ranks ranks={ranks} /> : <Spinner />}
          <Heading size="md">Contests</Heading>
          {contests ? <Contests contests={contests} currentRound={competition.currentRound} /> : <Spinner />}
        </>
      ) : (
        <Spinner />
      )}
    </VStack>
  );
}

export default TeamPage;
