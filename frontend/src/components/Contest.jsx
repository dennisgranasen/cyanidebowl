import React from 'react';
import { Center, Spinner, Td, Tooltip, Tr } from '@chakra-ui/react';
import Opponent from './Opponent';
import prettyPrint from '../util/PrettyPrint';
import formatter from '../util/Formatter';
import MatchStatusIcon from './MatchStatusIcon';
import config from '../config';
import DelayedIconTooltip from './DelayedIconTooltip';

const { smallBoxSize } = config;

function ScoreOrIcon({ contest }) {
  switch (contest.status) {
    case 'played':
      return (
        <>
          {contest.opponents[0].score} - {contest.opponents[1].score}
        </>
      );
    default:
      return <MatchStatusIcon status={contest.status} boxSize={smallBoxSize} />;
  }
}

function Contest({ contest }) {
  return contest !== null ? (
    <Tr>
      <Opponent
        opponent={contest.opponents[0]}
        winnerTeamUuid={contest.winner ? contest.winner.team.id : null}
        key={contest.opponents[0].id}
        reverse={false}
      />
      <Td>
        <DelayedIconTooltip label={`${prettyPrint(contest.status)} ${formatter.formatAsDate(contest.matchDate)}`}>
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
