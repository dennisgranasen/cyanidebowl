import React, { useEffect, useState } from 'react';
import {
  Alert,
  AlertDescription,
  AlertIcon,
  Box,
  FormControl,
  FormLabel,
  Heading,
  Select,
  Spinner,
  Stack,
  useMediaQuery,
} from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import CyanideApiService from '../CyanideApiService';
import config from '../config';
import Navigation from '../components/misc/Navigation';
import Competitions from '../components/competition/Competitions';
import ImageUrls from '../ImageUrls';
import formatter from '../util/Formatter';
import InfoArea from '../components/common/InfoArea';
import InfoItem from '../components/common/InfoItem';
import HeaderCard from '../components/common/HeaderCard';
import LiveContests from '../components/contest/LiveContests';
import LatestContests from '../components/contest/LatestContests';

function WarpScores() {
  const [smallscreen] = useMediaQuery('(max-width: 768px)');

  const { leagueUuid } = useParams();
  const [competitions, setCompetitions] = useState([]);
  const [activeCompetitionsCount, setActiveCompetitionsCount] = useState([]);
  const [registrationCompetitionsCount, setRegistrationCompetitionsCount] = useState([]);
  const [finishedCompetitionsCount, setFinishedCompetitionsCount] = useState([]);
  const [leagues, setLeagues] = useState([]);
  const [league, setLeague] = useState();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(undefined);

  const getLeagueByUuid = (uuid) => {
    if (uuid && uuid !== null) {
      const byUuid = leagues.filter((curr) => curr.uuid === uuid)[0];
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

  useEffect(() => {
    if (!competitions) return;
    let activeCompetitions = 0;
    let finishedCompetitions = 0;
    let registrationCompetitions = 0;
    competitions.forEach((comp) => {
      switch (comp.status) {
        case 'InProgress':
          activeCompetitions += 1;
          break;
        case 'Registration':
          registrationCompetitions += 1;
          break;
        case 'Finished':
          finishedCompetitions += 1;
          break;
        default:
          break;
      }
    });
    setActiveCompetitionsCount(activeCompetitions);
    setFinishedCompetitionsCount(finishedCompetitions);
    setRegistrationCompetitionsCount(registrationCompetitions);
  }, [competitions]);

  const changeLeague = (e) => {
    const byUuid = getLeagueByUuid(e.target.value);
    setLeague(byUuid);
  };

  return (
    <Stack>
      <Box>
        <Navigation currentPage="home" smallscreen={smallscreen ? 'smallscreen' : undefined} />
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
        <HeaderCard
          heading={league.name}
          detailsHeading="League details"
          mainImageSrc={ImageUrls.logo(league.logo)}
          smallscreen={smallscreen ? 'smallscreen' : undefined}
        >
          <InfoArea
            smallscreen={smallscreen ? 'smallscreen' : undefined}
            infoItems={[
              <InfoItem key="teams" label="Teams" info={league.teamCount} />,
              <InfoItem key="activeCompetitions" label="Active Competitions" info={activeCompetitionsCount} />,
              <InfoItem
                key="competitionsInRegistration"
                label="Competitions In Registration"
                info={registrationCompetitionsCount}
              />,
              <InfoItem key="finishedCompetitions" label="Finished Competitions" info={finishedCompetitionsCount} />,
              <InfoItem key="lastMatch" label="Last match" info={formatter.formatAsDate(league.dateLastMatch)} />,
            ]}
          />
        </HeaderCard>
      ) : null}
      <Box>
        {error ? (
          <Alert status={error.type}>
            <AlertIcon />
            <AlertDescription>{error.message}</AlertDescription>
          </Alert>
        ) : null}
        {loading ? (
          <Spinner />
        ) : (
          <Competitions competitions={competitions} smallscreen={smallscreen ? 'smallscreen' : undefined} />
        )}
      </Box>
      <LiveContests league={league} smallscreen={smallscreen ? 'smallscreen' : undefined} />
      <LatestContests league={league} smallscreen={smallscreen ? 'smallscreen' : undefined} />
    </Stack>
  );
}

export default WarpScores;
