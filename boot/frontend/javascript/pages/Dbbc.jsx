import React, {useEffect, useState} from 'react';
import CyanideApiService from "../CyanideApiService";
import {
    Alert,
    AlertDescription,
    AlertIcon,
    Box,
    Card,
    CardBody,
    Flex,
    FormControl,
    FormLabel,
    Heading,
    Image,
    Select,
    Spacer,
    Spinner,
    Stack
} from "@chakra-ui/react";
import config from "../config";
import Navigation from "../components/Navigation";
import {useParams} from "react-router-dom";
import Competitions from "../components/Competitions";
import ImageUrls from "../ImageUrls";
import logger from "../util/Logger";
import formatter from "../util/Formatter";

function Dbbc() {
    const {leagueUuid} = useParams();
    const [competitions, setCompetitions] = useState([]);
    const [leagues, setLeagues] = useState([]);
    const [league, setLeague] = useState();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState({});

    const getLeagueByUuid = (uuid) => {
        if (uuid && uuid !== null) {
            const league = leagues.filter((league) => league.uuid === uuid)[0];
            logger.debug("League by uuid (%s): %o.", uuid, league);
            return league;
        }
        return null;
    }

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
        logger.debug("Setting league (uuid: %s). Leagues: %o, count: %s", leagueUuid, leagues, leagues.length);
        if ((!leagueUuid || leagueUuid === null) && leagues.length === 1) {
            setLeague(leagues[0]);
        } else {
            const league = getLeagueByUuid(leagueUuid);
            if (league !== null) {
                setLeague(league);
            }
        }
    }, [leagues]);

    useEffect(() => {
        const fetchCompetitions = (leagueId) => {
            setError({});
            setCompetitions([]);
            setLoading(true);
            if (leagueId === null || leagueId.length === 0) {
                setLoading(false);
                setError({type: 'info', message: 'No League selected.'})
                return;
            }
            CyanideApiService.leagueCompetitions(leagueId)
                .then((data) => {
                    data.sort((compA, compB) => {
                        return compA.name.localeCompare(compB.name);
                    });
                    setCompetitions(data)
                })
                .then(() => setLoading(false))
                .catch((reason) => {
                    setError({type: 'error', message: reason.toLocaleString(config.locale)});
                })
        };
        const leagueId = league && league !== null ? league.uuid : null;
        fetchCompetitions(leagueId);
    }, [league]);

    const changeLeague = (e) => {
        const league = getLeagueByUuid(e.target.value)
        setLeague(league);
    };

    return <Stack>
        <Box>
            <Navigation currentPage="home"/>
        </Box>
        {leagues.length > 1 ?
            <Box>
                <Heading>DBBL</Heading>
                <FormControl>
                    <FormLabel>League</FormLabel>
                    <Select variant="filled" placeholder="Select league" onChange={changeLeague}
                            value={league ? league.uuid : null}>
                        {leagues.map((league) => <option value={league.uuid} key={league.uuid}>{league.name}</option>)}
                    </Select>
                </FormControl>
            </Box> : <></>}
        {league ? <Card direction={{base: 'column', sm: 'row'}}>
            <Image
                objectFit='cover'
                maxW={{base: '100%', sm: '120px'}}
                src={ImageUrls.logo(league.logo)}/>
            <CardBody>
                <Heading>{league.name}</Heading>
                <Flex>
                    <Box>Teams: {league.teamCount}</Box>
                    <Spacer/>
                    <Box>Active Competitions: {competitions.length}</Box>
                    <Spacer/>
                    <Box>Last match: {formatter.formatAsDate(league.dateLastMatch)}</Box>
                </Flex>
            </CardBody>
        </Card> : <></>
        }
        <Box>
            <Heading>Competitions</Heading>
            {error.type ?
                <Alert status={error.type}>
                    <AlertIcon/>
                    <AlertDescription>{error.message}</AlertDescription>
                </Alert>
                : (loading ? <Spinner/> : <Competitions competitions={competitions}/>)}
        </Box>
    </Stack>

}

export default Dbbc;
