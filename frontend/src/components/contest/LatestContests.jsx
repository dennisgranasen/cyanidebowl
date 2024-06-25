import React, { useEffect, useState } from 'react';
import { Heading } from '@chakra-ui/react';
import { FaRegFaceSadTear } from 'react-icons/fa6';
import formatter from '../../util/Formatter';
import WarpScoresApiService from '../../WarpScoresApiService';
import comparators from '../../util/Comparators';
import ContestMatchCards from './ContestMatchCards';

function LatestContests({ league, embeddable, limit }) {
  const [contests, setContests] = useState();
  const fetchLatestContests = (leagueUuid, limit) => {
    WarpScoresApiService.latestLeagueContests(leagueUuid, limit).then((data) => {
      data.sort((compA, compB) => comparators.compareAsDates(compB.matchDate, compA.matchDate));
      setContests(data);
    });
  };

  useEffect(() => {
    if (league) {
      fetchLatestContests(league.uuid, limit);
    }
  }, [league]);

  return (
    <>
      {!embeddable && <Heading size="md">Latest matches</Heading>}
      <ContestMatchCards
        embeddable={embeddable ? 'embeddable' : null}
        contests={contests}
        noContentIcon={FaRegFaceSadTear}
        noContentHeading="No matches played (yet?)..."
        noContentText={league ? `Last match was ${formatter.formatAsDate(league.dateLastMatch)}` : null}
      />
    </>
  );
}

export default LatestContests;
