import axios from 'axios';
import logger from './util/Logger';
import config from './config';

axios.defaults.baseURL = config.backendUrl;

const handleError = (reason) => {
  logger.error('Backend call failed.', reason);
  throw reason;
};

const returnData = (result) => {
  logger.debug('Backend call succeeded. Result: [%o].', result?.data);
  return result?.data !== null ? result.data : [];
};

export default {
  // misc
  backendVersion: async () => axios(`/version.json`).then(returnData).catch(handleError),
  status: async () => axios(`/status`).then(returnData).catch(handleError),
  // circuits
  newCircuit: async (name) => axios.post(`/circuits`, { circuitName: name }).then(returnData).catch(handleError),
  circuits: async (circuitId) =>
    axios(`/circuits${circuitId ? `/${circuitId}` : ''}`)
      .then(returnData)
      .catch(handleError),
  addLegToCircuit: async (circuitId, competitionId, legType, customLabel, game, platform, isCompleted, isKnockout) =>
    axios
      .post(`/circuits/${circuitId}/legs`, {
        competitionId,
        legType,
        label: customLabel,
        game,
        platform,
        isCompleted,
        isKnockout,
      })
      .then(returnData)
      .catch(handleError),
  // leagues
  leagues: async (leagueUuid) =>
    axios(`/leagues${leagueUuid ? `/${leagueUuid}` : ''}`)
      .then(returnData)
      .catch(handleError),
  // contests
  liveLeagueContests: async (leagueUuid) =>
    axios(`/contests/league/${leagueUuid}/live`).then(returnData).catch(handleError),
  latestLeagueContests: async (leagueUuid, limit) =>
    axios(`/contests/league/${leagueUuid}/latest${limit ? `/${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  competitionContests: async (competitionUuid) =>
    axios(`/contests/competition/${competitionUuid}`).then(returnData).catch(handleError),
  // competitions
  leagueCompetitions: async (leagueUuid) =>
    axios
      .post(`/competitions/league/${leagueUuid}`, ['Registration', 'InProgress', 'Finished'])
      .then(returnData)
      .catch(handleError),
  competition: async (competitionUuid) => axios(`/competitions/${competitionUuid}`).then(returnData).catch(handleError),
  competitionTeam: async (competitionUuid, teamUuid) =>
    axios(`/competitions/${competitionUuid}/team/${teamUuid}`).then(returnData).catch(handleError),
  competitionRanks: async (competitionUuid) =>
    axios(`/ranks/competition/${competitionUuid}`).then(returnData).catch(handleError),
  // team
  team: async (teamUuid) => axios(`/teams/${teamUuid}`).then(returnData).catch(handleError),
  teamMatches: async (teamUuid) => axios(`/teams/${teamUuid}/matches`).then(returnData).catch(handleError),
};
