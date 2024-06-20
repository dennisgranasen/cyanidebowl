import React, { useEffect, useState } from 'react';
import { Heading } from '@chakra-ui/react';
import { FaRegMoon } from 'react-icons/fa6';
import formatter from '../../util/Formatter';
import CyanideApiService from '../../CyanideApiService';
import comparators from '../../util/Comparators';
import ContestMatchCards from './ContestMatchCards';

function LiveContests({ league, embeddable }) {
  const [contests, setContests] = useState();

  const fetchLiveContests = (leagueUuid) => {
    CyanideApiService.liveLeagueContests(leagueUuid).then((data) => {
      data.sort((compA, compB) => comparators.compareAsDates(compB.matchDate, compA.matchDate));
      setContests(data);
    });
  };

  useEffect(() => {
    if (league) {
      fetchLiveContests(league.uuid);
    }
  }, [league]);

  return (
    <>
      {!embeddable && <Heading size="md">Live matches</Heading>}
      <ContestMatchCards
        embeddable={embeddable ? 'embeddable' : null}
        contests={contests}
        noContentIcon={FaRegMoon}
        noContentHeading="No matches live currently..."
        noContentText={league ? `Last match was ${formatter.formatAsDate(league.dateLastMatch)}` : null}
      />
    </>
  );
}

export default LiveContests;
