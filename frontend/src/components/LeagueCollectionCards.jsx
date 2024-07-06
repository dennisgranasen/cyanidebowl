import { SimpleGrid } from '@chakra-ui/react';
import React from 'react';
import { Link as RouteLink } from 'react-router-dom';
import LeagueCollectionCard from './LeagueCollectionCard';

function LeagueCollectionCards({ leagueCollections, noContentIcon, noContentHeading, noContentText }) {
  return leagueCollections && leagueCollections.length > 0 ? (
    <SimpleGrid columns={{ lg: 3, sm: 1, md: 2 }} spacing="20px">
      {leagueCollections.map((leagueCollection) => (
        <LeagueCollectionCard
          key={leagueCollection.circuitId}
          leagueCollection={leagueCollection}
          showConfigureLink={true}
          variant="outline"
        />
      ))}
    </SimpleGrid>
  ) : (
    <LeagueCollectionCard      
      noContentIcon={noContentIcon}
      noContentHeading={noContentHeading}
      noContentText={noContentText}
      variant="outline"
    />
  );
}

export default LeagueCollectionCards;
