import React from 'react';
import { Spinner, Table, TableContainer, Tbody, Td, Tfoot, Th, Thead, Tr } from '@chakra-ui/react';
import formatter from '../../util/Formatter';

function Match({ match }) {
  return (
    <Tr>
      <Td>{formatter.formatAsDate(match.started)}</Td>
      <Td>{match.leagueName}</Td>
      <Td>{match.competitionName}</Td>
      <Td>{`${match.teams[0].name} (${match.coaches[0].name})`}</Td>
      <Td>{`${match.teams[1].name} (${match.coaches[1].name})`}</Td>
      <Td>{`${match.teams[0].score} - ${match.teams[1].score}`}</Td>
    </Tr>
  );
}

const TableColumns = (
  <Tr>
    <Th>Date</Th>
    <Th>League</Th>
    <Th>Competition</Th>
    <Th>Opponent</Th>
    <Th>Opponent</Th>
    <Th>Result</Th>
  </Tr>
);

function Matches({ matches }) {
  return (
    <TableContainer>
      <Table variant="simpleClickable" size="sm">
        <Thead>{TableColumns}</Thead>
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
