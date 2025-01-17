import formatter from '../../util/formatter';

function WinRate({ identifier, winRate }) {
  return `${identifier ? `${identifier}: ` : ''}${
    winRate && winRate.winRate !== null
      ? `${formatter.formatAsPercentage(winRate.winRate)} (${winRate.wins}/${winRate.draws}/${winRate.losses})`
      : '-'
  }`;
}

export default WinRate;
