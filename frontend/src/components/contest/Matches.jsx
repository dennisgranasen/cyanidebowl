import React from 'react';
import { Spinner, Table, TableContainer, Tag, Tbody, Td, Text, Tfoot, Th, Thead, Tr } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import formatter from '../../util/Formatter';

function Match({ match, smallscreen }) {
  return (
    <>
      {smallscreen && (
        <Tr>
          <Td textAlign="right">
            <Tag size="sm">{formatter.formatAsDate(match.started)}</Tag>
          </Td>
          <Td textAlign="center">
            <Tag size="sm" color="grey">
              <RouteLink to={`/${match.leagueId}`}>{match.leagueName}</RouteLink>
            </Tag>
          </Td>
          <Td textAlign="left">
            <Tag size="sm">
              <RouteLink to={`/competition/${match.competitionId}`}>{match.competitionName}</RouteLink>
            </Tag>
          </Td>
        </Tr>
      )}
      <Tr>
        {!smallscreen && <Td>{formatter.formatAsDate(match.started)}</Td>}
        {!smallscreen && (
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

function TableColumns({ smallscreen }) {
  return (
    <Tr>
      {!smallscreen && <Th>Date</Th>}
      {!smallscreen && <Th>Competition</Th>}
      <Th textAlign="center">Home</Th>
      <Th textAlign="center">Result</Th>
      <Th textAlign="center">Away</Th>
    </Tr>
  );
}

function Matches({ matches, smallscreen }) {
  return (
    <TableContainer>
      <Table variant={smallscreen ? 'unstyled' : 'simple'} size="sm">
        <Thead>
          <TableColumns smallscreen={smallscreen ? 'smallscreen' : null} />
        </Thead>
        <Tbody>
          {matches ? (
            matches.map((match) => {
              return <Match key={match.matchId} match={match} smallscreen={smallscreen ? 'smallscreen' : null} />;
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
