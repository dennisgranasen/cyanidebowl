import React from 'react';
import {
  Box,
  Spinner,
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
} from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import formatter from '../../util/Formatter';
import config from '../../config';

const { smallScreenBreakpointValues } = config;

function Match({ match }) {
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);

  return (
    <>
      {isSmallScreen && (
        <Tr>
          <Td textAlign="right">
            <Tag size="sm">{formatter.formatAsDate(match.started)}</Tag>
          </Td>
          <Td textAlign="center">
            <Box>
              <Tag size="sm" color="grey">
                <RouteLink to={`/${match.leagueId}`}>{match.leagueName}</RouteLink>
              </Tag>
            </Box>
          </Td>
          <Td textAlign="left">
            <Tag size="sm">
              <RouteLink to={`/competition/${match.competitionId}`}>{match.competitionName}</RouteLink>
            </Tag>
          </Td>
        </Tr>
      )}
      <Tr>
        {!isSmallScreen && <Td>{formatter.formatAsDate(match.started)}</Td>}
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
          <Text color="grey">{match.coaches[0].name}</Text>
        </Td>
        <Td fontSize="md" textAlign="center">{`${match.teams[0].score} - ${match.teams[1].score}`}</Td>
        <Td>
          <Text>
            <RouteLink to={`/competition/${match.competitionId}/team/${match.teams[1].id}`}>
              {match.teams[1].name}
            </RouteLink>
          </Text>
          <Text color="grey">{match.coaches[1].name}</Text>
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

function Matches({ matches }) {
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);

  return (
    <TableContainer>
      <Table variant={isSmallScreen ? 'unstyled' : 'simple'} size="sm">
        <Thead>
          <TableColumns />
        </Thead>
        <Tbody>
          {matches ? (
            matches.map((match) => {
              return <Match key={match.matchId} match={match} />;
            })
          ) : (
            <Spinner />
          )}
        </Tbody>
        <Tfoot>{TableColumns}</Tfoot>
      </Table>
    </TableContainer>
  );
}

export default Matches;
