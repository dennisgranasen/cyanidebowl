import React from 'react';
import { Center, Spinner, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr } from '@chakra-ui/react';
import Player from './Player';

const TableColumns = (
  <Tr>
    <Th>#</Th>
    <Th>Name</Th>
    <Th>Type</Th>
    <Th>
      <Center>Level</Center>
    </Th>
    <Th>Skills</Th>
    <Th>Injuries</Th>
    <Th>
      <Center>MNG</Center>
    </Th>
    <Th>
      <Center>MA</Center>
    </Th>
    <Th>
      <Center>ST</Center>
    </Th>
    <Th>
      <Center>AG</Center>
    </Th>
    <Th>
      <Center>PA</Center>
    </Th>
    <Th>
      <Center>AV</Center>
    </Th>
    <Th isNumeric>XP</Th>
    <Th isNumeric>Value</Th>
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
