import React, { useEffect, useState } from 'react';
import {
  Box,
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
import WarpScoresApiService from '../../WarpScoresApiService';

function MatchModal({ isOpen, onClose, contest }) {
  const [match, setMatch] = useState();
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    if (!contest) {
      console.error('No contest provided to MatchModal');
      return;
    }
    let m = contest.match
    if (contest.match) {
      if (contest.adminResult) {
        m = {
          matchId: 'Admin Result',
          finished: contest.matchDate,
          teams: [{ name: contest.opponents[0].name }, { name: contest.opponents[1].name }],
        };
      }
      setMatch(m);
    } else if (contest.status !== 'Calculated' && contest.matchId) {
      console.log("Fetching contest match", contest);
      WarpScoresApiService.match(contest.matchId)
        .then((fetchedMatch) => {
          if (fetchedMatch) {
            setMatch(fetchedMatch);
            console.log(`Fetched match with ID ${contest.matchId}:`, fetchedMatch);
          } else {
            console.error(`Match with ID ${contest.matchId} not found.`);
          }
          setLoading(false);
        })
        .catch((error) => {
          console.error(`Error fetching match with ID ${contest.matchId}:`, error);
        });
    } else {
      console.warn('No match data available for contest:', contest);
      setMatch(null);
      console.log(contest);
    }
  }, [contest]);

  if (match)
    console.log(match);


  return (
    !loading && match.status !== 'Calculated' && 
    <Modal size={{ base: 'full', md: 'xl' }} isOpen={isOpen} onClose={onClose}>
      <ModalOverlay bg="blackAlpha.300" backdropFilter="blur(5px)" />
      <ModalContent backgroundColor="warpScoresBackgroundColor">
        <ModalCloseButton />
        <ModalHeader>{contest.competitionName}</ModalHeader>
        <ModalBody>
          <Box w="100%">
            <ContestMatchCard contestOrMatch={contest} contestHeader={null} variant="filled" />
          </Box>
          {match && match.teams && match.teams.length > 1 && (
            <TableContainer>
              <Table size="sm">
                <Tbody>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].inflictedtackles}</Td>
                    <Td textAlign="center">Inflicted Tackles</Td>
                    <Td textAlign="center">{match.teams[1].inflictedtackles}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].inflictedinjuries}</Td>
                    <Td textAlign="center">Inflicted Injuries</Td>
                    <Td textAlign="center">{match.teams[1].inflictedinjuries}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].inflictedko}</Td>
                    <Td textAlign="center">Inflicted K.O.s</Td>
                    <Td textAlign="center">{match.teams[1].inflictedko}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].inflictedcasualties}</Td>
                    <Td textAlign="center">Inflicted Casualties</Td>
                    <Td textAlign="center">{match.teams[1].inflictedcasualties}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].inflicteddead}</Td>
                    <Td textAlign="center">Inflicted Deaths</Td>
                    <Td textAlign="center">{match.teams[1].inflicteddead}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].inflictedpushouts}</Td>
                    <Td textAlign="center">Inflicted Pushouts</Td>
                    <Td textAlign="center">{match.teams[1].inflictedpushouts}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].inflictedinterceptions}</Td>
                    <Td textAlign="center">Inflicted Interceptions</Td>
                    <Td textAlign="center">{match.teams[1].inflictedinterceptions}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].sustainedinjuries}</Td>
                    <Td textAlign="center">Sustained injuries</Td>
                    <Td textAlign="center">{match.teams[1].sustainedinjuries}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].sustainedko}</Td>
                    <Td textAlign="center">Sustained K.O.s</Td>
                    <Td textAlign="center">{match.teams[1].sustainedko}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].sustainedcasualties}</Td>
                    <Td textAlign="center">Sustained casualties</Td>
                    <Td textAlign="center">{match.teams[1].sustainedcasualties}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].sustaineddead}</Td>
                    <Td textAlign="center">Sustained dead</Td>
                    <Td textAlign="center">{match.teams[1].sustaineddead}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].sustainedexpulsions}</Td>
                    <Td textAlign="center">Sustained expulsions</Td>
                    <Td textAlign="center">{match.teams[1].sustainedexpulsions}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].inflictedpasses}</Td>
                    <Td textAlign="center">Inflicted Passes</Td>
                    <Td textAlign="center">{match.teams[1].inflictedpasses}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].inflictedcatches}</Td>
                    <Td textAlign="center">Inflicted Catches</Td>
                    <Td textAlign="center">{match.teams[1].inflictedcatches}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].inflictedmetersrunning}</Td>
                    <Td textAlign="center">Inflicted meters Running</Td>
                    <Td textAlign="center">{match.teams[1].inflictedmetersrunning}</Td>
                  </Tr>
                  <Tr>
                    <Td textAlign="center">{match.teams[0].inflictedmeterspassing}</Td>
                    <Td textAlign="center">Inflicted meters Passing</Td>
                    <Td textAlign="center">{match.teams[1].inflictedmeterspassing}</Td>
                  </Tr>
                </Tbody>
              </Table>
            </TableContainer>
          )}
        </ModalBody>
      </ModalContent>
    </Modal>
  );
}

export default MatchModal;
