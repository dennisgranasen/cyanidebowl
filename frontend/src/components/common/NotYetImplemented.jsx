import React from 'react';
import { Icon, QuestionOutlineIcon } from '@chakra-ui/icons';
import { FaRegFaceSadTear } from 'react-icons/fa6';
import DelayedIconTooltip from './DelayedIconTooltip';

function NotYetImplemented({ ...props }) {
  return (
    <DelayedIconTooltip label="Not yet implemented/available." icon={FaRegFaceSadTear}>
      <Icon as={QuestionOutlineIcon} {...props} />
    </DelayedIconTooltip>
  );
}

export default NotYetImplemented;
