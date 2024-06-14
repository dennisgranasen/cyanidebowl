import React from 'react';
import { Box, ChakraProvider, CSSReset, extendTheme } from '@chakra-ui/react';
import { HashRouter as Router, Route, Routes } from 'react-router-dom';
import WarpScores from './pages/WarpScores';
import TeamPage from './pages/TeamPage';
import CompetitionPage from './pages/CompetitionPage';
import AboutPage from './pages/AboutPage';
import CoachPage from './pages/CoachPage';
import AdminPage from './pages/AdminPage';
import StatisticsPage from './pages/StatisticsPage';
import LoginPage from './pages/LoginPage';

const config = {
  initialColorMode: 'dark',
  useSystemColorMode: false,
};

const theme = extendTheme({
  config,
  components: {
    Table: {
      variants: {
        simpleClickable: {
          tbody: {
            tr: {
              borderBlock: 'thin solid',
              borderColor: 'gray.700',
              cursor: 'pointer',
              _hover: {
                background: 'gray.600',
              },
            },
          },
        },
        stripedClickable: {
          tbody: {
            tr: {
              cursor: 'pointer',
              _odd: {
                background: 'gray.700',
              },
              _hover: {
                background: 'gray.600',
              },
            },
          },
        },
      },
    },
  },
});

function App() {
  return (
    <ChakraProvider theme={theme}>
      <CSSReset />
      <Box padding="4">
        <Router>
          <Routes>
            <Route path="/" element={<WarpScores />} />
            <Route path="/admin" element={<AdminPage />} />
            <Route path="/statistics" element={<StatisticsPage />} />
            <Route path="/coachPage" element={<CoachPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/about" element={<AboutPage />} />
            <Route path="/:leagueUuid" element={<WarpScores />} />
            <Route path="/team/:teamUuid" element={<TeamPage />} />
            <Route path="/competition/:competitionUuid" element={<CompetitionPage />} />
            <Route path="/competition/:competitionUuid/team/:teamUuid" element={<TeamPage />} />
          </Routes>
        </Router>
      </Box>
    </ChakraProvider>
  );
}

export default App;
