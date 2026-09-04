import React from 'react';
import {
  Badge, Box, Center, Heading, HStack, Image, Tab, TabList, TabPanel, TabPanels,
  Tabs, Table, TableContainer, Tbody, Td, Text, Tfoot, Th, Thead, Tr,
  useBreakpointValue,
} from '@chakra-ui/react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import config from '../../config';
import imageUrls from '../../imageUrls';
import prettyPrint from '../../util/prettyPrint';
import { getRaceLogo, resolveRace } from '../../util/raceUtil';
import { getStarPlayerDisplayName, isStarPlayer } from '../../util/starplayerUtil';
import { useMyTeams } from '../../context/MyTeamsContext';

const { boxSize, smallScreenBreakpointValues } = config;
const editionOpus = (entry) => entry.opus || Number(String(entry.editions?.[0] || '').replace('BB', '')) || 3;

function RaceLogo({ entry }) {
  const opus = editionOpus(entry);
  const race = resolveRace(entry, opus);
  const logo = getRaceLogo(entry.raceId ?? race, opus);
  return <Image src={imageUrls.logo(logo, opus)} boxSize={boxSize} title={prettyPrint(race)}
    fallback={<QuestionOutlineIcon boxSize={boxSize} />} objectFit="scale-down" />;
}

function PlayerDisplayName({ name }) {
  const starPlayer = isStarPlayer(name);
  return <Text fontWeight="semibold" color={starPlayer ? '#FFD700' : 'inherit'}
    textShadow={starPlayer ? '1px 1px 2px rgba(0,0,0,0.8)' : 'none'}>
    {starPlayer && '⭐ '}{name ? getStarPlayerDisplayName(name) : 'Unknown player'}
  </Text>;
}

function PlayerColumns({ label, compact }) {
  return <Tr><Th><Center>{compact ? 'R' : 'Rank'}</Center></Th><Th>Player</Th><Th>Team</Th>
    {!compact && <Th />}<Th><Center>{label}</Center></Th>
    {!compact && <><Th>Coach</Th><Th>Position</Th><Th><Center>Games</Center></Th><Th><Center>SPP</Center></Th><Th>Skills</Th></>}</Tr>;
}

export function PlayerTable({ category }) {
  const compact = useBreakpointValue(smallScreenBreakpointValues);
  const { isMyTeam, isMyCoach } = useMyTeams();
  return <TableContainer><Table variant="stripedClickable" size="sm">
    <Thead><PlayerColumns label={category.label} compact={compact} /></Thead>
    <Tbody>{category.entries.map((player, index) => {
      const mine = isMyTeam(player.teamId) || isMyCoach(player.coachId);
      return <Tr key={`${player.playerId}-${index}`} boxShadow={mine ? 'inset 4px 0 var(--chakra-colors-green-400)' : undefined}>
      <Td><Center><Heading size="sm">{index + 1}</Heading></Center></Td>
      <Td><PlayerDisplayName name={player.name} />
        {compact && <Text color="gray.500" fontSize="xs">{prettyPrint(player.position) || '–'}</Text>}</Td>
      <Td><HStack><Text>{player.teamName || 'Unknown team'}</Text>{mine && <Badge colorScheme="green">My player</Badge>}</HStack>
        {compact && <Text color="gray.500" fontSize="xs">{player.coachName || '–'}</Text>}</Td>
      {!compact && <Td><RaceLogo entry={player} /></Td>}
      <Td><Center><Heading size="sm">{player.value}</Heading></Center></Td>
      {!compact && <><Td>{player.coachName || '–'}</Td><Td>{prettyPrint(player.position) || '–'}</Td>
        <Td><Center>{player.games}</Center></Td><Td><Center>{player.spp}</Center></Td>
        <Td maxW="24rem" whiteSpace="normal">{player.skills?.map(prettyPrint).join(', ') || '–'}</Td></>}
    </Tr>})}</Tbody><Tfoot><PlayerColumns label={category.label} compact={compact} /></Tfoot>
  </Table></TableContainer>;
}

function TeamColumns({ label, compact }) {
  return <Tr><Th><Center>{compact ? 'R' : 'Rank'}</Center></Th><Th>{compact ? 'Team / coach' : 'Team-name'}</Th><Th />
    {!compact && <Th>Coach-name</Th>}<Th><Center>{label}</Center></Th>
    <Th><Center>W</Center></Th><Th><Center>D</Center></Th><Th><Center>L</Center></Th><Th><Center>{compact ? 'GP' : 'Games'}</Center></Th>
    {!compact && <><Th><Center>TD+</Center></Th><Th><Center>TD-</Center></Th><Th><Center>TDD</Center></Th>
      <Th><Center>CAS+</Center></Th><Th><Center>CAS-</Center></Th><Th><Center>CASD</Center></Th></>}</Tr>;
}

export function TeamTable({ category, entries }) {
  const compact = useBreakpointValue(smallScreenBreakpointValues);
  const { isMyTeam, isMyCoach } = useMyTeams();
  const rows = entries || category.entries;
  const label = category?.label || 'Score';
  return <TableContainer><Table variant="stripedClickable" size="sm">
    <Thead><TeamColumns label={label} compact={compact} /></Thead><Tbody>{rows.map((team, index) => {
      const mine = isMyTeam(team.teamId) || isMyCoach(team.coachId);
      return <Tr key={`${team.teamId}-${index}`} boxShadow={mine ? 'inset 4px 0 var(--chakra-colors-green-400)' : undefined}>
      <Td><Center><Heading size="sm">{index + 1}</Heading></Center></Td>
      <Td><HStack><Text fontWeight="semibold">{team.name}</Text>{mine && <Badge colorScheme="green">My team</Badge>}{team.editions?.length > 1 && <Badge>{team.editions.join(' + ')}</Badge>}</HStack>
        {compact && <Text color="gray.500" fontSize="xs">{team.coachName || '–'}</Text>}</Td>
      <Td><RaceLogo entry={team} /></Td>{!compact && <Td>{team.coachName || '–'}</Td>}
      <Td><Center><Heading size="sm">{team.value ?? team.points}</Heading></Center></Td>
      <Td><Center>{team.wins}</Center></Td><Td><Center>{team.draws}</Center></Td><Td><Center>{team.losses}</Center></Td><Td><Center>{team.games}</Center></Td>
      {!compact && <><Td><Center>{team.touchdownsFor}</Center></Td><Td><Center>{team.touchdownsAgainst}</Center></Td><Td><Center>{team.touchdownsFor - team.touchdownsAgainst}</Center></Td>
        <Td><Center>{team.casualtiesFor}</Center></Td><Td><Center>{team.casualtiesAgainst}</Center></Td><Td><Center>{team.casualtiesFor - team.casualtiesAgainst}</Center></Td></>}
    </Tr>})}</Tbody><Tfoot><TeamColumns label={label} compact={compact} /></Tfoot>
  </Table></TableContainer>;
}

export function CategoryTabs({ categories, type }) {
  if (!categories?.length) return <Text color="gray.500">No statistics are available for this selection.</Text>;
  return <Box borderWidth="1px" borderRadius="md" overflow="hidden"><Tabs variant="enclosed" isLazy>
    <TabList overflowX="auto" px={2} pt={2}>{categories.map(category => <Tab flexShrink={0} key={category.key}>{category.label}</Tab>)}</TabList>
    <TabPanels>{categories.map(category => <TabPanel p={0} key={category.key}>
      {type === 'player' ? <PlayerTable category={category} /> : <TeamTable category={category} />}
    </TabPanel>)}</TabPanels></Tabs></Box>;
}

export function VersusTable({ rows }) {
  if (!rows?.length) return <Text color="gray.500">No opponents found for the mapped coach IDs.</Text>;
  return <TableContainer borderWidth="1px" borderRadius="md"><Table variant="stripedClickable" size="sm">
    <Thead><Tr><Th>Coach</Th><Th isNumeric>G</Th><Th isNumeric>W-D-L</Th><Th isNumeric>TD</Th><Th isNumeric>CAS</Th></Tr></Thead>
    <Tbody>{rows.map(row => <Tr key={row.coachId}><Td fontWeight="semibold">{row.coachName || row.coachId}</Td><Td isNumeric>{row.games}</Td>
      <Td isNumeric>{row.wins}-{row.draws}-{row.losses}</Td><Td isNumeric>{row.touchdownsFor}-{row.touchdownsAgainst}</Td>
      <Td isNumeric>{row.casualtiesFor}-{row.casualtiesAgainst}</Td></Tr>)}</Tbody>
  </Table></TableContainer>;
}
