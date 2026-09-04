import React, { useEffect, useState } from 'react';
import {
  Box,
  Alert,
  AlertIcon,
  Badge,
  Button,
  HStack,
  Heading,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalHeader,
  ModalOverlay,
  Table,
  TableContainer,
  Tbody,
  Td,
  Tr,
  Tabs,
  TabList,
  TabPanels,
  Tab,
  TabPanel,
  Text,
  Thead,
  Th,
  SimpleGrid,
  Spinner,
  Stat,
  StatLabel,
  StatNumber,
  VStack,
} from '@chakra-ui/react';
import ContestMatchCard from './ContestMatchCard';
import WarpScoresApiService from '../../WarpScoresApiService';
import { getStarPlayerDisplayName, isStarPlayer } from '../../util/starplayerUtil';

const PlayerNameCell = ({ player }) => {
  const playerIsStarPlayer = isStarPlayer(player.name);
  const displayName = player.name && getStarPlayerDisplayName(player.name);

  let content = '';
  if (!playerIsStarPlayer && player.number) {
    content += `#${player.number} `;
  }
  if (playerIsStarPlayer) {
    content += '⭐ ';
  }
  content += displayName;
  if (player.mvp) {
    content += ' 🏆';
  }

  return (
    <Td
      fontWeight="bold"
      minWidth="150px"
      color={playerIsStarPlayer ? '#FFD700' : 'inherit'}
      textShadow={playerIsStarPlayer ? '1px 1px 2px rgba(0,0,0,0.8)' : 'none'}
    >
      {content}
    </Td>
  );
};

const formatBytes = (value) => value == null ? '—' : value < 1024 * 1024
  ? `${Math.round(value / 1024)} KiB` : `${(value / 1024 / 1024).toFixed(1)} MiB`;

const eventSummary = (events = []) => Object.entries(events.reduce((result, event) => {
  const key = event.eventType || 'Unknown event';
  result[key] = (result[key] || 0) + 1;
  return result;
}, {})).sort((a, b) => b[1] - a[1]);

function ReplayPanel({ replay, loading, error, onDownload }) {
  if (loading) return <HStack><Spinner size="sm"/><Text>Hämtar replayinformation…</Text></HStack>;
  if (error) return <Text color="red.500">{error}</Text>;
  if (!replay?.available) return <Text color="gray.500">Ingen replay har laddats ned för den här matchen ännu.</Text>;
  const analysis = replay.analysis;
  const specialEvents = eventSummary(analysis?.specialEvents);
  const resourceEvents = eventSummary(analysis?.resourceEvents);
  return <VStack align="stretch" spacing={4}>
    <HStack justify="space-between" align="start" flexWrap="wrap">
      <Box><HStack><Badge colorScheme="green">Replay available</Badge><Badge colorScheme={replay.analysisStatus === 'PROCESSED' ? 'blue' : replay.analysisStatus === 'FAILED' ? 'red' : 'orange'}>{replay.analysisStatus || 'PENDING'}</Badge></HStack><Text mt={1} fontSize="sm" color="gray.500">Original {formatBytes(replay.originalSize)} · compact {formatBytes(replay.compactSize)}</Text></Box>
      <Button size="sm" colorScheme="blue" onClick={onDownload}>Download {replay.originalFormat === 'BBR' ? 'original .bbr' : 'stored replay'}</Button>
    </HStack>
    {!analysis && <Text color="gray.500">Replayen finns, men analysen är inte klar ännu.</Text>}
    {analysis && <>
      {analysis.analysisConfidence === 'RAW_UNMAPPED' && <Alert status="warning"><AlertIcon/>Replaystrukturen är bevarad, men event- och enumtolkningen är ännu experimentell. Värdena nedan är råa parserresultat, inte verifierad Blood Bowl-statistik.</Alert>}
      <SimpleGrid columns={{base:2,md:4}} spacing={3}>
        <Stat borderWidth="1px" borderRadius="md" p={3}><StatLabel>Turns/checkpoints</StatLabel><StatNumber>{analysis.checkpointCount || 0}</StatNumber></Stat>
        <Stat borderWidth="1px" borderRadius="md" p={3}><StatLabel>Events</StatLabel><StatNumber>{analysis.eventCount || 0}</StatNumber></Stat>
        <Stat borderWidth="1px" borderRadius="md" p={3}><StatLabel>Dice rolls</StatLabel><StatNumber>{analysis.diceRolls?.length || 0}</StatNumber></Stat>
        <Stat borderWidth="1px" borderRadius="md" p={3}><StatLabel>Replay steps</StatLabel><StatNumber>{analysis.stepCount || 0}</StatNumber></Stat>
      </SimpleGrid>
      <Box><Heading size="sm" mb={2}>Dice results</Heading><TableContainer><Table size="sm"><Thead><Tr><Th>Die / result</Th><Th isNumeric>Count</Th></Tr></Thead><Tbody>{Object.entries(analysis.dieValueCounts || {}).sort((a,b)=>b[1]-a[1]).map(([key,count])=><Tr key={key}><Td>{key}</Td><Td isNumeric>{count}</Td></Tr>)}</Tbody></Table></TableContainer></Box>
      <SimpleGrid columns={{base:1,md:2}} spacing={4}>
        <Box><Heading size="sm" mb={2}>Resources</Heading>{resourceEvents.length ? <Table size="sm"><Tbody>{resourceEvents.map(([name,count])=><Tr key={name}><Td>{name}</Td><Td isNumeric>{count}</Td></Tr>)}</Tbody></Table> : <Text color="gray.500">No reroll, apothecary or wizard events found.</Text>}</Box>
        <Box><Heading size="sm" mb={2}>Special events</Heading>{specialEvents.length ? <Table size="sm"><Tbody>{specialEvents.map(([name,count])=><Tr key={name}><Td>{name}</Td><Td isNumeric>{count}</Td></Tr>)}</Tbody></Table> : <Text color="gray.500">No special events found.</Text>}</Box>
      </SimpleGrid>
    </>}
  </VStack>;
}

function MatchModal({ isOpen, onClose, match, contest }) {
  const [matchData, setMatch] = useState(null);
  const [replay, setReplay] = useState(null);
  const [replayLoading, setReplayLoading] = useState(false);
  const [replayError, setReplayError] = useState('');
  
  useEffect(() => {
    const value = match || contest?.match;
    if (value && value.teams && value.teams.length > 0) {
      const sortedTeams = value.teams.map((team) => {
        const sortedPlayers = [...(team.players || [])].sort((a, b) => {
          if (a.number && b.number) {
            return a.number - b.number;
          } else if (a.number) {
            return -1; // a comes first
          } else if (b.number) {
            return 1; // b comes first
          } else {
            return a.name.localeCompare(b.name);
          }
        });
        return { ...team, players: sortedPlayers };
      });
      setMatch({ ...value, teams: sortedTeams });
    } else {
      setMatch(value || null);
    }
  }, [match, contest]);

  const replayMatchId = matchData?.id?.key || matchData?.id;
  useEffect(() => {
    if (!isOpen || !replayMatchId) return;
    setReplay(null); setReplayError(''); setReplayLoading(true);
    WarpScoresApiService.replay(replayMatchId).then(setReplay)
      .catch(() => setReplayError('Replayinformationen kunde inte hämtas.'))
      .finally(() => setReplayLoading(false));
  }, [isOpen, replayMatchId]);

  // Always render the Modal, but only show it when conditions are met
  const shouldShowModal = matchData && (!contest || contest.status === 'Validated') && isOpen;

  return (
    <Modal size={{ base: 'full', lg: '6xl' }} isOpen={shouldShowModal} onClose={onClose}>
      <ModalOverlay bg="blackAlpha.300" backdropFilter="blur(5px)" />
      <ModalContent backgroundColor="warpScoresBackgroundColor">
        <ModalCloseButton />
        <ModalHeader>{[matchData?.leagueName, matchData?.competitionName].filter(Boolean).join(' · ') || contest?.competitionName || 'Matchstatistik'}</ModalHeader>
        <ModalBody>
          {matchData && (!contest || contest.status === 'Validated') && (
            <>
              <Box w="100%">
                <ContestMatchCard contestOrMatch={contest || matchData} contestHeader={null} variant="filled" clickable={false}/>
              </Box>
              
              <Tabs>
                <TabList>
                  <Tab>Team Stats</Tab>
                  <Tab>Player Rosters</Tab>
                  <Tab>Replay {replay?.available && <Badge ml={1} colorScheme="green">✓</Badge>}</Tab>
                </TabList>
                <TabPanels>
                  <TabPanel>
                    {/* Existing team statistics table */}
                    {matchData && matchData.teams && matchData.teams.length > 1 && (
                      <TableContainer>
                        <Table size="sm">
                          <Tbody>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].inflictedtackles}</Td>
                              <Td textAlign="center">Inflicted Tackles</Td>
                              <Td textAlign="center">{matchData.teams[1].inflictedtackles}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].inflictedinjuries}</Td>
                              <Td textAlign="center">Inflicted Injuries</Td>
                              <Td textAlign="center">{matchData.teams[1].inflictedinjuries}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].inflictedko}</Td>
                              <Td textAlign="center">Inflicted K.O.s</Td>
                              <Td textAlign="center">{matchData.teams[1].inflictedko}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].inflictedcasualties}</Td>
                              <Td textAlign="center">Inflicted Casualties</Td>
                              <Td textAlign="center">{matchData.teams[1].inflictedcasualties}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].inflicteddead}</Td>
                              <Td textAlign="center">Inflicted Deaths</Td>
                              <Td textAlign="center">{matchData.teams[1].inflicteddead}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].inflictedpushouts}</Td>
                              <Td textAlign="center">Inflicted Pushouts</Td>
                              <Td textAlign="center">{matchData.teams[1].inflictedpushouts}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].inflictedinterceptions}</Td>
                              <Td textAlign="center">Inflicted Interceptions</Td>
                              <Td textAlign="center">{matchData.teams[1].inflictedinterceptions}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].sustainedinjuries}</Td>
                              <Td textAlign="center">Sustained injuries</Td>
                              <Td textAlign="center">{matchData.teams[1].sustainedinjuries}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].sustainedko}</Td>
                              <Td textAlign="center">Sustained K.O.s</Td>
                              <Td textAlign="center">{matchData.teams[1].sustainedko}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].sustainedcasualties}</Td>
                              <Td textAlign="center">Sustained casualties</Td>
                              <Td textAlign="center">{matchData.teams[1].sustainedcasualties}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].sustaineddead}</Td>
                              <Td textAlign="center">Sustained dead</Td>
                              <Td textAlign="center">{matchData.teams[1].sustaineddead}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].sustainedexpulsions}</Td>
                              <Td textAlign="center">Sustained expulsions</Td>
                              <Td textAlign="center">{matchData.teams[1].sustainedexpulsions}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].inflictedpasses}</Td>
                              <Td textAlign="center">Inflicted Passes</Td>
                              <Td textAlign="center">{matchData.teams[1].inflictedpasses}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].inflictedcatches}</Td>
                              <Td textAlign="center">Inflicted Catches</Td>
                              <Td textAlign="center">{matchData.teams[1].inflictedcatches}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].inflictedmetersrunning}</Td>
                              <Td textAlign="center">Inflicted meters Running</Td>
                              <Td textAlign="center">{matchData.teams[1].inflictedmetersrunning}</Td>
                            </Tr>
                            <Tr>
                              <Td textAlign="center">{matchData.teams[0].inflictedmeterspassing}</Td>
                              <Td textAlign="center">Inflicted meters Passing</Td>
                              <Td textAlign="center">{matchData.teams[1].inflictedmeterspassing}</Td>
                            </Tr>
                          </Tbody>
                        </Table>
                      </TableContainer>
                    )}
                  </TabPanel>

                  <TabPanel>
                    {/* Player roster tables with sub-tabs */}
                    {matchData && matchData.teams && matchData.teams.length > 1 && (
                      <Box>
                        {!matchData.teams.some((team) => team.players?.length > 0) && (
                          <Text color="gray.500">
                            {matchData.detailsStatus === 'PLAYER_DATA_UNAVAILABLE'
                              ? 'Matchen har kontrollerats, men Cyanide tillhandahåller ingen spelarstatistik.'
                              : 'Spelarstatistik saknas ännu. Matchen väntar på bakgrundskontroll.'}
                          </Text>
                        )}
                        {matchData.teams.map((team, teamIndex) => (
                          <Box key={teamIndex} mb={6}>
                            <Heading size="md" mb={3}>{team.name}</Heading>
                            {!team.players?.length ? <Text color="gray.500">Ingen spelartrupp finns lagrad.</Text> : (
                            
                            <Tabs variant="enclosed" size="sm">
                              <TabList>
                                <Tab>Overview</Tab>
                                <Tab>Core Stats</Tab>
                                <Tab>Combat Stats</Tab>
                                <Tab>Foul Play</Tab>
                              </TabList>

                              <TabPanels>
                                {/* Overview Tab */}
                                <TabPanel>
                                  <TableContainer>
                                    <Table size="sm">
                                      <Thead>
                                        <Tr>
                                          <Th>Player</Th>
                                          <Th>Level</Th>
                                          <Th>SPP</Th>
                                          <Th>TD</Th>
                                          <Th>Blocks</Th>
                                          <Th>Injuries</Th>
                                          <Th>Casualties</Th>
                                          <Th>Yards</Th>
                                        </Tr>
                                      </Thead>
                                      <Tbody>
                                        {team.players?.map((player, playerIndex) => (
                                          <Tr key={playerIndex} bg={playerIndex % 2 === 0 ? 'gray.50' : 'white'}>
                                            <PlayerNameCell player={player} />
                                            <Td textAlign="center">{player.level || 1}</Td>
                                            <Td textAlign="center" color={player.xpGain ? "green.500" : "inherit"}>
                                              {player.xpGain ? `+${player.xpGain}` : ''}
                                            </Td>
                                            <Td textAlign="center">{player.stats?.touchdowns_scored || player.stats?.inflictedtouchdowns || ''}</Td>
                                            <Td textAlign="center">{player.stats?.blocks_succeeded || player.stats?.inflictedtackles || ''}</Td>
                                            <Td textAlign="center" color={(player.stats?.injuries_inflicted || player.stats?.inflictedinjuries) ? "red.500" : "inherit"}>
                                              {player.stats?.injuries_inflicted || player.stats?.inflictedinjuries || ''}
                                            </Td>
                                            <Td textAlign="center" color={(player.stats?.casualties_inflicted || player.stats?.inflictedcasualties) ? "red.600" : "inherit"}>
                                              {player.stats?.casualties_inflicted || player.stats?.inflictedcasualties || ''}
                                            </Td>
                                            <Td textAlign="center">{player.stats?.yards_running || player.stats?.inflictedmetersrunning || ''}</Td>
                                          </Tr>
                                        ))}
                                      </Tbody>
                                    </Table>
                                  </TableContainer>
                                </TabPanel>

                                {/* Core Stats Tab */}
                                <TabPanel>
                                  <TableContainer>
                                    <Table size="sm">
                                      <Thead>
                                        <Tr>
                                          <Th>Player</Th>
                                          <Th>Rush</Th>
                                          <Th>Dodge</Th>
                                          <Th>Pickup</Th>
                                          <Th>Catch</Th>
                                          <Th>Passes</Th>
                                          <Th>Interceptions</Th>
                                          <Th>Yards Running</Th>
                                          <Th>Yards Passing</Th>
                                        </Tr>
                                      </Thead>
                                      <Tbody>
                                        {team.players?.map((player, playerIndex) => (
                                          <Tr key={playerIndex} bg={playerIndex % 2 === 0 ? 'gray.50' : 'white'}>
                                            <PlayerNameCell player={player} />
                                            <Td textAlign="center">
                                              {(player.stats?.rush_success || player.stats?.rush_try || player.rush_success || player.rush_try) ? 
                                                `${player.stats?.rush_success || player.rush_success || 0}/${player.stats?.rush_try || player.rush_try || 0}` : ''}
                                            </Td>
                                            <Td textAlign="center">
                                              {(player.stats?.dodge_success || player.stats?.dodge_try || player.dodge_success || player.dodge_try) ? 
                                                `${player.stats?.dodge_success || player.dodge_success || 0}/${player.stats?.dodge_try || player.dodge_try || 0}` : ''}
                                            </Td>
                                            <Td textAlign="center">
                                              {(player.stats?.pick_up_success || player.stats?.pick_up_try || player.pick_up_success || player.pick_up_try) ? 
                                                `${player.stats?.pick_up_success || player.pick_up_success || 0}/${player.stats?.pick_up_try || player.pick_up_try || 0}` : ''}
                                            </Td>
                                            <Td textAlign="center">
                                              {(player.stats?.catch_up_ball_success || player.stats?.catch_up_ball_try || player.catch_up_ball_success || player.catch_up_ball_try) ? 
                                                `${player.stats?.catch_up_ball_success || player.catch_up_ball_success || 0}/${player.stats?.catch_up_ball_try || player.catch_up_ball_try || 0}` : ''}
                                            </Td>
                                            <Td textAlign="center">{player.stats?.passes_attempted || player.stats?.inflictedpasses || ''}</Td>
                                            <Td textAlign="center">{player.stats?.interceptions_thrown || player.stats?.inflictedinterceptions || ''}</Td>
                                            <Td textAlign="center">{player.stats?.yards_running || player.stats?.inflictedmetersrunning || ''}</Td>
                                            <Td textAlign="center">{player.stats?.yards_rushing || player.stats?.inflictedmeterspassing || ''}</Td>
                                          </Tr>
                                        ))}
                                      </Tbody>
                                    </Table>
                                  </TableContainer>
                                </TabPanel>

                                {/* Combat Stats Tab */}
                                <TabPanel>
                                  <TableContainer>
                                    <Table size="sm">
                                      <Thead>
                                        <Tr>
                                          <Th>Player</Th>
                                          <Th>Blocks Made</Th>
                                          <Th>Blitz</Th>
                                          <Th>Armor Breaks</Th>
                                          <Th>Stuns</Th>
                                          <Th>KOs</Th>
                                          <Th>Casualties</Th>
                                          <Th>Blocks Taken</Th>
                                          <Th>Injuries Taken</Th>
                                          <Th>Stuns Taken</Th>
                                          <Th>KOs Taken</Th>
                                          <Th>Casualties Taken</Th>
                                        </Tr>
                                      </Thead>
                                      <Tbody>
                                        {team.players?.map((player, playerIndex) => (
                                          <Tr key={playerIndex} bg={playerIndex % 2 === 0 ? 'gray.50' : 'white'}>
                                            <PlayerNameCell player={player} />
                                            <Td textAlign="center">{player.stats?.blocks_succeeded || player.stats?.inflictedtackles || ''}</Td>
                                            <Td textAlign="center">{player.stats?.blitz_done || player.stats?.blitz_done || ''}</Td>
                                            <Td textAlign="center">{player.stats?.armour_breaks || player.stats?.armour_breaks || ''}</Td>
                                            <Td textAlign="center">{player.stats?.stun_inflicted || player.stats?.inflictedstuns || ''}</Td>
                                            <Td textAlign="center">{player.stats?.ko_inflicted || player.stats?.inflictedko || ''}</Td>
                                            <Td textAlign="center" color={(player.stats?.casualties_inflicted || player.stats?.inflictedcasualties) ? "red.600" : "inherit"}>
                                              {player.stats?.casualties_inflicted || player.stats?.inflictedcasualties || ''}
                                            </Td>
                                            <Td textAlign="center">{player.stats?.blocks_sustained || player.stats?.sustainedtackles || ''}</Td>
                                            <Td textAlign="center" color={(player.stats?.injuries_sustained || player.stats?.sustainedinjuries) ? "orange.500" : "inherit"}>
                                              {player.stats?.injuries_sustained || player.stats?.sustainedinjuries || ''}
                                            </Td>
                                            <Td textAlign="center" color={(player.stats?.stun_sustained || player.sustainedstuns) ? "orange.500" : "inherit"}>
                                              {player.stats?.stun_sustained || player.sustainedstuns || ''}
                                            </Td>
                                            <Td textAlign="center" color={(player.stats?.ko_sustained || player.sustainedko) ? "orange.500" : "inherit"}>
                                              {player.stats?.ko_sustained || player.sustainedko || ''}
                                            </Td>
                                            <Td textAlign="center" color={(player.stats?.casualties_sustained || player.stats?.sustainedcasualties) ? "red.500" : "inherit"}>
                                              {player.stats?.casualties_sustained || player.stats?.sustainedcasualties || ''}
                                            </Td>
                                          </Tr>
                                        ))}
                                      </Tbody>
                                    </Table>
                                  </TableContainer>
                                </TabPanel>

                                {/* Foul Play Tab */}
                                <TabPanel>
                                  <TableContainer>
                                    <Table size="sm">
                                      <Thead>
                                        <Tr>
                                          <Th>Player</Th>
                                          <Th>Fouls Made</Th>
                                          <Th>Fouls Taken</Th>
                                          <Th>Ejections</Th>
                                          <Th>Secret Weapons</Th>
                                          <Th>Referee Bribes</Th>
                                        </Tr>
                                      </Thead>
                                      <Tbody>
                                        {team.players?.map((player, playerIndex) => (
                                          <Tr key={playerIndex} bg={playerIndex % 2 === 0 ? 'gray.50' : 'white'}>
                                            <PlayerNameCell player={player} />
                                            <Td textAlign="center" color={(player.stats?.foul_done || player.foul_done) ? "yellow.600" : "inherit"}>
                                              {player.stats?.foul_done || player.foul_done || ''}
                                            </Td>
                                            <Td textAlign="center" color={(player.stats?.foul_sustained || player.foul_sustained) ? "orange.500" : "inherit"}>
                                              {player.stats?.foul_sustained || player.foul_sustained || ''}
                                            </Td>
                                            <Td textAlign="center" color={(player.stats?.ejections || player.ejections) ? "red.500" : "inherit"}>
                                              {player.stats?.ejections || player.ejections || ''}
                                            </Td>
                                            <Td textAlign="center">{player.stats?.secret_weapon_uses || player.secret_weapon_uses || ''}</Td>
                                            <Td textAlign="center">{player.stats?.referee_bribes || player.referee_bribes || ''}</Td>
                                          </Tr>
                                        ))}
                                      </Tbody>
                                    </Table>
                                  </TableContainer>
                                </TabPanel>
                              </TabPanels>
                            </Tabs>
                            )}
                          </Box>
                        ))}
                      </Box>
                    )}
                  </TabPanel>
                  <TabPanel>
                    <ReplayPanel replay={replay} loading={replayLoading} error={replayError}
                      onDownload={() => WarpScoresApiService.downloadOriginalReplay(replayMatchId).catch(() => setReplayError('Originalreplayen kunde inte laddas ned.'))}/>
                  </TabPanel>
                </TabPanels>
              </Tabs>
            </>
          )}
        </ModalBody>
      </ModalContent>
    </Modal>
  );
}

export default MatchModal;
