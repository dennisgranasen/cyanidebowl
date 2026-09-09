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

const BLOCK_FACES = [
  ['skull', '☠', 'Skull'],
  ['bothDown', '⇅', 'Both down'],
  ['push', '➜', 'Push'],
  ['tackle', '✦', 'Defender stumbles'],
  ['defenderDown', '★', 'Defender down'],
];

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

function D6Table({ rows, match }) {
  if (!rows.length) return null;
  const actionTypes = unique(rows.map((row) => row.eventType));
  const targets = orderedTargets(rows);

  return <Box>
    <Heading size="sm" mb={2}>D6 actions</Heading>
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

function RawDiceTable({ analysis }) {
  const values = Object.entries(analysis?.dieValueCounts || {}).sort((a, b) => b[1] - a[1]);
  if (!values.length) return null;
  return <Box>
    <Heading size="sm" mb={2}>Raw dice distribution</Heading>
    <Text fontSize="sm" color="gray.500" mb={2}>Raw rolls are diagnostic data and are intentionally separate from action success rates.</Text>
    <TableContainer><Table size="sm"><Thead><Tr><Th>Die / result</Th><Th isNumeric>Count</Th></Tr></Thead><Tbody>
      {values.map(([key, count]) => <Tr key={key}><Td>{key}</Td><Td isNumeric>{count}</Td></Tr>)}
    </Tbody></Table></TableContainer>
  </Box>;
}

export default function ReplayAnalysisPanel({ replay, match, loading, error, onDownload }) {
  if (loading) return <HStack><Spinner size="sm"/><Text>Hämtar replayinformation…</Text></HStack>;
  if (error) return <Text color="red.500">{error}</Text>;
  if (!replay?.available) return <Text color="gray.500">Ingen replay har laddats ned för den här matchen ännu.</Text>;

  const analysis = replay.analysis;
  const stats = analysis?.actionStatistics || [];
  const d6 = stats.filter((row) => row.kind === 'd6');
  const blockFaces = stats.filter((row) => row.kind === 'blockFaces');
  const blockActions = stats.filter((row) => row.kind === 'blockActions');
  const specials = stats.filter((row) => row.kind === 'special');
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
      <SimpleGrid columns={{ base: 2, md: 4 }} spacing={3}>
        <Stat borderWidth="1px" borderRadius="md" p={3}><StatLabel>Actions</StatLabel><StatNumber>{actionTotal(analysis)}</StatNumber></Stat>
        <Stat borderWidth="1px" borderRadius="md" p={3}><StatLabel>Raw rolls</StatLabel><StatNumber>{analysis.diceRolls?.length || 0}</StatNumber></Stat>
        <Stat borderWidth="1px" borderRadius="md" p={3}><StatLabel>Checkpoints</StatLabel><StatNumber>{analysis.checkpointCount || 0}</StatNumber></Stat>
        <Stat borderWidth="1px" borderRadius="md" p={3}><StatLabel>Replay steps</StatLabel><StatNumber>{analysis.stepCount || 0}</StatNumber></Stat>
      </SimpleGrid>

      {!hasCanonical && analysis.analysisConfidence !== 'RAW_BB2' && <Alert status="info"><AlertIcon/>Ingen canonical action-statistik hittades i den lagrade analysen. Reanalysera replayen med parser version 4 eller senare.</Alert>}
      <D6Table rows={d6} match={match}/>
      <BlockFaceTable rows={blockFaces} match={match}/>
      <BlockOutcomeTable rows={blockActions} match={match}/>
      <SpecialActionTable rows={specials} match={match}/>
      <RawDiceTable analysis={analysis}/>
    </>}
  </VStack>;
}
