import { SimpleGrid } from '@chakra-ui/react';
import React from 'react';
import { Link as RouteLink } from 'react-router-dom';
import ContestMatchCard from './ContestMatchCard';

function ContestMatchCards({ contests, noContentIcon, noContentHeading, noContentText }) {
  return contests && contests.length > 0 ? (
    <SimpleGrid columns={{ lg: 3, sm: 1, md: 2 }} spacing="20px">
      {contests.map((contest) => (
        <ContestMatchCard
          key={contest.contestUuid}
          contest={contest}
          contestHeader={<RouteLink to={`/competition/${contest.competitionId}`}>{contest.competitionName}</RouteLink>}
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
