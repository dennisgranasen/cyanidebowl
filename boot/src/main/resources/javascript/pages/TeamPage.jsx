import React, {useEffect, useState} from 'react';
import {
    Box,
    Card,
    CardBody,
    Center,
    Grid,
    GridItem,
    Heading,
    Spinner,
    Stat,
    StatGroup,
    StatLabel,
    StatNumber,
    VStack,
} from '@chakra-ui/react'
import CyanideApiService from "../CyanideApiService";
import {useParams} from "react-router-dom";
import Roster from "../components/Roster";
import prettyPrint from "../util/PrettyPrint";
import Navigation from "../components/Navigation";
import logger from "../util/Logger";
import Formatter from "../util/Formatter";
import ImageUrls from "../ImageUrls";

function TeamPage() {
    const {teamUuid} = useParams();
    const [team, setTeam] = useState();
    const [players, setPlayers] = useState();

    useEffect(() => {
        const fetchTeam = async () => {
            await CyanideApiService.team(teamUuid).then((data) => {
                setTeam(data);
                const players = data.players;
                if (players !== null) {
                    players.sort((playerA, playerB) => playerA.number - playerB.number);
                }
                setPlayers(players);
            });
        };
        const fetchMatches = async () => {
            await CyanideApiService.matches(teamUuid).then((data) => {
                logger.info("Matches: %o", data);
            });
        };
        fetchTeam();
        // fetchMatches();
    }, []);

    return <VStack align="left">
        <Box>
            <Navigation currentPage="team" league={ team ? [team.leagueId, team.leagueName] : [] } competition={team ? [ team.competitionIds[0], team.competitionName] : []} team={ team ? [ teamUuid, team.name ] : [] }/>
        </Box>
        {
            team ?
                <>
                    <Card>
                        <CardBody>
                            <Grid
                                h='200px'
                                templateRows='repeat(2, 1fr)'
                                templateColumns='repeat(5, 1fr)'
                                gap={4}
                            >
                                <GridItem rowSpan={2} colSpan={1}
                                          backgroundImage={`url('${ImageUrls.logo(team.logo)}')`}
                                          backgroundRepeat="no-repeat" backgroundSize="contain"/>
                                <GridItem colSpan={3}>
                                <Center><Heading>{team.name}</Heading></Center>
                                    <Center>Coach: {team.coachName}</Center>
                                </GridItem>
                                <GridItem rowSpan={2} colSpan={1}
                                          backgroundImage={`url('${ImageUrls.race(team.fraction)}')`}
                                          backgroundRepeat="no-repeat" backgroundSize="contain"/>
                                <GridItem colSpan={3}>
                                   <StatGroup>
                                        <Stat size="sm">
                                            <StatLabel>Race</StatLabel>
                                            <StatNumber>{prettyPrint(team.fraction)}</StatNumber>
                                        </Stat>
                                        <Stat size="sm">
                                            <StatLabel>Players</StatLabel>
                                            <StatNumber>{players !== null ? players.length : "-"}</StatNumber>
                                        </Stat>
                                        <Stat size="sm">
                                            <StatLabel>Score</StatLabel>
                                            <StatNumber>{team.score}</StatNumber>
                                        </Stat>
                                        <Stat size="sm">
                                            <StatLabel>Cash</StatLabel>
                                            <StatNumber>{Formatter.formatAsNumber(team.cash)}</StatNumber>
                                        </Stat>
                                        <Stat size="sm">
                                            <StatLabel>Value</StatLabel>
                                            <StatNumber>{Formatter.formatAsNumber(team.value)}</StatNumber>
                                        </Stat>
                                    </StatGroup>
                                </GridItem>
                            </Grid>
                        </CardBody>
                    </Card>
                    <Roster players={players}/>
                </>
                :
                <Spinner/>
        }
    </VStack>;
}

export default TeamPage;
