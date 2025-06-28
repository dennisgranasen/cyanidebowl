import React from 'react';
import { Avatar, Text } from '@chakra-ui/react';
import prettyPrint from '../../util/prettyPrint';
import DelayedIconTooltip from './DelayedIconTooltip';
import logger from '../../util/logger';

function RaceAvatar({ race, boxSize, size }) {
  logger.debug('Race %s', race);
  return (
    <DelayedIconTooltip label={prettyPrint(race)}>
      <Avatar
        color="warpScoresMenuTextColor"
        backgroundColor="warpScoresProgressBackgroundColor"
        boxSize={boxSize}
        size={size}
        fontFamily="bigStar"
        src={`/img/raceAvatars/${race}.png`}
        name={/*prettyPrint(*/race/*)*/}
      />
    </DelayedIconTooltip>
  );
}

function Race({ race, size, boxSize, asAvatar }) {
  return asAvatar ? (
    <RaceAvatar race={race} size={size} boxSize={boxSize} />
  ) : (
    <Text size={size}>{/*prettyPrint(*/race/*)*/}</Text>
  );
}

export default Race;
