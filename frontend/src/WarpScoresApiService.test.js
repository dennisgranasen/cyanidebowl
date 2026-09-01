jest.mock('axios', () => {
  const mockAxios = jest.fn();
  mockAxios.defaults = {};
  mockAxios.post = jest.fn();
  mockAxios.delete = jest.fn();
  return mockAxios;
});

import axios from 'axios';
import WarpScoresApiService from './WarpScoresApiService';

describe('WarpScoresApiService', () => {
  beforeEach(() => {
    axios.mockReset();
    axios.post.mockReset();
    axios.delete.mockReset();
    axios.mockResolvedValue({ data: [] });
    axios.post.mockResolvedValue({ data: {} });
  });

  test('uses canonical competition URLs and authenticated lookup requests', async () => {
    await WarpScoresApiService.competitionMatches({ key: 'competition-1' }, 10);
    await WarpScoresApiService.competitionTeams({ key: 'competition-1' });
    await WarpScoresApiService.lookup({ league_name: 'League' }, jest.fn(), jest.fn());

    expect(axios).toHaveBeenCalledWith('/matches/competition/competition-1?limit=10');
    expect(axios).toHaveBeenCalledWith('/teams/competition/competition-1');
    expect(axios.post).toHaveBeenCalledWith(
      '/lookup',
      { league_name: 'League' },
      { headers: { Authorization: 'Bearer dev-token' } },
    );
  });
});
