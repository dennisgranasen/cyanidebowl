import React from 'react';
import { Icon, TimeIcon } from '@chakra-ui/icons';
import { FaTowerBroadcast } from 'react-icons/fa6';

function MatchStatusIcon({ status, live, boxSize }) {
  switch (status) {
    case 'scheduled':
    case 'Scheduled':
    case 'in_progress':
    case 'InProgress':
      return live ? <Icon as={FaTowerBroadcast} boxSize={boxSize} /> : <TimeIcon boxSize={boxSize} />;
    default:
      return null;
  }
}

export default MatchStatusIcon;
