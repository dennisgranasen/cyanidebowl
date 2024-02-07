import React from 'react';
import { TimeIcon } from '@chakra-ui/icons';
import { FaTowerBroadcast } from 'react-icons/fa6';

function MatchStatusIcon({ status, boxSize }) {
  switch (status) {
    case 'scheduled':
    case 'Scheduled':
    case 'in_progress':
    case 'InProgress':
      return <TimeIcon boxSize={boxSize} />;
    default:
      return null;
  }
}

export default MatchStatusIcon;
