import React from 'react';
import { Center, Spinner, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr, useMediaQuery } from '@chakra-ui/react';
import Rank from './Rank';

function TableColumns(isSmallScreen) {
  return (
    <Tr>
      <Th>
        <Center>{isSmallScreen ? 'R' : 'Rank'}</Center>
      </Th>
      <Th>{isSmallScreen ? 'TN' : 'Team-Name'}</Th>
      <Th />
      <Th>{isSmallScreen ? 'CN' : 'Coach-Name'}</Th>
      <Th>{isSmallScreen ? null : 'Race'}</Th>
      <Th>
        <Center>{isSmallScreen ? 'GP' : 'Games played'}</Center>
      </Th>
      <Th>
        <Center>{isSmallScreen ? 'Sc.' : 'Score'}</Center>
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

function Ranks({ ranks }) {
  const [isSmallScreen] = useMediaQuery('(max-width: 768px)');

  return (
    <TableContainer>
      <Table variant="stripedClickable" size="sm">
        <Thead>{TableColumns(isSmallScreen)}</Thead>
        <Tbody>
          {ranks ? (
            ranks.map((rank) => {
              return <Rank rank={rank} key={rank.team.id} isSmallScreen={isSmallScreen} />;
            })
          ) : (
            <Spinner />
          )}
        </Tbody>
        <Tfoot>{TableColumns(isSmallScreen)}</Tfoot>
      </Table>
    </TableContainer>
  );
}

export default Ranks;
