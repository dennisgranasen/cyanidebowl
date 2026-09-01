import React, { useState } from 'react';
import { HamburgerIcon } from '@chakra-ui/icons';
import { Accordion, AccordionButton, AccordionIcon, AccordionItem, AccordionPanel, Box, Card, CardBody, Heading, HStack, IconButton, Menu, MenuButton, MenuItem, MenuList, SimpleGrid, Text, VStack } from '@chakra-ui/react';

function LeagueSystems({ leagueSystems }) {
  const [seasonBySystem, setSeasonBySystem] = useState({});

  const orderedSeasons = (leagueSystem) => [...(leagueSystem.seasons || [])]
    .sort((first, second) => (second.sequence ?? second.number ?? 0) - (first.sequence ?? first.number ?? 0));

  const selectedSeason = (leagueSystem) => {
    const selectedId = seasonBySystem[leagueSystem.id];
    const seasons = orderedSeasons(leagueSystem);
    if (selectedId) {
      return seasons.find((season) => season.id === selectedId);
    }
    return seasons.find((season) => season.recentMatches?.length > 0) || seasons[0];
  };

  return (
    <VStack align="stretch" spacing={3}>
      <Heading size="md">League systems</Heading>
      <Accordion allowMultiple defaultIndex={[0]}>
        {leagueSystems.map((leagueSystem) => (
          <AccordionItem key={leagueSystem.id}>
            <HStack><AccordionButton><Box flex="1" textAlign="left"><Heading size="md">{leagueSystem.name || leagueSystem.id}</Heading></Box><AccordionIcon /></AccordionButton><Menu><MenuButton as={IconButton} icon={<HamburgerIcon />} aria-label="Select season" variant="ghost" /><MenuList>{orderedSeasons(leagueSystem).map((season) => <MenuItem key={season.id} color={season.recentMatches?.length > 0 ? undefined : 'gray.500'} onClick={() => setSeasonBySystem({ ...seasonBySystem, [leagueSystem.id]: season.id })}>{season.name || `Season ${season.number}`}</MenuItem>)}</MenuList></Menu></HStack>
            <AccordionPanel>
              {selectedSeason(leagueSystem) && <><Heading size="sm" mb={2}>{selectedSeason(leagueSystem).name || `Season ${selectedSeason(leagueSystem).number}`}</Heading><Text color="gray.500" mb={3}>{selectedSeason(leagueSystem).stages.map((stage) => stage.name || stage.phase || stage.id).join(' · ') || 'No stages'}</Text></>}
              <Heading size="sm" mb={2}>Latest results</Heading>
              <SimpleGrid columns={{ base: 1, md: 2, xl: 3 }} spacing={3}>
                {(selectedSeason(leagueSystem)?.recentMatches || []).map((recent) => (
                  <Card key={`${recent.stageId}-${recent.match.sourceMatchKey}`} variant="outline" size="sm">
                    <CardBody><Text color="gray.500" fontSize="sm">{recent.stageName}</Text><Heading size="sm">{recent.match.teams?.map((team) => team.name || '-').join(' - ') || `${recent.match.sourceScore?.home ?? '-'} - ${recent.match.sourceScore?.away ?? '-'}`}</Heading><Text>{recent.match.teams?.map((team) => team.score ?? '-').join(' - ')}</Text><Text color="gray.500" fontSize="sm">{recent.match.finishedAt || recent.match.startedAt}</Text></CardBody>
                  </Card>
                ))}
              </SimpleGrid>
              {selectedSeason(leagueSystem)?.recentMatches?.length === 0 && <Text color="gray.500">No results yet</Text>}
            </AccordionPanel>
          </AccordionItem>
        ))}
      </Accordion>
    </VStack>
  );
}

export default LeagueSystems;
