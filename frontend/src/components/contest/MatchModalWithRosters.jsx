import React, { useEffect, useState } from 'react';
import {
  Box,
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

function MatchModal({ isOpen, onClose, match, contest }) {
  const [matchData, setMatch] = useState(null);
  
  useEffect(() => {
    setMatch(match || contest.match);

    if (matchData && matchData.teams && matchData.teams.length > 0) {
      let sortedTeams = [];
      matchData.teams.forEach((team) => {
        let sortedPlayers = team.players.sort((a, b) => {
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
        team.players = sortedPlayers;
        sortedTeams.push(team);
      });
      matchData.teams = sortedTeams;
    }

    if (matchData && matchData.teams) 
      console.log(matchData.teams);

  }, [match, contest]);

  // Always render the Modal, but only show it when conditions are met
  const shouldShowModal = matchData && (!contest || contest.status === 'Validated') && isOpen;

  return (
    <Modal size={{ base: 'full', lg: '6xl' }} isOpen={shouldShowModal} onClose={onClose}>
      <ModalOverlay bg="blackAlpha.300" backdropFilter="blur(5px)" />
      <ModalContent backgroundColor="warpScoresBackgroundColor">
        <ModalCloseButton />
        <ModalHeader>{([matchData?.leagueName, matchData?.competitionName].join(".")) ||  contest?.competitionName}</ModalHeader>
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
                        {matchData.teams.map((team, teamIndex) => (
                          <Box key={teamIndex} mb={6}>
                            <Heading size="md" mb={3}>{team.name}</Heading>
                            
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
                          </Box>
                        ))}
                      </Box>
                    )}
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
