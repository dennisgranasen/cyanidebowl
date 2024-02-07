import React from 'react';
import { Box, GridItem, Progress, SimpleGrid, VStack } from '@chakra-ui/react';
import DelayedIconTooltip from './DelayedIconTooltip';
import prettyPrint from '../util/PrettyPrint';

function Progresses({ currentRound, totalRounds, playedMatches, totalMatches }) {
  const roundLength = totalMatches ? totalMatches / totalRounds : 1;
  const roundProgresses = [];
  for (let round = 0; round < totalRounds; round += 1) {
    let progress = 0;
    let active = false;
    if (currentRound > round + 1) {
      progress = 100;
    } else if (currentRound === round + 1) {
      progress =
        playedMatches >= roundLength * currentRound ? 100 : (100 * (playedMatches % roundLength)) / roundLength;
      active = true;
    }
    roundProgresses.push({ name: `round${round + 1}`, progress, active });
  }
  return (
    <SimpleGrid columns={totalRounds} spacing="3px">
      {roundProgresses.map(({ name, progress, active }) => (
        <GridItem key={name}>
          <Progress value={progress} hasStripe={active} />
        </GridItem>
      ))}
    </SimpleGrid>
  );
}

function ProgressLabel({ text, additionalText }) {
  return (
    <VStack align="left">
      <Box>{text}</Box>
      {additionalText ? <Box> {additionalText} </Box> : null}
    </VStack>
  );
}

function CompetitionProgress({ status, currentRound, totalRounds, playedMatches, totalMatches }) {
  const currentRoundText = currentRound ? `Round ${currentRound}` : '';
  const totalRoundsText = currentRound && totalRounds ? `of ${totalRounds}` : '';
  const progressText = `${prettyPrint(status)}, ${currentRoundText} ${totalRoundsText}`;
  const progressAdditionalText =
    totalMatches && playedMatches ? `Played ${playedMatches} out of ${totalMatches} matches` : undefined;
  return (
    <Box p="4px">
      <DelayedIconTooltip label={<ProgressLabel text={progressText} additionalText={progressAdditionalText} />}>
        <Box>
          <Progresses
            currentRound={currentRound}
            totalRounds={totalRounds}
            totalMatches={totalMatches}
            playedMatches={playedMatches}
          />
        </Box>
      </DelayedIconTooltip>
    </Box>
  );
}

export default CompetitionProgress;
