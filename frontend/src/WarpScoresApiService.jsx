import axios from 'axios';
import logger from './util/logger';
import config from './config';

const { isProduction } = config;

axios.defaults.baseURL = config.backendUrl;

const authorizationParams = {
  authorizationParams: {
    audience: config.auth0Audience,
  },
};

const handleError = (reason) => {
  logger.error('Backend call failed.', reason);
  throw reason;
};

const returnData = async (result) => {
  logger.debug('Backend call succeeded. Result: [%o].', result?.data);
  return result?.data !== null ? result.data : [];
};

const offerDownloadData = (result, filename, contentType) => {
  if (!result) return;
  const link = document.createElement('a');
  const blob = new Blob([result.data], { type: contentType });
  link.href = window.URL.createObjectURL(blob);
  link.download = filename;
  link.click();
};

const getToken = async (getAccessTokenSilently, getAccessTokenWithPopup) => {
  let token;
  try {
    token = await getAccessTokenSilently(authorizationParams);
  } catch (e) {
    token = await getAccessTokenWithPopup(authorizationParams);
  }
  return token;
};

const getAuthHeaders = async (getAccessTokenSilently, getAccessTokenWithPopup) => {
  if (!isProduction) {
    return null;
  }
  const token = await getToken(getAccessTokenSilently, getAccessTokenWithPopup);
  return {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  };
};

const postDataWithAuthentication = async (endpoint, data, getAccessTokenSilently, getAccessTokenWithPopup) => {
  const authHeaders = await getAuthHeaders(getAccessTokenSilently, getAccessTokenWithPopup);
  return axios.post(endpoint, data, authHeaders);
};

const getDataWithAuthentication = async (endpoint, getAccessTokenSilently, getAccessTokenWithPopup, requestToken) => {
  let authHeaders;
  if (requestToken) {
    authHeaders = await getAuthHeaders(getAccessTokenSilently, getAccessTokenWithPopup);
  }
  return axios(endpoint, authHeaders);
};

export default {
  // misc
  backendVersion: async () => axios(`/version.json`).then(returnData).catch(handleError),
  status: async () => axios(`/status`).then(returnData).catch(handleError),
  // circuits
  newCircuit: async (name, getAccessTokenSilently, getAccessTokenWithPopup) =>
    postDataWithAuthentication(`/circuits`, { circuitName: name }, getAccessTokenSilently, getAccessTokenWithPopup)
      .then(returnData)
      .catch(handleError),
  circuits: async (circuitId) =>
    axios(`/circuits${circuitId ? `/${circuitId}` : ''}`)
      .then(returnData)
      .catch(handleError),
  addLegToCircuit: async (
    circuitId,
    competitionId,
    legType,
    customLabel,
    game,
    platform,
    isCompleted,
    isKnockout,
    getAccessTokenSilently,
    getAccessTokenWithPopup
  ) =>
    postDataWithAuthentication(
      `/circuits/${circuitId}/legs`,
      {
        competitionId,
        legType,
        label: customLabel,
        game,
        platform,
        isCompleted,
        isKnockout,
      },
      getAccessTokenSilently,
      getAccessTokenWithPopup
    )
      .then(returnData)
      .catch(handleError),
  // leagues
  leagues: async (leagueUuid) =>
    axios(`/leagues${leagueUuid ? `/${leagueUuid}` : ''}`)
      .then(returnData)
      .catch(handleError),
  // contests
  liveLeagueContests: async (leagueUuid, limit) =>
    axios(`/contests/league/${leagueUuid}/live${limit ? `/${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  latestLeagueContests: async (leagueUuid, limit) =>
    axios(`/contests/league/${leagueUuid}/latest${limit ? `/${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  liveCompetitionContests: async (competitionUuid, limit) =>
    axios(`/contests/competition/${competitionUuid}/live${limit ? `/${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  latestCompetitionContests: async (competitionUuid, limit) =>
    axios(`/contests/competition/${competitionUuid}/latest${limit ? `/${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  competitionContests: async (competitionUuid, limit) =>
    axios(`/contests/competition/${competitionUuid}${limit ? `/${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  arenaTopCoaches: async (competitionUuid) =>
    axios(`/arena/${competitionUuid}/topCoaches`).then(returnData).catch(handleError),
  arenaInfos: async (competitionUuid, race) =>
    axios(`/arena/${competitionUuid}/info${race ? `/${race}` : ''}`)
      .then(returnData)
      .catch(handleError),
  arenaTeams: async (competitionUuid, race, runType) =>
    axios(`/arena/${competitionUuid}/race/${race}/${runType}`).then(returnData).catch(handleError),
  arenaCoachTeams: async (competitionUuid, coachUuid) =>
    axios(`/arena/${competitionUuid}/coach/${coachUuid}`).then(returnData).catch(handleError),
  // competitions
  leagueCompetitions: async (leagueUuid) =>
    axios(`/competitions/league/${leagueUuid}`).then(returnData).catch(handleError),
  competition: async (competitionUuid) => axios(`/competitions/${competitionUuid}`).then(returnData).catch(handleError),
  competitionTeam: async (competitionUuid, teamUuid) =>
    axios(`/competitions/${competitionUuid}/team/${teamUuid}`).then(returnData).catch(handleError),
  competitionRanks: async (competitionUuid, limit) =>
    axios(`/ranks/competition/${competitionUuid}${limit ? `/${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  // team
  team: async (teamUuid) => axios(`/teams/${teamUuid}`).then(returnData).catch(handleError),
  teamMatches: async (teamUuid) => axios(`/teams/${teamUuid}/matches`).then(returnData).catch(handleError),
  // user
  userPermissions: async (getAccessTokenSilently, getAccessTokenWithPopup, requestToken) =>
    getDataWithAuthentication('/userPermissions', getAccessTokenSilently, getAccessTokenWithPopup, requestToken)
      .then(returnData)
      .catch(handleError),
  exportNafXml: async (competitionUuid, getAccessTokenSilently, getAccessTokenWithPopup, requestToken) =>
    getDataWithAuthentication(
      `/competitions/${competitionUuid}/exportNafData`,
      getAccessTokenSilently,
      getAccessTokenWithPopup,
      requestToken
    )
      .then((result) => offerDownloadData(result, `${competitionUuid}-nafReport.xml`, 'application/xml'))
      .catch(handleError),
};
