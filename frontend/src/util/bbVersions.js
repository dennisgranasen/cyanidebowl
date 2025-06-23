const bbVersions = [1, 2, 3];

const getOpusFromGame = (game) => {
  console.log(`getOpusFromGame: ${game}`);
  switch (game.toUpperCase()) {
    case 'BB1': return 1;
    case 'BB2': return 2;
    case 'BB3': return 3;
    default: return undefined;
  }
};
const getGameFromOpus = (opus) => {
  switch (opus) {
    case 1: return 'BB1';
    case 2: return 'BB2';
    case 3: return 'BB3';
    default: return undefined;
  }
};

export { bbVersions, getOpusFromGame, getGameFromOpus };