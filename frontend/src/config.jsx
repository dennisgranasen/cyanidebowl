const backendUrl = process.env.NODE_ENV === 'production' ? 'https://warp-scores.net/api' : 'http://localhost:8080';

const config = {
  backendUrl,
  locale: 'en-UK',
  boxSize: '32px',
  smallBoxSize: '24px',
};

export default config;
