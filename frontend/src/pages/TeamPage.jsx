import React, { useEffect, useState } from 'react';
import { Alert, AlertDescription, AlertIcon, Box, Heading, Spinner, VStack } from '@chakra-ui/react';
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
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';

function playersFromLiveRoster(roster, fallbackPlayers) {
  if (!Array.isArray(roster?.players) || (roster.players.length === 0 && fallbackPlayers.length > 0)) return null;
  return roster.players.map((player) => {
    const fallback = fallbackPlayers.find((item) => item.number === player.number) || {};
    const skillNames = Object.values(player.skill_names || {});
    return {
      ...fallback,
      id: { ...(fallback.id || {}), key: player.player_id || fallback.id?.key },
      name: player.name || fallback.name,
      number: player.number,
      level: player.level,
      spp: player.spp,
      value: player.value,
      attributes: player.attributes || fallback.attributes,
      skills: skillNames.length ? skillNames : (fallback.skills || fallback.skillStrings || []),
    };
  });
}

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

function latestTeamSnapshot(matches, teamId) {
  if (!matches?.length) return null;
  const snapshots = matches.flatMap((match) => (match.teams || []).map((matchTeam, index) => ({
    match,
    team: matchTeam,
    coach: match.coaches?.[index],
  }))).filter(({ team }) => identityUtils.key(team.id) === teamId);
  snapshots.sort((left, right) => new Date(right.match.finished || right.match.started || 0)
    - new Date(left.match.finished || left.match.started || 0));
  const latest = snapshots[0];
  if (!latest) return null;
  return {
    ...latest.team,
    coachName: latest.coach?.name || latest.team.coachName,
    leagueIds: latest.match.leagueId ? [latest.match.leagueId] : latest.team.leagueIds,
    leagueNames: latest.match.leagueName ? [latest.match.leagueName] : latest.team.leagueNames,
    competitionIds: latest.match.competitionId ? [latest.match.competitionId] : latest.team.competitionIds,
    competitionNames: latest.match.competitionName ? [latest.match.competitionName] : latest.team.competitionNames,
  };
}

function TeamPage() {
  const intl = useIntl();
  const { isAuthenticated, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();
  const { competitionId, teamId } = useParams();
  const [searchParams] = useSearchParams();
  const [team, setTeam] = useState();
  const [matches, setMatches] = useState();
  const [players, setPlayers] = useState();
  const [liveRoster, setLiveRoster] = useState(null);
  const [liveRosterError, setLiveRosterError] = useState(null);
  useEffect(() => {
    const id = searchParams.get('player');
    if (id && players?.length) document.getElementById(`player-${id}`)?.scrollIntoView({ block: 'center' });
  }, [players, searchParams]);
  const [loadingTeam, setLoadingTeam] = useState(true);
  const [teamError, setTeamError] = useState(undefined);
  const [loadingMatches, setLoadingMatches] = useState(false);
  const [matchesError, setMatchesError] = useState(undefined);
  const matchTeamSnapshot = latestTeamSnapshot(matches, teamId);
  const baseTeam = team || matchTeamSnapshot;
  const liveTeamDetails = Object.fromEntries(
    Object.entries(liveRoster?.team || {}).filter(([, value]) => value !== null && value !== undefined),
  );
  const displayedTeam = baseTeam ? { ...baseTeam, ...liveTeamDetails } : undefined;
  const fallbackPlayers = team ? (players || []) : (matchTeamSnapshot?.players || []);
  const displayedPlayers = playersFromLiveRoster(liveRoster, fallbackPlayers) || fallbackPlayers;

  useEffect(() => {
    setLoadingTeam(true);
    setTeam(undefined);
    setPlayers(undefined);
    setLiveRoster(null);
    setLiveRosterError(null);
    setTeamError(undefined);
    const fetchTeam = () => {
      const teamResponse = competitionId
        ? WarpScoresApiService.competitionTeam(competitionId, teamId)
        : WarpScoresApiService.team(teamId);

      teamResponse
        .then((data) => {
          const resolvedTeam = data && !(Array.isArray(data) && data.length === 0) ? data : undefined;
          setTeam(resolvedTeam);
          const currentPlayers = resolvedTeam?.players || [];
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
    if (isAuthenticated) {
      WarpScoresApiService.bb3LiveTeamRoster(teamId, getAccessTokenSilently, getAccessTokenWithPopup)
        .then((data) => {
          setLiveRoster(data);
          setLiveRosterError(null);
        })
        .catch((reason) => {
          setLiveRoster(null);
          setLiveRosterError(
            reason?.response?.data?.message
              || reason?.response?.data?.detail
              || reason?.message
              || 'Okänt fel',
          );
        });
    }
  }, [competitionId, teamId, isAuthenticated, getAccessTokenSilently, getAccessTokenWithPopup]);

  useEffect(() => {
    if (team || loadingTeam || loadingMatches) return;
    if (matchTeamSnapshot) setTeamError(undefined);
    else setTeamError({ type: 'error', message: 'Laget kunde inte hittas och saknas i sparade matcher.' });
  }, [team, loadingTeam, loadingMatches, matchTeamSnapshot]);

  const leagueId = displayedTeam?.leagueIds?.[0];
  const leagueName = displayedTeam?.leagueNames?.[0];
  const navLeague = leagueId && leagueName ? [identityUtils.key(leagueId), leagueName] : null;
  const navCompetitionId = displayedTeam?.competitionIds?.length === 1 ? displayedTeam.competitionIds[0] : null;
  const competitionName = displayedTeam?.competitionNames?.length === 1 ? displayedTeam.competitionNames[0] : null;
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
          team={displayedTeam ? [teamId, displayedTeam.name] : []}
        />
      </Box>
      <Box width="full">
        <LoadingOrErrorWrapper loading={loadingTeam} error={teamError}>
          {displayedTeam && (
            <>
              {liveRoster && (
                <Alert status={liveRoster.players?.length ? 'success' : 'warning'} mb={3}>
                  <AlertIcon />
                  <AlertDescription>
                    {liveRoster.players?.length
                      ? (Object.values(liveRoster.team || {}).some((value) => value !== null && value !== undefined)
                        ? 'Spelartrupp och aktuella laguppgifter hämtas direkt från BB3.'
                        : 'Spelartruppen hämtas direkt från BB3.')
                      : `BB3 returnerade inga spelare i den aktuella truppen.${fallbackPlayers.length ? ' Visar den senast sparade truppen i stället.' : ''}`}
                  </AlertDescription>
                </Alert>
              )}
              {!liveRoster && !liveRosterError && !team && matchTeamSnapshot && (
                <Alert status="info" mb={3}>
                  <AlertIcon />
                  <AlertDescription>Visar laguppgifter från den senaste sparade matchen.</AlertDescription>
                </Alert>
              )}
              {isAuthenticated && liveRosterError && (
                <Alert status="warning" mb={3}>
                  <AlertIcon />
                  <AlertDescription>
                    Kunde inte hämta BB3-liveuppgifter: {liveRosterError}
                  </AlertDescription>
                </Alert>
              )}
              <HeaderCard
                heading={displayedTeam.name}
                subHeading={intl.formatMessage({ id: 'team.coach' }, { name: displayedTeam.coachName })}
                detailsHeading={intl.formatMessage({ id: 'team.details' })}
                mainImageSrc={imageUrls.logo(displayedTeam.logo, identityUtils.opus(teamId))}
                additionalImageSrc={imageUrls.race(displayedTeam.race, identityUtils.opus(teamId))}
              >
                <InfoArea>
                  <InfoItem key="race" label={intl.formatMessage({ id: 'team.race' })} info={prettyPrint(displayedTeam.race)} />
                  <InfoItem key="players" label={intl.formatMessage({ id: 'common.players' })} info={displayedPlayers?.length ?? '-'} />
                  <InfoItem key="rerolls" label={intl.formatMessage({ id: 'team.rerolls' })} info={displayedTeam.rerolls} />
                  <InfoItem key="dedicatedFans" label={intl.formatMessage({ id: 'team.dedicatedFans' })} info={displayedTeam.dedicatedFans} />
                  <InfoItem key="cheerleaders" label={intl.formatMessage({ id: 'team.cheerleaders' })} info={displayedTeam.cheerleaders} />
                  <InfoItem key="assistantCoaches" label={intl.formatMessage({ id: 'team.assistantCoaches' })} info={displayedTeam.coachAssistants} />
                  <InfoItem key="apothecary" label={intl.formatMessage({ id: 'team.apothecary' })} info={displayedTeam.apothecary} />
                  <InfoItem key="cash" label={intl.formatMessage({ id: 'team.cash' })} info={formatter.formatAsNumber(displayedTeam.cash)} />
                  <InfoItem key="value" label={intl.formatMessage({ id: 'common.value' })} info={formatter.formatAsNumber(displayedTeam.value)} />
                  <InfoItem key="matches" label={intl.formatMessage({ id: 'common.matches' })}
                    info={<MatchesCount matches={matches} teamId={teamId} />}
                  />
                </InfoArea>
              </HeaderCard>
              <Box width="full" mt={6}>
                <Heading size="md" borderBottom="1px solid" borderColor="warpScoresBorderColor" pb={2} mb={3}>
                  {intl.formatMessage({ id: 'common.players' })}
                </Heading>
                <Roster players={displayedPlayers} />
              </Box>
              {identityUtils.opus(teamId) === 3 && <Bb3TeamManagement teamId={teamId} />}
              <TeamSupporters teamId={teamId} dedicatedFans={displayedTeam.dedicatedFans} />
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
