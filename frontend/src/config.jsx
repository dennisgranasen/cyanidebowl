const MAX_AGE_FOR_STATUS_IN_MILLIS = 35 * 60 * 1_000;
const backendUrl = process.env.NODE_ENV === 'production' ? 'https://warp-scores.net/api' : 'http://localhost:8080';

const config = {
  MAX_AGE_FOR_STATUS_IN_MILLIS,
  backendUrl,
  locale: 'en-UK',
  boxSize: '2rem',
  smallBoxSize: '1.5rem',
  hoverBoxShadow: 'inset 0 0 0 2000px rgba(255, 0, 150, 0.3);',
  smallScreenBreakpointValues: { base: true, sm: true, md: false },
};

export default config;
