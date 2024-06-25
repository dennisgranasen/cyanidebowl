import React, { useEffect, useState } from 'react';
import { Stack } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import LatestContests from '../components/contest/LatestContests';
import WarpScoresApiService from '../WarpScoresApiService';
import config from '../config';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';

function LatestMatchesPage() {
  const { leagueUuid, limit } = useParams();
  const [league, setLeague] = useState();
  const [loading, setLoading] = useState();
  const [error, setError] = useState(undefined);

  useEffect(() => {
    const fetchLeague = () => {
      WarpScoresApiService.league(leagueUuid)
        .then((data) => {
          setLeague(data);
        })
        .then(() => setLoading(false))
        .catch((reason) => {
          setError(reason.toLocaleString(config.locale));
        });
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
