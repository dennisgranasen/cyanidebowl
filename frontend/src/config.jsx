const MAX_AGE_FOR_STATUS_IN_MILLIS = 35 * 60 * 1_000;
const isProduction = process.env.NODE_ENV === 'production';
const backendUrl = isProduction ?  process.env.BACKEND_URI : 'http://localhost:8080';
const auth0ClientId = 'vds4UOVhMmdut8kNXOQ0XJ1s5st95vdu';
const auth0Domain = 'warp-scores.eu.auth0.com';
const auth0Audience = 'warp-scores-backend';
const devDelay = isProduction ? 0 : 1000;

const config = {
  MAX_AGE_FOR_STATUS_IN_MILLIS,
  backendUrl,
  isProduction,
  devDelay,
  auth0ClientId,
  auth0Domain,
  auth0Audience,
  showCircuitsFeature: false,
  locale: 'en-UK',
  boxSize: '2rem',
  smallBoxSize: '1.5rem',
  tinyBoxSize: '1.2rem',
  hoverBoxShadow: 'inset 0 0 0 2000px rgba(255, 0, 150, 0.3);',
  smallScreenBreakpointValues: { base: true, sm: true, md: false },
};

export default config;
