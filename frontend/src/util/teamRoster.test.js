import {
  careerStatsForTeam,
  fillMissingPlayerTypes,
  formerPlayersForTeam,
  mergeTeamDetails,
  markPlayersMissingFromLiveRoster,
  playerKey,
  playersFromLiveRoster,
} from './teamRoster';

describe('team roster views', () => {
  test('prefers live team details, then latest match snapshot, then stored team data', () => {
    const merged = mergeTeamDetails(
      { name: 'Stored name', rerolls: 2, dedicatedFans: 1, cash: 50000 },
      { name: 'Latest match name', rerolls: 3, dedicatedFans: 2, cash: 60000 },
      { rerolls: 4, dedicatedFans: null, cash: 0 },
    );

    expect(merged).toMatchObject({
      name: 'Latest match name', rerolls: 4, dedicatedFans: 2, cash: 0,
    });
  });

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

  test('uses the same normalized key for match history and former roster players', () => {
    const formerPlayer = { id: { key: '3_player-1' }, name: 'Gone', number: 4 };
    const stats = careerStatsForTeam([
      { teams: [{ id: '3_team', players: [{
        id: { key: '3_player-1' }, stats: { spp_gained: 5, touchdowns_scored: 1 },
      }] }] },
      { teams: [{ id: '3_team', players: [{
        id: { key: 'player-1' }, stats: { spp_gained: 2, touchdowns_scored: 1 },
      }] }] },
    ], '3_team');

    expect(stats.get(playerKey(formerPlayer))).toMatchObject({
      games: 2, sppEarned: 7, touchdowns: 2,
    });
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

  test('returns departed and explicitly dead players from the full team history', () => {
    const former = formerPlayersForTeam([
      { teams: [{ id: '3_team', players: [
        { id: { key: '3_active' }, name: 'Active', number: 1, type: 'Thrall' },
        { id: { key: '3_dead' }, name: 'Dead Player', number: 2, casualties: { newCasualties: ['dead'] } },
        { id: { key: '3_departed' }, name: 'Departed', number: 3 },
      ] }] },
    ], '3_team', [{ player_id: 'active', name: 'Active', number: 1 }]);

    expect(former.map((player) => player.number)).toEqual([2, 3]);
    expect(former[0]).toMatchObject({ dead: true, notInCurrentRoster: true });
    expect(former[1]).toMatchObject({ dead: false, notInCurrentRoster: true });
  });

  test('does not classify all historical players as departed without a current roster', () => {
    const former = formerPlayersForTeam([
      { teams: [{ id: '3_team', players: [
        { id: { key: '3_alive' }, name: 'Unknown', number: 1 },
        { id: { key: '3_dead' }, name: 'Dead Player', number: 2, dead: true },
      ] }] },
    ], '3_team', null);

    expect(former.map((player) => player.number)).toEqual([2]);
    expect(former[0].dead).toBe(true);
  });

  test('returns historical players absent from the latest saved roster when live data is unavailable', () => {
    const matches = [
      { teams: [{ id: '3_team', players: [
        { id: { key: '3_player-4' }, name: 'Former Player', number: 4 },
        { id: { key: '3_player-5' }, name: 'Current Player', number: 5 },
      ] }] },
      { teams: [{ id: '3_team', players: [
        { id: { key: 'player-5' }, name: 'Current Player', number: 5 },
      ] }] },
    ];
    const latestSavedRoster = matches[1].teams[0].players;

    const former = formerPlayersForTeam(matches, '3_team', latestSavedRoster);

    expect(former).toHaveLength(1);
    expect(former[0]).toMatchObject({ number: 4, notInCurrentRoster: true, dead: false });
  });
});