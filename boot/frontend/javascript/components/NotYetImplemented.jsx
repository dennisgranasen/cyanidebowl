import React from 'react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import { FaRegFaceSadTear } from 'react-icons/fa6';
import DelayedIconTooltip from './DelayedIconTooltip';

function NotYetImplemented() {
  return (
    <DelayedIconTooltip label="Not yet implemented/available." icon={FaRegFaceSadTear}>
      <QuestionOutlineIcon />
    </DelayedIconTooltip>
  );
}

export default NotYetImplemented;
