import React from 'react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import { Box, Image, Td, Text, useMediaQuery } from '@chakra-ui/react';
import ImageUrls from '../ImageUrls';
import config from '../config';
import abbrevators from '../util/Abbrevators';

const { boxSize } = config;

const Boxes = (opponent, reverse, winner) => {
  const fontWeight = winner ? 'bold' : 'normal';
  const textAlign = !reverse ? 'right' : 'left';
  const [isSmallScreen] = useMediaQuery('(max-width: 768px)');

  return [
    <Td key={opponent.coachName}>
      <Text fontWeight={fontWeight} textAlign={textAlign}>
        {isSmallScreen ? abbrevators.abbrevateCoachName(opponent.coachName) : opponent.coachName}
      </Text>
    </Td>,
    <Td key={opponent.name}>
      <Text fontWeight={fontWeight} textAlign={textAlign}>
        {isSmallScreen ? abbrevators.abbrevateTeamName(opponent.name) : opponent.name}
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

function Opponent({ opponent, reverse, winner }) {
  return reverse ? Boxes(opponent, reverse, winner).reverse() : Boxes(opponent);
}

export default Opponent;
