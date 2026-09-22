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
  pendingWork: async (silent, popup) => (await axios.get('/admin/ai-autonomous-work/pending', { ...(await authConfig(silent, popup)), timeout: 20000 })).data,
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

  autonomousWorkOverview: async (getAccessTokenSilently, getAccessTokenWithPopup) => {
    const controller = new AbortController();
    let timer;
    try {
      return await Promise.race([
        (async () => {
          const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
          if (controller.signal.aborted) throw new Error('Overview request expired');
          const { data } = await axios.get('/admin/ai-autonomous-work', {
            ...auth, signal: controller.signal, timeout: 20000,
          });
          if (!data || typeof data !== 'object' || Array.isArray(data)) {
            throw new Error('Unable to load the work overview: invalid server response.');
          }
          return data;
        })(),
        new Promise((resolve, reject) => {
          timer = setTimeout(() => {
            reject(new Error('Loading the work overview timed out. Please try Refresh.'));
            controller.abort();
          }, 20000);
        }),
      ]);
    } finally {
      clearTimeout(timer);
    }
  },

  setAutonomousWorkEnabled: async (
    enabled, getAccessTokenSilently, getAccessTokenWithPopup
  ) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.put('/admin/ai-autonomous-work/enabled', { enabled }, auth)).data;
  },

  reprioritizeAiQueueJob: async (path, priority, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.put(`/admin/ai-autonomous-work/${path}/priority`, { priority }, auth)).data;
  },
  deleteAiQueueJob: async (path, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.delete(`/admin/ai-autonomous-work/${path}`, auth)).data;
  },
  clearAiQueue: async (path, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.delete(`/admin/ai-autonomous-work/${path}`, auth)).data;
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
