import CommunityApi from './CommunityApi';

test('sends coach together with other directory filters and pagination', async () => {
  const previousFetch = global.fetch;
  global.fetch = jest.fn().mockResolvedValue({ ok: true, json: async () => ({ members: [] }) });
  try {
    await CommunityApi.directory('nst', { coach: '3_coach/name', season: 's7', status: 'active', page: 1, size: 24 });
    const url = new URL(global.fetch.mock.calls[0][0], 'http://localhost');
    expect(Object.fromEntries(url.searchParams)).toEqual({
      leagueSystemId: 'nst', coach: '3_coach/name', season: 's7', status: 'active', page: '1', size: '24',
    });
    await CommunityApi.directory('nst', { coach: '' });
    expect(new URL(global.fetch.mock.calls[1][0], 'http://localhost').searchParams.has('coach')).toBe(false);
  } finally {
    global.fetch = previousFetch;
  }
});
