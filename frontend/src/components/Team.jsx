import React from 'react';
import { Image, Spinner, Td, Tr } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import Race from './Race';
import Formatter from '../util/Formatter';
import ImageUrls from '../ImageUrls';

const boxSize = '32px';

function Team({ team }) {
  const goToTeam = () => {
    console.log('TODO: implement');
  };
  return team !== null ? (
    <Tr onClick={goToTeam}>
      <Td>
        <RouteLink to={`/competition/${team.competitionIds[0]}/team/${team.id}`}>{team.name}</RouteLink>
      </Td>
      <Td>
        <Image
          src={`${ImageUrls.logo(team.logo)}`}
          boxSize={boxSize}
          fallback={<QuestionOutlineIcon boxSize={boxSize} />}
          objectFit="scale-down"
        />
      </Td>
      <Td>{team.coachName}</Td>
      <Td>
        <Race race={team.race} />
      </Td>
      <Td isNumeric>{Formatter.formatAsNumber(team.value)}</Td>
      <Td isNumeric>{Formatter.formatAsNumber(team.cash)}</Td>
    </Tr>
  ) : (
    <Spinner />
  );
}

export default Team;
