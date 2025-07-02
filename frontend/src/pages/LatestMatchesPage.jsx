import React, { useEffect, useState } from 'react';
import { Stack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import LatestMatches from '../components/contest/LatestMatches';
import WarpScoresApiService from '../WarpScoresApiService';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';

function LatestMatchesPage() {
  const { leagueId, limit } = useParams();
  const [league, setLeague] = useState();
  const [loading, setLoading] = useState();
  const [error, setError] = useState(undefined);

  useEffect(() => {
    const fetchLeague = () => {
      setLoading(true);
      WarpScoresApiService.leagues(leagueId.value)
        .then((data) => {
          setLeague(data);
        })
        .catch((reason) => {
          setError({ type: 'error', message: reason.toLocaleString() });
        })
        .finally(() => setLoading(false));
    };
    fetchLeague();
  }, [leagueId, limit]);

  return (
    <Stack>
      <LoadingOrErrorWrapper loading={loading} error={error}>
        <LatestMatches embeddable league={league} limit={limit} />
      </LoadingOrErrorWrapper>
    </Stack>
  );
}

export default LatestMatchesPage;
