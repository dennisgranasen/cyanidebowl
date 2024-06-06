import React, { useEffect, useState } from 'react';
import {
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
import formatter from '../util/Formatter';

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
    <Modal size="full" isOpen={isOpen} onClose={onClose}>
      <ModalOverlay />
      <ModalContent>
        <ModalCloseButton />
        <ModalHeader>
          <Center>
            {contest && `${contest.competitionName}: `}
            {match && `${match.teams[0].name} vs ${match.teams[1].name}`}
          </Center>
          <Center>
            {match && `${formatter.formatAsDate(match.started)} - ${formatter.formatAsDate(match.finished)}`}
          </Center>
          <Center>{match && `Duration: ${formatter.formatAsDuration(match.started, match.finished)}`}</Center>
        </ModalHeader>
        <ModalBody>
          {match && (
            <Center>
              <TableContainer>
                <Table variant="striped" size="sm">
                  <Tbody>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].name}</Center>
                      </Td>
                      <Td>
                        <Center>vs</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].name}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.coaches ? match.coaches[0].name : ''}</Center>
                      </Td>
                      <Td>
                        <Center>vs</Center>
                      </Td>
                      <Td>
                        <Center>{match.coaches ? match.coaches[1].name : ''}</Center>
                      </Td>
                    </Tr>
                    <Tr>
                      <Td>
                        <Center>{match.teams[0].inflictedtouchdowns}</Center>
                      </Td>
                      <Td>
                        <Center>Touchdowns</Center>
                      </Td>
                      <Td>
                        <Center>{match.teams[1].inflictedtouchdowns}</Center>
                      </Td>
                    </Tr>
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
