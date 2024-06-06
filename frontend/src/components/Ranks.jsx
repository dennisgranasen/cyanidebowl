import React from 'react';
import { Center, Spinner, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr } from '@chakra-ui/react';
import Rank from './Rank';

function TableColumns(smallscreen) {
  return (
    <Tr>
      <Th>
        <Center>{smallscreen ? 'R' : 'Rank'}</Center>
      </Th>
      <Th>{smallscreen ? 'TN' : 'Team-Name'}</Th>
      <Th />
      <Th>{smallscreen ? 'CN' : 'Coach-Name'}</Th>
      <Th>{smallscreen ? null : 'Race'}</Th>
      <Th>
        <Center>{smallscreen ? 'GP' : 'Games played'}</Center>
      </Th>
      <Th>
        <Center>{smallscreen ? 'Sc.' : 'Score'}</Center>
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
}

function Ranks({ ranks, smallscreen }) {
  return (
    <TableContainer>
      <Table variant="stripedClickable" size="sm">
        <Thead>{TableColumns(smallscreen)}</Thead>
        <Tbody>
          {ranks ? (
            ranks.map((rank) => {
              return <Rank rank={rank} key={rank.team.id} smallscreen={smallscreen} />;
            })
          ) : (
            <Spinner />
          )}
        </Tbody>
        <Tfoot>{TableColumns(smallscreen)}</Tfoot>
      </Table>
    </TableContainer>
  );
}

export default Ranks;
