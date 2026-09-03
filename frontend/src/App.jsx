import React from 'react';
import { Box, ChakraProvider, CSSReset, DarkMode } from '@chakra-ui/react';
import { HashRouter as Router, Route, Routes, useNavigate } from 'react-router-dom';
import { Auth0Provider, withAuthenticationRequired } from '@auth0/auth0-react';
import WarpScores from './pages/WarpScores';
import TeamPage from './pages/TeamPage';
import AdminCircuitPage from './pages/AdminCircuitPage';
import CompetitionPage from './pages/CompetitionPage';
import AboutPage from './pages/AboutPage';
import CoachPage from './pages/CoachPage';
import AdminPage from './pages/AdminPage';
import CircuitLegPage from './pages/CircuitLegPage';
import CircuitLegEntityPage from './pages/CircuitLegEntityPage';
import StatisticsPage from './pages/StatisticsPage';
import LatestMatchesPage from './pages/LatestMatchesPage';
import LiveMatchesPage from './pages/LiveMatchesPage';
import LeaguePage from './pages/LeaguePage';
import config from './config';
import MarkdownPage from './pages/MarkdownPage';
import warpScoresTheme from './theme/WarpScoresTheme';
import CircuitPage from './pages/CircuitPage';
import AdminCircuitLegPage from './pages/AdminCircuitLegPage';
import Fonts from './theme/Fonts';
import ArenaPage from './pages/ArenaPage';
import ArenaCoachPage from './pages/ArenaCoachPage';
import CompetitionStatsPage from './pages/CompetitionStatsPage';
import AccountPage from './pages/AccountPage';
import { MyTeamsProvider } from './context/MyTeamsContext';

import { MockAuth0Provider } from './components/misc/MockAuthProvider';


const { isProduction } = config;

const withoutAuthentication = (Component) => {
  return function WithoutAuthentication() {
    return <Component />;
  };
};

function ProtectedRoute({ component, ...args }) {
  const Component = isProduction ? withAuthenticationRequired(component, args) : withoutAuthentication(component);
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

function AppRoutes() {
  return (
    <MyTeamsProvider>
      <Routes>
      {/* Public Routes */}
      <Route path="/" element={<WarpScores />} />
      <Route path="/statistics" element={<StatisticsPage />} />
      <Route path="/about" element={<AboutPage />} />
      <Route path="/terms.md" element={<MarkdownPage markdownDocument="/terms.md" title="Terms" />} />
      <Route
        path="/privacy.md"
        element={<MarkdownPage markdownDocument="/privacy.md" title="Privacy Policy" />}
      />
      <Route
        path="/discord-bot.md"
        element={<MarkdownPage markdownDocument="/discord-bot.md" title="Discord Bot" />}
      />
      <Route path="/README.md" element={<MarkdownPage markdownDocument="/README.md" title="Readme" />} />
      <Route
        path="/CHANGELOG.md"
        element={<MarkdownPage markdownDocument="/CHANGELOG.md" title="Changelog" />}
      />
      <Route path="/league/:leagueId" element={<LeaguePage />} />
      <Route path="/latestMatches/:leagueId" element={<LatestMatchesPage />} />
      <Route path="/latestMatches/:leagueId/:limit" element={<LatestMatchesPage />} />
      <Route path="/liveMatches/:leagueId" element={<LiveMatchesPage />} />
      <Route path="/team/:teamId" element={<TeamPage />} />
      <Route path="/competition/:competitionId" element={<CompetitionPage />} />
      <Route path="/competition/:competitionId/stats" element={<CompetitionStatsPage />} />
      <Route path="/competition/:competitionId/arena/:race" element={<ArenaPage />} />
      <Route path="/competition/:competitionId/arena/coach/:coachId" element={<ArenaCoachPage />} />
      <Route path="/competition/:competitionId/team/:teamId" element={<TeamPage />} />
      <Route path="/circuit/:circuitId/leg/:legId" element={<CircuitLegPage />} />
      <Route path="/circuit/:circuitId/leg/:legId/:entityId" element={<CircuitLegEntityPage />} />
      <Route path="/circuit/:circuitId" element={<CircuitPage />} />
      {/* Protected Routes/Needing authentication */}
      <Route path="/coachPage" element={<ProtectedRoute component={CoachPage} />} />
      <Route path="/account" element={<ProtectedRoute component={AccountPage} />} />
      <Route path="/admin" element={<ProtectedRoute component={AdminPage} />} />
      <Route path="/admin/circuit/:circuitId" element={<ProtectedRoute component={AdminCircuitPage} />} />
      <Route
        path="/admin/circuit/:circuitId/leg/:legId"
        element={<ProtectedRoute component={AdminCircuitLegPage} />}
      />
      </Routes>
    </MyTeamsProvider>
  );
}

function App() {
  return (
    <DarkMode>
      <ChakraProvider theme={warpScoresTheme}>
        <CSSReset />
        <Fonts />
        <Box>
          <Router>

            {isProduction ? (
              <Auth0ProviderWithRedirectCallback
                domain={config.auth0Domain}
                clientId={config.auth0ClientId}
                authorizationParams={{
                  redirect_uri: window.location.origin,
                  audience: config.auth0Audience,
                }}
              >
                <AppRoutes />
              </Auth0ProviderWithRedirectCallback>
            ) : (
              <MockAuth0Provider>
                <AppRoutes />
              </MockAuth0Provider>
            )}
          </Router>
        </Box>
      </ChakraProvider>
    </DarkMode>
  );
}

export default App;
