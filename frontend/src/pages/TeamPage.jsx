import React, { useEffect, useState } from 'react';
import { Box, Heading, Spinner, VStack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Roster from '../components/team/Roster';
import prettyPrint from '../util/prettyPrint';
import Navigation from '../components/misc/Navigation';
import formatter from '../util/formatter';
import imageUrls from '../imageUrls';
import InfoArea from '../components/common/InfoArea';
import InfoItem from '../components/common/InfoItem';
import Matches from '../components/contest/Matches';
import HeaderCard from '../components/common/HeaderCard';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';

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
  const { competitionUuid, teamUuid } = useParams();
  const [team, setTeam] = useState();
  const [matches, setMatches] = useState();
  const [players, setPlayers] = useState();
  const [loadingTeam, setLoadingTeam] = useState(false);
  const [teamError, setTeamError] = useState(undefined);
  const [loadingMatches, setLoadingMatches] = useState(false);
  const [matchesError, setMatchesError] = useState(undefined);

  useEffect(() => {
    const fetchTeam = () => {
      const teamResponse = competitionUuid
        ? WarpScoresApiService.competitionTeam(competitionUuid, teamUuid)
        : WarpScoresApiService.team(teamUuid);

      teamResponse
        .then((data) => {
          setLoadingTeam(false);
          setTeam(data);
          const currentPlayers = data.players || [];
          currentPlayers.sort((playerA, playerB) => playerA.number - playerB.number);
          setPlayers(currentPlayers);
        })
        .catch((reason) => {
          setTeamError({ type: 'error', message: reason.toLocaleString() });
        });
    };

    const fetchMatches = () => {
      setMatches([]);
      setLoadingMatches(true);
      const matchesResponse = WarpScoresApiService.teamMatches(teamUuid);
      matchesResponse
        .then((data) => {
          setLoadingMatches(false);
          setMatches(data);
        })
        .catch((reason) => {
          setMatchesError({ type: 'error', message: reason.toLocaleString() });
        });
    };

    fetchTeam();
    fetchMatches();
  }, [competitionUuid, teamUuid]);

  const navCompetition =
    team && team.competitionIds?.length === 1 ? [team.competitionIds[0], team.competitionName] : null;

  return (
    <VStack align="left">
      <Box>
        <Navigation
          currentPage="team"
          league={team && team.leagueIds ? [team.leagueIds[0], team.leagueName] : []}
          competition={navCompetition}
          team={team ? [teamUuid, team.name] : []}
        />
      </Box>
      <Box>
        <LoadingOrErrorWrapper loading={loadingTeam} error={teamError}>
          {team && (
            <>
              <HeaderCard
                heading={team?.name}
                subHeading={`Coach: ${team?.coachName}`}
                detailsHeading="Team details"
                mainImageSrc={imageUrls.logo(team?.logo)}
                additionalImageSrc={imageUrls.race(team?.race)}
              >
                <InfoArea>
                  <InfoItem key="race" label="Race" info={prettyPrint(team.race)} />
                  <InfoItem key="players" label="Players" info={players !== null ? players.length : '-'} />
                  <InfoItem key="rerolls" label="Rerolls" info={team.rerolls} />
                  <InfoItem key="dedicatedFans" label="Dedicated Fans" info={team.dedicatedFans} />
                  <InfoItem key="cheerleaders" label="Cheerleaders" info={team.cheerleaders} />
                  <InfoItem key="assistantCoaches" label="Assistant coaches" info={team.coachAssistants} />
                  <InfoItem key="apothecary" label="Apothecary" info={team.apothecary} />
                  <InfoItem key="cash" label="Cash" info={formatter.formatAsNumber(team.cash)} />
                  <InfoItem key="value" label="Value" info={formatter.formatAsNumber(team.value)} />
                  <InfoItem
                    key="matches"
                    label="Matches"
                    info={<MatchesCount matches={matches} teamUuid={teamUuid} />}
                  />
                </InfoArea>
              </HeaderCard>
              <Roster players={players} />
            </>
          )}
        </LoadingOrErrorWrapper>
      </Box>
      <Box>
        <Heading size="md">Matches</Heading>
        <LoadingOrErrorWrapper loading={loadingMatches} error={matchesError}>
          <Matches matches={matches} />
        </LoadingOrErrorWrapper>
      </Box>
    </VStack>
  );
}

export default TeamPage;
