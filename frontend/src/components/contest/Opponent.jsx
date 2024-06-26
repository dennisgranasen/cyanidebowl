import React from 'react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import { Box, Image, Td, Text, useBreakpointValue } from '@chakra-ui/react';
import ImageUrls from '../../ImageUrls';
import config from '../../config';

const { boxSize } = config;

const Boxes = (opponent, reverse, winner) => {
  const fontWeight = winner ? 'bold' : 'normal';
  const textAlign = !reverse ? 'right' : 'left';
  const isSmallScreen = useBreakpointValue({ base: true, sm: true, md: false });

  return isSmallScreen
    ? [
        <Td key={opponent.name}>
          <Text fontWeight={fontWeight} textAlign={textAlign} fontSize="xs">
            {opponent.name}
          </Text>
          <Text fontWeight={fontWeight} textAlign={textAlign} color="grey" fontSize="xs">
            {opponent.coachName}
          </Text>
        </Td>,
      ]
    : [
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

function Opponent({ opponent, reverse, winner }) {
  return reverse ? Boxes(opponent, reverse, winner).reverse() : Boxes(opponent, reverse, winner);
}

export default Opponent;
