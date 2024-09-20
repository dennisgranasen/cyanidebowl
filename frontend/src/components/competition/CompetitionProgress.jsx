import React from 'react';
import {Box, GridItem, Progress, SimpleGrid, VStack} from '@chakra-ui/react';
import {CalendarIcon, QuestionIcon} from '@chakra-ui/icons';
import {FaFlagCheckered} from 'react-icons/fa6';
import DelayedIconTooltip from '../common/DelayedIconTooltip';
import prettyPrint from '../../util/PrettyPrint';

function RoundRobinProgresses({
                                  currentRound,
                                  totalRounds,
                                  liveMatches,
                                  playedMatches,
                                  totalMatches,
                                  notValidatedMatches,
                                  status,
                                  withPadding,
                              }) {
    const roundLength = totalMatches ? totalMatches / totalRounds : 1;
    const finishedMatches = playedMatches - notValidatedMatches;
    const needsValidation = notValidatedMatches - liveMatches > 0;
    const live = liveMatches > 0;
    const roundProgresses = [];
    for (let round = 0; round < totalRounds; round += 1) {
        let progress = 0;
        let active = false;
        if (currentRound > round + 1) {
            progress = 100;
        } else if (currentRound === round + 1) {
            progress =
                finishedMatches >= roundLength * currentRound ? 100 : (100 * (finishedMatches % roundLength)) / roundLength;
            active = status === 'InProgress';
        }
        roundProgresses.push({name: `round${round + 1}`, progress, active});
    }
    return (
        <SimpleGrid columns={totalRounds} spacing="0.25rem" paddingTop={withPadding ? '0.25rem' : null}>
            {roundProgresses.map(({name, progress, active}) => (
                <GridItem key={name}>
                    <Progress
                        value={progress}
                        isIndeterminate={active && live}
                        hasStripe={active}
                        variant={needsValidation ? 'validationNeeded' : null}
                    />
                </GridItem>
            ))}
        </SimpleGrid>
    );
}

function WissenProgresses({playedMatches, liveMatches, notValidatedMatches, totalMatches, status, withPadding}) {
    const finishedMatches = playedMatches - notValidatedMatches;
    const needsValidation = notValidatedMatches - liveMatches > 0;
    const live = liveMatches > 0;
    const color = needsValidation ? 'orange' : null;
    const progress =
        finishedMatches > 0 && finishedMatches === totalMatches
            ? 100
            : (100 * (finishedMatches % totalMatches)) / totalMatches;
    return <Progress paddingTop={withPadding ? '0.25rem' : null} value={progress} isIndeterminate={live}
                     hasStripe={status === 'InProgress'} colorScheme={color}/>;
}

function Progresses({
                        currentRound,
                        totalRounds,
                        playedMatches,
                        totalMatches,
                        notValidatedMatches,
                        liveMatches,
                        status,
                        format,
                        withPadding,
                    }) {
    if (status === 'Finished') return <FaFlagCheckered/>;
    if (status === 'Registration') return <CalendarIcon/>;
    switch (format) {
        case 'RoundRobin':
            return (
                <RoundRobinProgresses
                    currentRound={currentRound}
                    totalRounds={totalRounds}
                    totalMatches={totalMatches}
                    playedMatches={playedMatches}
                    notValidatedMatches={notValidatedMatches}
                    liveMatches={liveMatches}
                    status={status}
                    withPadding={withPadding ? 'withPadding' : null}
                />
            );
        case 'Wissen':
            return (
                <WissenProgresses
                    playedMatches={playedMatches}
                    liveMatches={liveMatches}
                    notValidatedMatches={notValidatedMatches}
                    totalMatches={totalMatches}
                    status={status}
                    withPadding={withPadding ? 'withPadding' : null}
                />
            );
        case 'Knockout':
            return `Round ${currentRound} of ${totalRounds}`;
        case 'Ladder':
            return `${playedMatches || 0} played match${playedMatches !== 1 ? 'es' : ''}`;
        default:
            return <QuestionIcon/>;
    }
}

function ProgressLabel({text, additionalText}) {
    return (
        <VStack align="left">
            <Box>{text}</Box>
            {additionalText ? <Box> {additionalText} </Box> : null}
        </VStack>
    );
}

function CompetitionProgress({
                                 status,
                                 format,
                                 currentRound,
                                 totalRounds,
                                 playedMatches,
                                 totalMatches,
                                 notValidatedMatches,
                                 liveMatches,
                                 withPadding,
                             }) {
    const currentRoundText = currentRound ? `, Round ${currentRound}` : '';
    const totalRoundsText = currentRound && totalRounds ? `of ${totalRounds}` : '';
    const progressText = `${prettyPrint(status)}${currentRoundText} ${totalRoundsText}`;
    const finishedMatches = playedMatches - notValidatedMatches;
    const notYetValidatedMatches = notValidatedMatches > 0 ? ` (${notValidatedMatches} not yet validated)` : '';
    const outOfTotalMatchesText = totalMatches ? ` out of ${totalMatches}` : '';
    const progressAdditionalText = playedMatches
        ? `Finished ${finishedMatches}${outOfTotalMatchesText} matches${notYetValidatedMatches}`
        : undefined;
    return (
        <Box>
            <DelayedIconTooltip
                p="0.4rem"
                label={<ProgressLabel text={progressText} additionalText={progressAdditionalText}/>}
            >
                <Box>
                    <Progresses
                        currentRound={currentRound}
                        totalRounds={totalRounds}
                        totalMatches={totalMatches}
                        playedMatches={playedMatches}
                        notValidatedMatches={notValidatedMatches}
                        liveMatches={liveMatches}
                        status={status}
                        format={format}
                        withPadding={withPadding ? 'withPadding' : null}
                    />
                </Box>
            </DelayedIconTooltip>
        </Box>
    );
}

export default CompetitionProgress;
