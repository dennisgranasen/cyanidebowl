import React, { useEffect, useState } from 'react';
import {
  Alert,
  AlertDescription,
  AlertIcon,
  Box,
  Card,
  CardBody,
  FormControl,
  FormLabel,
  Heading,
  Image,
  Select,
  Spinner,
  Stack,
} from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import CyanideApiService from '../CyanideApiService';
import config from '../config';
import Navigation from '../components/Navigation';
import Competitions from '../components/Competitions';
import ImageUrls from '../ImageUrls';
import logger from '../util/Logger';
import formatter from '../util/Formatter';
import InfoArea from '../components/InfoArea';
import InfoItem from '../components/InfoItem';

function WarpScores() {
  const { leagueUuid } = useParams();
  const [competitions, setCompetitions] = useState([]);
  const [leagues, setLeagues] = useState([]);
  const [league, setLeague] = useState();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(undefined);

  const getLeagueByUuid = (uuid) => {
    if (uuid && uuid !== null) {
      const byUuid = leagues.filter((curr) => curr.uuid === uuid)[0];
      logger.debug('League by uuid (%s): %o.', uuid, league);
      return byUuid;
    }
    return null;
  };

  useEffect(() => {
    const fetchLeagues = () => {
      CyanideApiService.league()
        .then((data) => {
          setLeagues(data);
        })
        .then(() => setLoading(false))
        .catch((reason) => {
          setError(reason.toLocaleString(config.locale));
        });
    };
    fetchLeagues();
  }, []);

  useEffect(() => {
    logger.debug('Setting league (uuid: %s). Leagues: %o, count: %s', leagueUuid, leagues, leagues.length);
    if ((!leagueUuid || leagueUuid === null) && leagues.length === 1) {
      setLeague(leagues[0]);
    } else {
      const byUuid = getLeagueByUuid(leagueUuid);
      if (byUuid !== null) {
        setLeague(byUuid);
      }
    }
  }, [leagues]);

  useEffect(() => {
    const fetchCompetitions = (leagueId) => {
      setError(undefined);
      setCompetitions([]);
      setLoading(true);
      if (leagueId === null || leagueId.length === 0) {
        setLoading(false);
        setError({ type: 'info', message: 'No League selected.' });
        return;
      }
      CyanideApiService.leagueCompetitions(leagueId)
        .then((data) => {
          setCompetitions(data);
        })
        .then(() => setLoading(false))
        .catch((reason) => {
          setError({ type: 'error', message: reason.toLocaleString(config.locale) });
        });
    };
    const leagueId = league && league !== null ? league.uuid : null;
    fetchCompetitions(leagueId);
  }, [league]);

  const changeLeague = (e) => {
    const byUuid = getLeagueByUuid(e.target.value);
    setLeague(byUuid);
  };

  return (
    <Stack>
      <Box>
        <Navigation currentPage="home" />
      </Box>
      {leagues.length > 1 ? (
        <Box>
          <Heading>Warp-Scores</Heading>
          <FormControl>
            <FormLabel>League</FormLabel>
            <Select
              variant="filled"
              placeholder="Select league"
              onChange={changeLeague}
              value={league ? league.uuid : null}
            >
              {leagues.map((currLeague) => (
                <option value={currLeague.uuid} key={currLeague.uuid}>
                  {currLeague.name}
                </option>
              ))}
            </Select>
          </FormControl>
        </Box>
      ) : null}
      {league ? (
        <Card direction="row">
          <Box>
            <Image objectFit="contain" maxW="140px" src={ImageUrls.logo(league.logo)} />
          </Box>
          <CardBody>
            <Heading>{league.name}</Heading>
            <InfoArea
              infoItems={[
                <InfoItem key="1" label="Teams" info={league.teamCount} />,
                <InfoItem key="2" label="Active Competitions" info={competitions.length} />,
                <InfoItem key="3" label="Last match" info={formatter.formatAsDate(league.dateLastMatch)} />,
              ]}
            />
          </CardBody>
        </Card>
      ) : null}
      <Box>
        {error ? (
          <Alert status={error.type}>
            <AlertIcon />
            <AlertDescription>{error.message}</AlertDescription>
          </Alert>
        ) : null}
        {loading ? <Spinner /> : <Competitions competitions={competitions} />}
      </Box>
    </Stack>
  );
}

export default WarpScores;
