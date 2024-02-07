import React from 'react';
import { Spinner, Td, Tr } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import CompetitionProgress from './CompetitionProgress';
import prettyPrint from '../util/PrettyPrint';

function Competition({ competition }) {
  return competition !== null ? (
    <Tr>
      <Td>
        <RouteLink to={`/competition/${competition.uuid}`}>{competition.name}</RouteLink>
      </Td>
      <Td>{prettyPrint(competition.format)}</Td>
      <Td>
        <CompetitionProgress
          status={competition.status}
          currentRound={competition.currentRound}
          totalRounds={competition.totalRounds}
          totalMatches={competition.totalMatches}
          playedMatches={competition.playedMatches}
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
