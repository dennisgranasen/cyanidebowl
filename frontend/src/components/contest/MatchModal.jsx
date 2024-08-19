import React, { useEffect, useState } from 'react';
import {
  Box,
  Center,
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
} from '@chakra-ui/react';
import ContestMatchCard from './ContestMatchCard';

function MatchModal({ isOpen, onClose, contest }) {
  const [match, setMatch] = useState();
  useEffect(() => {
    let m = contest.match;
    if (contest.adminResult) {
      m = {
        matchId: 'Admin Result',
        finished: contest.matchDate,
        teams: [{ name: contest.opponents[0].name }, { name: contest.opponents[1].name }],
      };
    }
    setMatch(m);
  }, [contest]);

  return (
    <Modal size={{ base: 'full', md: 'xl' }} isOpen={isOpen} onClose={onClose}>
      <ModalOverlay bg="blackAlpha.300" backdropFilter="blur(5px)" />
      <ModalContent backgroundColor="warpScoresBackgroundColor">
        <ModalCloseButton />
        <ModalHeader>
          <Center>{contest.competitionName}</Center>
        </ModalHeader>
        <ModalBody>
          <Center>
            <Box w="100%">
              <ContestMatchCard contest={contest} contestHeader={null} variant="filled" />
            </Box>
          </Center>
          {match && (
            <Center>
              <TableContainer>
                <Table variant="striped" size="sm">
                  <Tbody>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].inflictedtackles}</Center>
                      </Td>
                      <Td>
                        <Center>Inflicted Tackles</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].inflictedtackles}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].inflictedinjuries}</Center>
                      </Td>
                      <Td>
                        <Center>Inflicted Injuries</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].inflictedinjuries}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].inflictedko}</Center>
                      </Td>
                      <Td>
                        <Center>Inflicted K.O.s</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].inflictedko}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].inflictedcasualties}</Center>
                      </Td>
                      <Td>
                        <Center>Inflicted Casualties</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].inflictedcasualties}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].inflicteddead}</Center>
                      </Td>
                      <Td>
                        <Center>Inflicted Deaths</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].inflicteddead}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].inflictedpushouts}</Center>
                      </Td>
                      <Td>
                        <Center>Inflicted Pushouts</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].inflictedpushouts}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].inflictedinterceptions}</Center>
                      </Td>
                      <Td>
                        <Center>Inflicted Interceptions</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].inflictedinterceptions}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].sustainedinjuries}</Center>
                      </Td>
                      <Td>
                        <Center>Sustained injuries</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].sustainedinjuries}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].sustainedko}</Center>
                      </Td>
                      <Td>
                        <Center>Sustained K.O.s</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].sustainedko}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].sustainedcasualties}</Center>
                      </Td>
                      <Td>
                        <Center>Sustained casualties</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].sustainedcasualties}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].sustaineddead}</Center>
                      </Td>
                      <Td>
                        <Center>Sustained dead</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].sustaineddead}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].sustainedexpulsions}</Center>
                      </Td>
                      <Td>
                        <Center>Sustained expulsions</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].sustainedexpulsions}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].inflictedpasses}</Center>
                      </Td>
                      <Td>
                        <Center>Inflicted Passes</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].inflictedpasses}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].inflictedcatches}</Center>
                      </Td>
                      <Td>
                        <Center>Inflicted Catches</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].inflictedcatches}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].inflictedmetersrunning}</Center>
                      </Td>
                      <Td>
                        <Center>Inflicted meters Running</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].inflictedmetersrunning}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].inflictedmeterspassing}</Center>
                      </Td>
                      <Td>
                        <Center>Inflicted meters Passing</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].inflictedmeterspassing}</Center>
                      </Td>
                    </Tr>
                  </Tbody>
                </Table>
              </TableContainer>
            </Center>
          )}
        </ModalBody>
      </ModalContent>
    </Modal>
  );
}

export default MatchModal;
