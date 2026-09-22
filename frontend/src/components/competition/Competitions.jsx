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
import { useIntl } from 'react-intl';

const { smallScreenBreakpointValues } = config;

function TableColumns() {
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);
  const intl = useIntl();
  return (
    <Tr>
      <Th>{intl.formatMessage({ id: 'common.competition' })}</Th>
      <Th isNumeric>{isSmallScreen ? 'T' : intl.formatMessage({ id: 'common.teams' })}</Th>
      <Th>{isSmallScreen ? 'F' : intl.formatMessage({ id: 'competition.format' })}</Th>
      {!isSmallScreen && <Th>{intl.formatMessage({ id: 'competition.statusLabel' })}</Th>}
      <Th>{isSmallScreen ? 'CR' : intl.formatMessage({ id: 'competition.currentRound' })}</Th>
      <Th>{isSmallScreen ? 'RML' : intl.formatMessage({ id: 'competition.roundMatchesLeft' })}</Th>
      <Th>{isSmallScreen ? 'TML' : intl.formatMessage({ id: 'competition.totalMatchesLeft' })}</Th>
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
                    <Competition competition={competition} league={league} key={competition.id.key} />
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
  const intl = useIntl();
  //console.log('Rendering Competitions with competitions:', competitions);
  return (
    <Accordion variant="simple" allowMultiple defaultIndex={[0]}>
      <CompetitionsAccordionItem
        key="InProgress"
        header={intl.formatMessage({ id: 'league.activeCompetitions' })}
        league={league}
        competitions={competitions?.filter((competition) => competition.status === 'InProgress')}
      />
      <CompetitionsAccordionItem
        key="Registration"
        header={intl.formatMessage({ id: 'league.registrationCompetitions' })}
        league={league}
        competitions={competitions?.filter((competition) => competition.status === 'Registration')}
      />
      <CompetitionsAccordionItem
        key="Finished"
        header={intl.formatMessage({ id: 'league.finishedCompetitions' })}
        league={league}
        competitions={competitions?.filter((competition) => competition.status === 'Finished')}
      />
      <CompetitionsAccordionItem
        key="Unknown"
        header={intl.formatMessage({ id: 'league.unknownCompetitions' })}
        league={league}
        competitions={competitions?.filter((competition) => 
            competition.status === 'Unknown' || 
            !competition.status || 
            competition.status === null || 
            competition.status === undefined)}
      />
    </Accordion>
  );
}

export default Competitions;
