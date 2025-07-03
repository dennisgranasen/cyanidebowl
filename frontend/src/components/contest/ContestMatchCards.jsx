import { SimpleGrid } from '@chakra-ui/react';
import React from 'react';
import { Link as RouteLink } from 'react-router-dom';
import ContestMatchCard from './ContestMatchCard';
import { identityUtils } from '../../util/identityUtil';

function ContestMatchCards({ contests, noContentIcon, noContentHeading, noContentText, embeddable }) {
  return contests?.length > 0 ? (
    <SimpleGrid columns={{ lg: 3, sm: 1, md: 2 }} spacing="1.25rem">
      {contests.map((contest) => (
        //console.log("contest :", contest) ||
        <ContestMatchCard
          key={identityUtils.key(contest.id)}
          contestOrMatch={contest}
          contestHeader={
            embeddable ? (
              contest?.competitionName
            ) : (
              contest?.competitionId?.value &&
                <RouteLink to={`/competition/${identityUtils.key(contest.competitionId)}`}>{contest?.competitionName}</RouteLink>
                ||
                <RouteLink to={`/league/${identityUtils.key(contest?.leagueId)}`}>{contest?.leagueName}</RouteLink>

            )
          }
          variant="outline"
          clickable={!contest.live}
        />
      ))}
    </SimpleGrid>
  ) : (
    <ContestMatchCard
      noContentIcon={noContentIcon}
      noContentHeading={noContentHeading}
      noContentText={noContentText}
      variant="outline"
      clickable={false}
    />
  );
}

export default ContestMatchCards;
