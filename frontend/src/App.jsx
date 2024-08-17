import React from 'react';
import { Box, ChakraProvider, CSSReset, DarkMode, extendTheme } from '@chakra-ui/react';
import { HashRouter as Router, Route, Routes, useNavigate } from 'react-router-dom';
import { Auth0Provider, withAuthenticationRequired } from '@auth0/auth0-react';
import WarpScores from './pages/WarpScores';
import TeamPage from './pages/TeamPage';
import CompetitionPage from './pages/CompetitionPage';
import CircuitLegPage from './pages/CircuitLegPage';
import AboutPage from './pages/AboutPage';
import CoachPage from './pages/CoachPage';
import AdminPage from './pages/AdminPage';
import StatisticsPage from './pages/StatisticsPage';
import LatestMatchesPage from './pages/LatestMatchesPage';
import LiveMatchesPage from './pages/LiveMatchesPage';
import CircuitPage from './pages/CircuitPage';
import LeaguePage from './pages/LeaguePage';
import config from './config';
import linkTheme from './theme/components/Link';
import tableTheme from './theme/components/Table';

const theme = extendTheme({
  components: {
    Link: linkTheme,
    RouteLink: linkTheme,
    Table: tableTheme,
  },
});

function ProtectedRoute({ component, ...args }) {
  const Component = withAuthenticationRequired(component, args);
  return <Component />;
}

function Auth0ProviderWithRedirectCallback({ children, ...props }) {
  const navigate = useNavigate();
  const onRedirectCallback = (appState) => {
    navigate((appState && appState.returnTo) || window.location.pathname);
  };
  return (
    <Auth0Provider onRedirectCallback={onRedirectCallback} {...props}>
      {children}
    </Auth0Provider>
  );
}

function App() {
  return (
    <ChakraProvider theme={theme}>
      <CSSReset />
      <DarkMode>
        <Box padding="4">
          <Router>
            <Auth0ProviderWithRedirectCallback
              domain={config.auth0Domain}
              clientId={config.auth0ClientId}
              authorizationParams={{
                redirect_uri: window.location.origin,
              }}
            >
              <Routes>
                {/* Public Routes */}
                <Route path="/" element={<WarpScores />} />
                <Route path="/statistics" element={<StatisticsPage />} />
                <Route path="/about" element={<AboutPage />} />
                <Route path="/:leagueUuid" element={<LeaguePage />} />
                <Route path="/latestMatches/:leagueUuid" element={<LatestMatchesPage />} />
                <Route path="/latestMatches/:leagueUuid/:limit" element={<LatestMatchesPage />} />
                <Route path="/liveMatches/:leagueUuid" element={<LiveMatchesPage />} />
                <Route path="/team/:teamUuid" element={<TeamPage />} />
                <Route path="/competition/:competitionUuid" element={<CompetitionPage />} />
                <Route path="/competition/:competitionUuid/team/:teamUuid" element={<TeamPage />} />
                {/* Protected Routes/Needing authentication */}
                <Route path="/coachPage" element={<ProtectedRoute component={CoachPage} />} />
                <Route path="/admin" element={<ProtectedRoute component={AdminPage} />} />
                <Route path="/admin/circuit/:circuitId" element={<ProtectedRoute component={CircuitPage} />} />
                <Route
                  path="/admin/circuit/:circuitId/leg/:legId"
                  element={<ProtectedRoute component={CircuitLegPage} />}
                />
              </Routes>
            </Auth0ProviderWithRedirectCallback>
          </Router>
        </Box>
      </DarkMode>
    </ChakraProvider>
  );
}

export default App;
