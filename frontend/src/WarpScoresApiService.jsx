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
    return {
      headers: {
        Authorization: 'Bearer dev-token',
      },
    }; 
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

const deleteDataWithAuthentication = async (endpoint, getAccessTokenSilently, getAccessTokenWithPopup) => {
  const authHeaders = await getAuthHeaders(getAccessTokenSilently, getAccessTokenWithPopup);
  return axios.delete(endpoint, authHeaders);
};

/*
    public List<IdWithName> lookupLeague(Optional<String> leagueName) {
        if (leagueName.isEmpty()) {
            return Collections.emptyList();
        }

        LookupRequest lookupRequest = new LookupRequest();
        lookupRequest.setLeague_name(leagueName.get());
        AuthenticatedHttpEntity<LookupRequest> authenticatedHttpEntity = new AuthenticatedHttpEntity<>(
                Optional.of(lookupRequest));

        RestTemplate restTemplate = new RestTemplate();
        ParameterizedTypeReference<LookupResponse> lookupResponseRef = new ParameterizedTypeReference<>() {};

        IdWithName[] leagues = null;
        try {
            ResponseEntity<LookupResponse> lookupResponse = restTemplate.exchange(
                    String.format("%s/lookup", warpScoresProperties.getBaseUrls().getApiBackend()),
                    HttpMethod.POST,
                    authenticatedHttpEntity.create(), lookupResponseRef);
            leagues = Optional.ofNullable(lookupResponse.getBody())
                    .map(LookupResponse::getLeagues)
                    .orElse(null);
        }  catch (HttpClientErrorException e) {
            if (404 == e.getStatusCode().value()) {
                log.warn("Lookup for {} did return {}.", leagueName, e.getStatusCode());
            } else {
                log.error("Error {} while lookup.", e.getStatusCode());
            }
        }
        if (leagues == null || leagues.length == 0) {
            return Collections.emptyList();
        } else {
            return List.of(leagues);
        }
    }
*/

export default {
  // misc
  backendVersion: async () => axios(`/version.json`).then(returnData).catch(handleError),


  lookup: async(lookupFields, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const authHeaders = await getAuthHeaders(getAccessTokenSilently, getAccessTokenWithPopup);
    return axios.post('/lookup', lookupFields, authHeaders)
        .then(returnData)
        .catch(handleError)
  },
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
    leagueId,
    competitionId,
    legType,
    customLabel,
    game,
    platform,
    ruleset,
    isCollected,
    isArchived,
    ladderOption,
    getAccessTokenSilently,
    getAccessTokenWithPopup
  ) =>
    postDataWithAuthentication(
      `/circuits/${circuitId}/legs`,
      {
        leagueId,
        competitionId: competitionId  || null,
        legType,
        label: customLabel,
        game: game.toUpperCase(),
        platform,
        ruleset: ruleset.toUpperCase(),
        isCollected: isCollected,
        isArchived: isArchived,
        ladderOption: ladderOption || null,
      },
      getAccessTokenSilently,
      getAccessTokenWithPopup
    )
      .then(returnData)
      .catch(handleError),
  removeCircuitLeg: async (
    circuitId, 
    circuitLegId,
    getAccessTokenSilently,
    getAccessTokenWithPopup
  ) =>
    deleteDataWithAuthentication(
      `/circuits/${circuitId}/legs/${circuitLegId}`,
      getAccessTokenSilently,
      getAccessTokenWithPopup
    )
      .then(returnData)
      .catch(handleError),
  updateCircuitLeg: async (
    circuitId, 
    circuitLegId, 
    updateFields, 
    getAccessTokenSilently, 
    getAccessTokenWithPopup
  ) =>
    postDataWithAuthentication(
      `/circuits/${circuitId}/legs/${circuitLegId}/update`,
      updateFields,
      getAccessTokenSilently,
      getAccessTokenWithPopup
    )
      .then(returnData)
      .catch(handleError),
  // leagues
  leagues: async (leagueUuid, opus) =>
    axios(`/leagues${leagueUuid ? `/${leagueUuid}` : ''}${opus !== undefined && opus !== null ? `?opus=${opus}` : ''}`)
      .then(returnData)
      .catch(handleError),
  fetchCompetitionCountByStatus: async (leagues) => 
    axios(`/league/competitionCountByStatus?leagueIds=${leagues.map((l) => l.id.key).join(',')}`) 
      .then(returnData).catch(handleError),
  // contests
  liveLeagueContests: async (leagueUuid, limit) =>
    axios(`/contests/league/${leagueUuid}/live${limit ? `/${limit}` : ''}`)
      .then(returnData)
      .catch(handleError),
  liveCompetitionContests: async (competitionUuid, limit) =>
    axios(`/contests/competition/${competitionUuid}/live${limit ? `/${limit}` : ''}`)
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
  // matches
  latestCompetitionMatches: async (competitionUuid, opus, limit) =>
    axios(`/matches/competition/${competitionUuid}/latest${limit ? `/${limit}` : ''}${opus ? `?opus=${opus}` : ''}`)
      .then(returnData)
      .catch(handleError),
  latestLeagueMatches: async (leagueUuid, opus, limit) =>
    axios(`/matches/league/${leagueUuid}/latest${limit ? `/${limit}` : ''}${opus ? `?opus=${opus}` : ''}`)
      .then(returnData)
      .catch(handleError),
  // competitions
  leagueCompetitions: async (leagueUuid, opus, initialized) =>
    axios(
      `/competitions/league/${leagueUuid}${initialized ? '/initialized' : ''}${opus !== undefined && opus !== null ? `?opus=${opus}`: ''}`
    )
      .then(returnData)
      .catch(handleError),
  competition: async (competitionUuid, opus) =>
    axios(`/competition${competitionUuid ? `/${competitionUuid}` : ''}${opus !== undefined && opus !== null ? `?opus=${opus}` : ''}`)
      .then(returnData)
      .catch(handleError),
  competitionTeam: async (competitionUuid, teamUuid) =>
    axios(`/competitions/${competitionUuid}/team/${teamUuid}`).then(returnData).catch(handleError),
  competitionRanks: async (competitionId, opus, limit) =>
    axios(`/ranks/competition/${competitionId}${limit ? `/${limit}` : ''}${opus !== undefined && opus !== null ? `?opus=${opus}` : ''}`)
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
