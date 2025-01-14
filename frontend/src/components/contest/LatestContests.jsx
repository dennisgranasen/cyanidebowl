import React, { useEffect, useState } from 'react';
import { Heading } from '@chakra-ui/react';
import { FaRegFaceSadTear } from 'react-icons/fa6';
import Formatter from '../../util/Formatter';
import WarpScoresApiService from '../../WarpScoresApiService';
import comparators from '../../util/Comparators';
import ContestMatchCards from './ContestMatchCards';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';

function LatestContests({ league, competition, embeddable, limit }) {
  const [contests, setContests] = useState();
  const [loading, setLoading] = useState();
  const [error, setError] = useState();

  const fetchLatestLeagueContests = (leagueUuid, contestLimit) => {
    setLoading(true);
    WarpScoresApiService.latestLeagueContests(leagueUuid, contestLimit)
      .then((data) => {
        data.sort(comparators.compareContestsByMatchOrContestUuidAsDatesDesc);
        setContests(data);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setLoading(false));
  };

  const fetchLatestCompetitionContests = (competitionUuid, contestLimit) => {
    setLoading(true);
    WarpScoresApiService.latestCompetitionContests(competitionUuid, contestLimit)
      .then((data) => {
        data.sort(comparators.compareContestsByMatchOrContestUuidAsDatesDesc);
        setContests(data);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (league) {
      fetchLatestLeagueContests(league.uuid, limit);
    }
    if (competition) {
      fetchLatestCompetitionContests(competition.uuid, limit);
    }
  }, [league, competition]);

  return (
    <>
      {!embeddable && <Heading size="md">Latest matches</Heading>}
      <LoadingOrErrorWrapper loading={loading} error={error}>
        <ContestMatchCards
          embeddable={embeddable ? 'embeddable' : null}
          contests={contests}
          noContentIcon={FaRegFaceSadTear}
          noContentHeading="No matches played (yet?)..."
          noContentText={league ? `Last match was ${Formatter.formatAsDate(league.dateLastMatch, '-')}` : null}
        />
      </LoadingOrErrorWrapper>
    </>
  );
}

export default LatestContests;
