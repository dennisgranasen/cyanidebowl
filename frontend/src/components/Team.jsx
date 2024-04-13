import React, { useEffect, useState } from 'react';
import { Image, Spinner, Td, Tr, useBoolean } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import Race from './Race';
import Formatter from '../util/Formatter';
import ImageUrls from '../ImageUrls';
import config from '../config';

const boxSize = '32px';

function Team({ team }) {
  const [flag, setFlag] = useBoolean();
  const [background, setBackground] = useState();
  useEffect(() => {
    if (flag) setBackground(config.hoverBackgroundColor);
    else setBackground(null);
  }, [flag]);

  return team !== null ? (
    <Tr
      onMouseEnter={setFlag.on}
      onMouseLeave={setFlag.off}
      css={`
        background: ${background};
      `}
    >
      <Td>
        <RouteLink to={`/team/${team.id}`}>{team.name}</RouteLink>
      </Td>
      <Td>
        <RouteLink to={`/team/${team.id}`}>
          <Image
            src={`${ImageUrls.logo(team.logo)}`}
            boxSize={boxSize}
            fallback={<QuestionOutlineIcon boxSize={boxSize} />}
            objectFit="scale-down"
          />
        </RouteLink>
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
