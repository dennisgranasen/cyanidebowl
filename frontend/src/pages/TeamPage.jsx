import React, { useEffect, useState } from 'react';
import { Box, Heading, Spinner, VStack } from '@chakra-ui/react';
import { useParams, useSearchParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Roster from '../components/team/Roster';
import TeamSupporters from '../components/team/TeamSupporters';
import CommentThread from '../components/community/CommentThread';
import prettyPrint from '../util/prettyPrint';
import Navigation from '../components/misc/Navigation';
import formatter from '../util/formatter';
import imageUrls from '../imageUrls';
import InfoArea from '../components/common/InfoArea';
import InfoItem from '../components/common/InfoItem';
import Matches from '../components/contest/Matches';
import ArticleFeed from '../components/community/ArticleFeed';
import HeaderCard from '../components/common/HeaderCard';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import { identityUtils } from '../util/identityUtil';
import { useIntl } from 'react-intl';
import Bb3TeamManagement from '../components/team/Bb3TeamManagement';

function MatchesCount({ matches, teamId }) {
  if (!matches) return <Spinner />;

  let won = 0;
  let lost = 0;

  matches.forEach((match) => {
    const myTeam = identityUtils.key(match.teams[0].id) === teamId ? match.teams[0] : match.teams[1];
    const otherTeam = identityUtils.key(match.teams[0].id) !== teamId ? match.teams[0] : match.teams[1];
    if (myTeam.score > otherTeam.score) won += 1;
    else if (myTeam.score < otherTeam.score) lost += 1;
  });

  const drawn = matches.length - won - lost;
  return `${matches.length} (${won}/${drawn}/${lost})`;
}

function TeamPage() {
  const intl = useIntl();
  const { competitionId, teamId } = useParams();
  const [searchParams] = useSearchParams();
  const [team, setTeam] = useState();
  const [matches, setMatches] = useState();
  const [players, setPlayers] = useState();
  useEffect(() => {
    const id = searchParams.get('player');
    if (id && players?.length) document.getElementById(`player-${id}`)?.scrollIntoView({ block: 'center' });
  }, [players, searchParams]);
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

  const leagueId = team?.leagueIds?.[0];
  const leagueName = team?.leagueNames?.[0];
  const navLeague = leagueId && leagueName ? [identityUtils.key(leagueId), leagueName] : null;
  const navCompetitionId = team?.competitionIds?.length === 1 ? team.competitionIds[0] : null;
  const competitionName = team?.competitionNames?.length === 1 ? team.competitionNames[0] : null;
  const navCompetition = navCompetitionId && competitionName
    ? [identityUtils.key(navCompetitionId), competitionName]
    : null;
  return (
    <VStack align="stretch" spacing={6} width="full">
      <Box width="full">
        <Navigation
          currentPage="team"
          league={navLeague}
          competition={navCompetition}
          team={team ? [teamId, team.name] : []}
        />
      </Box>
      <Box width="full">
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
                  <InfoItem key="players" label={intl.formatMessage({ id: 'common.players' })} info={players?.length ?? '-'} />
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
              <Box width="full" mt={6}>
                <Heading size="md" borderBottom="1px solid" borderColor="warpScoresBorderColor" pb={2} mb={3}>
                  {intl.formatMessage({ id: 'common.players' })}
                </Heading>
                <Roster players={players} />
              </Box>
              {identityUtils.opus(teamId) === 3 && <Bb3TeamManagement teamId={teamId} />}
              <TeamSupporters teamId={teamId} dedicatedFans={team.dedicatedFans} />
            </>
          )}
        </LoadingOrErrorWrapper>
      </Box>
      <Box width="full">
        <Heading size="md" borderBottom="1px solid" borderColor="warpScoresBorderColor" pb={2} mb={3}>
          {intl.formatMessage({ id: 'common.matches' })}
        </Heading>
        <LoadingOrErrorWrapper loading={loadingMatches} error={matchesError}>
          <Matches matches={matches} />
        </LoadingOrErrorWrapper>
      </Box>
      <Box width="full">
        <Heading size="md" borderBottom="1px solid" borderColor="warpScoresBorderColor" pb={2} mb={3}>
          {intl.formatMessage({ id: 'common.news' })}
        </Heading>
        <ArticleFeed type="TEAM" subjectId={teamId} limit={6} />
      </Box>
      <Box width="full">
        <Heading size="md" borderBottom="1px solid" borderColor="warpScoresBorderColor" pb={2} mb={3}>Kommentarer</Heading>
        <CommentThread targetType="TEAM" targetId={teamId} />
      </Box>
    </VStack>
  );
}

export default TeamPage;
