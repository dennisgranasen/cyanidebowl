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
import { useIntl } from 'react-intl';

const { boxSize } = config;

function RaceOrCoachColumn({ coachOrRace, arenaTeam, competitionId }) {
  const navigate = useNavigate();
  const showRace = coachOrRace === 'Race';
  const goToCoach = () => {
    navigate(`/competition/${competitionId}/arena/coach/${arenaTeam.coachId}`);
  };
  const goToRace = () => {
    navigate(`/competition/${competitionId}/arena/${arenaTeam.race}`);
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

function TeamRow({ competitionId, arenaTeam, coachOrRace }) {
  const navigate = useNavigate();
  const goToTeam = () => {
    navigate(`/competition/${competitionId}/team/${arenaTeam.teamId}`);
  };
  const sortedMatches = [].concat(arenaTeam?.matches ?? []);
  sortedMatches.sort(comparators.compareContestsByDateWithMatchAsFallbackAsc);
  const clusteredSortedMatches = arenaHelpers.clusterMatches(arenaTeam.teamId, sortedMatches);
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
      <RaceOrCoachColumn coachOrRace={coachOrRace} arenaTeam={arenaTeam} competitionId={competitionId} />
      <Td>{formatter.formatAsDate(arenaHelpers.earliestFinished(arenaTeam.matches), '-')}</Td>
      <Td>{formatter.formatAsDate(arenaHelpers.latestFinished(arenaTeam.matches), '-')}</Td>
      <Td>{sortedMatches.length}</Td>
      <Td>{clusteredSortedMatches.length}</Td>
      <Td>
        <HStack spacing="0.8rem">
          {clusteredSortedMatches.map((clusteredMatches) => (
            <ArenaProgress
              key={`arenaProgress-${arenaHelpers.getLastMatch(clusteredMatches).matchId}`}
              teamId={arenaTeam.teamId}
              matches={clusteredMatches}
            />
          ))}
        </HStack>
      </Td>
    </Tr>
  );
}

function ArenaRunAccordionItem({ competitionId, label, loading, error, arenaTeams, coachOrRace }) {
  const intl = useIntl();
  const coachOrRaceLabel = intl.formatMessage({ id: coachOrRace === 'Race' ? 'team.race' : 'common.coach' });
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
              <Box>{intl.formatMessage({ id: 'arena.notAvailable' })}</Box>
            </HStack>
          )}
          {arenaTeams && arenaTeams.length === 0 && <Box>{intl.formatMessage({ id: 'arena.none' })}</Box>}
          {arenaTeams && arenaTeams.length > 0 && (
            <TableContainer>
              <Table variant="striped" size="sm">
                <Thead>
                  <Tr>
                    <Th>{intl.formatMessage({ id: 'common.team' })}</Th>
                    <Th>{coachOrRaceLabel}</Th>
                    <Th>{intl.formatMessage({ id: 'arena.firstGame' })}</Th>
                    <Th>{intl.formatMessage({ id: 'arena.lastGame' })}</Th>
                    <Th>{intl.formatMessage({ id: 'arena.gamesPlayed' })}</Th>
                    <Th>{intl.formatMessage({ id: 'arena.runs' })}</Th>
                    <Th>{intl.formatMessage({ id: 'arena.progress' })}</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {arenaTeams.map((arenaTeam) => (
                    <TeamRow
                      key={arenaTeam.teamId}
                      competitionId={competitionId}
                      arenaTeam={arenaTeam}
                      coachOrRace={coachOrRace}
                    />
                  ))}
                </Tbody>
                <Tfoot>
                  <Tr>
                    <Th>{intl.formatMessage({ id: 'common.team' })}</Th>
                    <Th>{coachOrRaceLabel}</Th>
                    <Th>{intl.formatMessage({ id: 'arena.firstGame' })}</Th>
                    <Th>{intl.formatMessage({ id: 'arena.lastGame' })}</Th>
                    <Th>{intl.formatMessage({ id: 'arena.gamesPlayed' })}</Th>
                    <Th>{intl.formatMessage({ id: 'arena.runs' })}</Th>
                    <Th>{intl.formatMessage({ id: 'arena.progress' })}</Th>
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
