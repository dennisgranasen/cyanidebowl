import config from './config';

const baseUrl = config.backendUrl || '';

async function json(url) {
  const response = await fetch(`${baseUrl}${url}`);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}

const CommunityApi = {
  directory: system => json(`/community/directory?leagueSystemId=${encodeURIComponent(system)}`),
  comments: (id, page = 0) => json(`/community/fans/${encodeURIComponent(id)}/comments?page=${page}`),
  discussion: (type, id) => json(`/community/discussion/${encodeURIComponent(type)}/${encodeURIComponent(id)}`),
  fans: (teamId) => json(
    `/community/fans${teamId ? `?teamId=${encodeURIComponent(teamId)}` : ''}`
  ),
  fan: (id) => json(`/community/fans/${encodeURIComponent(id)}`),
  assetUrl: (value) => {
    if (!value) return undefined;
    if (/^https?:\/\//i.test(value)) return value;
    return `${baseUrl}${value}`;
  },
};

export default CommunityApi;
