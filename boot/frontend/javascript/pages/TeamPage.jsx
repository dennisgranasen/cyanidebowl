import React, { useEffect, useState } from 'react';
import { Box, Card, CardBody, Flex, Heading, Image, Spinner, Tooltip, VStack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import CyanideApiService from '../CyanideApiService';
import Roster from '../components/Roster';
import prettyPrint from '../util/PrettyPrint';
import Navigation from '../components/Navigation';
import Formatter from '../util/Formatter';
import ImageUrls from '../ImageUrls';
import InfoArea from '../components/InfoArea';
import InfoItem from '../components/InfoItem';
import NotYetImplemented from '../components/NotYetImplemented';
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
          league={team ? [team.leagueId, team.leagueName] : []}
          competition={team ? [team.competitionIds[0], team.competitionName] : []}
          team={team ? [teamUuid, team.name] : []}
        />
      </Box>
      {team ? (
        <>
          <Card direction="row">
            <Box>
              <Image objectFit="contain" maxW="140px" src={ImageUrls.logo(team.logo)} />
            </Box>
            <CardBody>
              <Flex>
                <Box flex="1">
                  <Heading>{team.name}</Heading>
                  <Box mb="10px">Coach: {team.coachName}</Box>
                  <InfoArea
                    infoItems={[
                      <InfoItem key="1" label="Race" info={prettyPrint(team.race)} />,
                      <InfoItem key="2" label="Players" info={players !== null ? players.length : '-'} />,
                      <InfoItem key="5" label="Dedicated Fans" info={<NotYetImplemented />} />,
                      <InfoItem key="5" label="Rerolls" info={<NotYetImplemented />} />,
                      <InfoItem key="6" label="Apothecary" info={<NotYetImplemented />} />,
                      <InfoItem key="3" label="Cash" info={Formatter.formatAsNumber(team.cash)} />,
                      <InfoItem key="4" label="Value" info={Formatter.formatAsNumber(team.value)} />,
                    ]}
                  />
                </Box>
                <DelayedIconTooltip label={prettyPrint(team.race)}>
                  <Box>
                    <Image
                      hideBelow="lg"
                      objectFit="contain"
                      maxW="140px"
                      src={ImageUrls.race(team.race)}
                      fallback={null}
                    />
                  </Box>
                </DelayedIconTooltip>
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
