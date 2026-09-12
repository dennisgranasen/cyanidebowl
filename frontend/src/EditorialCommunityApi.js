import axios from 'axios';
import config from './config';

const base = config.backendUrl || '';

const authHeaders = async (getAccessTokenSilently) => {
  if (!getAccessTokenSilently) return {};
  const token = await getAccessTokenSilently();
  return { Authorization: `Bearer ${token}` };
};

const get = async (url) => (await axios.get(`${base}${url}`)).data;
const authGet = async (url, getToken) =>
  (await axios.get(`${base}${url}`, { headers: await authHeaders(getToken) })).data;
const authPost = async (url, payload, getToken) =>
  (await axios.post(`${base}${url}`, payload, { headers: await authHeaders(getToken) })).data;
const authPut = async (url, payload, getToken) =>
  (await axios.put(`${base}${url}`, payload, { headers: await authHeaders(getToken) })).data;

const EditorialCommunityApi = {
  articles: (leagueSystemId, limit = 20) =>
    get(`/articles?limit=${limit}${leagueSystemId ? `&leagueSystemId=${encodeURIComponent(leagueSystemId)}` : ''}`),
  article: (slug) => get(`/articles/${encodeURIComponent(slug)}`),
  comments: (targetType, targetId) =>
    get(`/community/comments/${targetType}/${encodeURIComponent(targetId)}`),
  reactions: (targetType, targetId) =>
    get(`/community/reactions/${targetType}/${encodeURIComponent(targetId)}`),
  createArticle: async (payload, getToken) =>
    (await axios.post(`${base}/articles`, payload, { headers: await authHeaders(getToken) })).data,
  updateArticle: async (id, payload, getToken) =>
    (await axios.put(`${base}/articles/${id}`, payload, { headers: await authHeaders(getToken) })).data,
  comment: async (targetType, targetId, body, getToken) =>
    (await axios.post(`${base}/community/comments/${targetType}/${encodeURIComponent(targetId)}`,
      { body }, { headers: await authHeaders(getToken) })).data,
  deleteComment: async (id, getToken) =>
    axios.delete(`${base}/community/comments/${id}`, { headers: await authHeaders(getToken) }),
  react: async (targetType, targetId, type, getToken) =>
    (await axios.put(`${base}/community/reactions/${targetType}/${encodeURIComponent(targetId)}`,
      { type }, { headers: await authHeaders(getToken) })).data,
  matchPlayers: (matchId) => get(`/community/matches/${encodeURIComponent(matchId)}/players`),
  matchRatings: (matchId) => get(`/community/matches/${encodeURIComponent(matchId)}/ratings`),
  ratePlayer: async (matchId, playerId, score, getToken) =>
    (await axios.put(`${base}/community/matches/${encodeURIComponent(matchId)}/players/${encodeURIComponent(playerId)}/rating`,
      { score }, { headers: await authHeaders(getToken) })).data,
  matchArticles: (matchId, getToken) =>
    authGet(`/matches/${encodeURIComponent(matchId)}/articles`, getToken),
  matchArticleCapabilities: (matchId, getToken) =>
    authGet(`/matches/${encodeURIComponent(matchId)}/articles/capabilities`, getToken),
  createMatchArticle: (matchId, payload, getToken) =>
    authPost(`/matches/${encodeURIComponent(matchId)}/articles`, payload, getToken),
  updateMatchArticle: (matchId, articleId, payload, getToken) =>
    authPut(`/matches/${encodeURIComponent(matchId)}/articles/${encodeURIComponent(articleId)}`, payload, getToken),
  submitMatchArticle: (matchId, articleId, getToken) =>
    authPost(`/matches/${encodeURIComponent(matchId)}/articles/${encodeURIComponent(articleId)}/submit`, {}, getToken),
  publishMatchArticle: (matchId, articleId, getToken) =>
    authPost(`/matches/${encodeURIComponent(matchId)}/articles/${encodeURIComponent(articleId)}/publish`, {}, getToken),
  rejectMatchArticle: (matchId, articleId, getToken) =>
    authPost(`/matches/${encodeURIComponent(matchId)}/articles/${encodeURIComponent(articleId)}/reject`, {}, getToken),
  requestAiMatchArticle: (matchId, payload, getToken) =>
    authPost(`/matches/${encodeURIComponent(matchId)}/articles/ai`, payload, getToken),
};

export default EditorialCommunityApi;
