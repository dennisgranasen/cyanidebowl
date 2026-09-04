const MAX_AGE_FOR_STATUS_IN_MILLIS = 35 * 60 * 1_000;
const isProduction = process.env.NODE_ENV === 'production';
//const backendUrl = isProduction ?  'https://cyanidebowl.fly.dev' : 'http://localhost:8080';
const backendUrl = process.env.REACT_APP_BACKEND_URI
  || (isProduction
    ? 'https://api.blaskscore.example'
    : 'http://localhost:8080');

const auth0ClientId = process.env.REACT_APP_AUTH0_CLIENT_ID;
const auth0Domain = process.env.REACT_APP_AUTH0_DOMAIN;
const auth0Audience = process.env.REACT_APP_AUTH0_AUDIENCE;
const devDelay = isProduction ? 0 : 1000;

const config = {
  MAX_AGE_FOR_STATUS_IN_MILLIS,
  backendUrl,
  defaultOpus: 3,
  isProduction,
  devDelay,
  auth0ClientId,
  auth0Domain,
  auth0Audience,
  showCircuitsFeature: false,
  showRaceLogo: true,
  locale: 'en-UK',
  boxSize: '2rem',
  smallBoxSize: '1.5rem',
  tinyBoxSize: '1.2rem',
  hoverBoxShadow: 'inset 0 0 0 2000px rgba(255, 0, 150, 0.3);',
  smallScreenBreakpointValues: { base: true, sm: true, md: false },
};

export default config;
