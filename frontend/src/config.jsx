const backendUrl = process.env.NODE_ENV === 'production' ? 'https://warp-scores.net/api' : 'http://localhost:18080';

const config = {
  backendUrl,
  locale: 'en-UK',
  boxSize: '32px',
  smallBoxSize: '24px',
  hoverBoxShadow: 'inset 0 0 0 2000px rgba(255, 0, 150, 0.3);',
};

export default config;
