import React from 'react';
import { Box, Progress, Tooltip } from '@chakra-ui/react';

const calcProgress = (currentRound, totalRounds, playedMatches, totalMatches) => {
  let progressValue;
  if (totalMatches && playedMatches) {
    progressValue = playedMatches / totalMatches;
  } else if (totalRounds && currentRound) {
    progressValue = currentRound / totalRounds;
  }
  return progressValue ? progressValue * 100 : undefined;
};

function ProgressLabel({ text, additionalText }) {
  return (
    <Box>
      {text}
      {additionalText ? <Box>{additionalText} </Box> : null}
    </Box>
  );
}

function CompetitionProgress({ currentRound, totalRounds, playedMatches, totalMatches }) {
  const currentRoundText = currentRound ? `Round ${currentRound}` : '';
  const totalRoundsText = currentRound && totalRounds ? `of ${totalRounds}` : '';
  const progressText = `${currentRoundText} ${totalRoundsText}`;
  const progressAdditionalText =
    totalMatches && playedMatches ? `Played ${playedMatches} out of ${totalMatches} matches` : undefined;
  const calcedProgress = calcProgress(currentRound, totalRounds, playedMatches, totalMatches);
  return calcedProgress ? (
    <Box p="4px">
      <Tooltip label={<ProgressLabel text={progressText} additionalText={progressAdditionalText} />}>
        <Progress value={calcedProgress} />
      </Tooltip>
    </Box>
  ) : (
    progressText
  );
}

export default CompetitionProgress;
