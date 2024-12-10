import React, { useEffect, useState } from 'react';
import { Stack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import LatestContests from '../components/contest/LatestContests';
import WarpScoresApiService from '../WarpScoresApiService';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';

function LatestMatchesPage() {
  const { leagueUuid, limit } = useParams();
  const [league, setLeague] = useState();
  const [loading, setLoading] = useState();
  const [error, setError] = useState(undefined);

  useEffect(() => {
    const fetchLeague = () => {
      setLoading(true);
      WarpScoresApiService.leagues(leagueUuid)
        .then((data) => {
          setLeague(data);
        })
        .catch((reason) => {
          setError({ type: 'error', message: reason.toLocaleString() });
        })
        .finally(() => setLoading(false));
    };
    fetchLeague();
  }, [leagueUuid, limit]);

  return (
    <Stack>
      <LoadingOrErrorWrapper loading={loading} error={error}>
        <LatestContests embeddable league={league} limit={limit} />
      </LoadingOrErrorWrapper>
    </Stack>
  );
}

export default LatestMatchesPage;
