import React from 'react';
import {Box, ChakraProvider, CSSReset, extendTheme} from '@chakra-ui/react';
import Dbbc from "./pages/Dbbc";
import {HashRouter as Router, Route, Routes} from "react-router-dom";
import TeamPage from "./pages/TeamPage";
import CompetitionPage from "./pages/CompetitionPage";

const colors = {
    brand: {
        50: "#ecefff",
        100: "#cbceeb",
        200: "#a9aed6",
        300: "#888ec5",
        400: "#666db3",
        500: "#4d5499",
        600: "#3c4178",
        700: "#2a2f57",
        800: "#181c37",
        900: "#080819"
    }
};

const config = {
    initialColorMode: 'dark',
    useSystemColorMode: false,
};

const theme = extendTheme({colors, config});

function App() {
    return (<ChakraProvider theme={theme}>
            <CSSReset/>
            <Box padding="4">
                <Router>
                    <Routes>
                        <Route path="/" element={<Dbbc/>}/>
                        <Route path="/:leagueUuid" element={<Dbbc/>}/>
                        <Route path="/team/:teamUuid" element={<TeamPage/>}/>
                        <Route path="/competition/:competitionUuid" element={<CompetitionPage/>}/>
                    </Routes>
                </Router>
            </Box>
        </ChakraProvider>
    );
}

export default App;
