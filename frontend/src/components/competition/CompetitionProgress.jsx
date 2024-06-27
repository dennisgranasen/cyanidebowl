import React from 'react';
import { Box, GridItem, Progress, SimpleGrid, VStack } from '@chakra-ui/react';
import { CalendarIcon, QuestionIcon } from '@chakra-ui/icons';
import { FaFlagCheckered } from 'react-icons/fa6';
import DelayedIconTooltip from '../common/DelayedIconTooltip';
import prettyPrint from '../../util/PrettyPrint';

function RoundRobinProgresses({
  currentRound,
  totalRounds,
  liveMatches,
  playedMatches,
  totalMatches,
  validatedMatches,
  status,
}) {
  const roundLength = totalMatches ? totalMatches / totalRounds : 1;
  const finishedMatches = Math.max(playedMatches, validatedMatches);
  const needsValidation = validatedMatches + liveMatches < playedMatches;
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
    roundProgresses.push({ name: `round${round + 1}`, progress, active });
  }
  const color = needsValidation ? 'orange' : null;
  return (
    <SimpleGrid columns={totalRounds} spacing="0.25rem">
      {roundProgresses.map(({ name, progress, active }) => (
        <GridItem key={name}>
          <Progress value={progress} isIndeterminate={active && live} hasStripe={active} colorScheme={color} />
        </GridItem>
      ))}
    </SimpleGrid>
  );
}

function WissenProgresses({ playedMatches, liveMatches, validatedMatches, teamsMax, status }) {
  const roundLength = teamsMax / 2;
  const totalMatches = roundLength * 6; // FIXME how many really?
  const finishedMatches = Math.max(playedMatches, validatedMatches);
  const needsValidation = validatedMatches + liveMatches < playedMatches;
  const live = liveMatches > 0;
  const color = needsValidation ? 'orange' : null;
  const progress =
    finishedMatches > 0 && finishedMatches === totalMatches
      ? 100
      : (100 * (finishedMatches % totalMatches)) / totalMatches;
  return <Progress value={progress} isIndeterminate={live} hasStripe={status === 'InProgress'} colorScheme={color} />;
}

function Progresses({
  currentRound,
  totalRounds,
  playedMatches,
  totalMatches,
  validatedMatches,
  liveMatches,
  teamsMax,
  status,
  format,
}) {
  if (status === 'Finished') return <FaFlagCheckered />;
  if (status === 'Registration') return <CalendarIcon />;
  switch (format) {
    case 'RoundRobin':
      return (
        <RoundRobinProgresses
          currentRound={currentRound}
          totalRounds={totalRounds}
          totalMatches={totalMatches}
          playedMatches={playedMatches}
          validatedMatches={validatedMatches}
          liveMatches={liveMatches}
          status={status}
        />
      );
    case 'Wissen':
      return (
        <WissenProgresses
          currentRound={currentRound}
          teamsMax={teamsMax}
          playedMatches={playedMatches}
          validatedMatches={validatedMatches}
          liveMatches={liveMatches}
          status={status}
        />
      );
    case 'Knockout':
      return `${currentRound} of ${totalRounds}`;
    default:
      return <QuestionIcon />;
  }
}

function ProgressLabel({ text, additionalText }) {
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
  validatedMatches,
  liveMatches,
  teamsMax,
}) {
  const currentRoundText = currentRound ? `, Round ${currentRound}` : '';
  const totalRoundsText = currentRound && totalRounds ? `of ${totalRounds}` : '';
  const progressText = `${prettyPrint(status)}${currentRoundText} ${totalRoundsText}`;
  const finishedMatches = Math.max(playedMatches, validatedMatches);
  const notYetValidatedMatches =
    playedMatches > validatedMatches ? ` (${playedMatches - validatedMatches} not yet validated)` : '';
  const progressAdditionalText =
    totalMatches && playedMatches
      ? `Finished ${finishedMatches} out of ${totalMatches} matches${notYetValidatedMatches}`
      : undefined;
  return (
    <Box p="0.25rem">
      <DelayedIconTooltip label={<ProgressLabel text={progressText} additionalText={progressAdditionalText} />}>
        <Box>
          <Progresses
            teamsMax={teamsMax}
            currentRound={currentRound}
            totalRounds={totalRounds}
            totalMatches={totalMatches}
            playedMatches={playedMatches}
            validatedMatches={validatedMatches}
            liveMatches={liveMatches}
            status={status}
            format={format}
          />
        </Box>
      </DelayedIconTooltip>
    </Box>
  );
}

export default CompetitionProgress;
