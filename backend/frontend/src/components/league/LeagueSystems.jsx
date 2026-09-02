import React from 'react';
import { HamburgerIcon } from '@chakra-ui/icons';
import { SingleEliminationBracket } from 'react-tournament-brackets/dist/cjs';
import { Badge, Box, Heading, HStack, IconButton, Menu, MenuButton, MenuItem, MenuList, SimpleGrid, Text, VStack } from '@chakra-ui/react';
import { MatchComponent } from '../competition/KnockoutCompetition';
import Standings from '../common/Standings';

const orderedSeasons = (system) => [...(system?.seasons || [])].sort((a, b) => (b.sequence ?? b.number ?? 0) - (a.sequence ?? a.number ?? 0));
const matchScore = (match, index) => match.officialScore?.[index ? 'away' : 'home'] ?? match.sourceScore?.[index ? 'away' : 'home'] ?? match.teams?.[index]?.score;

function standingsFor(stage) {
  const teams = new Map();
  (stage.matches || []).filter((match) => match.countsFor?.standings !== false).forEach((match) => {
    if (!match.teams || match.teams.length < 2) return;
    const homeScore = matchScore(match, 0); const awayScore = matchScore(match, 1);
    if (homeScore == null || awayScore == null) return;
    match.teams.forEach((team) => {
      if (!teams.has(team.name)) teams.set(team.name, {
        teamId: team.id || { key: team.name, value: team.name, opus: Number(String(match.game || 'BB3').replace('BB', '')) || 3 },
        teamName: team.name, teamLogo: team.logo, race: team.race, raceId: team.raceId, coachName: team.coachName,
        matchCount: 0, wins: 0, draws: 0, losses: 0, totalTouchdownsFor: 0,
        totalTouchdownsAgainst: 0, totalCasualtiesFor: 0, totalCasualtiesAgainst: 0, points: 0,
      });
    });
    const home = teams.get(match.teams[0].name); const away = teams.get(match.teams[1].name);
    home.matchCount += 1; away.matchCount += 1;
    home.totalTouchdownsFor += homeScore; home.totalTouchdownsAgainst += awayScore;
    away.totalTouchdownsFor += awayScore; away.totalTouchdownsAgainst += homeScore;
    home.totalCasualtiesFor += match.teams[0].casualties || 0; home.totalCasualtiesAgainst += match.teams[1].casualties || 0;
    away.totalCasualtiesFor += match.teams[1].casualties || 0; away.totalCasualtiesAgainst += match.teams[0].casualties || 0;
    if (homeScore === awayScore) { home.draws += 1; away.draws += 1; home.points += 1; away.points += 1; }
    else if (homeScore > awayScore) { home.wins += 1; away.losses += 1; home.points += 3; }
    else { away.wins += 1; home.losses += 1; away.points += 3; }
  });
  return [...teams.values()].map((team) => ({ ...team,
    netTouchdowns: team.totalTouchdownsFor - team.totalTouchdownsAgainst,
    netCasualties: team.totalCasualtiesFor - team.totalCasualtiesAgainst,
  })).sort((a, b) => b.points - a.points || b.netTouchdowns - a.netTouchdowns || b.totalTouchdownsFor - a.totalTouchdownsFor || a.teamName.localeCompare(b.teamName));
}

function GroupTable({ stage }) {
  const standings = standingsFor(stage);
  return <Box><Heading size="sm" mb={2}>{stage.name || 'Group'}</Heading>{standings.length ? <Standings ranks={standings} loading={false} /> : <Text color="gray.500">No table results yet</Text>}</Box>;
}

const matchTime = (match) => new Date(match.finishedAt || match.startedAt || 0).getTime();
const winnerName = (match) => {
  const home = matchScore(match, 0); const away = matchScore(match, 1);
  if (home == null || away == null || home === away || !match.teams?.[0] || !match.teams?.[1]) return null;
  return match.teams[home > away ? 0 : 1].name;
};
const loserName = (match) => {
  const home = matchScore(match, 0); const away = matchScore(match, 1);
  if (home == null || away == null || home === away || !match.teams?.[0] || !match.teams?.[1]) return null;
  return match.teams[home > away ? 1 : 0].name;
};

export function inferPlayoffRounds(matches) {
  const unique = new Map();
  matches.filter((match) => match.countsFor?.bracket !== false).forEach((match) => {
    // Legacy imports can contain one Cyanide match as multiple Mongo documents.
    // Within a single-elimination phase, participants plus final score are stable.
    const participants = (match.teams || []).map((team, index) => `${team.name}:${matchScore(match, index)}`).sort();
    const key = participants.length === 2
      ? `${match.game || ''}:${match.platform || ''}:${participants.join('|')}`
      : match.sourceMatchKey || `${matchTime(match)}:${participants.join('|')}`;
    if (!unique.has(key)) unique.set(key, match);
  });
  const ordered = [...unique.values()]
    .sort((a, b) => matchTime(a) - matchTime(b));
  const nextByMatch = new Map();
  ordered.forEach((match, index) => {
    const winner = winnerName(match);
    if (!winner) return;
    const next = ordered.slice(index + 1).find((candidate) => candidate.teams?.some((team) => team.name === winner));
    if (next) nextByMatch.set(match.sourceMatchKey, next.sourceMatchKey);
  });
  const depthCache = new Map();
  const depth = (matchKey, visited = new Set()) => {
    if (depthCache.has(matchKey)) return depthCache.get(matchKey);
    if (visited.has(matchKey)) return 0;
    const next = nextByMatch.get(matchKey);
    const value = next ? 1 + depth(next, new Set([...visited, matchKey])) : 0;
    depthCache.set(matchKey, value);
    return value;
  };
  const maxDepth = ordered.reduce((max, match) => Math.max(max, depth(match.sourceMatchKey)), 0);
  const grouped = new Map();
  ordered.forEach((match) => {
    const roundIndex = maxDepth - depth(match.sourceMatchKey);
    if (!grouped.has(roundIndex)) grouped.set(roundIndex, []);
    grouped.get(roundIndex).push(match);
  });
  const indexes = [...grouped.keys()].sort((a, b) => a - b);
  const rounds = indexes.map((roundIndex, index) => {
    const roundMatches = grouped.get(roundIndex);
    let name = `Round ${index + 1}`;
    if (roundMatches.length === 1 && index === indexes.length - 1) name = 'Final';
    else if (roundMatches.length === 2 && index === indexes.length - 2) name = 'Semifinals';
    else if (roundMatches.length === 4) name = 'Quarterfinals';
    else if (index === 0 && indexes.length > 3) name = 'Play-in';
    return { id: `inferred-${roundIndex}`, name, matches: roundMatches };
  });
  const terminal = rounds[rounds.length - 1];
  const semifinals = rounds[rounds.length - 2];
  if (terminal?.matches.length === 2 && semifinals?.matches.length === 2) {
    const semifinalWinners = new Set(semifinals.matches.map(winnerName).filter(Boolean));
    const semifinalLosers = new Set(semifinals.matches.map(loserName).filter(Boolean));
    terminal.name = 'Finals';
    terminal.matchNames = {};
    terminal.matches.forEach((match) => {
      const participants = match.teams?.map((team) => team.name) || [];
      if (participants.length === 2 && participants.every((name) => semifinalWinners.has(name))) terminal.matchNames[match.sourceMatchKey] = 'Final';
      else if (participants.length === 2 && participants.every((name) => semifinalLosers.has(name))) terminal.matchNames[match.sourceMatchKey] = 'Bronze match';
    });
    terminal.matches.sort((a, b) => {
      const priority = { Final: 0, 'Bronze match': 1 };
      return (priority[terminal.matchNames[a.sourceMatchKey]] ?? 2) - (priority[terminal.matchNames[b.sourceMatchKey]] ?? 2);
    });
  }

  // Work backwards from the final. The winner feeding the home slot of the
  // upper match is placed above the winner feeding its away slot.
  for (let roundIndex = rounds.length - 2; roundIndex >= 0; roundIndex -= 1) {
    const nextRound = rounds[roundIndex + 1];
    rounds[roundIndex].matches.sort((a, b) => {
      const feederPosition = (match) => {
        const winner = winnerName(match);
        for (let nextIndex = 0; nextIndex < nextRound.matches.length; nextIndex += 1) {
          const teamIndex = nextRound.matches[nextIndex].teams?.findIndex((team) => team.name === winner) ?? -1;
          if (teamIndex >= 0) return nextIndex * 2 + teamIndex;
        }
        return Number.MAX_SAFE_INTEGER;
      };
      return feederPosition(a) - feederPosition(b) || matchTime(a) - matchTime(b);
    });
  }
  return rounds;
}

function PlayoffBracket({ phase }) {
  const explicitRounds = [...(phase.stages || [])].sort((a, b) => (a.step ?? 0) - (b.step ?? 0) || (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
  const rounds = explicitRounds.length === 1 && explicitRounds[0].matches?.length > 1
    ? inferPlayoffRounds(explicitRounds[0].matches) : explicitRounds;
  const bronze = rounds.flatMap((round) => round.matches.filter((match) => round.matchNames?.[match.sourceMatchKey] === 'Bronze match'))[0];
  const mainMatches = rounds.flatMap((round) => round.matches.filter((match) => round.matchNames?.[match.sourceMatchKey] !== 'Bronze match'));
  const bracketMatches = mainMatches.map((match, matchIndex) => {
    const currentRound = rounds.find((round) => round.matches.includes(match));
    const winner = winnerName(match);
    const next = mainMatches.slice(matchIndex + 1).find((candidate) => candidate.teams?.some((team) => team.name === winner));
    return {
      id: match.sourceMatchKey,
      nextMatchId: next?.sourceMatchKey || null,
      participants: (match.teams || []).map((team, index) => ({
        id: team.id || { value: team.name, opus: Number(String(match.game || 'BB3').replace('BB', '')) || 3 },
        resultText: `${matchScore(match, index) ?? '-'}`,
        isWinner: team.name === winner,
        status: 'PLAYED',
        teamName: team.name,
        coachName: team.coachName,
        race: team.race,
        picture: team.logo,
      })),
      startTime: match.finishedAt || match.startedAt,
      state: match.finishedAt ? 'DONE' : null,
      tournamentRoundText: currentRound?.name,
    };
  });
  const bronzeBracketMatch = bronze && bracketMatches.find((match) => match.id === bronze.sourceMatchKey);
  const bronzeWinner = bronze && winnerName(bronze);
  const bronzeParties = bronze && bronze.teams.map((team, index) => ({ id: team.id || { value: team.name, opus: 3 }, resultText: `${matchScore(bronze, index) ?? '-'}`, teamName: team.name, coachName: team.coachName, race: team.race, picture: team.logo }));
  const ResponsiveBracket = ({ children }) => <Box w="full" sx={{ '& > svg': { display: 'block', width: '100%', height: 'auto' } }}>{children}</Box>;
  return <Box w="full" overflow="visible">{bracketMatches.length > 0 && <SingleEliminationBracket matches={bracketMatches} matchComponent={MatchComponent} svgWrapper={ResponsiveBracket} />}{bronze && <Box maxW="20rem" ml="auto" mt={4}><Heading size="sm" textAlign="center">Bronze match</Heading><MatchComponent match={{ ...bronzeBracketMatch, id: bronze.sourceMatchKey, state: 'DONE' }} topParty={bronzeParties[0]} bottomParty={bronzeParties[1]} topWon={bronzeParties[0].teamName === bronzeWinner} bottomWon={bronzeParties[1].teamName === bronzeWinner} topHovered={false} bottomHovered={false} connectorColor="gray.500" onMouseEnter={() => {}} onMouseLeave={() => {}} onMatchClick={() => {}} /></Box>}</Box>;
}

function RichMatchCard({ match }) {
  const winner = winnerName(match);
  const parties = (match.teams || []).map((team, index) => ({
    id: team.id || { value: team.name, opus: Number(String(match.game || 'BB3').replace('BB', '')) || 3 },
    resultText: `${matchScore(match, index) ?? '-'}`,
    teamName: team.name,
    coachName: team.coachName,
    race: team.race,
    picture: team.logo,
  }));
  if (parties.length < 2) return null;
  return <Box borderWidth="1px" borderRadius="md" p={2}><MatchComponent match={{ id: match.sourceMatchKey, state: match.finishedAt ? 'DONE' : null, startTime: match.finishedAt || match.startedAt }} topParty={parties[0]} bottomParty={parties[1]} topWon={parties[0].teamName === winner} bottomWon={parties[1].teamName === winner} topHovered={false} bottomHovered={false} connectorColor="gray.500" topText={match.finishedAt || match.startedAt} onMouseEnter={() => {}} onMouseLeave={() => {}} onMatchClick={() => {}} /></Box>;
}

function LatestResults({ season }) {
  return <><Heading size="sm" mb={2}>Latest results</Heading><SimpleGrid columns={{ base: 1, md: 2, xl: 3 }} spacing={3}>{(season?.recentMatches || []).map((recent) => <Box key={`${recent.stageId}-${recent.match.sourceMatchKey}`}><Text color="gray.500" fontSize="xs" mb={1}>{[recent.phaseName, recent.stageName].filter(Boolean).join(' · ')}</Text><RichMatchCard match={recent.match} /></Box>)}</SimpleGrid>{!season?.recentMatches?.length && <Text color="gray.500">No results yet</Text>}</>;
}

function LeagueSystems({ summaries, leagueSystem, onSelectSystem, onSelectSeason }) {
  const seasons = orderedSeasons(leagueSystem);
  const selectedSeason = seasons.find((season) => (season.phases || []).some((phase) => (phase.stages || []).some((stage) => stage.matches?.length))) || seasons[0];
  const phasesWithMatches = (selectedSeason?.phases || []).filter((phase) => (phase.stages || []).some((stage) => stage.matches?.length));
  const activePhase = [...phasesWithMatches].sort((a, b) => (b.sequence ?? 0) - (a.sequence ?? 0))[0] || selectedSeason?.phases?.[0];
  const primary = summaries.find((item) => item.id === leagueSystem?.id)?.primary;
  const isPlayoffs = activePhase?.type === 'PLAYOFFS';
  return <VStack align="stretch" spacing={4} w="full">
    <HStack justify="space-between"><HStack><Heading size="md">{leagueSystem?.name || 'League system'}</Heading>{primary && <Badge colorScheme="blue">Primary</Badge>}</HStack><Menu><MenuButton as={IconButton} icon={<HamburgerIcon />} aria-label="Select league system" variant="outline" /><MenuList>{summaries.map((item) => <MenuItem key={item.id} onClick={() => onSelectSystem(item.id)}>{item.primary ? '★ ' : ''}{item.name || item.id}</MenuItem>)}</MenuList></Menu></HStack>
    {selectedSeason && <HStack justify="space-between"><Heading size="sm">{selectedSeason.name || `Season ${selectedSeason.number}`}</Heading><Menu><MenuButton as={IconButton} icon={<HamburgerIcon />} aria-label="Select season" size="sm" variant="ghost" /><MenuList>{seasons.map((season) => <MenuItem key={season.id} onClick={() => onSelectSeason(season.id)}>{season.name || `Season ${season.number}`}</MenuItem>)}</MenuList></Menu></HStack>}
    {activePhase && <Box borderWidth={isPlayoffs ? 0 : '1px'} borderRadius="md" p={isPlayoffs ? 1 : 4} w={isPlayoffs ? 'calc(100vw - 1rem)' : 'full'} maxW="none" alignSelf="flex-start"><Heading size="md" mb={4}>{activePhase.name}</Heading>{isPlayoffs ? <PlayoffBracket phase={activePhase} /> : <SimpleGrid columns={{ base: 1, xl: Math.min(2, activePhase.stages?.length || 1) }} spacing={6}>{(activePhase.stages || []).map((stage) => <GroupTable key={stage.id} stage={stage} />)}</SimpleGrid>}</Box>}
    <LatestResults season={selectedSeason} />
  </VStack>;
}

export default LeagueSystems;
