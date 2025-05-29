import React from 'react';
import { Image, Spinner, Td, Tr } from '@chakra-ui/react';
import { useNavigate } from 'react-router-dom';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import formatter from '../../util/formatter';
import imageUrls from '../../imageUrls';
import config from '../../config';
import Race from '../common/Race';

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
          src={`${imageUrls.logo(team.logo)}`}
          boxSize={boxSize}
          fallback={<QuestionOutlineIcon boxSize={boxSize} />}
          objectFit="scale-down"
        />
      </Td>
      <Td>{team.coachName}</Td>
      <Td>
        <Race race={team.race} />
      </Td>
      <Td isNumeric>{formatter.formatAsNumber(team.value)}</Td>
      <Td isNumeric>{formatter.formatAsNumber(team.cash)}</Td>
    </Tr>
  ) : (
    <Spinner />
  );
}

export default Team;
