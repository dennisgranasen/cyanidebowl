import config from './config';

const baseUrl = config.backendUrl || '';

async function json(url) {
  const response = await fetch(`${baseUrl}${url}`);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}

const CommunityApi = {
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
