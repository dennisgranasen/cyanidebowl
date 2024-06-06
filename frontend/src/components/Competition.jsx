import React from 'react';
import { Spinner, Td, Tr } from '@chakra-ui/react';
import { useNavigate } from 'react-router-dom';
import CompetitionProgress from './CompetitionProgress';
import prettyPrint from '../util/PrettyPrint';
import abbreviators from '../util/Abbreviators';

function Competition({ competition, smallscreen }) {
  const navigate = useNavigate();
  const goToCompetition = () => {
    navigate(`/competition/${competition.uuid}`);
  };

  return competition !== null ? (
    <Tr onClick={goToCompetition}>
      <Td>{competition.name}</Td>
      <Td>{smallscreen ? abbreviators.abbreviate(competition.format) : prettyPrint(competition.format)}</Td>
      <Td>
        <CompetitionProgress
          status={competition.status}
          format={competition.format}
          teamsMax={competition.teamsMax}
          currentRound={competition.currentRound}
          totalRounds={competition.totalRounds}
          totalMatches={competition.totalMatches}
          playedMatches={competition.playedMatches}
          validatedMatches={competition.validatedMatches}
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

/*
                <Td><RouteLink to={`/competition/${competition.uuid}`}><Image src={`${ImageUrls.logo(competition.logo)}`}
                                                                            boxSize={boxSize}
                                                                            fallback={<QuestionOutlineIcon boxSize={boxSize}/>}
                                                                            objectFit="scale-down"/></RouteLink></Td>
 */
