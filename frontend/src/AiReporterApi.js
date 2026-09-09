import axios from 'axios';
import config from './config';

axios.defaults.baseURL = config.backendUrl;

const authorizationParams = {
  authorizationParams: {
    audience: config.auth0Audience,
  },
};

const getToken = async (getAccessTokenSilently, getAccessTokenWithPopup) => {
  if (!config.isProduction) return 'dev-token';
  try {
    return await getAccessTokenSilently(authorizationParams);
  } catch (e) {
    return getAccessTokenWithPopup(authorizationParams);
  }
};

const authConfig = async (getAccessTokenSilently, getAccessTokenWithPopup) => {
  const token = await getToken(getAccessTokenSilently, getAccessTokenWithPopup);
  return {
    withCredentials: true,
    headers: { Authorization: `Bearer ${token}` },
  };
};

const AiReporterApi = {
  reporters: async () => (await axios.get('/ai-reporters')).data,
  reporter: async (id) => (await axios.get(`/ai-reporters/${encodeURIComponent(id)}`)).data,
  reports: async (id) => (await axios.get(`/ai-reporters/${encodeURIComponent(id)}/reports`)).data,

  adminReporters: async (getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.get('/admin/ai-reporters', auth)).data;
  },

  updateRuntime: async (id, data, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.put(
      `/admin/ai-reporters/${encodeURIComponent(id)}/runtime`,
      data,
      auth
    )).data;
  },
};

export default AiReporterApi;
