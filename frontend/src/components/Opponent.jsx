import React from 'react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import { Box, Image, Td, Text } from '@chakra-ui/react';
import ImageUrls from '../ImageUrls';
import config from '../config';
import logger from '../util/Logger';

const { boxSize } = config;

const Boxes = (opponent, reverse, winnerTeamUuid) => {
  const winner = winnerTeamUuid && opponent.id === winnerTeamUuid;
  const fontWeight = winner ? 'bold' : 'normal';
  const textAlign = !reverse ? 'right' : 'left';

  logger.debug('Opponent: %o', opponent);

  return [
    <Td key={opponent.coachName}>
      <Text fontWeight={fontWeight} textAlign={textAlign}>
        {opponent.coachName}
      </Text>
    </Td>,
    <Td key={opponent.name}>
      <Text fontWeight={fontWeight} textAlign={textAlign}>
        {opponent.name}
      </Text>
    </Td>,
    <Td key={`${opponent.name}${opponent.logo}`}>
      <Box align={textAlign}>
        <Image
          align={textAlign}
          src={`${ImageUrls.logo(opponent.logo)}`}
          boxSize={boxSize}
          fallback={<QuestionOutlineIcon boxSize={boxSize} />}
          objectFit="scale-down"
        />
      </Box>
    </Td>,
  ];
};

function Opponent({ opponent, reverse, winnerTeamUuid }) {
  return reverse ? Boxes(opponent, reverse, winnerTeamUuid).reverse() : Boxes(opponent);
}

export default Opponent;
