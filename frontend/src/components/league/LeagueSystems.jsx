import React from 'react';
import { HamburgerIcon } from '@chakra-ui/icons';
import { Badge, Box, Card, CardBody, Heading, HStack, IconButton, Menu, MenuButton, MenuItem, MenuList, SimpleGrid, Table, TableContainer, Tbody, Td, Text, Th, Thead, Tr, VStack } from '@chakra-ui/react';

const orderedSeasons = (system) => [...(system?.seasons || [])].sort((a, b) => (b.sequence ?? b.number ?? 0) - (a.sequence ?? a.number ?? 0));
const matchScore = (match, index) => match.officialScore?.[index ? 'away' : 'home'] ?? match.sourceScore?.[index ? 'away' : 'home'] ?? match.teams?.[index]?.score;

function standingsFor(stage) {
  const teams = new Map();
  (stage.matches || []).filter((match) => match.countsFor?.standings !== false).forEach((match) => {
    if (!match.teams || match.teams.length < 2) return;
    const homeScore = matchScore(match, 0); const awayScore = matchScore(match, 1);
    if (homeScore == null || awayScore == null) return;
    match.teams.forEach((team) => { if (!teams.has(team.name)) teams.set(team.name, { name: team.name, played: 0, wins: 0, draws: 0, losses: 0, for: 0, against: 0, points: 0 }); });
    const home = teams.get(match.teams[0].name); const away = teams.get(match.teams[1].name);
    home.played += 1; away.played += 1; home.for += homeScore; home.against += awayScore; away.for += awayScore; away.against += homeScore;
    if (homeScore === awayScore) { home.draws += 1; away.draws += 1; home.points += 1; away.points += 1; }
    else if (homeScore > awayScore) { home.wins += 1; away.losses += 1; home.points += 3; }
    else { away.wins += 1; home.losses += 1; away.points += 3; }
  });
  return [...teams.values()].sort((a, b) => b.points - a.points || (b.for - b.against) - (a.for - a.against) || b.for - a.for || a.name.localeCompare(b.name));
}

function GroupTable({ stage }) {
  const standings = standingsFor(stage);
  return <Box><Heading size="sm" mb={2}>{stage.name || 'Group'}</Heading>{standings.length ? <TableContainer><Table size="sm" variant="striped"><Thead><Tr><Th>#</Th><Th>Team</Th><Th isNumeric>GP</Th><Th isNumeric>W</Th><Th isNumeric>D</Th><Th isNumeric>L</Th><Th isNumeric>TD</Th><Th isNumeric>Pts</Th></Tr></Thead><Tbody>{standings.map((team, index) => <Tr key={team.name}><Td>{index + 1}</Td><Td>{team.name}</Td><Td isNumeric>{team.played}</Td><Td isNumeric>{team.wins}</Td><Td isNumeric>{team.draws}</Td><Td isNumeric>{team.losses}</Td><Td isNumeric>{team.for}-{team.against}</Td><Td isNumeric fontWeight="bold">{team.points}</Td></Tr>)}</Tbody></Table></TableContainer> : <Text color="gray.500">No table results yet</Text>}</Box>;
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
    const key = match.sourceMatchKey || `${matchTime(match)}:${match.teams?.map((team) => team.name).join('|')}`;
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
  }
  return rounds;
}

function PlayoffBracket({ phase }) {
  const explicitRounds = [...(phase.stages || [])].sort((a, b) => (a.step ?? 0) - (b.step ?? 0) || (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
  const rounds = explicitRounds.length === 1 && explicitRounds[0].matches?.length > 1
    ? inferPlayoffRounds(explicitRounds[0].matches) : explicitRounds;
  return <Box overflowX="auto"><HStack align="stretch" spacing={4} minW="max-content">{rounds.map((round) => <VStack key={round.id} align="stretch" minW="15rem" justify="space-around"><Heading size="sm" textAlign="center">{round.name || `Round ${round.step}`}</Heading>{(round.matches || []).filter((match) => match.countsFor?.bracket !== false).map((match) => <Card key={match.sourceMatchKey} variant="outline"><CardBody py={2}>{round.matchNames?.[match.sourceMatchKey] && <Text fontSize="xs" color="gray.500" fontWeight="bold">{round.matchNames[match.sourceMatchKey]}</Text>}{(match.teams || []).map((team, index) => <HStack key={`${match.sourceMatchKey}-${team.name}`} justify="space-between"><Text>{team.name || '-'}</Text><Text fontWeight="bold">{matchScore(match, index) ?? '-'}</Text></HStack>)}</CardBody></Card>)}{!(round.matches || []).length && <Text color="gray.500" textAlign="center">No matches</Text>}</VStack>)}</HStack></Box>;
}

function LatestResults({ season }) {
  return <><Heading size="sm" mb={2}>Latest results</Heading><SimpleGrid columns={{ base: 1, md: 2, xl: 3 }} spacing={3}>{(season?.recentMatches || []).map((recent) => <Card key={`${recent.stageId}-${recent.match.sourceMatchKey}`} variant="outline" size="sm"><CardBody><Text color="gray.500" fontSize="sm">{[recent.phaseName, recent.stageName].filter(Boolean).join(' · ')}</Text><Heading size="sm">{recent.match.teams?.map((team) => team.name || '-').join(' - ')}</Heading><Text>{recent.match.teams?.map((team, index) => matchScore(recent.match, index) ?? '-').join(' - ')}</Text></CardBody></Card>)}</SimpleGrid>{!season?.recentMatches?.length && <Text color="gray.500">No results yet</Text>}</>;
}

function LeagueSystems({ summaries, leagueSystem, onSelectSystem, onSelectSeason }) {
  const seasons = orderedSeasons(leagueSystem);
  const selectedSeason = seasons.find((season) => (season.phases || []).some((phase) => (phase.stages || []).some((stage) => stage.matches?.length))) || seasons[0];
  const phasesWithMatches = (selectedSeason?.phases || []).filter((phase) => (phase.stages || []).some((stage) => stage.matches?.length));
  const activePhase = [...phasesWithMatches].sort((a, b) => (b.sequence ?? 0) - (a.sequence ?? 0))[0] || selectedSeason?.phases?.[0];
  const primary = summaries.find((item) => item.id === leagueSystem?.id)?.primary;
  return <VStack align="stretch" spacing={4}><HStack justify="space-between"><HStack><Heading size="md">{leagueSystem?.name || 'League system'}</Heading>{primary && <Badge colorScheme="blue">Primary</Badge>}</HStack><Menu><MenuButton as={IconButton} icon={<HamburgerIcon />} aria-label="Select league system" variant="outline" /><MenuList>{summaries.map((item) => <MenuItem key={item.id} onClick={() => onSelectSystem(item.id)}>{item.primary ? '★ ' : ''}{item.name || item.id}</MenuItem>)}</MenuList></Menu></HStack>{selectedSeason && <HStack justify="space-between"><Heading size="sm">{selectedSeason.name || `Season ${selectedSeason.number}`}</Heading><Menu><MenuButton as={IconButton} icon={<HamburgerIcon />} aria-label="Select season" size="sm" variant="ghost" /><MenuList>{seasons.map((season) => <MenuItem key={season.id} onClick={() => onSelectSeason(season.id)}>{season.name || `Season ${season.number}`}</MenuItem>)}</MenuList></Menu></HStack>}{activePhase && <Box borderWidth="1px" borderRadius="md" p={4}><Heading size="md" mb={4}>{activePhase.name}</Heading>{activePhase.type === 'PLAYOFFS' ? <PlayoffBracket phase={activePhase} /> : <SimpleGrid columns={{ base: 1, xl: Math.min(2, activePhase.stages?.length || 1) }} spacing={6}>{(activePhase.stages || []).map((stage) => <GroupTable key={stage.id} stage={stage} />)}</SimpleGrid>}</Box>}<LatestResults season={selectedSeason} /></VStack>;
}

export default LeagueSystems;
