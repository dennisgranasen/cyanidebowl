import React, { useEffect, useState } from 'react';
import { Heading } from '@chakra-ui/react';
import { FaRegMoon } from 'react-icons/fa6';
import formatter from '../../util/formatter';
import WarpScoresApiService from '../../WarpScoresApiService';
import comparators from '../../util/comparators';
import ContestMatchCards from './ContestMatchCards';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';

function LiveContests({ league, competition, embeddable, limit }) {
  const [contests, setContests] = useState();
  const [loading, setLoading] = useState();
  const [error, setError] = useState();

  const fetchLiveLeagueContests = (leagueUuid, contestLimit) => {
    setLoading(true);
    WarpScoresApiService.liveLeagueContests(leagueUuid, contestLimit)
      .then((data) => {
        data.sort(comparators.compareContestsByMatchOrContestUuidAsDatesDesc);
        setContests(data);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setLoading(false));
  };

  const fetchLiveCompetitionContests = (competitionUuid, contestLimit) => {
    setLoading(true);
    WarpScoresApiService.liveCompetitionContests(competitionUuid, contestLimit)
      .then((data) => {
        data.sort(comparators.compareContestsByMatchOrContestUuidAsDatesDesc);
        setContests(data);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (league) {
      fetchLiveLeagueContests(league.uuid, limit);
    } else if (competition) {
      fetchLiveCompetitionContests(competition.uuid, limit);
    }
  }, [league, competition]);

  return (
    <>
      {!embeddable && <Heading size="md">Live matches</Heading>}
      <LoadingOrErrorWrapper loading={loading} error={error}>
        <ContestMatchCards
          embeddable={embeddable ? 'embeddable' : null}
          contests={contests}
          noContentIcon={FaRegMoon}
          noContentHeading="No matches live currently..."
          noContentText={league ? `Last match was ${formatter.formatAsDate(league.dateLastMatch, '-')}` : null}
        />
      </LoadingOrErrorWrapper>
    </>
  );
}

export default LiveContests;
