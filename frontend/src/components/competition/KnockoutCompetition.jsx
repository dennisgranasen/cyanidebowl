import {
    Accordion,
    AccordionButton,
    AccordionIcon,
    AccordionItem,
    AccordionPanel,
    Box,
    Center,
    Grid,
    GridItem,
    Heading,
    Image,
} from '@chakra-ui/react';
import {SingleEliminationBracket} from 'react-tournament-brackets/dist/esm';
import React, {useEffect, useState} from 'react';
import logger from '../../util/Logger';
import ImageUrls from '../../ImageUrls';
import prettyPrint from '../../util/PrettyPrint';
import DelayedIconTooltip from '../common/DelayedIconTooltip';
import Formatter from '../../util/Formatter';
import Ranks from './Ranks';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';
import {QuestionOutlineIcon} from "@chakra-ui/icons";
import config from "../../config";

const {boxSize} = config;

function toParticipant(opponent, winner) {
    return {
        id: opponent?.id,
        resultText: opponent ? `${opponent.score}` : null,
        isWinner: winner?.team?.id === opponent?.id,
        status: opponent ? 'PLAYED' : null,
        teamName: opponent?.name,
        coachName: opponent?.coachName,
        race: opponent?.race,
        picture: opponent?.logo,
    };
}

function toParticipants(opponents, index, winner) {
    logger.debug('Opponents: %o', opponents);
    const participants = [];
    if (opponents) {
        opponents.forEach((opponent) => participants.push(toParticipant(opponent, winner)));
    }
    logger.debug('Participants: %o', participants);
    return participants;
}

function toBracketMatch(contest) {
    return {
        id: contest?.contestUuid,
        nextMatchId: contest?.nextContestUuid,
        participants: toParticipants(contest?.opponents, contest?.winner),
        startTime: Formatter.formatAsDate(contest?.matchDate, '-'),
        state: contest?.matchDate ? 'DONE' : null,
        tournamentRoundText: `${contest?.round}`,
    };
}

function toBracketMatches(contests) {
    const matches = [];
    if (contests) {
        contests.forEach((contest, index) => matches.push(toBracketMatch(contest)));
    }
    return matches;
}

function Participant({
                         match,
                         party,
                         won,
                         hovered,
                         borderTopRadius,
                         borderBottomRadius,
                         connectorColor,
                         teamNameFallback,
                         resultFallback,
                         onMouseEnter,
                         onMouseLeave,
                         onMatchClick,
                         onPartyClick,
                     }) {
    const borderColor = hovered ? 'warpScoresHoverColor' : connectorColor;
    const backgroundColor = hovered ? 'warpScoresHoverColor' : null;
    return (
        <Box
            m="0"
            p="2px"
            borderColor={borderColor}
            borderWidth="1px"
            borderTopRadius={borderTopRadius}
            borderBottomRadius={borderBottomRadius}
            overflow="hidden"
            backgroundColor={backgroundColor}
            onMouseEnter={() => onMouseEnter(party.id)}
            onMouseLeave={() => onMouseLeave(party.id)}
            onPartyClick={onPartyClick}
            onMatchClick={onMatchClick}
        >
            <Grid
                w="100%"
                templateAreas={`"image team score"
                  "image coach score"`}
                gridTemplateColumns="40px 1fr 32px"
            >
                <GridItem pl="4px" pr="4px" area="image" textAlign="center">
                    <Center w="100%" h="100%">
                        <Image src={ImageUrls.logo(party.picture)} fallback={<QuestionOutlineIcon boxSize={boxSize}/>}
                               objectFit="contain"/>
                    </Center>
                </GridItem>
                <GridItem pl="4px" area="team" w="100%" textAlign="left" fontWeight={won ? 'bold' : null}
                          style={{whiteSpace: 'nowrap'}}>
                    <Box>{party.teamName || "To be defined"}</Box>
                </GridItem>
                <GridItem pl="4px" area="coach" textAlign="left" fontSize="sm" color="grey"
                          style={{whiteSpace: 'nowrap'}}>
                    {`${party.coachName || 'TBD'}, ${party.race ? prettyPrint(party.race) : 'unknown'}`}
                </GridItem>
                <GridItem area="score" textAlign="center" fontWeight={won ? 'bold' : null}>
                    <Center w="100%" h="100%">
                        {match.state === 'DONE' ? party.resultText : ''}
                    </Center>
                </GridItem>
            </Grid>
        </Box>
    );
}

function MatchComponent({
                            match,
                            topParty,
                            bottomParty,
                            topWon,
                            bottomWon,
                            topHovered,
                            bottomHovered,
                            topText,
                            connectorColor,
                            teamNameFallback,
                            resultFallback,
                            onPartyClick,
                            onMatchClick,
                            onMouseEnter,
                            onMouseLeave,
                        }) {
    logger.debug("Match %o", match);
    return (
        <DelayedIconTooltip label={match.state === 'DONE' ? `Played ${topText}` : 'Scheduled'}>
            <div
                style={{
                    cursor: 'pointer',
                    marginTop: '4px',
                }}
            >
                <Participant
                    match={match}
                    party={topParty}
                    won={topWon}
                    hovered={topHovered}
                    borderTopRadius="sm"
                    teamNameFallback={teamNameFallback}
                    resultFallback={resultFallback}
                    connectorColor={connectorColor}
                    onMouseEnter={onMouseEnter}
                    onMouseLeave={onMouseLeave}
                    onPartyClick={onPartyClick}
                    onMatchClick={onMatchClick}
                />
                <Participant
                    match={match}
                    party={bottomParty}
                    won={bottomWon}
                    hovered={bottomHovered}
                    borderBottomRadius="sm"
                    teamNameFallback={teamNameFallback}
                    resultFallback={resultFallback}
                    connectorColor={connectorColor}
                    onMouseEnter={onMouseEnter}
                    onMouseLeave={onMouseLeave}
                    onPartyClick={onPartyClick}
                    onMatchClick={onMatchClick}
                />
            </div>
        </DelayedIconTooltip>
    );
}

function KnockoutCompetition({ranks, contests, competition, ranksLoading, contestsLoading, competitionLoading}) {
    const [matches, setMatches] = useState([]);

    useEffect(() => {
        const bracketMatches =
            !competitionLoading && contests && competition ? toBracketMatches(contests) : [];
        setMatches(bracketMatches);
    }, [competition, competitionLoading, contests]);
    return (
        <>
            <Heading size="md">Knockout-Bracket</Heading>
            <Box align="center" height="100%" width="100%" overflowX="scroll">
                <LoadingOrErrorWrapper loading={contestsLoading}>
                    {matches && matches.length > 0 && (
                        <SingleEliminationBracket matches={matches} matchComponent={MatchComponent}/>
                    )}
                </LoadingOrErrorWrapper>
            </Box>
            <Accordion allowMultiple>
                <AccordionItem>
                    <AccordionButton>
                        <Box as="span" flex="1" textAlign="left">
                            <Heading size="md">Ranks</Heading>
                        </Box>
                        <AccordionIcon/>
                    </AccordionButton>
                    <AccordionPanel>
                        <Ranks loading={ranksLoading} ranks={ranks}/>
                    </AccordionPanel>
                </AccordionItem>
            </Accordion>
        </>
    );
}

export default KnockoutCompetition;
