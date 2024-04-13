import React from 'react';
import { Center, Spinner, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr } from '@chakra-ui/react';
import Rank from './Rank';

const TableColumns = (
  <Tr>
    <Th>
      <Center>Rank</Center>
    </Th>
    <Th>Team-Name</Th>
    <Th />
    <Th>Coach-Name</Th>
    <Th>Race</Th>
    <Th>
      <Center>Games played</Center>
    </Th>
    <Th>
      <Center>Score</Center>
    </Th>
    <Th>
      <Center>W</Center>
    </Th>
    <Th>
      <Center>D</Center>
    </Th>
    <Th>
      <Center>L</Center>
    </Th>
    <Th>
      <Center>TD+</Center>
    </Th>
    <Th>
      <Center>TD-</Center>
    </Th>
    <Th>
      <Center>TDD</Center>
    </Th>
    <Th>
      <Center>CAS+</Center>
    </Th>
    <Th>
      <Center>CAS-</Center>
    </Th>
    <Th>
      <Center>CASD</Center>
    </Th>
  </Tr>
);

function Ranks({ ranks }) {
  return (
    <TableContainer>
      <Table variant="stripedClickable" size="sm">
        <Thead>{TableColumns}</Thead>
        <Tbody>
          {ranks ? (
            ranks.map((rank) => {
              return <Rank rank={rank} key={rank.team.id} />;
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

export default Ranks;
