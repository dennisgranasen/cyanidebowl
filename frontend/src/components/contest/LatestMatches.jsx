import React, { useEffect, useState } from 'react';
import { Heading } from '@chakra-ui/react';
import { FaRegFaceSadTear } from 'react-icons/fa6';
import formatter from '../../util/formatter';
import WarpScoresApiService from '../../WarpScoresApiService';
import comparators from '../../util/comparators';
import ContestMatchCards from './ContestMatchCards';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';

function LatestMatches({ league, competition, embeddable, limit }) {
  const [contests, setContests] = useState();
  const [loading, setLoading] = useState();
  const [error, setError] = useState();

  const fetchLatestLeagueMatches = (leagueId, contestLimit) => {
    setLoading(true);
    WarpScoresApiService.latestLeagueMatches(leagueId, contestLimit)
      .then((data) => {
        data.sort(comparators.compareMatchesByDateDesc);
        setContests(data);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setLoading(false));
  };

  const fetchLatestCompetitionMatches = (competitionId, contestLimit) => {
    setLoading(true);
    WarpScoresApiService.latestCompetitionMatches(competitionId, contestLimit)
      .then((data) => {
        data.sort((x,y) => x.finished - y.finished);
        setContests(data);
      })
      .catch((reason) => setError({ type: 'error', message: reason.toLocaleString() }))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (league) {
      fetchLatestLeagueMatches(league.id, limit);
    }
    if (competition) {
      fetchLatestCompetitionMatches(competition.id, limit);
    }
  }, [league, competition]);

  console.debug('LatestMatches', { league, competition, embeddable, limit, contests, loading, error });
  return (
    <>
      {!embeddable && <Heading size="md">Latest matches</Heading>}
      <LoadingOrErrorWrapper loading={loading} error={error}>
        <ContestMatchCards
          embeddable={embeddable ? 'embeddable' : null}
          contests={contests}
          noContentIcon={FaRegFaceSadTear}
          noContentHeading="No matches played (yet?)..."
          noContentText={league ? `Last match was ${formatter.formatAsDate(league.dateLastMatch, '-')}` : null}
        />
      </LoadingOrErrorWrapper>
    </>
  );
}

export default LatestMatches;
