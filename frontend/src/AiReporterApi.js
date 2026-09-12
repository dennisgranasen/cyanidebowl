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

  adminReporter: async (id, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.get(
      `/admin/ai-reporters/${encodeURIComponent(id)}`,
      auth
    )).data;
  },

  adminSettings: async (getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.get('/admin/ai-reporters/settings', auth)).data;
  },

  updateAdminSettings: async (data, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.put('/admin/ai-reporters/settings', data, auth)).data;
  },

  updateRuntime: async (id, data, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.put(
      `/admin/ai-reporters/${encodeURIComponent(id)}/runtime`,
      data,
      auth
    )).data;
  },

  inspectorState: async (id, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.get(
      `/admin/ai-reporters/${encodeURIComponent(id)}/inspector`,
      auth
    )).data;
  },

  injectMemory: async (id, data, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.post(
      `/admin/ai-reporters/${encodeURIComponent(id)}/inspector/memories`,
      data,
      auth
    )).data;
  },

  updateMemory: async (id, memoryId, data, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.put(
      `/admin/ai-reporters/${encodeURIComponent(id)}/inspector/memories/${encodeURIComponent(memoryId)}`,
      data,
      auth
    )).data;
  },

  setManualRelationship: async (id, data, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.post(
      `/admin/ai-reporters/${encodeURIComponent(id)}/inspector/relationships/manual`,
      data,
      auth
    )).data;
  },

  clearManualRelationship: async (
    id, subjectType, subjectId, getAccessTokenSilently, getAccessTokenWithPopup
  ) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return axios.delete(
      `/admin/ai-reporters/${encodeURIComponent(id)}/inspector/relationships/manual`,
      {
        ...auth,
        params: { subjectType, subjectId },
      }
    );
  },

  reconsolidateReporter: async (id, data, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.post(
      `/admin/ai-reporters/${encodeURIComponent(id)}/inspector/reconsolidate`,
      data,
      auth
    )).data;
  },
};

export default AiReporterApi;
