import React from 'react';
import {Box, ChakraProvider, CSSReset, DarkMode} from '@chakra-ui/react';
import {HashRouter as Router, Route, Routes, useNavigate} from 'react-router-dom';
import {Auth0Provider, withAuthenticationRequired} from '@auth0/auth0-react';
import WarpScores from './pages/WarpScores';
import TeamPage from './pages/TeamPage';
import AdminCircuitPage from './pages/AdminCircuitPage';
import CompetitionPage from './pages/CompetitionPage';
import AboutPage from './pages/AboutPage';
import CoachPage from './pages/CoachPage';
import AdminPage from './pages/AdminPage';
import StatisticsPage from './pages/StatisticsPage';
import LatestMatchesPage from './pages/LatestMatchesPage';
import LiveMatchesPage from './pages/LiveMatchesPage';
import LeaguePage from './pages/LeaguePage';
import config from './config';
import MarkdownPage from "./pages/MarkdownPage";
import {warpScoresTheme} from "./theme/WarpScoresTheme";

const {isProduction} = config;

const withoutAuthentication = (Component) => {
    return function WithoutAuthentication() {
        return <Component/>;
    };
};

function ProtectedRoute({component, ...args}) {
    const Component = isProduction ? withAuthenticationRequired(component, args) : withoutAuthentication(component);
    return <Component/>;
}

function Auth0ProviderWithRedirectCallback({children, ...props}) {
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
        <DarkMode>
            <ChakraProvider theme={warpScoresTheme}>
                <CSSReset/>
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
                                <Route path="/" element={<WarpScores/>}/>
                                <Route path="/statistics" element={<StatisticsPage/>}/>
                                <Route path="/about" element={<AboutPage/>}/>
                                <Route path="/terms.md"
                                       element={<MarkdownPage markdownDocument='/terms.md' title='Terms'/>}/>
                                <Route path="/privacy.md"
                                       element={<MarkdownPage markdownDocument='/privacy.md' title='Privacy Policy'/>}/>
                                <Route path="/discord-bot.md" element={<MarkdownPage markdownDocument='/discord-bot.md'
                                                                                     title='Discord Bot'/>}/>
                                <Route path="/README.md"
                                       element={<MarkdownPage markdownDocument='/README.md' title='Readme'/>}/>
                                <Route path="/CHANGELOG.md"
                                       element={<MarkdownPage markdownDocument='/CHANGELOG.md' title='Changelog'/>}/>
                                <Route path="/:leagueUuid" element={<LeaguePage/>}/>
                                <Route path="/latestMatches/:leagueUuid" element={<LatestMatchesPage/>}/>
                                <Route path="/latestMatches/:leagueUuid/:limit" element={<LatestMatchesPage/>}/>
                                <Route path="/liveMatches/:leagueUuid" element={<LiveMatchesPage/>}/>
                                <Route path="/team/:teamUuid" element={<TeamPage/>}/>
                                <Route path="/competition/:competitionUuid" element={<CompetitionPage/>}/>
                                <Route path="/competition/:competitionUuid/team/:teamUuid" element={<TeamPage/>}/>
                                <Route path="/circuit/:circuitId" element={<CircuitPage/>}/>
                                {/* Protected Routes/Needing authentication */}
                                <Route path="/coachPage" element={<ProtectedRoute component={CoachPage}/>}/>
                                <Route path="/admin" element={<ProtectedRoute component={AdminPage}/>}/>
                                <Route path="/admin/circuit/:circuitId"
                                       element={<ProtectedRoute component={AdminCircuitPage}/>}/>
                                <Route
                                    path="/admin/circuit/:circuitId/leg/:legId"
                                    element={<ProtectedRoute component={AdminCircuitLegPage}/>}
                                />
                            </Routes>
                        </Auth0ProviderWithRedirectCallback>
                    </Router>
                </Box>
            </ChakraProvider>
        </DarkMode>
    );
}

export default App;
