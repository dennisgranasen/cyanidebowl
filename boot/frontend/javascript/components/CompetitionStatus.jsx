import React from 'react';
import { FaAddressCard, FaFlagCheckered, FaSpinner } from 'react-icons/fa6';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import { Box, Tooltip } from '@chakra-ui/react';
import config from '../config';
import prettyPrint from '../util/PrettyPrint';

const boxSize = config.smallBoxSize;
function Icon({ status }) {
  switch (status) {
    case 'InProgress':
      return <FaSpinner boxSize={boxSize} />;
    case 'Finished':
      return <FaFlagCheckered boxSize={boxSize} />;
    case 'Registration':
      return <FaAddressCard boxSize={boxSize} />;
    default:
      return <QuestionOutlineIcon boxSize={boxSize} />;
  }
}

function CompetitionStatus({ status }) {
  return (
    <Tooltip label={prettyPrint(status)}>
      <Box>
        <Icon status={status} />
      </Box>
    </Tooltip>
  );
}

export default CompetitionStatus;
