import config from './config';

const baseUrl = config.apiUrl || '';

async function json(url) {
  const response = await fetch(`${baseUrl}${url}`);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}

const CommunityApi = {
  fans: () => json('/community/fans'),
  fan: (id) => json(`/community/fans/${encodeURIComponent(id)}`),
};

export default CommunityApi;
