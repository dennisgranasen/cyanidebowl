import {
  careerStatsForTeam,
  fillMissingPlayerTypes,
  markPlayersMissingFromLiveRoster,
  playersFromLiveRoster,
} from './teamRoster';

describe('team roster views', () => {
  test('maps the current BB3 roster into the shared player row model', () => {
    const snapshot = [{
      id: { key: 'player-1' }, name: 'Old Name', number: 7, type: 'Thrall', casualtiesStates: ['BrokenJaw'],
    }];
    const current = playersFromLiveRoster({ players: [{
      player_id: 'player-1', name: 'Current Name', type: 'Thrall', number: 7, level: 2, spp: 6, value: 80000,
      attributes: { ma: 6, st: 3, ag: 3, pa: 4, av: 8 },
      skill_names: { 1: 'Block' }, casualty_ids: [3], miss_next_game: true,
    }] }, snapshot);

    expect(current[0]).toMatchObject({
      id: { key: 'player-1' }, name: 'Current Name', type: 'Thrall', level: 2, spp: 6, xp: 6,
      attributes: { ma: 6, st: 3, ag: 3, pa: 4, av: 8 },
      value: 80000, skills: ['Block'], casualtiesStates: ['BrokenJaw'], suspendedNextMatch: true,
    });
  });

  test('marks snapshot players absent from BB3 without guessing why they left', () => {
    const snapshot = [
      { id: { key: 'player-1' }, name: 'Still Here', number: 1 },
      { id: { key: 'player-2' }, name: 'Gone', number: 2 },
    ];
    const marked = markPlayersMissingFromLiveRoster(snapshot, [
      { player_id: 'player-1', name: 'Still Here', number: 1 },
    ]);

    expect(marked.map((player) => player.notInCurrentRoster)).toEqual([false, true]);
    expect(marked[1]).not.toHaveProperty('dead');
  });

  test('provides a current roster when no saved match snapshot is available', () => {
    const current = playersFromLiveRoster({ players: [{
      player_id: 'player-1', name: 'Current Player', number: 1,
    }] }, []);

    expect(current).toHaveLength(1);
    expect(current[0].name).toBe('Current Player');
  });

  test('sums per-match career stats by player ID for the requested team', () => {
    const stats = careerStatsForTeam([
      { teams: [{ id: '3_team', players: [{ id: { key: 'player-1' }, stats: {
        touchdowns_scored: 1, spp_gained: 3, inflictedpasses: 2,
      }, mvp: true }] }] },
      { teams: [{ id: '3_team', players: [{ id: { key: 'player-1' }, stats: {
        touchdowns_scored: 2, spp_gained: 4, inflictedpasses: 1,
      } }] }, { id: '3_other', players: [{ id: { key: 'other' }, stats: { touchdowns_scored: 20 } }] }] },
    ], '3_team');

    expect(stats.get('player-1')).toMatchObject({ games: 2, sppEarned: 7, touchdowns: 3, mvps: 1, passes: 3 });
    expect(stats.has('other')).toBe(false);
  });

  test('fills a missing snapshot type from another match for the same player', () => {
    const player = { id: { key: 'player-4' }, name: 'Jitterbug', number: 4 };
    const completed = fillMissingPlayerTypes([player], [
      { teams: [{ id: '3_team', players: [{
        id: { key: 'player-4' }, name: 'Jitterbug', number: 4, type: 'vampire_humanThrall',
      }] }] },
    ], '3_team');

    expect(completed[0].type).toBe('vampire_humanThrall');
  });

  test('fills a live BB3 player type when its ID format differs from match IDs', () => {
    const livePlayer = {
      player_id: 'player-4', name: 'Jitterbug', number: 4, type: null,
    };
    const completed = fillMissingPlayerTypes([livePlayer], [
      { teams: [{ id: '3_team', players: [{
        id: { key: '3_player-4' }, name: 'Jitterbug', number: 4, type: 'vampire_humanThrall',
      }] }] },
    ], '3_team');

    expect(completed[0].type).toBe('vampire_humanThrall');
  });
});