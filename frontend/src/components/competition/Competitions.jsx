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
      <Th isNumeric>{isSmallScreen ? 'T' : 'Teams'}</Th>
      <Th>{isSmallScreen ? 'F' : 'Format'}</Th>
      {!isSmallScreen && <Th>Status</Th>}
      <Th>{isSmallScreen ? 'CR' : 'Current Round'}</Th>
      <Th>{isSmallScreen ? 'RML' : 'Round matches left'}</Th>
      <Th>{isSmallScreen ? 'TML' : 'Total matches left'}</Th>
    </Tr>
  );
}

function CompetitionsAccordionItem({ competitions, league, header }) {
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
                  competitions.map((competition) => (
                    <Competition competition={competition} league={league} key={competition.id} />
                  ))
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

function Competitions({ competitions, league }) {
  return (
    <Accordion variant="simple" allowMultiple defaultIndex={[0]}>
      <CompetitionsAccordionItem
        key="InProgress"
        header="Competitions In Progress"
        league={league}
        competitions={competitions?.filter((competition) => competition.status === 'InProgress')}
      />
      <CompetitionsAccordionItem
        key="Registration"
        header="Competitions In Registration"
        league={league}
        competitions={competitions?.filter((competition) => competition.status === 'Registration')}
      />
      <CompetitionsAccordionItem
        key="Finished"
        header="Finished Competitions"
        league={league}
        competitions={competitions?.filter((competition) => competition.status === 'Finished')}
      />
    </Accordion>
  );
}

export default Competitions;
