import {
  AccordionButton,
  AccordionIcon,
  AccordionItem,
  AccordionPanel,
  Box,
  Heading,
  HStack,
  Image,
  Table,
  TableContainer,
  Tbody,
  Td,
  Tfoot,
  Th,
  Thead,
  Tr,
} from '@chakra-ui/react';
import React from 'react';
import { Icon, QuestionOutlineIcon } from '@chakra-ui/icons';
import { useNavigate } from 'react-router-dom';
import { FaRegFaceSadTear } from 'react-icons/fa6';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';
import comparators from '../../util/comparators';
import arenaHelpers from './arenaHelpers';
import imageUrls from '../../imageUrls';
import formatter from '../../util/formatter';
import ArenaProgress from './ArenaProgress';
import config from '../../config';
import prettyPrint from '../../util/prettyPrint';

const { boxSize } = config;

function RaceOrCoachColumn({ coachOrRace, arenaTeam, competitionUuid }) {
  const navigate = useNavigate();
  const showRace = coachOrRace === 'Race';
  const goToCoach = () => {
    navigate(`/competition/${competitionUuid}/arena/coach/${arenaTeam.coachUuid}`);
  };
  const goToRace = () => {
    navigate(`/competition/${competitionUuid}/arena/${arenaTeam.race}`);
  };

  return (
    <Td
      _hover={{ textEmphasisColor: 'warpScoresHoverColor', textDecoration: 'underline' }}
      cursor="pointer"
      onClick={showRace ? goToRace : goToCoach}
    >
      {showRace ? prettyPrint(arenaTeam.race) : arenaTeam.coachName}
    </Td>
  );
}

function TeamRow({ competitionUuid, arenaTeam, coachOrRace }) {
  const navigate = useNavigate();
  const goToTeam = () => {
    navigate(`/competition/${competitionUuid}/team/${arenaTeam.teamUuid}`);
  };
  const sortedMatches = [].concat(arenaTeam?.matches ?? []);
  sortedMatches.sort(comparators.compareContestsByMatchOrContestUuidAsDatesAsc);
  const clusteredSortedMatches = arenaHelpers.clusterMatches(arenaTeam.teamUuid, sortedMatches);
  return (
    <Tr>
      <Td
        _hover={{ textEmphasisColor: 'warpScoresHoverColor', textDecoration: 'underline' }}
        cursor="pointer"
        onClick={goToTeam}
      >
        <HStack>
          <Image
            src={`${imageUrls.logo(arenaTeam?.teamLogo)}`}
            boxSize={boxSize}
            fallback={<QuestionOutlineIcon boxSize={boxSize} />}
          />
          <Box>{arenaTeam.teamName}</Box>
        </HStack>
      </Td>
      <RaceOrCoachColumn coachOrRace={coachOrRace} arenaTeam={arenaTeam} competitionUuid={competitionUuid} />
      <Td>{formatter.formatAsDate(arenaTeam.matches[0].finished, '-')}</Td>
      <Td>{formatter.formatAsDate(arenaTeam.matches[arenaTeam.matches.length - 1].finished, '-')}</Td>
      <Td>{sortedMatches.length}</Td>
      <Td>{clusteredSortedMatches.length}</Td>
      <Td>
        <HStack spacing="0.8rem">
          {clusteredSortedMatches.map((clusteredMatches) => (
            <ArenaProgress
              key={`arenaProgress-${arenaHelpers.getLastMatch(clusteredMatches).matchId}`}
              teamUuid={arenaTeam.teamUuid}
              matches={clusteredMatches}
            />
          ))}
        </HStack>
      </Td>
    </Tr>
  );
}

function ArenaRunAccordionItem({ competitionUuid, label, loading, error, arenaTeams, coachOrRace }) {
  return (
    <AccordionItem>
      <AccordionButton>
        <Box as="span" flex="1" textAlign="left">
          <Heading size="md">{label}</Heading>
        </Box>
        <AccordionIcon />
      </AccordionButton>
      <AccordionPanel>
        <LoadingOrErrorWrapper loading={loading} error={error}>
          {!arenaTeams && (
            <HStack gap="1rem">
              <Icon as={FaRegFaceSadTear} boxSize={boxSize} />
              <Box>Not yet available...</Box>
            </HStack>
          )}
          {arenaTeams && arenaTeams.length === 0 && <Box>None...</Box>}
          {arenaTeams && arenaTeams.length > 0 && (
            <TableContainer>
              <Table variant="striped" size="sm">
                <Thead>
                  <Tr>
                    <Th>Team</Th>
                    <Th>{coachOrRace}</Th>
                    <Th>First game</Th>
                    <Th>Last game</Th>
                    <Th>Games played</Th>
                    <Th>Runs</Th>
                    <Th>Progress</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {arenaTeams.map((arenaTeam) => (
                    <TeamRow
                      key={arenaTeam.teamUuid}
                      competitionUuid={competitionUuid}
                      arenaTeam={arenaTeam}
                      coachOrRace={coachOrRace}
                    />
                  ))}
                </Tbody>
                <Tfoot>
                  <Tr>
                    <Th>Team</Th>
                    <Th>{coachOrRace}</Th>
                    <Th>First game</Th>
                    <Th>Last game</Th>
                    <Th>Games played</Th>
                    <Th>Runs</Th>
                    <Th>Progress</Th>
                  </Tr>
                </Tfoot>
              </Table>
            </TableContainer>
          )}
        </LoadingOrErrorWrapper>
      </AccordionPanel>
    </AccordionItem>
  );
}

export default ArenaRunAccordionItem;
