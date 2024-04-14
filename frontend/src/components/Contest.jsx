import React from 'react';
import { Center, Spinner, Td, Text, Tr, useDisclosure } from '@chakra-ui/react';
import Opponent from './Opponent';
import prettyPrint from '../util/PrettyPrint';
import formatter from '../util/Formatter';
import MatchStatusIcon from './MatchStatusIcon';
import config from '../config';
import DelayedIconTooltip from './DelayedIconTooltip';
import MatchModal from './MatchModal';

const { smallBoxSize } = config;

function ScoreOrIcon({ contest }) {
  switch (contest.status) {
    case 'played':
    case 'Validated':
      return (
        <Text color={contest.adminResult ? 'orange' : null}>
          {contest.opponents[0].score} - {contest.opponents[1].score}
        </Text>
      );
    default:
      return <MatchStatusIcon status={contest.status} boxSize={smallBoxSize} />;
  }
}

function Contest({ contest }) {
  const { isOpen, onOpen, onClose } = useDisclosure();
  const contestTooltip = `${prettyPrint(contest.status)} ${
    contest.adminResult ? ' - Admin result' : formatter.formatAsDate(contest.matchDate)
  }`;

  const openIfValidated = () => {
    if (contest.status === 'Validated') onOpen();
  };

  return contest ? (
    <Tr onClick={openIfValidated}>
      <MatchModal isOpen={isOpen} onClose={onClose} contest={contest} />
      <Opponent
        opponent={contest.opponents[0]}
        winnerTeamUuid={contest.winner ? contest.winner.team.id : null}
        key={contest.opponents[0].id}
        reverse={false}
      />
      <Td>
        <DelayedIconTooltip label={contestTooltip}>
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
