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
const authDelete = async (url, getToken) =>
  axios.delete(`${base}${url}`, { headers: await authHeaders(getToken) });

const EditorialCommunityApi = {
  articles: (leagueSystemId, limit = 20, seasonId, type, subjectId) =>
    get(`/articles?limit=${limit}${leagueSystemId ? `&leagueSystemId=${encodeURIComponent(leagueSystemId)}` : ''}${seasonId ? `&seasonId=${encodeURIComponent(seasonId)}` : ''}${type && subjectId ? `&type=${encodeURIComponent(type)}&subjectId=${encodeURIComponent(subjectId)}` : ''}`),
  resolveAssociation: (type, id) => get(`/articles/associations/resolve?${new URLSearchParams({ type, id })}`),
  associationOptions: (type, q, leagueSystemId) => get(`/articles/associations?${new URLSearchParams({ type, q, ...(leagueSystemId ? { leagueSystemId } : {}) })}`),
  myArticles: token => authGet('/articles/mine', token),
  articleCapabilities: (id, payload, token) => authPost(`/articles/capabilities${id ? `?id=${encodeURIComponent(id)}` : ''}`, payload, token),
  editorArticle: (id, token) => authGet(`/articles/editor/${encodeURIComponent(id)}`, token),
  reviewQueue: (system, token) => authGet(`/articles/review${system ? `?leagueSystemId=${encodeURIComponent(system)}` : ''}`, token),
  reviewArticle: (id, accept, confirmGlobal, token) => authPost(`/articles/${encodeURIComponent(id)}/review`, { accept, confirmGlobal }, token),
  reporters: (system, token) => authGet(`/articles/tools/reporters${system ? `?leagueSystemId=${encodeURIComponent(system)}` : ''}`, token),
  articlePolicy: (system, token) => authGet(`/articles/tools/policy${system ? `?leagueSystemId=${encodeURIComponent(system)}` : ''}`, token),
  setArticlePolicy: (system, autoAccept, token) => authPut(`/articles/tools/policy${system ? `?leagueSystemId=${encodeURIComponent(system)}` : ''}`, { autoAccept }, token),
  generateArticle: (payload, token) => authPost('/articles/tools/generate', payload, token),
  generateArticleImage: (payload, token) => authPost('/articles/tools/image', payload, token),
  uploadArticleImage: (file, system, token) => {
    const data = new FormData(); data.append('file', file);
    return authPost(`/articles/tools/upload${system ? `?leagueSystemId=${encodeURIComponent(system)}` : ''}`, data, token);
  },
  assetUrl: (url) => url?.startsWith('/community/media/assets/') ? `${base}${url}` : url,
  article: (slug) => get(`/articles/${encodeURIComponent(slug)}`),
  comments: (targetType, targetId) =>
    get(`/community/comments/${targetType}/${encodeURIComponent(targetId)}`),
  reactions: (targetType, targetId, getToken = null) =>
    getToken
      ? authGet(`/community/reactions/${targetType}/${encodeURIComponent(targetId)}`, getToken)
      : get(`/community/reactions/${targetType}/${encodeURIComponent(targetId)}`),
  createArticle: async (payload, getToken) =>
    (await axios.post(`${base}/articles`, payload, { headers: await authHeaders(getToken) })).data,
  updateArticle: async (id, payload, getToken) =>
    (await axios.put(`${base}/articles/${id}`, payload, { headers: await authHeaders(getToken) })).data,
  comment: async (targetType, targetId, body, getToken, replyToCommentId = null) =>
    (await axios.post(`${base}/community/comments/${targetType}/${encodeURIComponent(targetId)}`,
      { body, replyToCommentId }, { headers: await authHeaders(getToken) })).data,
  deleteComment: async (id, getToken) =>
    axios.delete(`${base}/community/comments/${id}`, { headers: await authHeaders(getToken) }),
  react: async (targetType, targetId, type, getToken) =>
    (await axios.put(`${base}/community/reactions/${targetType}/${encodeURIComponent(targetId)}`,
      { type }, { headers: await authHeaders(getToken) })).data,
  removeReaction: async (targetType, targetId, getToken) =>
    axios.delete(`${base}/community/reactions/${targetType}/${encodeURIComponent(targetId)}`,
      { headers: await authHeaders(getToken) }),
  matchPlayers: (matchId) => get(`/community/matches/${encodeURIComponent(matchId)}/players`),
  matchRatings: (matchId) => get(`/community/matches/${encodeURIComponent(matchId)}/ratings`),
  matchRatingOverview: (matchId, getToken = null) =>
    getToken
      ? authGet(`/community/matches/${encodeURIComponent(matchId)}/rating-overview`, getToken)
      : get(`/community/matches/${encodeURIComponent(matchId)}/rating-overview`),
  ratePlayer: async (matchId, playerId, score, getToken) =>
    (await axios.put(`${base}/community/matches/${encodeURIComponent(matchId)}/players/${encodeURIComponent(playerId)}/rating`,
      { score }, { headers: await authHeaders(getToken) })).data,
  aiPlayerRatings: (matchId) =>
    get(`/matches/${encodeURIComponent(matchId)}/ai-player-ratings`),
  generateAiPlayerRatings: (matchId, payload, getToken) =>
    authPost(`/matches/${encodeURIComponent(matchId)}/ai-player-ratings/generate`, payload, getToken),
  fanPlayerRatingAvailability: (matchId) =>
    get(`/matches/${encodeURIComponent(matchId)}/ai-player-ratings/fan-availability`),
  generateFanPlayerRatings: (matchId, payload, getToken) =>
    authPost(`/matches/${encodeURIComponent(matchId)}/ai-player-ratings/generate-fans`, payload, getToken),
  fanPlayerRatingStatus: (matchId) =>
    get(`/matches/${encodeURIComponent(matchId)}/ai-player-ratings/fan-generation-status`),
  aiPlayerRatingStatus: (matchId) =>
    get(`/matches/${encodeURIComponent(matchId)}/ai-player-ratings/generation-status`),
  cancelAiPlayerRatingJob: (matchId, jobId, getToken) =>
    authPost(`/matches/${encodeURIComponent(matchId)}/ai-player-ratings/generation-jobs/${encodeURIComponent(jobId)}/cancel`, {}, getToken),
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
  deleteMatchArticle: (matchId, articleId, getToken) =>
    authDelete(`/matches/${encodeURIComponent(matchId)}/articles/${encodeURIComponent(articleId)}`, getToken),
  requestAiMatchArticle: (matchId, payload, getToken) =>
    authPost(`/matches/${encodeURIComponent(matchId)}/articles/ai`, payload, getToken),
  rejectedMatchArticles: (getToken) =>
    authGet('/admin/match-articles/rejected', getToken),
  deleteRejectedMatchArticle: (articleId, getToken) =>
    authDelete(`/admin/match-articles/${encodeURIComponent(articleId)}`, getToken),
};

export default EditorialCommunityApi;
