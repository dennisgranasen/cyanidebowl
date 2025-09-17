import React, { useEffect, useState } from 'react';
import {
  Box,
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
  useEditable,
} from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import formatter from '../../util/formatter';
import config from '../../config';
import prettyPrint from '../../util/prettyPrint';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';
import WarpScoresApiService from '../../WarpScoresApiService';
import TabbedMatches from '../contest/TabbedMatches';

const { smallScreenBreakpointValues } = config;

function Match({ match }) {
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);

  return (
    <>
      {isSmallScreen && (
        <Tr>
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
      <Tr>
        {!isSmallScreen && <Td>{formatter.formatAsDate(match.started, '-')}</Td>}
        {!isSmallScreen && (
          <Td>
            <Text>
              <RouteLink to={`/competition/${match.competitionId}`}>{match.competitionName}</RouteLink>
            </Text>
            <Text color="grey">
              <RouteLink to={`/${match.leagueId}`}>{match.leagueName}</RouteLink>
            </Text>
          </Td>
        )}
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
        <Td fontSize="md" textAlign="center">{`${match.teams[0].score} - ${match.teams[1].score}`}</Td>
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
      </Tr>
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

function CircuitLegMatches({circuitId, circuitLeg}) {
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (circuitLeg) {
      setLoading(true);
      WarpScoresApiService.circuitLegMatches(circuitId, circuitLeg.circuitLegId, 100)
        .then((data) => {
          setMatches(data);
        })
        .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
        .finally(() => setLoading(false));      
    }
  }, [circuitLeg]);

  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);
  console.log('CircuitLegMatches', circuitLeg, matches);

  return (
    <LoadingOrErrorWrapper loading={loading} error={error}>
      <TabbedMatches 
        matches={matches}
        currentRound={null} 
        loading={loading}
        error={error}
      />
      {/*
      <TableContainer>
        <Table variant={isSmallScreen ? 'unstyled' : 'simple'} size="sm">
          <Thead>
            <TableColumns />
          </Thead>
          <Tbody>{matches && matches.map((match) => <Match key={match.matchId} match={match} />)}</Tbody>
          <Tfoot>
            <TableColumns />
          </Tfoot>
        </Table>
      </TableContainer>
      */}
    </LoadingOrErrorWrapper>
  );
}

export default CircuitLegMatches;
