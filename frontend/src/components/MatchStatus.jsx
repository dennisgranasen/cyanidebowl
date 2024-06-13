import React from 'react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import { Avatar } from '@chakra-ui/react';
import ImageUrls from '../ImageUrls';
import prettyPrint from '../util/PrettyPrint';
import formatter from '../util/Formatter';
import MatchStatusIcon from './MatchStatusIcon';
import config from '../config';
import DelayedIconTooltip from './common/DelayedIconTooltip';

const { boxSize, smallBoxSize } = config;

function StatusAsIcon({ status, stadium }) {
  switch (status) {
    case 'played':
      return (
        <Avatar
          src={`${ImageUrls.stadium(stadium)}`}
          boxSize={boxSize}
          icon={<QuestionOutlineIcon boxSize={boxSize} />}
        />
      );
    case 'scheduled':
    case 'in_progress':
      return <MatchStatusIcon status={status} boxSize={smallBoxSize} />;
    default:
      return <QuestionOutlineIcon boxSize={boxSize} />;
  }
}

function MatchStatus({ status, matchDate, stadium }) {
  return (
    <DelayedIconTooltip label={`${prettyPrint(status)} ${formatter.formatAsDate(matchDate)}`}>
      <StatusAsIcon status={status} stadium={stadium} />
    </DelayedIconTooltip>
  );
}

export default MatchStatus;
