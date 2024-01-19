import React from 'react';
import { Box, Image, Tooltip } from '@chakra-ui/react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import prettyPrint from '../util/PrettyPrint';
import config from '../config';
import ImageUrls from '../ImageUrls';
import DelayedIconTooltip from './DelayedIconTooltip';

const { smallBoxSize: boxSize } = config;

function Skill({ skill }) {
  return (
    <DelayedIconTooltip label={prettyPrint(skill)}>
      <Box boxSize={boxSize}>
        <Image
          src={`${ImageUrls.skill(skill)}`}
          alt={prettyPrint(skill)}
          objectFit="cover"
          fallback={<QuestionOutlineIcon boxSize={boxSize} />}
        />
      </Box>
    </DelayedIconTooltip>
  );
}

export default Skill;
