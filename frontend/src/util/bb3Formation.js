const coordinate = (position, lowerKey, upperKey) => {
  const value = position?.[lowerKey] ?? position?.[upperKey];
  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isInteger(parsed) ? parsed : null;
};

export const formationPlayers = (pitchMap, rosterPlayers = []) => {
  const entries = Object.entries(pitchMap && typeof pitchMap === 'object' ? pitchMap : {});
  const positions = entries.map(([playerId, position]) => ({
    playerId,
    player: rosterPlayers.find((candidate) => String(candidate.player_id) === playerId),
    x: coordinate(position, 'x', 'X'),
    y: coordinate(position, 'y', 'Y'),
  }));
  const onFullPitch = positions.filter(({ x }) => x !== null && x >= 0 && x < 26);
  const farHalf = onFullPitch.length > 0 && onFullPitch.every(({ x }) => x >= 13);

  return positions.map((position) => {
    const x = position.x === null ? null : farHalf ? 25 - position.x : position.x;
    const onPitch = x !== null && x >= 0 && x < 13
      && position.y !== null && position.y >= 0 && position.y < 15;
    return {
      ...position,
      x,
      onPitch,
      label: position.player?.number == null ? '?' : String(position.player.number),
      name: position.player?.name || `Player ${position.playerId}`,
    };
  });
};
