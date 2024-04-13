import React, { useEffect, useState } from 'react';
import { Box, Card, CardBody, Center, Flex, Heading, Image, Spinner, VStack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import CyanideApiService from '../CyanideApiService';
import Roster from '../components/Roster';
import prettyPrint from '../util/PrettyPrint';
import Navigation from '../components/Navigation';
import Formatter from '../util/Formatter';
import ImageUrls from '../ImageUrls';
import InfoArea from '../components/InfoArea';
import InfoItem from '../components/InfoItem';
import DelayedIconTooltip from '../components/DelayedIconTooltip';

function TeamPage() {
  const { teamUuid } = useParams();
  const [team, setTeam] = useState();
  const [players, setPlayers] = useState();

  useEffect(() => {
    const fetchTeam = () => {
      CyanideApiService.team(teamUuid).then((data) => {
        setTeam(data);
        const currentPlayers = data.players || [];
        currentPlayers.sort((playerA, playerB) => playerA.number - playerB.number);
        setPlayers(currentPlayers);
      });
    };
    /*    const fetchMatches = async () => {
          await CyanideApiService.matches(teamUuid).then((data) => {
            logger.info('Matches: %o', data);
          });
        }; */
    fetchTeam();
    // fetchMatches();
  }, []);

  return (
    <VStack align="left">
      <Box>
        <Navigation
          currentPage="team"
          league={team ? [team.leagueIds[0], team.leagueName] : []}
          competition={team ? [team.competitionIds[0], team.competitionName] : []}
          team={team ? [teamUuid, team.name] : []}
        />
      </Box>
      {team ? (
        <>
          <Card direction="row">
            <Center>
              <Box>
                <Image objectFit="contain" maxH="140px" src={ImageUrls.logo(team.logo)} />
              </Box>
            </Center>
            <CardBody>
              <Flex>
                <Box flex="1">
                  <Heading>{team.name}</Heading>
                  <Box mb="10px">Coach: {team.coachName}</Box>
                  <InfoArea
                    infoItems={[
                      <InfoItem key="race" label="Race" info={prettyPrint(team.race)} />,
                      <InfoItem key="players" label="Players" info={players !== null ? players.length : '-'} />,
                      <InfoItem key="rerolls" label="Rerolls" info={team.rerolls} />,
                      <InfoItem key="dedicatedFans" label="Dedicated Fans" info={team.dedicatedFans} />,
                      <InfoItem key="cheerleaders" label="Cheerleaders" info={team.cheerleaders} />,
                      <InfoItem key="assistantCoaches" label="Assistant coaches" info={team.coachAssistants} />,
                      <InfoItem key="apothecary" label="Apothecary" info={team.apothecary} />,
                      <InfoItem key="cash" label="Cash" info={Formatter.formatAsNumber(team.cash)} />,
                      <InfoItem key="value" label="Value" info={Formatter.formatAsNumber(team.value)} />,
                    ]}
                  />
                </Box>
                <Center>
                  <Box>
                    <DelayedIconTooltip label={prettyPrint(team.race)}>
                      <Image
                        hideBelow="lg"
                        objectFit="cover"
                        maxH="140px"
                        src={ImageUrls.race(team.race)}
                        fallback={null}
                      />
                    </DelayedIconTooltip>
                  </Box>
                </Center>
              </Flex>
            </CardBody>
          </Card>
          <Roster players={players} />
        </>
      ) : (
        <Spinner />
      )}
    </VStack>
  );
}

export default TeamPage;
