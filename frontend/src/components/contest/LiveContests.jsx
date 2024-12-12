import React, { useEffect, useState } from 'react';
import { Heading } from '@chakra-ui/react';
import { FaRegMoon } from 'react-icons/fa6';
import Formatter from '../../util/Formatter';
import WarpScoresApiService from '../../WarpScoresApiService';
import comparators from '../../util/Comparators';
import ContestMatchCards from './ContestMatchCards';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';

function LiveContests({ league, embeddable }) {
  const [contests, setContests] = useState();
  const [loading, setLoading] = useState();
  const [error, setError] = useState();

  const fetchLiveContests = (leagueUuid) => {
    setLoading(true);
    WarpScoresApiService.liveLeagueContests(leagueUuid)
      .then((data) => {
        data.sort((compA, compB) => comparators.compareAsDatesAsc(compB.matchDate, compA.matchDate));
        setContests(data);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (league) {
      fetchLiveContests(league.uuid);
    }
  }, [league]);

  return (
    <>
      {!embeddable && <Heading size="md">Live matches</Heading>}
      <LoadingOrErrorWrapper loading={loading} error={error}>
        <ContestMatchCards
          embeddable={embeddable ? 'embeddable' : null}
          contests={contests}
          noContentIcon={FaRegMoon}
          noContentHeading="No matches live currently..."
          noContentText={league ? `Last match was ${Formatter.formatAsDate(league.dateLastMatch, '-')}` : null}
        />
      </LoadingOrErrorWrapper>
    </>
  );
}

export default LiveContests;
