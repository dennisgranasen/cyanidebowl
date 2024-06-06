import React from 'react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import { Box, Image, Td, Text } from '@chakra-ui/react';
import ImageUrls from '../ImageUrls';
import config from '../config';
import abbreviators from '../util/Abbreviators';

const { boxSize } = config;

const Boxes = (opponent, reverse, winner, isSmallScreen) => {
  const fontWeight = winner ? 'bold' : 'normal';
  const textAlign = !reverse ? 'right' : 'left';

  return [
    <Td key={opponent.coachName}>
      <Text fontWeight={fontWeight} textAlign={textAlign}>
        {isSmallScreen ? abbreviators.abbreviateCoachName(opponent.coachName) : opponent.coachName}
      </Text>
    </Td>,
    <Td key={opponent.name}>
      <Text fontWeight={fontWeight} textAlign={textAlign}>
        {isSmallScreen ? abbreviators.abbreviateTeamName(opponent.name) : opponent.name}
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

function Opponent({ opponent, reverse, winner, isSmallScreen }) {
  return reverse
    ? Boxes(opponent, reverse, winner, isSmallScreen).reverse()
    : Boxes(opponent, reverse, winner, isSmallScreen);
}

export default Opponent;
