import React, { useEffect, useState } from 'react';
import { Stack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import LiveContests from '../components/contest/LiveContests';

function LiveMatchesPage() {
  const { leagueId } = useParams();
  const [league, setLeague] = useState();
  const [loading, setLoading] = useState();
  const [error, setError] = useState(undefined);

  useEffect(() => {
    const fetchLeague = () => {
      WarpScoresApiService.leagues(leagueId)
        .then((data) => {
          setLeague(data);
        })
        .then(() => setLoading(false))
        .catch((reason) => {
          setError({ type: 'error', message: reason.toLocaleString() });
        });
    };
    fetchLeague();
  }, [leagueId]);

  return (
    <Stack>
      <LoadingOrErrorWrapper loading={loading} error={error}>
        <LiveContests embeddable league={league} />
      </LoadingOrErrorWrapper>
    </Stack>
  );
}

export default LiveMatchesPage;
