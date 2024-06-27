import React from 'react';
import {
  Heading,
  Spinner,
  Table,
  TableContainer,
  Tbody,
  Tfoot,
  Th,
  Thead,
  Tr,
  useBreakpointValue,
} from '@chakra-ui/react';
import Competition from './Competition';
import config from '../../config';

const { smallScreenBreakpointValues } = config;

function TableColumns() {
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);
  return (
    <Tr>
      <Th>Competition</Th>
      <Th>{isSmallScreen ? 'F' : 'Format'}</Th>
      <Th>Status</Th>
      <Th isNumeric>{isSmallScreen ? 'T' : 'Teams'}</Th>
    </Tr>
  );
}

function Competitions({ competitions }) {
  return (
    <TableContainer>
      <Heading size="md">Competitions</Heading>
      <Table variant="stripedClickable" size="sm">
        <Thead>
          <TableColumns />
        </Thead>
        <Tbody>
          {competitions ? (
            competitions.map((competition) => <Competition competition={competition} key={competition.uuid} />)
          ) : (
            <Spinner />
          )}
        </Tbody>
        <Tfoot>
          <TableColumns />
        </Tfoot>
      </Table>
    </TableContainer>
  );
}

export default Competitions;
