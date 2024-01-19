import React from 'react';
import { FaAddressCard, FaFlagCheckered, FaSpinner } from 'react-icons/fa6';
import { Icon, QuestionOutlineIcon } from '@chakra-ui/icons';
import prettyPrint from '../util/PrettyPrint';
import DelayedIconTooltip from './DelayedIconTooltip';

function StatusAsIcon({ status }) {
  switch (status) {
    case 'InProgress':
      return <FaSpinner />;
    case 'Finished':
      return <FaFlagCheckered />;
    case 'Registration':
      return <FaAddressCard />;
    default:
      return <QuestionOutlineIcon />;
  }
}

function CompetitionStatus({ status }) {
  const statusText = prettyPrint(status);
  return (
    <DelayedIconTooltip label={statusText}>
      <StatusAsIcon status={status} />
    </DelayedIconTooltip>
  );
}

export default CompetitionStatus;
