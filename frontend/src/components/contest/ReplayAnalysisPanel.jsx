import React from 'react';
import {
  Alert,
  AlertIcon,
  Badge,
  Box,
  Button,
  Heading,
  HStack,
  SimpleGrid,
  Spinner,
  Stat,
  StatLabel,
  StatNumber,
  Table,
  TableContainer,
  Tbody,
  Td,
  Text,
  Th,
  Thead,
  Tr,
  VStack,
} from '@chakra-ui/react';

import MatchTimelineBar from './MatchTimelineBar';

const BLOCK_FACES = [
  ['skull', '☠', 'Skull'],
  ['bothDown', '⇅', 'Both down'],
  ['push', '➜', 'Push'],
  ['tackle', '✦', 'Defender stumbles'],
  ['defenderDown', '★', 'Defender down'],
];

const KICKOFF_OUTCOMES = {
  2: 'Get the Ref',
  3: 'Time-Out',
  4: 'Solid Defence',
  5: 'High Kick',
  6: 'Cheering Fans',
  7: 'Brilliant Coaching',
  8: 'Changing Weather',
  9: 'Quick Snap',
  10: 'Blitz',
  11: 'Officious Ref',
  12: 'Pitch Invasion',
};

const formatBytes = (value) => value == null ? '—' : value < 1024 * 1024
  ? `${Math.round(value / 1024)} KiB`
  : `${(value / 1024 / 1024).toFixed(1)} MiB`;

export const rowTeamIndex = (row) => {
  for (const value of [row?.teamIndex, row?.sourceTeamId, row?.teamId]) {
    if (typeof value === 'number' && Number.isInteger(value)) return value;
    if (typeof value === 'string' && /^\d+$/.test(value)) return Number(value);
  }
  return -1;
};

const teamName = (match, index) => match?.teams?.[index]?.name || `Team ${index + 1}`;

const rowsForTeam = (rows, teamIndex) => rows.filter((row) => rowTeamIndex(row) === teamIndex);

const findTeamRow = (rows, teamIndex, predicate) => rows
  .find((row) => rowTeamIndex(row) === teamIndex && predicate(row));

const unique = (values) => [...new Set(values.filter((value) => value != null))];

const actionTotal = (analysis) => Array.isArray(analysis?.canonicalActions)
  ? analysis.canonicalActions.length
  : (analysis?.actionStatistics || []).reduce((sum, row) => sum + Number(row.total || 0), 0);

const difficultyOrder = (value) => {
  if (typeof value === 'string') {
    const match = value.match(/^(\d+)\+$/);
    if (match) return Number(match[1]);
  }
  return Number.MAX_SAFE_INTEGER;
};

const orderedTargets = (rows) => unique(rows.map((row) => row.target))
  .sort((left, right) => difficultyOrder(left) - difficultyOrder(right) || String(left).localeCompare(String(right)));

const formatSuccessTotal = (row) => row ? `${row.success || 0}/${row.total || 0}` : '—';

const sumActionRows = (rows) => rows.reduce((totals, row) => ({
  success: totals.success + Number(row?.success || 0),
  total: totals.total + Number(row?.total || 0),
}), { success: 0, total: 0 });

function D6Table({ rows, match, title = 'D6 actions' }) {
  if (!rows.length) return null;
  const actionTypes = unique(rows.map((row) => row.eventType));
  const targets = orderedTargets(rows);

  return <Box>
    <Heading size="sm" mb={2}>{title}</Heading>
    <Text fontSize="sm" color="gray.500" mb={2}>Successful actions / attempted actions. Rerolls belong to the same action.</Text>
    <TableContainer>
      <Table size="sm">
        <Thead>
          <Tr>
            <Th rowSpan={2}>Action</Th>
            {[0, 1].map((team) => (
              <Th key={`team-${team}`} textAlign="center" colSpan={targets.length + 1}>{teamName(match, team)}</Th>
            ))}
          </Tr>
          <Tr>
            {[0, 1].flatMap((team) => [
              ...targets.map((target) => <Th key={`${team}-${target}`} isNumeric>{target}</Th>),
              <Th key={`${team}-total`} isNumeric>Total</Th>,
            ])}
          </Tr>
        </Thead>
        <Tbody>{actionTypes.map((eventType) => {
          const teamRows = [0, 1].map((team) => rowsForTeam(rows, team).filter((row) => row.eventType === eventType));
          const totals = teamRows.map((entries) => sumActionRows(entries));
          return <Tr key={eventType}>
            <Td fontWeight="semibold">{eventType}</Td>
            {teamRows.flatMap((entries, team) => [
              ...targets.map((target) => {
                const row = entries.find((entry) => entry.target === target);
                return <Td key={`${team}-${eventType}-${target}`} isNumeric>{formatSuccessTotal(row)}</Td>;
              }),
              <Td key={`${team}-${eventType}-total`} isNumeric fontWeight="semibold">{formatSuccessTotal(totals[team])}</Td>,
            ])}
          </Tr>;
        })}</Tbody>
      </Table>
    </TableContainer>
  </Box>;
}

function BlockFaceTable({ rows, match }) {
  if (!rows.length) return null;
  const targets = unique(rows.map((row) => row.target));
  return <Box>
    <Heading size="sm" mb={2}>Selected block dice</Heading>
    <Text fontSize="sm" color="gray.500" mb={2}>Counts the selected face, not every die offered. Negative dice mean the defender chose the result.</Text>
    <TableContainer>
      <Table size="sm">
        <Thead>
          <Tr><Th rowSpan={2}>Dice</Th><Th textAlign="center" colSpan={5}>{teamName(match, 0)}</Th><Th textAlign="center" colSpan={5}>{teamName(match, 1)}</Th></Tr>
          <Tr>{[0, 1].flatMap((team) => BLOCK_FACES.map(([key, symbol, label]) => <Th key={`${team}-${key}`} isNumeric title={label} aria-label={label}>{symbol}</Th>))}</Tr>
        </Thead>
        <Tbody>{targets.map((target) => {
          const teamRows = [0, 1].map((team) => findTeamRow(rows, team, (row) => row.target === target));
          return <Tr key={target}>
            <Td fontWeight="semibold">{target}</Td>
            {teamRows.flatMap((row, team) => BLOCK_FACES.map(([key]) => <Td key={`${team}-${key}`} isNumeric>{row?.[key] || 0}</Td>))}
          </Tr>;
        })}</Tbody>
      </Table>
    </TableContainer>
  </Box>;
}

function BlockOutcomeTable({ rows, match }) {
  if (!rows.length) return null;
  const targets = unique(rows.map((row) => row.target));
  return <Box>
    <Heading size="sm" mb={2}>Resolved block outcomes</Heading>
    <Text fontSize="sm" color="gray.500" mb={2}>Success = opponent down or surfed. Push and both-down are neutral; attacker down is a failure.</Text>
    <TableContainer>
      <Table size="sm">
        <Thead>
          <Tr><Th rowSpan={2}>Dice</Th><Th textAlign="center" colSpan={4}>{teamName(match, 0)}</Th><Th textAlign="center" colSpan={4}>{teamName(match, 1)}</Th></Tr>
          <Tr>{[0, 1].flatMap((team) => ['Success', 'Neutral', 'Fail', 'Total'].map((label) => <Th key={`${team}-${label}`} isNumeric>{label}</Th>))}</Tr>
        </Thead>
        <Tbody>{targets.map((target) => {
          const teamRows = [0, 1].map((team) => findTeamRow(rows, team, (row) => row.target === target));
          return <Tr key={target}>
            <Td fontWeight="semibold">{target}</Td>
            {teamRows.flatMap((row, team) => ['success', 'neutral', 'fail', 'total'].map((key) => <Td key={`${team}-${key}`} isNumeric>{row?.[key] || 0}</Td>))}
          </Tr>;
        })}</Tbody>
      </Table>
    </TableContainer>
  </Box>;
}

function SpecialActionTable({ rows, match }) {
  if (!rows.length) return null;
  const actions = unique(rows.map((row) => row.eventType));
  const display = (row) => row ? `${row.success || 0} / ${row.neutral || 0} / ${row.fail || 0} · ${row.total || 0}` : '—';
  return <Box>
    <Heading size="sm" mb={2}>Special actions</Heading>
    <Text fontSize="sm" color="gray.500" mb={2}>Success / neutral / fail · total. Special-action dice are not double-counted as ordinary D6 actions.</Text>
    <TableContainer>
      <Table size="sm">
        <Thead><Tr><Th>Action</Th><Th isNumeric>{teamName(match, 0)}</Th><Th isNumeric>{teamName(match, 1)}</Th></Tr></Thead>
        <Tbody>{actions.map((action) => <Tr key={action}>
          <Td fontWeight="semibold">{action}</Td>
          <Td isNumeric>{display(findTeamRow(rows, 0, (row) => row.eventType === action))}</Td>
          <Td isNumeric>{display(findTeamRow(rows, 1, (row) => row.eventType === action))}</Td>
        </Tr>)}</Tbody>
      </Table>
    </TableContainer>
  </Box>;
}

const diceRowsForTeam = (rows, team) => rows.filter((row) => rowTeamIndex(row) === team);

const resultCount = (rows, result) => rows.reduce(
  (sum, row) => sum + Number(row?.resultCounts?.[String(result)] || 0), 0,
);

const resultTotal = (rows) => rows.reduce(
  (sum, row) => sum + Object.values(row?.resultCounts || {}).reduce((subtotal, count) => subtotal + Number(count || 0), 0), 0,
);

function DiceHistogramTable({ rows, match, title, outcomes, description, outcomeLabels, rotateOutcomeHeaders = false, matchWide = false }) {
  if (!rows.length) return null;
  const lanes = matchWide ? [-1] : [0, 1];
  return <Box>
    <Heading size="sm" mb={2}>{title}</Heading>
    {description && <Text fontSize="sm" color="gray.500" mb={2}>{description}</Text>}
    <TableContainer>
      <Table size="sm">
        <Thead>
          <Tr>
            <Th>Team</Th>
            {outcomes.map((value) => {
              const label = outcomeLabels?.[value];
              return <Th
                key={value}
                isNumeric={!rotateOutcomeHeaders}
                height={rotateOutcomeHeaders ? '92px' : undefined}
                minWidth={rotateOutcomeHeaders ? '62px' : undefined}
                verticalAlign="bottom"
                px={rotateOutcomeHeaders ? 1 : undefined}
              >{rotateOutcomeHeaders
                ? <Box transform="rotate(-45deg)" transformOrigin="bottom left" whiteSpace="nowrap">{value} – {label}</Box>
                : (label ? `${value} – ${label}` : value)}
              </Th>;
            })}
            <Th isNumeric>Total</Th>
          </Tr>
        </Thead>
        <Tbody>{lanes.map((team) => {
          const teamRows = diceRowsForTeam(rows, team);
          return <Tr key={team}>
            <Td fontWeight="semibold" whiteSpace="nowrap">{team < 0 ? 'Match' : teamName(match, team)}</Td>
            {outcomes.map((value) => <Td key={`${team}-${value}`} isNumeric>{resultCount(teamRows, value) || '—'}</Td>)}
            <Td isNumeric fontWeight="semibold">{resultTotal(teamRows) || '—'}</Td>
          </Tr>;
        })}</Tbody>
      </Table>
    </TableContainer>
  </Box>;
}

const diceDisplay = (rows) => rows.length
  ? rows.flatMap((row) => Object.entries(row.resultCounts || {}).flatMap(([value, count]) => Array(Number(count)).fill(value))).join(', ')
  : '—';

function DiceContextTable({ rows, match, title, description }) {
  if (!rows.length) return null;
  const labels = unique(rows.map((row) => `${row.label}\u0000${row.dieTypeName || 'Unknown'}`));
  return <Box>
    <Heading size="sm" mb={2}>{title}</Heading>
    {description && <Text fontSize="sm" color="gray.500" mb={2}>{description}</Text>}
    <TableContainer>
      <Table size="sm">
        <Thead><Tr><Th>Roll</Th><Th>Die</Th><Th>{teamName(match, 0)}</Th><Th>{teamName(match, 1)}</Th><Th>Match</Th></Tr></Thead>
        <Tbody>{labels.map((key) => {
          const [label, dieTypeName] = key.split('\u0000');
          const matching = rows.filter((row) => row.label === label && (row.dieTypeName || 'Unknown') === dieTypeName);
          const neutral = matching.filter((row) => rowTeamIndex(row) < 0);
          const inferred = matching.some((row) => row.inferred);
          return <Tr key={key}>
            <Td fontWeight="semibold">{label}</Td>
            <Td title={inferred ? 'Die type inferred from replay context' : undefined}>{dieTypeName}{inferred ? '*' : ''}</Td>
            <Td>{diceDisplay(diceRowsForTeam(matching, 0))}</Td>
            <Td>{diceDisplay(diceRowsForTeam(matching, 1))}</Td>
            <Td>{diceDisplay(neutral)}</Td>
          </Tr>;
        })}</Tbody>
      </Table>
    </TableContainer>
  </Box>;
}

const timelineTeam = (event, match) => {
  const index = rowTeamIndex(event);
  return index >= 0 ? teamName(match, index) : null;
};

const timelinePosition = (event) => [
  event.half ? `Half ${event.half}` : null,
  event.drive ? `Drive ${event.drive}` : null,
  event.turn != null ? `Turn ${event.turn}` : null,
].filter(Boolean).join(' · ');

const diceExpression = (details = {}) => {
  const dice = details.dice || [];
  if (!dice.length) return null;
  const raw = dice.join(' + ');
  const total = details.rawTotal ?? dice.reduce((sum, value) => sum + Number(value || 0), 0);
  if (details.modifiedTotal != null && details.modifiedTotal !== total) {
    return `${raw} = ${total} → ${details.modifiedTotal}`;
  }
  return dice.length > 1 ? `${raw} = ${total}` : raw;
};

function WeatherPanel({ events }) {
  if (!events.length) return null;
  const current = [...events].reverse().find((event) => event.details?.weather);
  return <Box>
    <Heading size="sm" mb={2}>Weather</Heading>
    {current && <Text fontSize="lg" fontWeight="semibold" mb={2}>{current.details.weather}</Text>}
    <VStack align="stretch" spacing={2}>
      {events.map((event, index) => <Box key={event.id} borderWidth="1px" borderRadius="md" p={3}>
        <Text fontSize="xs" color="gray.500">{index === 0 ? 'Starting weather' : timelinePosition(event) || 'Weather change'}</Text>
        <HStack flexWrap="wrap">
          {diceExpression(event.details) && <Text>{diceExpression(event.details)}</Text>}
          {event.details?.weather && <Badge>{event.details.weather}</Badge>}
          {event.details?.tableName && <Text fontSize="sm" color="gray.500">{event.details.tableName} weather table</Text>}
        </HStack>
      </Box>)}
    </VStack>
  </Box>;
}

function MatchTimeline({ events, match }) {
  if (!events.length) return null;
  const children = events.reduce((map, event) => {
    if (!event.parentEventId) return map;
    map[event.parentEventId] = [...(map[event.parentEventId] || []), event];
    return map;
  }, {});
  const roots = events.filter((event) => !event.parentEventId && event.type !== 'WEATHER');

  const renderEvent = (event, nested = false) => {
    const team = timelineTeam(event, match);
    const details = event.details || {};
    const roll = diceExpression(details);
    return <Box key={event.id} ml={nested ? 5 : 0} pl={3} py={2} borderLeftWidth={nested ? '2px' : '3px'}>
      <Box>
        <Text fontSize="xs" color="gray.500">{timelinePosition(event) || `Replay step ${event.sequence}`}</Text>
        <HStack flexWrap="wrap">
          <Text fontWeight="semibold">{event.title}</Text>
          {team && <Badge>{team}</Badge>}
          {event.sppAwarded != null && <Badge colorScheme="purple">+{event.sppAwarded} SPP</Badge>}
        </HStack>
        {event.type === 'KICKOFF' && <Text fontSize="sm">
          {roll || 'Kick-off'}
          {details.resultName ? ` → ${details.resultName}` : details.resultId != null ? ` → result ${details.resultId}` : ''}
        </Text>}
        {event.type !== 'KICKOFF' && roll && <Text fontSize="sm">{roll}</Text>}
        {event.score && <Text fontSize="sm">Score {event.score.home}–{event.score.away}</Text>}
      </Box>
      {(children[event.id] || []).map((child) => renderEvent(child, true))}
    </Box>;
  };

  return <Box>
    <Heading size="sm" mb={2}>Match timeline</Heading>
    <Text fontSize="sm" color="gray.500" mb={2}>Chronological key events. Explicit SPP-awarding replay events are highlighted.</Text>
    <VStack align="stretch" spacing={1}>{roots.map((event) => renderEvent(event))}</VStack>
  </Box>;
}


export default function ReplayAnalysisPanel({ replay, match, loading, error, onDownload }) {
  if (loading) return <HStack><Spinner size="sm"/><Text>Hämtar replayinformation…</Text></HStack>;
  if (error) return <Text color="red.500">{error}</Text>;
  if (!replay?.available) return <Text color="gray.500">Ingen replay har laddats ned för den här matchen ännu.</Text>;

  const analysis = replay.analysis;
  const stats = analysis?.actionStatistics || [];
  const d6 = stats.filter((row) => row.kind === 'd6');
  const actionD6 = d6.filter((row) => (
    (!row.rollCategory || row.rollCategory === 'action')
    && row.eventType !== 'Bomb Explosion Hit'
  ));
  const traitD6 = d6.filter((row) => row.rollCategory === 'skillTrait');
  const recoveryD6 = d6.filter((row) => row.rollCategory === 'injuryRecovery');
  const systemD6 = d6.filter((row) => !['action', 'skillTrait', 'injuryRecovery'].includes(row.rollCategory) && row.rollCategory);
  const blockFaces = stats.filter((row) => row.kind === 'blockFaces');
  const blockActions = stats.filter((row) => row.kind === 'blockActions');
  const bombExplosionSpecials = d6
    .filter((row) => row.eventType === 'Bomb Explosion Hit')
    .map((row) => ({
      ...row,
      kind: 'special',
      neutral: 0,
      fail: Math.max(0, Number(row.total || 0) - Number(row.success || 0)),
      unknown: 0,
    }));
  const specials = [...stats.filter((row) => row.kind === 'special'), ...bombExplosionSpecials];
  const diceStats = analysis?.diceStatistics || [];
  const armourDice = diceStats.filter((row) => row.category === 'injury' && row.label === 'Armour');
  const injuryDice = diceStats.filter((row) => row.category === 'injury' && row.label === 'Injury');
  const casualtyDice = diceStats.filter((row) => row.category === 'injury' && row.label === 'Casualty');
  const kickoffRollOffDice = diceStats.filter((row) => row.category === 'kickoff' && row.label !== 'Kick-off Table');
  const kickoffTableDice = diceStats.filter((row) => row.category === 'kickoff' && row.label === 'Kick-off Table');
  const matchEvents = analysis?.matchEvents || [];
  const weatherEvents = analysis?.weatherEvents || matchEvents.filter((event) => event.type === 'WEATHER');
  const narrativeTimeline = analysis?.timeline?.format === 'pybb3-narrative-timeline'
    ? analysis.timeline
    : null;
  const hasCanonical = stats.length > 0 || (analysis?.canonicalActions?.length || 0) > 0;

  return <VStack align="stretch" spacing={5}>
    <HStack justify="space-between" align="start" flexWrap="wrap">
      <Box>
        <HStack flexWrap="wrap">
          <Badge colorScheme="green">Replay available</Badge>
          <Badge colorScheme={replay.analysisStatus === 'PROCESSED' ? 'blue' : replay.analysisStatus === 'FAILED' ? 'red' : 'orange'}>{replay.analysisStatus || 'PENDING'}</Badge>
          {analysis?.sourceFormat && <Badge>{analysis.sourceFormat}</Badge>}
          {hasCanonical && <Badge colorScheme="purple">Canonical actions</Badge>}
        </HStack>
        <Text mt={1} fontSize="sm" color="gray.500">Original {formatBytes(replay.originalSize)} · compact {formatBytes(replay.compactSize)}</Text>
      </Box>
      <Button size="sm" colorScheme="blue" onClick={onDownload}>Download {replay.originalFormat === 'BBR' ? 'original .bbr' : 'stored replay'}</Button>
    </HStack>

    {!analysis && <Text color="gray.500">Replayen finns, men analysen är inte klar ännu.</Text>}
    {analysis && <>
      {analysis.analysisConfidence === 'RAW_UNMAPPED' && <Alert status="warning"><AlertIcon/>Den här analysen kommer från den äldre råevent-parsern. Kör om replayanalysen för canonical action-statistik.</Alert>}
      {analysis.analysisConfidence === 'RAW_BB2' && <Alert status="warning"><AlertIcon/>BB2-replayen är importerad och rådata är bevarad, men canonical BB2-actionmappning är ännu inte implementerad.</Alert>}
      <SimpleGrid columns={{ base: 2, md: 2 }} spacing={3}>
        <Stat borderWidth="1px" borderRadius="md" p={3}><StatLabel>Actions</StatLabel><StatNumber>{actionTotal(analysis)}</StatNumber></Stat>
        <Stat borderWidth="1px" borderRadius="md" p={3}><StatLabel>Replay steps</StatLabel><StatNumber>{analysis.stepCount || 0}</StatNumber></Stat>
      </SimpleGrid>

      {!hasCanonical && analysis.analysisConfidence !== 'RAW_BB2' && <Alert status="info"><AlertIcon/>Ingen canonical action-statistik hittades i den lagrade analysen. Reanalysera replayen med parser version 4 eller senare.</Alert>}
      <WeatherPanel events={weatherEvents}/>
      {analysis.sourceFormat === 'BB3' && !narrativeTimeline && <Alert status="info"><AlertIcon/>
        Den lagrade replayanalysen saknar pybb3 narrative timeline. Reanalysera replayen för att bygga tidslinjen.
      </Alert>}
      <MatchTimelineBar timeline={narrativeTimeline} events={narrativeTimeline ? [] : matchEvents} match={match}/>
      <D6Table rows={actionD6} match={match} title="Actions"/>
      <D6Table rows={traitD6} match={match} title="Skill & trait checks"/>
      <D6Table rows={recoveryD6} match={match} title="Injury & recovery checks"/>
      <D6Table rows={systemD6} match={match} title="Other D6 checks"/>
      <BlockFaceTable rows={blockFaces} match={match}/>
      <BlockOutcomeTable rows={blockActions} match={match}/>
      <SpecialActionTable rows={specials} match={match}/>
      <DiceContextTable rows={diceStats.filter((row) => row.category === 'pregame' && row.label !== 'Weather')} match={match} title="Pregame dice" description="Team-specific pre-match rolls such as Fan Factor. Weather has its own match-state presentation above."/>
      <DiceContextTable rows={kickoffRollOffDice} match={match} title="Kick-off event roll-offs" description="One D6 per team for kick-off events resolved by a roll-off."/>
      <DiceHistogramTable rows={kickoffTableDice} match={match} title="Kick-off table rolls" outcomes={[2,3,4,5,6,7,8,9,10,11,12]} outcomeLabels={KICKOFF_OUTCOMES} rotateOutcomeHeaders matchWide description="2D6 totals mapped to the Blood Bowl kick-off table. Component dice remain preserved in replay analysis data."/>
      <DiceHistogramTable rows={armourDice} match={match} title="Armour rolls" outcomes={[2,3,4,5,6,7,8,9,10,11,12]} description="2D6 totals. Component dice remain preserved in replay analysis data."/>
      <DiceHistogramTable rows={injuryDice} match={match} title="Injury rolls" outcomes={[2,3,4,5,6,7,8,9,10,11,12]} description="2D6 totals. Component dice remain preserved in replay analysis data."/>
      <DiceHistogramTable rows={casualtyDice} match={match} title="Casualty rolls" outcomes={[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]} description="D16 outcomes. Multiple values in one replay group are counted separately rather than added."/>
      <DiceContextTable rows={diceStats.filter((row) => !['pregame', 'injury', 'scatter', 'action', 'block', 'kickoff', 'special'].includes(row.category))} match={match} title="Other replay dice"/>
    </>}
  </VStack>;
}