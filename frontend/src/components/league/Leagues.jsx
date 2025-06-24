import React from 'react';
import {
  Accordion,
  AccordionButton,
  AccordionIcon,
  AccordionItem,
  AccordionPanel,
  Box,
  Heading,
} from '@chakra-ui/react';
import LeagueCard from './LeagueCard';

function LeaguesAccordionItem({ leagues, header }) {
  console.log(`Rendering LeaguesAccordionItem with header: ${header} and leagues:`, leagues);
  return (
    leagues?.length > 0 && (
      <AccordionItem>
        <AccordionButton>
          <Box as="span" flex="1" textAlign="left">
            <Heading size="md">{`${header} (${leagues.length})`}</Heading>
          </Box>
          <AccordionIcon />
        </AccordionButton>
        <AccordionPanel>
          {leagues.map((currLeague) => (
            <LeagueCard mb={2} league={currLeague} key={currLeague.uuid} />
          ))}
        </AccordionPanel>
      </AccordionItem>
    )
  );
}

function Leagues({ leagues }) {
  console.log('Rendering Leagues component with leagues:', leagues);
  return (
    <Accordion variant="simple" allowMultiple defaultIndex={[0]}>
      <LeaguesAccordionItem
        key="Active"
        header="Active Leagues"
        leagues={leagues?.filter(
          (league) =>
            league.countsByCompetitionStatus?.InProgress > 0 || league.countsByCompetitionStatus?.Registration > 0
        )}
      />
      <LeaguesAccordionItem
        key="Inactive"
        header="Inactive Leagues"
        leagues={leagues?.filter(
          (league) => league.teamCount === 0 || league.countsByCompetitionStatus?.InProgress === 0
        )}
      />
    </Accordion>
  );
}

export default Leagues;
