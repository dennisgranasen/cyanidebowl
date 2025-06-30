import { SimpleGrid } from '@chakra-ui/react';
import React from 'react';
import { Link as RouteLink } from 'react-router-dom';
import ContestMatchCard from './ContestMatchCard';

function ContestMatchCards({ contests, noContentIcon, noContentHeading, noContentText, embeddable }) {
  return contests?.length > 0 ? (
    <SimpleGrid columns={{ lg: 3, sm: 1, md: 2 }} spacing="1.25rem">
      {contests.map((contest) => (
        //console.log("contest :", contest) ||
        <ContestMatchCard
          key={contest.id?.key || contest.id }
          contestOrMatch={contest}
          contestHeader={
            embeddable ? (
              contest?.competitionName
            ) : (
              contest?.competitionId?.value &&
                <RouteLink to={`/competition/${contest?.leagueId?.opus}/${contest?.leagueId?.value}_${contest?.competitionId?.value}`}>{contest?.competitionName}</RouteLink>
                ||
                <RouteLink to={`/league/${contest?.leagueId?.opus}/${contest?.leagueId?.value}`}>{contest?.leagueName}</RouteLink>

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
