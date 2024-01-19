import React from 'react';
import { Spinner, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr } from '@chakra-ui/react';
import Player from './Player';

const TableColumns = (
  <Tr>
    <Th>#</Th>
    <Th>Name</Th>
    <Th>Type</Th>
    <Th>Skills</Th>
    <Th>Injuries</Th>
    <Th>MNG</Th>
    <Th>MA</Th>
    <Th>ST</Th>
    <Th>AG</Th>
    <Th>PA</Th>
    <Th>AV</Th>
    <Th>Value</Th>
    <Th>XP</Th>
  </Tr>
);

function Roster({ players }) {
  return (
    <TableContainer width="100%">
      <Table variant="striped" size="sm">
        <Thead>{TableColumns}</Thead>
        <Tbody>
          {players !== null ? (
            players.map((player) => {
              return <Player player={player} key={player.id} />;
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

export default Roster;
