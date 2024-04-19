import React from 'react';
import { Image, Tag, TagLabel } from '@chakra-ui/react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import prettyPrint from '../util/PrettyPrint';
import config from '../config';
import DelayedIconTooltip from './DelayedIconTooltip';

const { smallBoxSize: boxSize } = config;

const imageOrIconFor = (injury) => {
  let imageName = '';
  switch (injury.toLowerCase()) {
    case 'groinstrain':
      imageName = 'seriously_hurt';
      break;
    case 'brokenjaw':
    case 'fracturedleg':
      imageName = 'smashed_knee';
      break;
    case 'fracturedarm':
      imageName = 'broken_arm';
      break;
    default:
      imageName = injury.toLowerCase();
      break;
  }
  return <Image src={`/img/${imageName}.png`} alt={injury} boxSize={boxSize} fallback={<QuestionOutlineIcon />} />;
};

function Injury({ injury, count }) {
  return (
    <DelayedIconTooltip label={prettyPrint(injury)}>
      <Tag size={boxSize} borderRadius="full" ml={-1} mr={2}>
        {imageOrIconFor(injury)}
        {count > 1 ? <TagLabel>{count}</TagLabel> : ''}
      </Tag>
    </DelayedIconTooltip>
  );
}

export default Injury;
