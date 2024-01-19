import React from 'react';
import { Spinner, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr } from '@chakra-ui/react';
import Team from './Team';

const TableColumns = (
  <Tr>
    <Th>Team-Name</Th>
    <Th />
    <Th>Coach-Name</Th>
    <Th>Race</Th>
    <Th isNumeric>CTV</Th>
    <Th isNumeric>Cash</Th>
  </Tr>
);

function Teams({ teams }) {
  return (
    <TableContainer>
      <Table variant="striped" size="sm">
        <Thead>{TableColumns}</Thead>
        <Tbody>
          {teams ? (
            teams.map((team) => {
              return <Team team={team} key={team.id} />;
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

export default Teams;
