import React, { useEffect, useState } from 'react';
import { Box, FormControl, FormLabel, Heading, Select, Stack, useMediaQuery } from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
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
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';

function WarpScores() {
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
      WarpScoresApiService.league()
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
      WarpScoresApiService.leagueCompetitions(leagueId)
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
        <Navigation currentPage="home" />
      </Box>
      {leagues?.length > 1 ? (
        <Box>
          <Heading>Warp-Scores</Heading>
          <FormControl>
            <FormLabel>
              {' '}
              <Heading size="md">Leagues</Heading>
            </FormLabel>
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
      {league && (
        <HeaderCard heading={league.name} detailsHeading="League details" mainImageSrc={ImageUrls.logo(league.logo)}>
          <InfoArea
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
      )}
      <LoadingOrErrorWrapper loading={loading} error={error}>
        <Competitions competitions={competitions} />
        <LiveContests league={league} />
        <LatestContests league={league} />
      </LoadingOrErrorWrapper>
    </Stack>
  );
}

export default WarpScores;
