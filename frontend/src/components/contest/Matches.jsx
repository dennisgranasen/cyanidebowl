import React from 'react';
import { useState, useEffect } from 'react';
import {
  Box,
  Center,
  Table,
  TableContainer,
  Tag,
  Tbody,
  Td,
  Text,
  Tfoot,
  Th,
  Thead,
  Tr,
  useBreakpointValue,
  useDisclosure
} from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import formatter from '../../util/formatter';
import config from '../../config';
import MatchModal from './MatchModalWithRosters';
import ScoreOrIcon from './ScoreOrIcon';
import prettyPrint from '../../util/prettyPrint';
import { identityUtils } from '../../util/identityUtil';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';

const { smallScreenBreakpointValues, smallBoxSize } = config;


function Match({ match }) {
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);
  const { isOpen, onOpen, onClose } = useDisclosure();

  const openMatch = () => {
    if (/*identityUtils.opus(match.id) === 1 || ((contest.status === 'Validated' && !contest.adminResult))*/ true) // validate
    {
      onOpen();
    }
  };

  return (
    <>
      {isSmallScreen && (
        <Tr onClick={openMatch} key={identityUtils.key(match.id)}>
          <Td textAlign="right">
            <Box>
              <Tag size="sm">{formatter.formatAsDate(match.started, '-')}</Tag>
            </Box>
          </Td>
          <Td textAlign="center">
            <Box>
              <Tag size="sm" color="grey">
                <RouteLink to={`/${match.leagueId}`}>{match.leagueName}</RouteLink>
              </Tag>
            </Box>
          </Td>
          <Td textAlign="left">
            <Box>
              <Tag size="sm">
                <RouteLink to={`/competition/${match.competitionId}`}>{match.competitionName}</RouteLink>
              </Tag>
            </Box>
          </Td>
        </Tr>
      )}
      {!isSmallScreen && <Tr onClick={openMatch} key={identityUtils.key(match.id)}>
        <Td>{formatter.formatAsDate(match.started, '-')}</Td>      
        <Td>
          <Text>
            <RouteLink to={`/competition/${match.competitionId}`}>{match.competitionName}</RouteLink>
          </Text>
          <Text color="grey">
            <RouteLink to={`/${match.leagueId}`}>{match.leagueName}</RouteLink>
          </Text>
        </Td>
        <Td textAlign="right">
          <Text>
            <RouteLink to={`/competition/${match.competitionId}/team/${match.teams[0].id}`}>
              {match.teams[0].name}
            </RouteLink>
          </Text>
          <Text color="grey">
            {prettyPrint(match.teams[0].race)} ({match.coaches[0].name})
          </Text>
        </Td>

        <Td>      
          <Center>
            <MatchModal isOpen={isOpen} onClose={onClose} match={match} />
            <ScoreOrIcon contestOrMatch={match} boxSize={smallBoxSize} size="sm" />
          </Center>
        </Td>

        {
        /*<Td fontSize="md" textAlign="center">{`${match.teams[0].score} - ${match.teams[1].score}`}</Td>*/
        }
        <Td>
          <Text>
            <RouteLink to={`/competition/${match.competitionId}/team/${match.teams[1].id}`}>
              {match.teams[1].name}
            </RouteLink>
          </Text>
          <Text color="grey">
            ({match.coaches[1].name}) {prettyPrint(match.teams[1].race)}
          </Text>
        </Td>
      </Tr>}
    </>
  );
}

function TableColumns() {
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);
  return (
    <Tr>
      {!isSmallScreen && <Th>Date</Th>}
      {!isSmallScreen && <Th>Competition</Th>}
      <Th textAlign="center">Home</Th>
      <Th textAlign="center">Result</Th>
      <Th textAlign="center">Away</Th>
    </Tr>
  );
}

function Matches({ matches }) {
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);


  useEffect(() => {
    if (!matches) return;
    /* sort players by number */
    matches.forEach((match) => 
    {
      match.teams && match.teams.forEach((team) => {
        if (!team.players) return;
        team.players = team.players.sort((playerA, playerB) => playerA.number - playerB.number);
      });
    });
    setLoading(false);
  }, [matches]);

  return (
    <LoadingOrErrorWrapper loading={loading} error={error}>
      <TableContainer>
        <Table variant={isSmallScreen ? 'unstyled' : 'simpleClickable'} size="sm">
          <Thead>
            <TableColumns />
          </Thead>
          <Tbody>{matches && matches.map((match) => <Match key={match.matchId} match={match} />)}</Tbody>
          <Tfoot>
            <TableColumns />
          </Tfoot>
        </Table>
      </TableContainer>    
    </LoadingOrErrorWrapper>
    );
}

export default Matches;
