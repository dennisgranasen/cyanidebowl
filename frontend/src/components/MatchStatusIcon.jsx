import React from 'react';
import { Icon } from '@chakra-ui/icons';
import { FaCalculator, FaRegCalendar, FaRegClock, FaTowerBroadcast } from 'react-icons/fa6';

function MatchStatusIcon({ status, live, boxSize }) {
  switch (status) {
    case 'Calculated':
      return <Icon as={FaCalculator} boxSize={boxSize} />;
    case 'scheduled':
    case 'Scheduled':
      return <Icon as={FaRegCalendar} boxSize={boxSize} />;
    case 'in_progress':
    case 'InProgress':
      return live ? <Icon as={FaTowerBroadcast} boxSize={boxSize} /> : <Icon as={FaRegClock} boxSize={boxSize} />;
    default:
      return null;
  }
}

export default MatchStatusIcon;
