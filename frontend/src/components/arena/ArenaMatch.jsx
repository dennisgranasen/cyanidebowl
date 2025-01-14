import { Box } from '@chakra-ui/react';
import React from 'react';
import DelayedIconTooltip from '../common/DelayedIconTooltip';
import ArenaMatchLabel from './ArenaMatchLabel';
import ArenaScore from './ArenaScore';

import arenaHelpers from './arenaHelpers';
import config from '../../config';

const { boxSize } = config;

function ArenaMatch({ match, teamUuid }) {
  return (
    <DelayedIconTooltip label={<ArenaMatchLabel teamUuid={teamUuid} match={match} />}>
      <Box
        borderRadius="0.5rem"
        boxSize={boxSize}
        background={arenaHelpers.isWinner(teamUuid, match) ? 'green.500' : 'red.500'}
        fontFamily="EmbeddedBigStarRegular"
        height={boxSize}
      >
        <ArenaScore teamUuid={teamUuid} match={match} />
      </Box>
    </DelayedIconTooltip>
  );
}

export default ArenaMatch;
