import React from 'react';
import { Spinner, Td, Tr, useBreakpointValue } from '@chakra-ui/react';
import { useNavigate } from 'react-router-dom';
import CompetitionProgress from './CompetitionProgress';
import prettyPrint from '../../util/PrettyPrint';
import abbreviators from '../../util/Abbreviators';
import config from '../../config';

const { smallScreenBreakpointValues } = config;

function Competition({ competition }) {
  const navigate = useNavigate();
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);

  const goToCompetition = () => {
    navigate(`/competition/${competition.uuid}`);
  };

  return competition !== null ? (
    <Tr onClick={goToCompetition}>
      <Td>{competition.name}</Td>
      <Td>{isSmallScreen ? abbreviators.makeInitials(competition.format) : prettyPrint(competition.format)}</Td>
      <Td>
        <CompetitionProgress
          status={competition.status}
          format={competition.format}
          teamsMax={competition.teamsMax}
          currentRound={competition.currentRound}
          totalRounds={competition.totalRounds}
          totalMatches={competition.totalMatches}
          playedMatches={competition.playedMatches}
          notValidatedMatches={competition.notValidatedMatches}
          liveMatches={competition.liveMatches}
        />
      </Td>
      <Td isNumeric>{competition.teamsMax}</Td>
    </Tr>
  ) : (
    <Spinner />
  );
}

export default Competition;
