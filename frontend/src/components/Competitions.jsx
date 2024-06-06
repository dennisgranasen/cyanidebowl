import React from 'react';
import { Heading, Spinner, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr } from '@chakra-ui/react';
import Competition from './Competition';

function TableColumns(smallscreen) {
  return (
    <Tr>
      <Th>Competition</Th>
      <Th>{smallscreen ? 'F' : 'Format'}</Th>
      <Th>Status</Th>
      <Th isNumeric>{smallscreen ? 'T' : 'Teams'}</Th>
    </Tr>
  );
}

function Competitions({ competitions, smallscreen }) {
  return (
    <TableContainer>
      <Heading size="md">Competitions</Heading>
      <Table variant="stripedClickable" size="sm">
        <Thead>{TableColumns(smallscreen)}</Thead>
        <Tbody>
          {competitions ? (
            competitions.map((competition) => {
              return <Competition smallscreen={smallscreen} competition={competition} key={competition.uuid} />;
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

export default Competitions;
