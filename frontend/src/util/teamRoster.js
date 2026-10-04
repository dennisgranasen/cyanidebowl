const normalizedName = (name) => String(name || '').trim().toLocaleLowerCase();
const playerId = (player) => String(player?.player_id || player?.id?.key || '').toLocaleLowerCase();
const teamId = (team) => String(team?.id?.key || team?.id || '').toLocaleLowerCase();

const statValue = (stats, ...keys) => {
  const value = keys.map((key) => stats?.[key]).find((candidate) => candidate !== null && candidate !== undefined);
  const number = Number(value);
  return Number.isFinite(number) ? number : 0;
};

function matchingSnapshotPlayer(player, snapshotPlayers) {
  const id = playerId(player);
  if (id) {
    const byId = snapshotPlayers.find((snapshotPlayer) => playerId(snapshotPlayer) === id);
    if (byId) return byId;
  }
  return snapshotPlayers.find((snapshotPlayer) => snapshotPlayer.number === player.number
    && normalizedName(snapshotPlayer.name) === normalizedName(player.name));
}

export function playersFromLiveRoster(roster, fallbackPlayers) {
  if (!Array.isArray(roster?.players) || (roster.players.length === 0 && fallbackPlayers.length > 0)) return null;
  return roster.players.map((player) => {
    const fallback = matchingSnapshotPlayer(player, fallbackPlayers) || {};
    const skillNames = Object.values(player.skill_names || {});
    return {
      ...fallback,
      id: { ...(fallback.id || {}), key: player.player_id || fallback.id?.key },
      name: player.name || fallback.name,
      type: player.type || fallback.type,
      number: player.number,
      level: player.level,
      spp: player.spp,
      xp: player.spp,
      value: player.value,
      attributes: player.attributes || fallback.attributes,
      skills: skillNames.length ? skillNames : (fallback.skills || fallback.skillStrings || []),
      casualtiesStates: fallback.casualtiesStates || [],
      suspendedNextMatch: player.miss_next_game ?? fallback.suspendedNextMatch,
      dead: Boolean(player.dead),
      retirementStatus: player.retirement_status,
    };
  });
}

export function markPlayersMissingFromLiveRoster(snapshotPlayers, currentPlayers) {
  if (!Array.isArray(currentPlayers)) return snapshotPlayers;
  return snapshotPlayers.map((player) => ({
    ...player,
    notInCurrentRoster: !matchingSnapshotPlayer(player, currentPlayers),
  }));
}

export function fillMissingPlayerTypes(players, matches, requestedTeamId) {
  const wantedTeamId = String(requestedTeamId || '').toLocaleLowerCase();
  const knownPlayers = (matches || []).flatMap((match) => (match.teams || [])
    .filter((team) => teamId(team) === wantedTeamId)
    .flatMap((team) => team.players || []));
  return (players || []).map((player) => {
    if (player.type) return player;
    const knownPlayer = matchingSnapshotPlayer(player, knownPlayers);
    return knownPlayer?.type ? { ...player, type: knownPlayer.type } : player;
  });
}

export function careerStatsForTeam(matches, requestedTeamId) {
  const careerStats = new Map();
  const wantedTeamId = String(requestedTeamId || '').toLocaleLowerCase();
  (matches || []).forEach((match) => {
    (match.teams || []).filter((team) => teamId(team) === wantedTeamId).forEach((team) => {
      (team.players || []).forEach((player) => {
        const id = playerId(player) || `${player.number}:${normalizedName(player.name)}`;
        const stats = player.stats || {};
        const total = careerStats.get(id) || {
          games: 0, sppEarned: 0, touchdowns: 0, casualties: 0, mvps: 0,
          passes: 0, catches: 0, blocks: 0, knockouts: 0, interceptions: 0,
        };
        total.games += 1;
        total.sppEarned += statValue(stats, 'spp_gained', 'sppGained');
        total.touchdowns += statValue(stats, 'touchdowns_scored', 'touchdownsScored', 'inflictedtouchdowns');
        total.casualties += statValue(stats, 'casualties_inflicted', 'casualtiesInflicted', 'inflictedcasualties');
        total.mvps += player.mvp === true ? 1 : 0;
        total.passes += statValue(stats, 'inflictedpasses', 'inflictedPasses');
        total.catches += statValue(stats, 'inflictedcatches', 'inflictedCatches');
        total.blocks += statValue(stats, 'blocks_succeeded', 'blocksSucceeded', 'inflictedtackles');
        total.knockouts += statValue(stats, 'ko_inflicted', 'koInflicted', 'inflictedko');
        total.interceptions += statValue(stats, 'inflictedinterceptions', 'inflictedInterceptions');
        careerStats.set(id, total);
      });
    });
  });
  return careerStats;
}