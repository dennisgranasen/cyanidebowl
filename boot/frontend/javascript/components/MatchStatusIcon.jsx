import React from 'react';
import { TimeIcon } from '@chakra-ui/icons';
import { FaTowerBroadcast } from 'react-icons/fa6';

function MatchStatusIcon({ status, boxSize }) {
  switch (status) {
    case 'scheduled':
      return <TimeIcon boxSize={boxSize} />;
    case 'live':
      return <FaTowerBroadcast boxSize={boxSize} />;
    default:
      return null;
  }
}

export default MatchStatusIcon;
