import React, {useEffect, useState} from 'react';
import CyanideApiService from "../CyanideApiService";
import Teams from "../components/Teams";
import {
    Alert,
    AlertDescription,
    AlertIcon,
    AlertTitle,
    Box,
    FormControl,
    FormLabel,
    Select,
    Spinner,
    Stack,
    Text
} from "@chakra-ui/react";
import config from "../config";
import Navigation from "../components/Navigation";

function Dbbc() {
    const [teams, setTeams] = useState([]);
    const [leagues, setLeagues] = useState([]);
    const [leagueId, setLeagueId] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        const fetchLeagues = () => {
            CyanideApiService.league().then((data) => {
                setLeagues(data)
            })
                .then(() => setLoading(false))
                .catch((reason) => {
                    setError(reason.toLocaleString(config.locale));
                })
        }
        fetchLeagues();
    }, []);

    useEffect(() => {
        const fetchTeams = (leagueId) => {
            if (leagueId === null || leagueId.length === 0)
                return;
            setError("");
            setLoading(true);
            CyanideApiService.teams(leagueId)
                .then((data) => {
                    data.sort((teamA, teamB) => {
                        let result = teamA.team.competitionName.localeCompare(teamB.team.competitionName);
                        if (result !== 0)
                            return result;
                        return teamA.team.coachName.localeCompare(teamB.team.coachName)
                    });
                    setTeams(data)
                })
                .then(() => setLoading(false))
                .catch((reason) => {
                    setError(reason.toLocaleString(config.locale));
                })
        };
        fetchTeams(leagueId);
    }, [leagueId]);

    const changeLeague = (e) => setLeagueId(e.target.value);

    return <Stack>
        <Box>
            <Navigation currentPage="home"/>
        </Box>
        <Box>
            <FormControl>
                <FormLabel>League</FormLabel>
                <Select variant="filled" placeholder="Select league" onChange={changeLeague} value={leagueId}>
                    {leagues.map((league) => <option value={league.uuid} key={league.uuid}>{league.name}</option>)}
                </Select>
            </FormControl>
        </Box>
        <Box>
            <Text>Teams</Text>
            {error.length > 0 ?
                <Alert status='error'>
                    <AlertIcon/>
                    <AlertTitle>There was an error!</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
                : (loading ? <Spinner/> : <Teams teams={teams}/>)}
        </Box>
    </Stack>

}

export default Dbbc;
