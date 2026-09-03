import React from 'react';
import { HamburgerIcon } from '@chakra-ui/icons';
import { SingleEliminationBracket } from 'react-tournament-brackets/dist/cjs';
import { Badge, Box, Heading, HStack, IconButton, Menu, MenuButton, MenuItem, MenuList, Modal, ModalBody, ModalCloseButton, ModalContent, ModalHeader, ModalOverlay, SimpleGrid, Spinner, Tab, TabList, TabPanel, TabPanels, Tabs, Text, useDisclosure, VStack } from '@chakra-ui/react';
import { MatchComponent } from '../competition/KnockoutCompetition';
import Standings from '../common/Standings';
import MatchModalWithRosters from '../contest/MatchModalWithRosters';
import WarpScoresApiService from '../../WarpScoresApiService';

const orderedSeasons = (system) => [...(system?.seasons || [])].sort((a, b) => (b.sequence ?? b.number ?? 0) - (a.sequence ?? a.number ?? 0));
const matchScore = (match, index) => match.officialScore?.[index ? 'away' : 'home'] ?? match.sourceScore?.[index ? 'away' : 'home'] ?? match.teams?.[index]?.score;
const playedScore = (match, index) => match.teams?.[index]?.touchdowns ?? match.sourceScore?.[index ? 'away' : 'home'] ?? match.teams?.[index]?.score;

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
const normalizedTeamName = (name) => String(name || '').trim().toLocaleLowerCase();
const teamKey = (team) => team?.id?.key
  || (team?.id?.value != null ? `${team.id.opus ?? ''}:${team.id.value}` : null)
  || normalizedTeamName(team?.name);
const sameTeam = (first, second) => Boolean(teamKey(first)) && teamKey(first) === teamKey(second);
const winnerTeam = (match) => {
  const home = matchScore(match, 0); const away = matchScore(match, 1);
  if (home == null || away == null || home === away || !match.teams?.[0] || !match.teams?.[1]) return null;
  return match.teams[home > away ? 0 : 1];
};
const winnerName = (match) => {
  if (match.seriesWinnerName) return match.seriesWinnerName;
  return winnerTeam(match)?.name || null;
};
const resolvedWinnerTeam = (match) => match.seriesWinnerName
  ? match.teams?.find((team) => normalizedTeamName(team.name) === normalizedTeamName(match.seriesWinnerName))
  : winnerTeam(match);
const resolvedLoserTeam = (match) => {
  const winner = resolvedWinnerTeam(match);
  return winner ? match.teams?.find((team) => !sameTeam(team, winner)) : null;
};
const hasExactlyTeams = (match, expectedTeams) => {
  const participants = (match.teams || []).map(teamKey).filter(Boolean);
  return participants.length === 2 && expectedTeams.size === 2
    && participants.every((participant) => expectedTeams.has(participant));
};

export function inferPlayoffRounds(matches) {
  const unique = new Map();
  matches.filter((match) => match.countsFor?.bracket !== false).forEach((match) => {
    // Legacy imports can contain one Cyanide match as multiple Mongo documents.
    // Within a single-elimination phase, participants plus final score are stable.
    const participants = (match.teams || []).map((team, index) => `${team.name}:${playedScore(match, index)}`).sort();
    const key = participants.length === 2
      ? `${match.game || ''}:${match.platform || ''}:${matchTime(match)}:${participants.join('|')}`
      : match.sourceMatchKey || `${matchTime(match)}:${participants.join('|')}`;
    if (!unique.has(key)) unique.set(key, match);
  });
  // A knockout tie can contain one or more drawn replays. Collapse all games
  // between the same two teams into one bracket node and use the latest
  // decisive game as the representative result.
  const series = new Map();
  [...unique.values()].sort((a, b) => matchTime(a) - matchTime(b)).forEach((match) => {
    const teams = (match.teams || []).map(teamKey).filter(Boolean).sort();
    const key = teams.length === 2 ? teams.join('|') : match.sourceMatchKey;
    if (!series.has(key)) series.set(key, []);
    series.get(key).push(match);
  });
  const ordered = [...series.values()].map((seriesMatches) => {
    const chronological = [...seriesMatches].sort((a, b) => matchTime(a) - matchTime(b));
    const decisive = [...chronological].reverse().find((match) => winnerName(match));
    const representative = decisive || chronological[chronological.length - 1];
    return {
      ...representative,
      seriesMatches: chronological,
      seriesLength: chronological.length,
      replayCount: Math.max(0, chronological.length - 1),
      seriesWinnerName: decisive ? winnerName(decisive) : null,
    };
  }).sort((a, b) => matchTime(a) - matchTime(b));
  // Legacy seasons store every knockout source in one stage. Work backwards
  // from the final by following the winners feeding each participant. Sibling
  // rounds may overlap in time (a quarterfinal can start before every play-in
  // is done), so global chronological slices are not reliable.
  let terminalMatches = ordered.slice(-1);
  let terminalMatchNames = null;
  let terminalCount = Math.min(1, ordered.length);

  if (ordered.length >= 8) {
    const possibleTerminal = ordered.slice(-2);
    const possibleSemifinals = ordered.slice(-4, -2);
    const semifinalWinners = new Set(possibleSemifinals.map(resolvedWinnerTeam).map(teamKey).filter(Boolean));
    const semifinalLosers = new Set(possibleSemifinals.map(resolvedLoserTeam).map(teamKey).filter(Boolean));
    const final = possibleTerminal.find((match) => hasExactlyTeams(match, semifinalWinners));
    const bronze = possibleTerminal.find((match) => hasExactlyTeams(match, semifinalLosers));
    if (final && bronze && final !== bronze) {
      terminalCount = 2;
      terminalMatches = [final, bronze];
      terminalMatchNames = {
        [final.sourceMatchKey]: 'Final',
        [bronze.sourceMatchKey]: 'Bronze match',
      };
    }
  }

  const terminalKeys = new Set(terminalMatches.map((match) => match.sourceMatchKey));
  const remaining = ordered.filter((match) => !terminalKeys.has(match.sourceMatchKey));
  const final = terminalMatchNames
    ? terminalMatches.find((match) => terminalMatchNames[match.sourceMatchKey] === 'Final')
    : terminalMatches[0];
  const takeFeeders = (parents, candidates, expectedCount) => {
    const selected = [];
    parents.forEach((parent) => {
      (parent.teams || []).forEach((participant) => {
        const feeder = [...candidates]
          .filter((candidate) => !selected.includes(candidate)
            && matchTime(candidate) <= matchTime(parent)
            && sameTeam(resolvedWinnerTeam(candidate), participant))
          .sort((a, b) => matchTime(b) - matchTime(a))[0];
        if (feeder) selected.push(feeder);
      });
    });
    // Preserve every match even when an old import has an incomplete team ID.
    // The fallback only fills missing feeder slots; linked matches always win.
    if (selected.length < expectedCount) {
      [...candidates].filter((match) => !selected.includes(match))
        .sort((a, b) => matchTime(b) - matchTime(a))
        .slice(0, expectedCount - selected.length)
        .forEach((match) => selected.push(match));
    }
    return selected.sort((a, b) => matchTime(a) - matchTime(b));
  };
  const semifinals = final ? takeFeeders([final], remaining, Math.min(2, remaining.length)) : [];
  const semifinalKeys = new Set(semifinals.map((match) => match.sourceMatchKey));
  const beforeSemifinals = remaining.filter((match) => !semifinalKeys.has(match.sourceMatchKey));
  const quarterfinals = semifinals.length === 2 && beforeSemifinals.length >= 4
    ? takeFeeders(semifinals, beforeSemifinals, 4) : [];
  const quarterfinalKeys = new Set(quarterfinals.map((match) => match.sourceMatchKey));
  const playIns = beforeSemifinals.filter((match) => !quarterfinalKeys.has(match.sourceMatchKey));
  const rounds = [];
  if (playIns.length > 0) {
    rounds.push({ id: 'inferred-play-in', name: 'Play-in', matches: playIns });
  }
  if (quarterfinals.length > 0) {
    rounds.push({ id: 'inferred-quarterfinals', name: 'Quarterfinals', matches: quarterfinals });
  }
  if (semifinals.length > 0) {
    rounds.push({ id: 'inferred-semifinals', name: 'Semifinals', matches: semifinals });
  }
  if (terminalCount > 0) {
    rounds.push({
      id: 'inferred-finals',
      name: terminalCount === 2 ? 'Finals' : 'Final',
      matches: terminalMatches,
      ...(terminalMatchNames ? { matchNames: terminalMatchNames } : {}),
    });
  }

  // Work backwards from the final. The winner feeding the home slot of the
  // upper match is placed above the winner feeding its away slot.
  for (let roundIndex = rounds.length - 2; roundIndex >= 0; roundIndex -= 1) {
    const nextRound = rounds[roundIndex + 1];
    rounds[roundIndex].matches.sort((a, b) => {
      const feederPosition = (match) => {
        for (let nextIndex = 0; nextIndex < nextRound.matches.length; nextIndex += 1) {
          const teamIndex = nextRound.matches[nextIndex].teams?.findIndex((team) =>
            match.teams?.some((candidate) => sameTeam(candidate, team))) ?? -1;
          if (teamIndex >= 0) return nextIndex * 2 + teamIndex;
        }
        return Number.MAX_SAFE_INTEGER;
      };
      return feederPosition(a) - feederPosition(b) || matchTime(a) - matchTime(b);
    });
  }
  return rounds;
}

export function nextPlayoffMatch(rounds, match) {
  const currentRoundIndex = rounds.findIndex((round) => round.matches.includes(match));
  if (currentRoundIndex < 0) return null;
  const nextRound = rounds[currentRoundIndex + 1];
  if (!nextRound) return null;
  const eligibleNextMatches = nextRound.matches
    .filter((candidate) => nextRound.matchNames?.[candidate.sourceMatchKey] !== 'Bronze match')
  const participantMatch = eligibleNextMatches.find((candidate) => candidate.teams?.some((team) =>
    match.teams?.some((participant) => sameTeam(participant, team))));
  if (participantMatch) return participantMatch;

  // Keep incomplete/legacy brackets connected even when a team identity or
  // name changed between rounds. Equal-sized preliminary rounds map one to
  // one; ordinary knockout rounds map adjacent pairs to the same next match.
  const currentMatches = rounds[currentRoundIndex].matches;
  const currentIndex = currentMatches.indexOf(match);
  const fallbackIndex = Math.min(
    eligibleNextMatches.length - 1,
    Math.floor(currentIndex * eligibleNextMatches.length / Math.max(1, currentMatches.length)));
  return fallbackIndex >= 0 ? eligibleNextMatches[fallbackIndex] : null;
}

export function bracketCanvasHeight(rounds, style) {
  const mainMatchCounts = rounds.map((round) => round.matches
    .filter((match) => round.matchNames?.[match.sourceMatchKey] !== 'Bronze match').length);
  const requiredRows = Math.max(1, ...mainMatchCounts.map((count, columnIndex) => count * (2 ** columnIndex)));
  return requiredRows * (style.boxHeight + style.spaceBetweenRows)
    + style.canvasPadding * 2
    + (style.roundHeader.isShown ? style.roundHeader.height + style.roundHeader.marginBottom : 0);
}

function PlayoffBracket({ phase, onMatchClick }) {
  const explicitRounds = [...(phase.stages || [])].sort((a, b) => (a.step ?? 0) - (b.step ?? 0) || (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
  const rounds = explicitRounds.length === 1 && explicitRounds[0].matches?.length > 1
    ? inferPlayoffRounds(explicitRounds[0].matches) : explicitRounds;
  const bronze = rounds.flatMap((round) => round.matches.filter((match) => round.matchNames?.[match.sourceMatchKey] === 'Bronze match'))[0];
  const mainMatches = rounds.flatMap((round) => round.matches.filter((match) => round.matchNames?.[match.sourceMatchKey] !== 'Bronze match'));
  const bracketMatches = mainMatches.map((match) => {
    const currentRound = rounds.find((round) => round.matches.includes(match));
    const next = nextPlayoffMatch(rounds, match);
    const feederSlot = next?.teams?.findIndex((team) => match.teams?.some((participant) => sameTeam(participant, team))) ?? -1;
    const advancingTeam = feederSlot >= 0 ? next.teams[feederSlot]?.name : null;
    const winner = advancingTeam || winnerName(match);
    return {
      id: match.sourceMatchKey,
      nextMatchId: next?.sourceMatchKey || null,
      name: feederSlot >= 0 ? `slot-${feederSlot}` : match.sourceMatchKey,
      feederSlot,
      participants: (match.teams || []).map((team, index) => ({
        id: team.id || { value: team.name, opus: Number(String(match.game || 'BB3').replace('BB', '')) || 3 },
        resultText: match.seriesMatches?.length > 1
          ? match.seriesMatches.map((seriesMatch) => playedScore(seriesMatch, index) ?? '-').join(' / ')
          : `${playedScore(match, index) ?? '-'}`,
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
      seriesLength: match.seriesLength,
      replayCount: match.replayCount,
      compact: true,
      seriesResults: match.seriesMatches?.map((seriesMatch, index) => ({
        id: seriesMatch.sourceMatchKey,
        label: `Match ${index + 1}`,
        score: `${playedScore(seriesMatch, 0) ?? '-'}–${playedScore(seriesMatch, 1) ?? '-'}`,
        date: seriesMatch.finishedAt || seriesMatch.startedAt,
        deciding: seriesMatch.sourceMatchKey === match.sourceMatchKey,
      })),
    };
  });
  // The bracket package only lays out a strict binary tree. A directly seeded
  // team is not represented by a match, so add an invisible feeder for that
  // slot. This keeps one play-in plus one bye aligned with its quarterfinal.
  const matchesById = new Map(bracketMatches.map((match) => [match.id, match]));
  rounds.slice(1).forEach((targetRound, roundIndex) => {
    const sourceRound = rounds[roundIndex];
    const sourceIds = new Set(sourceRound.matches.map((match) => match.sourceMatchKey));
    targetRound.matches
      .filter((match) => targetRound.matchNames?.[match.sourceMatchKey] !== 'Bronze match')
      .forEach((target) => {
        const feeders = bracketMatches.filter((match) => sourceIds.has(match.id) && match.nextMatchId === target.sourceMatchKey);
        const occupiedSlots = new Set(feeders.map((match) => match.feederSlot).filter((slot) => slot >= 0));
        [0, 1].forEach((slot) => {
          if (occupiedSlots.has(slot)) return;
          const id = `bye-${target.sourceMatchKey}-${slot}`;
          if (!matchesById.has(id)) {
            const placeholder = {
              id,
              nextMatchId: target.sourceMatchKey,
              name: `slot-${slot}`,
              participants: [],
              state: null,
              tournamentRoundText: sourceRound.name,
              placeholder: true,
              compact: true,
            };
            bracketMatches.push(placeholder);
            matchesById.set(id, placeholder);
          }
        });
      });
  });
  const bronzeBracketMatch = bronze && bracketMatches.find((match) => match.id === bronze.sourceMatchKey);
  const bronzeWinner = bronze && winnerName(bronze);
  const bronzeParties = bronze && bronze.teams.map((team, index) => ({ id: team.id || { value: team.name, opus: 3 }, resultText: `${playedScore(bronze, index) ?? '-'}`, teamName: team.name, coachName: team.coachName, race: team.race, picture: team.logo }));
  const roundLabels = ['Play-in', 'Quarterfinals', 'Semifinals', 'Final'];
  const bracketStyle = { width: 240, boxHeight: 86, canvasPadding: 8, spaceBetweenColumns: 18, spaceBetweenRows: 8, roundSeparatorWidth: 8, horizontalOffset: 6, roundHeader: { isShown: true, height: 20, marginBottom: 6, fontSize: 10, roundTextGenerator: (roundNumber, totalRounds) => roundLabels[roundLabels.length - totalRounds + roundNumber - 1] || `Round ${roundNumber}` } };
  const requiredHeight = bracketCanvasHeight(rounds, bracketStyle);
  const ResponsiveBracket = ({ children, bracketWidth }) => <Box w="full">{React.cloneElement(children, {
    height: requiredHeight,
    viewBox: `0 0 ${bracketWidth} ${requiredHeight}`,
    style: { display: 'block', width: `min(100%, ${bracketWidth}px)`, height: 'auto' },
  })}</Box>;
  const bracketOptions = { style: bracketStyle };
  const BracketMatchComponent = (props) => props.match.placeholder
    ? <div style={{ width: `${props.computedStyles.width}px`, height: `${props.computedStyles.boxHeight}px`, visibility: 'hidden' }} />
    : <MatchComponent {...props} />;
  const openBracketMatch = (matchId) => {
    const selected = mainMatches.find((match) => match.sourceMatchKey === matchId);
    if (selected) onMatchClick(selected);
  };
  return <Box w="full" overflow="visible">{bracketMatches.length > 0 && <SingleEliminationBracket matches={bracketMatches} matchComponent={BracketMatchComponent} svgWrapper={ResponsiveBracket} options={bracketOptions} onMatchClick={openBracketMatch} />}{bronze && <Box maxW="20rem" ml="auto" mt={4}><Heading size="sm" textAlign="center">Bronze match</Heading><MatchComponent match={{ ...bronzeBracketMatch, id: bronze.sourceMatchKey, state: 'DONE' }} topParty={bronzeParties[0]} bottomParty={bronzeParties[1]} topWon={bronzeParties[0].teamName === bronzeWinner} bottomWon={bronzeParties[1].teamName === bronzeWinner} topHovered={false} bottomHovered={false} connectorColor="gray.500" onMouseEnter={() => {}} onMouseLeave={() => {}} onMatchClick={() => onMatchClick(bronze)} /></Box>}</Box>;
}

function RichMatchCard({ match, onMatchClick }) {
  const winner = winnerName(match);
  const parties = (match.teams || []).map((team, index) => ({
    id: team.id || { value: team.name, opus: Number(String(match.game || 'BB3').replace('BB', '')) || 3 },
    resultText: `${playedScore(match, index) ?? '-'}`,
    teamName: team.name,
    coachName: team.coachName,
    race: team.race,
    picture: team.logo,
  }));
  if (parties.length < 2) return null;
  return <Box borderWidth="1px" borderRadius="md" p={2} cursor="pointer" _hover={{ boxShadow: 'md' }} onClick={() => onMatchClick(match)}><MatchComponent match={{ id: match.sourceMatchKey, state: match.finishedAt ? 'DONE' : null, startTime: match.finishedAt || match.startedAt }} topParty={parties[0]} bottomParty={parties[1]} topWon={parties[0].teamName === winner} bottomWon={parties[1].teamName === winner} topHovered={false} bottomHovered={false} connectorColor="gray.500" topText={match.finishedAt || match.startedAt} onMouseEnter={() => {}} onMouseLeave={() => {}} onMatchClick={() => {}} /><Text mt={1} px={1} fontSize="xs" color="gray.500">{match.finishedAt || match.startedAt || 'No date'}{match.teams?.length === 2 ? ` · Casualties ${match.teams[0].casualties ?? '-'}–${match.teams[1].casualties ?? '-'}` : ''}{match.conceded ? ' · Conceded/WO' : ''}</Text></Box>;
}

export function matchesByMatchDay(matches) {
  const scheduledTime = (match) => {
    const value = match.finishedAt || match.startedAt;
    return value ? new Date(value).getTime() : Number.MAX_SAFE_INTEGER;
  };
  let remaining = [...matches].sort((first, second) => scheduledTime(first) - scheduledTime(second)
    || String(first.sourceMatchKey).localeCompare(String(second.sourceMatchKey)));
  const matchDays = [];

  // Extract one maximum matching at a time. This reconstructs round-robin
  // match days without allowing a postponed match to create an extra round.
  while (remaining.length) {
    const teamIds = [...new Set(remaining.flatMap((match) => (match.teams || []).map(teamKey).filter(Boolean)))];
    const teamIndex = new Map(teamIds.map((team, index) => [team, index]));
    const candidates = remaining.map((match, priority) => {
      const teams = (match.teams || []).map(teamKey).filter(Boolean);
      return { match, priority, first: teamIndex.get(teams[0]), second: teamIndex.get(teams[1]) };
    }).filter(({ first, second }) => first != null && second != null && first !== second);
    const incident = teamIds.map(() => []);
    candidates.forEach((candidate) => {
      incident[candidate.first].push(candidate);
      incident[candidate.second].push(candidate);
    });
    const memo = new Map();
    const bestMatching = (used) => {
      if (memo.has(used)) return memo.get(used);
      let firstFree = -1;
      for (let index = 0; index < teamIds.length; index += 1) {
        if ((used & (1n << BigInt(index))) === 0n) { firstFree = index; break; }
      }
      if (firstFree < 0) return { matches: [], cost: 0 };
      const firstBit = 1n << BigInt(firstFree);
      let best = bestMatching(used | firstBit);
      incident[firstFree].forEach((candidate) => {
        const other = candidate.first === firstFree ? candidate.second : candidate.first;
        const otherBit = 1n << BigInt(other);
        if ((used & otherBit) !== 0n) return;
        const tail = bestMatching(used | firstBit | otherBit);
        const option = { matches: [candidate.match, ...tail.matches], cost: candidate.priority + tail.cost };
        if (option.matches.length > best.matches.length
          || (option.matches.length === best.matches.length && option.cost < best.cost)) best = option;
      });
      memo.set(used, best);
      return best;
    };
    const selected = bestMatching(0n).matches;
    // Matches without usable participants cannot be paired safely.
    const matchDay = selected.length ? selected : [remaining[0]];
    matchDays.push(matchDay.sort((first, second) => scheduledTime(first) - scheduledTime(second)));
    const selectedKeys = new Set(matchDay.map((match) => match.sourceMatchKey));
    remaining = remaining.filter((match) => !selectedKeys.has(match.sourceMatchKey));
  }
  return matchDays.map((dayMatches, index) => [index + 1, dayMatches]);
}

function StageRoundMatches({ stage, onMatchClick }) {
  const rounds = matchesByMatchDay(stage.matches || []);
  let initialIndex = 0;
  rounds.forEach(([, matches], index) => { if (matches.some((match) => match.finishedAt)) initialIndex = index; });
  if (!rounds.length) return <Text color="gray.500">No matches yet</Text>;
  return <Box mt={5}><Heading size="sm" mb={2}>Matcher per omgång</Heading><Tabs defaultIndex={initialIndex} isLazy><TabList overflowX="auto">{rounds.map(([round]) => <Tab key={round} flexShrink={0}>Omgång {round}</Tab>)}</TabList><TabPanels>{rounds.map(([round, matches]) => <TabPanel key={round} px={0}><SimpleGrid columns={{ base: 1, md: 2, xl: 3 }} spacing={3}>{[...matches].sort((a, b) => matchTime(a) - matchTime(b)).map((match) => <RichMatchCard key={match.sourceMatchKey} match={match} onMatchClick={onMatchClick} />)}</SimpleGrid></TabPanel>)}</TabPanels></Tabs></Box>;
}

export const matchResourceIdFor = (summary) => summary?.matchResourceId
  || summary?.sourceMatchId?.key
  || (typeof summary?.sourceMatchId === 'string' && /^\d+_/.test(summary.sourceMatchId)
    ? summary.sourceMatchId : null);

function MatchDetails({ summary, isOpen, onClose }) {
  const [match, setMatch] = React.useState(null);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState(null);
  React.useEffect(() => {
    if (!isOpen || !summary) return;
    const matchResourceId = matchResourceIdFor(summary);
    if (!matchResourceId) {
      setMatch(null); setLoading(false); setError('Matchen saknar ett internt BlaskScore-ID.');
      return;
    }
    setMatch(null); setError(null); setLoading(true);
    WarpScoresApiService.match(matchResourceId)
      .then((result) => result ? setMatch(result) : setError('Matchstatistiken hittades inte.'))
      .catch(() => setError('Matchstatistiken kunde inte hämtas.'))
      .finally(() => setLoading(false));
  }, [isOpen, summary]);
  if (match) return <MatchModalWithRosters isOpen={isOpen} onClose={onClose} match={match} />;
  return <Modal isOpen={isOpen} onClose={onClose}><ModalOverlay /><ModalContent><ModalHeader>Matchstatistik</ModalHeader><ModalCloseButton /><ModalBody pb={6}>{loading ? <HStack><Spinner /><Text>Hämtar matchstatistik…</Text></HStack> : <Text color="red.500">{error}</Text>}</ModalBody></ModalContent></Modal>;
}

function LeagueSystems({ summaries, leagueSystem, onSelectSystem, onSelectSeason }) {
  const matchDetails = useDisclosure();
  const [selectedMatch, setSelectedMatch] = React.useState(null);
  const openMatch = (match) => { setSelectedMatch(match); matchDetails.onOpen(); };
  const seasons = orderedSeasons(leagueSystem);
  const selectedSeason = seasons.find((season) => (season.phases || []).some((phase) => (phase.stages || []).some((stage) => stage.matches?.length))) || seasons[0];
  const phasesWithMatches = (selectedSeason?.phases || []).filter((phase) => (phase.stages || []).some((stage) => stage.matches?.length));
  const playoffPhase = [...phasesWithMatches].filter((phase) => phase.type === 'PLAYOFFS').sort((a, b) => (b.sequence ?? 0) - (a.sequence ?? 0))[0];
  const groupStages = (selectedSeason?.phases || []).filter((phase) => phase.type === 'GROUP_STAGE')
    .flatMap((phase) => phase.stages || []).filter((stage) => stage.type === 'GROUP' || stage.matches?.length);
  const primary = summaries.find((item) => item.id === leagueSystem?.id)?.primary;
  return <VStack align="stretch" spacing={4} w="full">
    <HStack justify="space-between"><HStack><Heading size="md">{leagueSystem?.name || 'League system'}</Heading>{primary && <Badge colorScheme="blue">Primary</Badge>}</HStack><Menu><MenuButton as={IconButton} icon={<HamburgerIcon />} aria-label="Select league system" variant="outline" /><MenuList>{summaries.map((item) => <MenuItem key={item.id} onClick={() => onSelectSystem(item.id)}>{item.primary ? '★ ' : ''}{item.name || item.id}</MenuItem>)}</MenuList></Menu></HStack>
    {selectedSeason && <HStack justify="space-between"><Heading size="sm">{selectedSeason.name || `Season ${selectedSeason.number}`}</Heading><Menu><MenuButton as={IconButton} icon={<HamburgerIcon />} aria-label="Select season" size="sm" variant="ghost" /><MenuList>{seasons.map((season) => <MenuItem key={season.id} onClick={() => onSelectSeason(season.id)}>{season.name || `Season ${season.number}`}</MenuItem>)}</MenuList></Menu></HStack>}
    {playoffPhase && <Box borderWidth={0} p={1} w="calc(100vw - 1rem)" maxW="none" alignSelf="flex-start"><Heading size="md" mb={4}>{playoffPhase.name}</Heading><PlayoffBracket phase={playoffPhase} onMatchClick={openMatch} /></Box>}
    {groupStages.map((stage) => <Box key={stage.id} borderWidth="1px" borderRadius="md" p={4}><GroupTable stage={stage} /><StageRoundMatches stage={stage} onMatchClick={openMatch} /></Box>)}
    {!playoffPhase && groupStages.length === 0 && <Text color="gray.500">No group stage or playoff matches yet</Text>}
    <MatchDetails summary={selectedMatch} isOpen={matchDetails.isOpen} onClose={matchDetails.onClose} />
  </VStack>;
}

export default LeagueSystems;
