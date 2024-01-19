import React from 'react';
import { Box, Progress, Tooltip } from '@chakra-ui/react';

function CompetitionProgress({ currentRound, totalRounds }) {
  const currentRoundText = currentRound ? `Round ${currentRound}` : '';
  const totalRoundsText = currentRound && totalRounds ? `of ${totalRounds}` : '';
  const progressLabel = `${currentRoundText} ${totalRoundsText}`;
  const progressValue = (currentRound / totalRounds) * 100;
  return totalRounds ? (
    <Box p="4px">
      <Tooltip label={progressLabel}>
        <Progress value={progressValue} />
      </Tooltip>
    </Box>
  ) : (
    progressLabel
  );
}

export default CompetitionProgress;
