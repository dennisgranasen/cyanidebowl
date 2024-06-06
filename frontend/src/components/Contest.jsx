import React from 'react';
import { Center, Spinner, Td, Text, Tr, useDisclosure } from '@chakra-ui/react';
import Opponent from './Opponent';
import prettyPrint from '../util/PrettyPrint';
import formatter from '../util/Formatter';
import MatchStatusIcon from './MatchStatusIcon';
import config from '../config';
import MatchModal from './MatchModal';
import DelayedIconTooltip from './DelayedIconTooltip';

const { smallBoxSize } = config;

function ScoreOrIconTooltip({ contest }) {
  let matchPlayed = false;
  let matchValidated = false;

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
  return `${status} ${contest.adminResult ? ' - Admin result' : formatter.formatAsDate(contest.matchDate)}`;
}

function ScoreOrIcon({ contest }) {
  let matchPlayed = false;
  let matchValidated = false;

  switch (contest.status) {
    case 'played':
    case 'Validated':
      matchPlayed = true;
      matchValidated = true;
      break;
    case 'InProgress':
      matchPlayed = contest.matchDate && contest.winner && contest.status === 'InProgress' && !contest.live;
      break;
    default:
      break;
  }

  let color = null;
  if (!matchValidated) color = 'grey';
  if (contest.adminResult) color = 'orange';
  return matchPlayed ? (
    <Text color={color}>
      {contest.opponents[0].score} - {contest.opponents[1].score}
    </Text>
  ) : (
    <MatchStatusIcon status={contest.status} live={contest.live} boxSize={smallBoxSize} />
  );
}

function Contest({ contest, smallscreen }) {
  const { isOpen, onOpen, onClose } = useDisclosure();

  const openIfValidatedAndNotAdminResult = () => {
    if (contest.status === 'Validated' && !contest.adminResult) onOpen();
  };

  const winnerTeamUuid =
    contest.winner && contest.opponents[0].score !== contest.opponents[1].score ? contest.winner.team.id : null;
  return contest ? (
    <Tr onClick={openIfValidatedAndNotAdminResult}>
      <MatchModal isOpen={isOpen} onClose={onClose} contest={contest} />
      <Opponent
        smallscreen={smallscreen}
        opponent={contest.opponents[0]}
        key={contest.opponents[0].id}
        winner={contest.opponents[0].id === winnerTeamUuid}
      />
      <Td>
        <DelayedIconTooltip label={<ScoreOrIconTooltip contest={contest} />}>
          <Center>
            <ScoreOrIcon contest={contest} />
          </Center>
        </DelayedIconTooltip>
      </Td>
      <Opponent
        smallscreen={smallscreen}
        opponent={contest.opponents[1]}
        key={contest.opponents[1].id}
        winner={contest.opponents[1].id === winnerTeamUuid}
        reverse
      />
    </Tr>
  ) : (
    <Spinner />
  );
}

export default Contest;
