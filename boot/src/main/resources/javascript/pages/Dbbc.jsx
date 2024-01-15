import React, {useEffect, useState} from 'react';
import CyanideApiService from "../CyanideApiService";
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
import {useParams} from "react-router-dom";
import Competitions from "../components/Competitions";

function Dbbc() {
    const {leagueUuid} = useParams();
    const [competitions, setCompetitions] = useState([]);
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
        if (leagueUuid && leagueUuid !== null)
            setLeagueId(leagueUuid);
    }, []);

    useEffect(() => {
        const fetchCompetitions = (leagueId) => {
            if (leagueId === null || leagueId.length === 0)
                return;
            setError("");
            setLoading(true);
            CyanideApiService.leagueCompetitions(leagueId)
                .then((data) => {
                    data.sort((compA, compB) => {
                        return compA.name.localeCompare(compB.name);
                    });
                    setCompetitions(data)
                })
                .then(() => setLoading(false))
                .catch((reason) => {
                    setError(reason.toLocaleString(config.locale));
                })
        };
        fetchCompetitions(leagueId);
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
            <Text>Competitions</Text>
            {error.length > 0 ?
                <Alert status='error'>
                    <AlertIcon/>
                    <AlertTitle>There was an error!</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
                : (loading ? <Spinner/> : <Competitions competitions={competitions}/>)}
        </Box>
    </Stack>

}

export default Dbbc;
