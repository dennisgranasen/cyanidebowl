import { SimpleGrid } from '@chakra-ui/react';
import React from 'react';
import { Link as RouteLink } from 'react-router-dom';
import ContestMatchCard from './ContestMatchCard';

function ContestMatchCards({ contests, noContentIcon, noContentHeading, noContentText, embeddable }) {
  return contests?.length > 0 ? (
    <SimpleGrid columns={{ lg: 3, sm: 1, md: 2 }} spacing="1.25rem">
      {contests.map((contest) => (
        <ContestMatchCard
          key={contest.contestUuid}
          contestOrMatch={contest}
          contestHeader={
            embeddable ? (
              contest?.competitionName
            ) : (
              <RouteLink to={`/competition/${contest?.competitionId}`}>{contest?.competitionName}</RouteLink>
            )
          }
          variant="outline"
        />
      ))}
    </SimpleGrid>
  ) : (
    <ContestMatchCard
      noContentIcon={noContentIcon}
      noContentHeading={noContentHeading}
      noContentText={noContentText}
      variant="outline"
    />
  );
}

export default ContestMatchCards;
