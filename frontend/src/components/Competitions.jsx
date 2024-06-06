import React from 'react';
import { Heading, Spinner, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr } from '@chakra-ui/react';
import Competition from './Competition';

function TableColumns(isSmallScreen) {
  return (
    <Tr>
      <Th>Competition</Th>
      <Th>{isSmallScreen ? 'F' : 'Format'}</Th>
      <Th>Status</Th>
      <Th isNumeric>T.</Th>
    </Tr>
  );
}

function Competitions({ competitions, isSmallScreen }) {
  return (
    <TableContainer>
      <Heading size="md">Competitions</Heading>
      <Table variant="stripedClickable" size="sm">
        <Thead>{TableColumns(isSmallScreen)}</Thead>
        <Tbody>
          {competitions ? (
            competitions.map((competition) => {
              return <Competition isSmallScreen={isSmallScreen} competition={competition} key={competition.uuid} />;
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

export default Competitions;
