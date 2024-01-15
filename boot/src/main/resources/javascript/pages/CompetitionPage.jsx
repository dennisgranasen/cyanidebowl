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
import Navigation from "../components/Navigation";
import Formatter from "../util/Formatter";
import Contests from "../components/Contests";
import comparators from "../util/Comparators";
import ImageUrls from "../ImageUrls";
import Ranks from "../components/Ranks";

function TeamPage() {
    const {competitionUuid} = useParams();
    const [competition, setCompetition] = useState();
    const [ranks, setRanks] = useState();
    const [contests, setContests] = useState();
    const [matches, setMatches] = useState();

    useEffect(() => {
        const fetchCompetition = async () => {
            await CyanideApiService.competition(competitionUuid).then((data) => {
                setCompetition(data);
            });
        };
        const fetchTeams = async () => {
            await CyanideApiService.competitionRanks(competitionUuid).then((data) => {
                data.sort((rankA, rankB) => {
                    let result = (rankB.score || 0) - (rankA.score || 0);
                    if (result !== 0) return result;
                    result = (rankB.inflictedTouchdowns || 0) - (rankA.inflictedTouchdowns || 0);
                    if (result !== 0) return result;
                    result = (rankB.inflictedCasualties || 0) - (rankA.inflictedCasualties || 0);
                    if (result !== 0) return result;
                    result = (rankB.gamesPlayed || 0) - (rankA.gamesPlayed || 0);
                    if (result !== 0) return result;
                    result = (rankA.sustainedTouchdowns || 0) - (rankB.sustainedTouchdowns || 0);
                    if (result !== 0) return result;
                    result = (rankA.sustainedCasualties || 0) - (rankB.sustainedCasualties || 0);
                    if (result !== 0) return result;
                    return rankA.team.name.localeCompare(rankB.team.name);
                })
                setRanks(data);
            });
        };

        const fetchContests = async () => {
            await CyanideApiService.competitionContests(competitionUuid).then((data) => {
                data.sort((compA, compB) => comparators.compareAsDates(compA.matchDate, compB.matchDate));
                setContests(data);
            });
        };

        fetchCompetition();
        fetchTeams();
        fetchContests();
    }, []);

    return <VStack align="left">
        <Box>
            <Navigation currentPage="competition"
                        league={competition ? [competition.leagueId, competition.leagueName] : []}
                        competition={[competitionUuid, competition ? competition.name : ""]}/>
        </Box>
        {
            competition ?
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
                                          backgroundImage={`url('${ImageUrls.logo(competition.logo)}')`}
                                          backgroundRepeat="no-repeat" backgroundSize="contain"/>
                                <GridItem colSpan={3}>
                                    <Center><Heading>{competition.name}</Heading></Center>
                                    <Center>League: {competition.leagueName}</Center>
                                </GridItem>
                                <GridItem rowSpan={2} colSpan={1}
                                          backgroundImage={`url('${ImageUrls.logo(competition.leagueLogo)}')`}
                                          backgroundRepeat="no-repeat" backgroundSize="contain"/>
                                <GridItem colSpan={3}>
                                    <StatGroup>
                                        <Stat size="sm">
                                            <StatLabel>Created</StatLabel>
                                            <StatNumber>{Formatter.formatAsDate(competition.dateCreated)}</StatNumber>
                                        </Stat>
                                        <Stat size="sm">
                                            <StatLabel>Format</StatLabel>
                                            <StatNumber>{competition.format}</StatNumber>
                                        </Stat>
                                        <Stat size="sm">
                                            <StatLabel>Status</StatLabel>
                                            <StatNumber>{competition.status}</StatNumber>
                                        </Stat>
                                        <Stat size="sm">
                                            <StatLabel>Teams</StatLabel>
                                            <StatNumber>{Formatter.formatAsNumber(competition.teamsMax)}</StatNumber>
                                        </Stat>
                                        <Stat size="sm">
                                            <StatLabel>Time settings</StatLabel>
                                            <StatNumber>Turn: {Formatter.formatAsNumber(competition.turnDuration / 60)}m
                                                -
                                                Bonus: {Formatter.formatAsNumber(competition.timeBonusDuration / 60)}m</StatNumber>
                                        </Stat>
                                    </StatGroup>
                                </GridItem>
                            </Grid>
                        </CardBody>
                    </Card>
                    <Heading>Ranking</Heading>
                    {ranks ? <Ranks ranks={ranks}/> : <Spinner/>}
                    <Heading>Contests</Heading>
                    {contests ? <Contests contests={contests}/> : <Spinner/>}
                </>
                :
                <Spinner/>
        }
    </VStack>;
}

export default TeamPage;
