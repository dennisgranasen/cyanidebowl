import axios from 'axios';
import logger from './util/Logger';
import config from './config';

axios.defaults.baseURL = config.backendUrl;

const handleError = (reason) => {
  logger.error('FAILED!!!', reason);
  throw reason;
};

const returnData = (result) => {
  logger.debug('Result: %o', result.data);
  return result.data && result.data !== null ? result.data : [];
};

export default {
  newCircuit: async (name) => axios.post(`/circuit/${name}/new`).then(returnData).catch(handleError),
  circuit: async (id) => axios(`/circuit/${id}`).then(returnData).catch(handleError),
  circuits: async () => axios('/circuits' ).then(returnData).catch(handleError),
  addLegToCircuit: async (circuitId, competitionId, legType, customLabel, game, platform, isCompleted, isKnockout) => 
    axios.post(`/circuit/${circuitId}/addLeg`, {
      competitionId: competitionId,
      legType: legType,
      label: customLabel,
      game: game,
      platform: platform,
      isCompleted: isCompleted,
      isKnockout: isKnockout
    }).then(returnData).catch(handleError),
  status: async () => axios(`/status`).then(returnData).catch(handleError),
  league: async (leagueUuid) =>
    axios(`/league${leagueUuid ? `/${leagueUuid}` : ''}`)
      .then(returnData)
      .catch(handleError),
  leagueCollections: async () => axios("/leagueCollections").then(returnData).catch(handleError),
  leagueCompetitions: async (leagueUuid) =>
    axios
      .post(`/competitions/league/${leagueUuid}`, ['Registration', 'InProgress', 'Finished'])
      .then(returnData)
      .catch(handleError),
  competition: async (competitionUuid) => axios(`/competition/${competitionUuid}`).then(returnData).catch(handleError),
  match: async (matchUuid) => axios(`/match/${matchUuid}`).then(returnData).catch(handleError),
  team: async (teamUuid) => axios(`/team/${teamUuid}`).then(returnData).catch(handleError),
  teamMatches: async (teamUuid) => axios(`/team/${teamUuid}/matches`).then(returnData).catch(handleError),
  competitionTeam: async (competitionUuid, teamUuid) =>
    axios(`/competition/${competitionUuid}/team/${teamUuid}`).then(returnData).catch(handleError),
  competitionRanks: async (competitionUuid) =>
    axios(`/ranks/competition/${competitionUuid}`).then(returnData).catch(handleError),
  competitionContests: async (competitionUuid) =>
    axios(`/contests/competition/${competitionUuid}`).then(returnData).catch(handleError),
  liveLeagueContests: async (leagueUuid) =>
    axios(`/contests/league/${leagueUuid}/live`).then(returnData).catch(handleError),
  latestLeagueContests: async (leagueUuid, limit) =>
    axios(`/contests/league/${leagueUuid}/latest${limit ? `/${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  backendVersion: async () => axios(`/public/version.json`).then(returnData).catch(handleError),
};

// leagueTeams: async (leagueUuid) => axios(`/teams/league/${leagueUuid}`).then(returnData).catch(handleError),
// competitionTeams: async (competitionUuid) =>
//  axios(`/teams/competition/${competitionUuid}`).then(returnData).catch(handleError),
// competitionMatches: async (competitionUuid) =>
//  axios(`/matches/competition/${competitionUuid}`).then(returnData).catch(handleError),
// lookup: async (lookupRequest) => axios.post('/lookup', lookupRequest).then(returnData).catch(handleError),
