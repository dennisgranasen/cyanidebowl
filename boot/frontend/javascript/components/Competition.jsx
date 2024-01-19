import React from 'react';
import { Image, Spinner, Td, Tr } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import ImageUrls from '../ImageUrls';
import CompetitionStatus from './CompetitionStatus';
import config from '../config';

const { boxSize } = config;

function Competition({ competition }) {
  return competition !== null ? (
    <Tr>
      <Td>
        <RouteLink to={`/${competition.leagueId}`}>
          <Image
            src={`${ImageUrls.logo(competition.leagueLogo)}`}
            boxSize={boxSize}
            fallback={<QuestionOutlineIcon boxSize={boxSize} />}
            objectFit="scale-down"
          />
        </RouteLink>
      </Td>
      <Td>
        <RouteLink to={`/${competition.leagueId}`}>{competition.leagueName}</RouteLink>
      </Td>
      <Td>
        <RouteLink to={`/competition/${competition.uuid}`}>{competition.name}</RouteLink>
      </Td>
      <Td>{competition.format}</Td>
      <Td>
        <CompetitionStatus status={competition.status} />
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
