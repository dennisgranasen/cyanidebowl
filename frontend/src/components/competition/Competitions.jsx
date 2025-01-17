import React from 'react';
import {
  Accordion,
  AccordionButton,
  AccordionIcon,
  AccordionItem,
  AccordionPanel,
  Box,
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

function CompetitionsAccordionItem({ competitions, header }) {
  return (
    competitions?.length > 0 && (
      <AccordionItem>
        <AccordionButton>
          <Box as="span" flex="1" textAlign="left">
            <Heading size="md">{`${header} (${competitions.length})`}</Heading>
          </Box>
          <AccordionIcon />
        </AccordionButton>
        <AccordionPanel>
          <TableContainer>
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
        </AccordionPanel>
      </AccordionItem>
    )
  );
}

function Competitions({ competitions }) {
  return (
    <Accordion variant="simple" allowMultiple defaultIndex={[0]}>
      <CompetitionsAccordionItem
        key="InProgress"
        header="Competitions In Progress"
        competitions={competitions?.filter((competition) => competition.status === 'InProgress')}
      />
      <CompetitionsAccordionItem
        key="Registration"
        header="Competitions In Registration"
        competitions={competitions?.filter((competition) => competition.status === 'Registration')}
      />
      <CompetitionsAccordionItem
        key="Finished"
        header="Finished Competitions"
        competitions={competitions?.filter((competition) => competition.status === 'Finished')}
      />
    </Accordion>
  );
}

export default Competitions;
