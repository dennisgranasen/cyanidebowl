import React from 'react';
import { LinkBox, LinkOverlay, Spinner, Td, Tr } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import CompetitionStatus from './CompetitionStatus';
import CompetitionProgress from './CompetitionProgress';
import prettyPrint from '../util/PrettyPrint';

function Competition({ competition }) {
  return competition !== null ? (
    <LinkBox as={Tr}>
      <Td>
        <LinkOverlay as={RouteLink} to={`/competition/${competition.uuid}`} />
        {competition.name}
      </Td>
      <Td>{prettyPrint(competition.format)}</Td>
      <Td>
        <CompetitionStatus status={competition.status} />
      </Td>
      <Td>
        <CompetitionProgress
          currentRound={competition.currentRound}
          totalRounds={competition.totalRounds}
          totalMatches={competition.totalMatches}
          playedMatches={competition.playedMatches}
        />
      </Td>
      <Td isNumeric>{competition.teamsMax}</Td>
    </LinkBox>
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
