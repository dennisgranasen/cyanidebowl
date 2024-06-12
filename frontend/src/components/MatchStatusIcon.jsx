import React from 'react';
import { Icon } from '@chakra-ui/icons';
import { FaCalculator, FaRegCalendar, FaRegClock, FaTowerBroadcast } from 'react-icons/fa6';
import { Box, Progress } from '@chakra-ui/react';

function MatchStatusIcon({ status, live, boxSize }) {
  switch (status) {
    case 'Calculated':
      return <Icon as={FaCalculator} boxSize={boxSize} />;
    case 'scheduled':
    case 'Scheduled':
      return <Icon as={FaRegCalendar} boxSize={boxSize} />;
    case 'in_progress':
    case 'InProgress':
      return live ? (
        <Box>
          <Icon as={FaTowerBroadcast} boxSize={boxSize} />
          <Progress size="xs" w="100%" isIndeterminate />
        </Box>
      ) : (
        <Icon as={FaRegClock} boxSize={boxSize} />
      );
    default:
      return null;
  }
}

export default MatchStatusIcon;
