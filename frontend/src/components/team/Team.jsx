import React from 'react';
import { Image, Spinner, Td, Tr } from '@chakra-ui/react';
import { useNavigate } from 'react-router-dom';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import Race from './Race';
import Formatter from '../../util/Formatter';
import ImageUrls from '../../ImageUrls';
import config from '../../config';

const { boxSize } = config;

function Team({ team }) {
  const navigate = useNavigate();
  const goToTeam = () => {
    navigate(`/competition/${team.competitionIds[0]}/team/${team.id}`);
  };

  return team !== null ? (
    <Tr onClick={goToTeam}>
      <Td>{team.name}</Td>
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
