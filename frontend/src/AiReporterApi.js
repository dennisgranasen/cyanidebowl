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
  pendingWorkTargets: async (kind, silent, popup) => (await axios.get(
    '/admin/ai-autonomous-work/pending/targets',
    { ...(await authConfig(silent, popup)), params: { kind } }
  )).data,
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

  playerRatingRouting: async (getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.get('/admin/ai-reporters/player-rating-routing', auth)).data;
  },

  updatePlayerRatingRouting: async (data, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.put('/admin/ai-reporters/player-rating-routing', data, auth)).data;
  },

  leagueSystemInitiativePolicy: async (leagueSystemId, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.get(
      `/admin/league-systems/${encodeURIComponent(leagueSystemId)}/ai-initiative-policy`,
      auth
    )).data;
  },

  updateLeagueSystemInitiativePolicy: async (
    leagueSystemId, policy, getAccessTokenSilently, getAccessTokenWithPopup
  ) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.put(
      `/admin/league-systems/${encodeURIComponent(leagueSystemId)}/ai-initiative-policy`,
      policy,
      auth
    )).data;
  },

  backfillFanMatchComments: async (
    leagueSystemId, daysBack, getAccessTokenSilently, getAccessTokenWithPopup
  ) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.post(
      `/admin/league-systems/${encodeURIComponent(leagueSystemId)}/ai-initiative-policy/fan-match-comments/backfill`,
      { daysBack },
      auth
    )).data;
  },

  imageApprovalPolicy: async (silent, popup) => {
    const auth = await authConfig(silent, popup);
    return (await axios.get('/admin/ai-reporters/image-approval-policy', auth)).data;
  },

  updateImageApprovalPolicy: async (policy, silent, popup) => {
    const auth = await authConfig(silent, popup);
    return (await axios.put('/admin/ai-reporters/image-approval-policy', { policy }, auth)).data;
  },

  editorialImageRequests: async (silent, popup) => {
    const auth = await authConfig(silent, popup);
    return (await axios.get('/admin/ai-reporters/editorial-image-requests', auth)).data;
  },

  reviewEditorialImageRequest: async (id, approve, silent, popup) => {
    const auth = await authConfig(silent, popup);
    return (await axios.post(`/admin/ai-reporters/editorial-image-requests/${encodeURIComponent(id)}/review`, { approve }, auth)).data;
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
            ...auth, signal: controller.signal, timeout: 90000,
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
          }, 90000);
        }),
      ]);
    } finally {
      clearTimeout(timer);
    }
  },

  aiJobInput: async (type, id, queue, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.get('/admin/ai-autonomous-work/job-input', {
      ...auth,
      params: { type, id, ...(queue ? { queue } : {}) },
      timeout: 30000,
    })).data;
  },

  copilotQuota: async (getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.get('/admin/ai-autonomous-work/copilot-quota', auth)).data;
  },

  communityImageProviders: async (getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.get('/admin/community-fans/media/providers', auth)).data;
  },

  moveCommunityMediaJob: async (id, provider, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.put(
      `/admin/ai-autonomous-work/media-queue/jobs/${encodeURIComponent(id)}/provider`,
      { provider },
      auth
    )).data;
  },

  setCopilotQuotaThreshold: async (minimumRemainingPercent, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.put(
      '/admin/ai-autonomous-work/copilot-quota/threshold',
      { minimumRemainingPercent },
      auth
    )).data;
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
  movePendingWork: async (path, targetId, confirmCopilotQuota, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.put(
      `/admin/ai-autonomous-work/${path}/target`,
      { targetId, confirmCopilotQuota },
      auth
    )).data;
  },
  moveAiExecutionJob: async (target, id, targetId, confirmCopilotQuota, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.put(
      `/admin/ai-autonomous-work/execution-queues/${encodeURIComponent(target)}/jobs/${encodeURIComponent(id)}/target`,
      { targetId, confirmCopilotQuota },
      auth
    )).data;
  },
  submitExternalAiResult: async (target, id, content, getAccessTokenSilently, getAccessTokenWithPopup) => {
    const auth = await authConfig(getAccessTokenSilently, getAccessTokenWithPopup);
    return (await axios.put(
      `/admin/ai-autonomous-work/execution-queues/${encodeURIComponent(target)}/jobs/${encodeURIComponent(id)}/external-result`,
      { content },
      auth
    )).data;
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
