import React from 'react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import { Avatar } from '@chakra-ui/react';
import imageUrls from '../../imageUrls';
import prettyPrint from '../../util/prettyPrint';
import formatter from '../../util/formatter';
import MatchStatusIcon from './MatchStatusIcon';
import config from '../../config';
import DelayedIconTooltip from '../common/DelayedIconTooltip';

const { boxSize, smallBoxSize } = config;

function StatusAsIcon({ status, stadium, opus }) {
  switch (status) {
    case 'played':
      return (
        <Avatar
          src={`${imageUrls.stadium(stadium, opus)}`}
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
    <DelayedIconTooltip label={`${prettyPrint(status)} ${formatter.formatAsDate(matchDate, '')}`}>
      <StatusAsIcon status={status} stadium={stadium} />
    </DelayedIconTooltip>
  );
}

export default MatchStatus;
