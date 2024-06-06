import React, { useEffect, useState } from 'react';
import { Box, Card, CardBody, Center, Flex, Heading, Image, Spinner, useMediaQuery, VStack } from '@chakra-ui/react';
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
import Matches from '../components/Matches';
import HeaderCard from '../components/HeaderCard';

function MatchesCount({ matches, teamUuid }) {
  if (!matches) return <Spinner />;

  let won = 0;
  let lost = 0;

  matches.forEach((match) => {
    const myTeam = match.teams[0].id === teamUuid ? match.teams[0] : match.teams[1];
    const otherTeam = match.teams[0].id !== teamUuid ? match.teams[0] : match.teams[1];
    if (myTeam.score > otherTeam.score) won += 1;
    else if (myTeam.score < otherTeam.score) lost += 1;
  });

  const drawn = matches.length - won - lost;
  return `${matches.length} (${won}/${drawn}/${lost})`;
}

function TeamPage() {
  const [isSmallScreen] = useMediaQuery('(max-width: 768px)');
  const { competitionUuid, teamUuid } = useParams();
  const [team, setTeam] = useState();
  const [matches, setMatches] = useState();
  const [players, setPlayers] = useState();

  useEffect(() => {
    const fetchTeam = () => {
      const teamResponse = competitionUuid
        ? CyanideApiService.competitionTeam(competitionUuid, teamUuid)
        : CyanideApiService.team(teamUuid);

      teamResponse.then((data) => {
        setTeam(data);
        const currentPlayers = data.players || [];
        currentPlayers.sort((playerA, playerB) => playerA.number - playerB.number);
        setPlayers(currentPlayers);
      });
    };

    const fetchMatches = () => {
      const matchesResponse = CyanideApiService.teamMatches(teamUuid);
      matchesResponse.then((data) => {
        setMatches(data);
      });
    };

    fetchTeam();
    fetchMatches();
  }, []);

  const navCompetition =
    team && team.competitionIds.length === 1 ? [team.competitionIds[0], team.competitionName] : null;

  return (
    <VStack align="left">
      <Box>
        <Navigation
          currentPage="team"
          league={team ? [team.leagueIds[0], team.leagueName] : []}
          competition={navCompetition}
          team={team ? [teamUuid, team.name] : []}
        />
      </Box>
      <Box>
        {team ? (
          <>
            <HeaderCard
              heading={team.name}
              subHeading={`Coach: ${team.coachName}`}
              mainImageSrc={ImageUrls.logo(team.logo)}
              additionalImageSrc={ImageUrls.race(team.race)}
              isSmallScreen={isSmallScreen}
            >
              <InfoArea
                isSmallScreen={isSmallScreen}
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
                  <InfoItem
                    key="matches"
                    label="Matches"
                    info={<MatchesCount matches={matches} teamUuid={teamUuid} />}
                  />,
                ]}
              />
            </HeaderCard>
            <Roster players={players} />
          </>
        ) : (
          <Spinner />
        )}
      </Box>
      <Box>
        <Heading size="md">Matches</Heading>
        <Matches matches={matches} />
      </Box>
    </VStack>
  );
}

export default TeamPage;
