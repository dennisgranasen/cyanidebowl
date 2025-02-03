import React from 'react';
import { Box, Flex, GridItem, Progress, SimpleGrid, VStack } from '@chakra-ui/react';
import { CalendarIcon, QuestionIcon } from '@chakra-ui/icons';
import { FaFlagCheckered } from 'react-icons/fa6';
import DelayedIconTooltip from '../common/DelayedIconTooltip';
import prettyPrint from '../../util/prettyPrint';
import logger from '../../util/logger';

function extractRoundData(currentRound, round, finishedMatchesInRound, roundLength, status) {
  let progress = 0;
  let active = false;
  if (currentRound > round + 1) {
    progress = 100;
  } else if (currentRound === round + 1) {
    progress = finishedMatchesInRound === roundLength ? 100 : (100 * finishedMatchesInRound) / roundLength;
    active = status === 'InProgress';
  }
  return { progress, active };
}

function RoundRobinOrWissenProgresses({
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
    const { progress, active } = extractRoundData(
      currentRound,
      round,
      finishedMatches === currentRound * roundLength ? roundLength : finishedMatches % roundLength,
      roundLength,
      status
    );
    roundProgresses.push({ name: `round${round + 1}`, progress, active });
  }
  return (
    <SimpleGrid columns={totalRounds} spacing="0.25rem" paddingTop={withPadding ? '0.25rem' : null}>
      {roundProgresses.map(({ name, progress, active }) => (
        <GridItem key={name}>
          <Progress
            value={progress}
            isAnimated={active && live && progress > 0}
            isIndeterminate={active && live && progress === 0}
            hasStripe={active}
            variant={needsValidation ? 'validationNeeded' : null}
          />
        </GridItem>
      ))}
    </SimpleGrid>
  );
}

function KnockoutProgresses({
  currentRound,
  totalRounds,
  liveMatches,
  playedMatches,
  totalMatches,
  notValidatedMatches,
  status,
  withPadding,
}) {
  const participants = totalMatches + 1;
  const finishedMatches = playedMatches - notValidatedMatches;
  const needsValidation = notValidatedMatches - liveMatches > 0;
  const live = liveMatches > 0;
  const roundProgresses = [];
  let finishedMatchesInRound = 0;
  let roundLength = participants;
  let matchesCumulated = 0;
  for (let round = 0; round < totalRounds; round += 1) {
    roundLength /= 2;
    matchesCumulated += roundLength;
    if (finishedMatches > matchesCumulated) {
      finishedMatchesInRound = roundLength;
    } else if (currentRound === round + 1) {
      finishedMatchesInRound = matchesCumulated - finishedMatches;
    } else {
      finishedMatchesInRound = 0;
    }
    const { progress, active } = extractRoundData(currentRound, round, finishedMatchesInRound, roundLength, status);
    const width = Math.max(Math.floor((roundLength / totalMatches) * 95), 5);
    roundProgresses.push({ name: `round${round + 1}`, progress, active, width });
  }
  return (
    <Flex paddingTop={withPadding ? '0.25rem' : null}>
      {roundProgresses.map(({ name, progress, active, width }, index) => (
        <Box key={name} width={`${width}%`} padding="0" paddingLeft={index > 0 ? '0.25rem' : ''}>
          <Progress
            value={progress}
            isIndeterminate={active && live}
            hasStripe={active}
            variant={needsValidation ? 'validationNeeded' : null}
          />
        </Box>
      ))}
    </Flex>
  );
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
  if (status === 'Finished') return <FaFlagCheckered />;
  if (status === 'Registration') return <CalendarIcon />;
  switch (format) {
    case 'RoundRobin':
    case 'Wissen':
      return (
        <RoundRobinOrWissenProgresses
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
    case 'Knockout':
      return (
        <KnockoutProgresses
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
    case 'Ladder':
    case 'Arena':
      return `${playedMatches || 0} played match${playedMatches !== 1 ? 'es' : ''}`;
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
        label={<ProgressLabel text={progressText} additionalText={progressAdditionalText} />}
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
