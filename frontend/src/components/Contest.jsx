import React from 'react';
import { Center, Spinner, Td, Text, Tr, useDisclosure } from '@chakra-ui/react';
import Opponent from './Opponent';
import prettyPrint from '../util/PrettyPrint';
import formatter from '../util/Formatter';
import MatchStatusIcon from './MatchStatusIcon';
import config from '../config';
import MatchModal from './MatchModal';
import DelayedIconTooltip from './DelayedIconTooltip';
import logger from '../util/Logger';

const { smallBoxSize } = config;

function ScoreOrIconTooltip({ contest }) {
  let matchPlayed = false;
  let matchValidated = false;

  switch (contest.status) {
    case 'played':
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

  const status = `${matchPlayed && !matchValidated ? 'Awaiting validation' : prettyPrint(contest.status)}`;
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
      matchPlayed = contest.matchDate && contest.winner && contest.status === 'InProgress';
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

function Contest({ contest }) {
  const { isOpen, onOpen, onClose } = useDisclosure();

  const openIfValidatedAndNotAdminResult = () => {
    if (contest.status === 'Validated' && !contest.adminResult) onOpen();
  };

  return contest ? (
    <Tr onClick={openIfValidatedAndNotAdminResult}>
      <MatchModal isOpen={isOpen} onClose={onClose} contest={contest} />
      <Opponent
        opponent={contest.opponents[0]}
        winnerTeamUuid={contest.winner ? contest.winner.team.id : null}
        key={contest.opponents[0].id}
        reverse={false}
      />
      <Td>
        <DelayedIconTooltip label={<ScoreOrIconTooltip contest={contest} />}>
          <Center>
            <ScoreOrIcon contest={contest} />
          </Center>
        </DelayedIconTooltip>
      </Td>
      <Opponent
        opponent={contest.opponents[1]}
        winnerTeamUuid={contest.winner ? contest.winner.team.id : null}
        key={contest.opponents[1].id}
        reverse
      />
    </Tr>
  ) : (
    <Spinner />
  );
}

export default Contest;
