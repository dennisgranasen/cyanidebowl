import React from 'react';
import { Center, Spinner, Td, Tr, useDisclosure } from '@chakra-ui/react';
import Opponent from './Opponent';
import prettyPrint from '../../util/prettyPrint';
import formatter from '../../util/formatter';
import config from '../../config';
import MatchModal from './MatchModal';
import DelayedIconTooltip from '../common/DelayedIconTooltip';
import ScoreOrIcon from './ScoreOrIcon';
import { identityUtils } from '../../util/identityUtil';

const { smallBoxSize } = config;

function ScoreOrIconTooltip({ contest }) {
  let matchPlayed = false;
  let matchValidated = false;
  console.log('ScoreOrIconTooltip', contest);
  switch (contest.status) {
    case 'played':
    case 'Played':
    case 'Validated':
      matchPlayed = true;
      matchValidated = true;
      break;
    case 'InProgress':
      matchPlayed = contest.matchDate && contest.winner && contest.status === 'InProgress';
      break;
    default:
      break;
  }

  let status = `${matchPlayed && !matchValidated ? 'Awaiting validation' : prettyPrint(contest.status)}`;
  if (contest.live) status = 'Live';
  const matchDate = contest.live
    ? formatter.formatAsDuration(contest.matchDate, null)
    : formatter.formatAsDate(contest.matchDate, '');
  const concedeText = contest.concede ? ' - Conceded' : '';
  const overtimeText = contest.overtime ? ' - Decided in Overtime' : '';
  const matchDateText = contest.adminResult ? ' - Admin result' : ` - ${matchDate}`;
  return `${status}${concedeText}${overtimeText}${matchDateText}`;
}

function Contest({ contest }) {
  const { isOpen, onOpen, onClose } = useDisclosure();

  const openIfValidatedAndNotAdminResult = () => {
    if (identityUtils.opus(contest.id) === 1 || ((contest.status === 'Validated' && !contest.adminResult))) 
      onOpen();
  };

  const winnerTeamId =
    contest.winner && contest.opponents[0].score !== contest.opponents[1].score ? contest.winner.team.id : null;
  console.log('Contest', { contest, winnerTeamId });
  return contest ? (
    <Tr onClick={openIfValidatedAndNotAdminResult} key={identityUtils.key(contest.id)}>
      <MatchModal isOpen={isOpen} onClose={onClose} contest={contest} />
      <Opponent
        opponent={contest.opponents[0]}
        key={identityUtils.key(contest.opponents[0].id)}
        winner={contest.opponents[0].id === winnerTeamId}
      />
      <Td>
        <DelayedIconTooltip label={<ScoreOrIconTooltip contest={contest} />}>
          <Center>
            <ScoreOrIcon contestOrMatch={contest} boxSize={smallBoxSize} size="sm" />
          </Center>
        </DelayedIconTooltip>
      </Td>
      <Opponent
        opponent={contest.opponents[1]}
        key={identityUtils.key(contest.opponents[1].id)}
        winner={contest.opponents[1].id === winnerTeamId}
        reverse
      />
    </Tr>
  ) : (
    <Spinner />
  );
}

export default Contest;
