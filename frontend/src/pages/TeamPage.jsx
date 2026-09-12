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
import { identityUtils } from '../util/identityUtil';
import { useIntl } from 'react-intl';

function MatchesCount({ matches, teamId }) {
  if (!matches) return <Spinner />;

  let won = 0;
  let lost = 0;

  matches.forEach((match) => {
    const myTeam = match.teams[0].id === teamId ? match.teams[0] : match.teams[1];
    const otherTeam = match.teams[0].id !== teamId ? match.teams[0] : match.teams[1];
    if (myTeam.score > otherTeam.score) won += 1;
    else if (myTeam.score < otherTeam.score) lost += 1;
  });

  const drawn = matches.length - won - lost;
  return `${matches.length} (${won}/${drawn}/${lost})`;
}

function TeamPage() {
  const intl = useIntl();
  const { competitionId, teamId } = useParams();
  const [team, setTeam] = useState();
  const [matches, setMatches] = useState();
  const [players, setPlayers] = useState();
  const [loadingTeam, setLoadingTeam] = useState(false);
  const [teamError, setTeamError] = useState(undefined);
  const [loadingMatches, setLoadingMatches] = useState(false);
  const [matchesError, setMatchesError] = useState(undefined);

  useEffect(() => {
    const fetchTeam = () => {
      const teamResponse = competitionId
        ? WarpScoresApiService.competitionTeam(competitionId, teamId)
        : WarpScoresApiService.team(teamId);

      teamResponse
        .then((data) => {
          setTeam(data);
          const currentPlayers = data.players || [];
          currentPlayers.sort((playerA, playerB) => playerA.number - playerB.number);
          setPlayers(currentPlayers);
        })
        .catch((reason) => {
          setTeamError({ type: 'error', message: reason.toLocaleString() });
        })
        .finally(() => setLoadingTeam(false));
    };

    const fetchMatches = () => {
      setMatches([]);
      setLoadingMatches(true);
      const matchesResponse = WarpScoresApiService.teamMatches(teamId);
      matchesResponse
        .then((data) => {
          setMatches(data);
        })
        .catch((reason) => {
          setMatchesError({ type: 'error', message: reason.toLocaleString() });
        })
        .finally(() => setLoadingMatches(false));
    };

    fetchTeam();
    fetchMatches();
  }, [competitionId, teamId]);

  const navCompetition =
    team && team.competitionIds?.length === 1 ? [team.competitionIds[0], team.competitionNames[0]] : null;
  console.log('Rendering TeamPage', { team, players, matches });
  return (
    <VStack align="left">
      <Box>
        <Navigation
          currentPage="team"
          league={team && team.leagueIds ? [team.leagueIds[0].key, team.leagueNames[0]] : []}
          competition={navCompetition}
          team={team ? [teamId, team.name] : []}
        />
      </Box>
      <Box>
        <LoadingOrErrorWrapper loading={loadingTeam} error={teamError}>
          {team && (
            <>
              <HeaderCard
                heading={team?.name}
                subHeading={intl.formatMessage({ id: 'team.coach' }, { name: team?.coachName })}
                detailsHeading={intl.formatMessage({ id: 'team.details' })}
                mainImageSrc={imageUrls.logo(team?.logo, identityUtils.opus(teamId))}
                additionalImageSrc={imageUrls.race(team?.race, identityUtils.opus(teamId))}
              >
                <InfoArea>
                  <InfoItem key="race" label={intl.formatMessage({ id: 'team.race' })} info={prettyPrint(team.race)} />
                  <InfoItem key="players" label={intl.formatMessage({ id: 'common.players' })} info={players !== null ? players.length : '-'} />
                  <InfoItem key="rerolls" label={intl.formatMessage({ id: 'team.rerolls' })} info={team.rerolls} />
                  <InfoItem key="dedicatedFans" label={intl.formatMessage({ id: 'team.dedicatedFans' })} info={team.dedicatedFans} />
                  <InfoItem key="cheerleaders" label={intl.formatMessage({ id: 'team.cheerleaders' })} info={team.cheerleaders} />
                  <InfoItem key="assistantCoaches" label={intl.formatMessage({ id: 'team.assistantCoaches' })} info={team.coachAssistants} />
                  <InfoItem key="apothecary" label={intl.formatMessage({ id: 'team.apothecary' })} info={team.apothecary} />
                  <InfoItem key="cash" label={intl.formatMessage({ id: 'team.cash' })} info={formatter.formatAsNumber(team.cash)} />
                  <InfoItem key="value" label={intl.formatMessage({ id: 'common.value' })} info={formatter.formatAsNumber(team.value)} />
                  <InfoItem key="matches" label={intl.formatMessage({ id: 'common.matches' })}
                    info={<MatchesCount matches={matches} teamId={teamId} />}
                  />
                </InfoArea>
              </HeaderCard>
              <Roster players={players} />
            </>
          )}
        </LoadingOrErrorWrapper>
      </Box>
      <Box>
        <Heading size="md">{intl.formatMessage({ id: 'common.matches' })}</Heading>
        <LoadingOrErrorWrapper loading={loadingMatches} error={matchesError}>
          <Matches matches={matches} />
        </LoadingOrErrorWrapper>
      </Box>
    </VStack>
  );
}

export default TeamPage;
