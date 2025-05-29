import React from 'react';
import { QuestionOutlineIcon } from '@chakra-ui/icons';
import { Box, Image, Td, Text, useBreakpointValue } from '@chakra-ui/react';
import imageUrls from '../../imageUrls';
import config from '../../config';
import prettyPrint from '../../util/prettyPrint';

const { boxSize, smallScreenBreakpointValues } = config;

const Boxes = (opponent, reverse, winner) => {
  const fontWeight = winner ? 'bold' : 'normal';
  const textAlign = !reverse ? 'right' : 'left';
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);

  return isSmallScreen
    ? [
        <Td key={opponent.name}>
          <Text fontWeight={fontWeight} textAlign={textAlign} fontSize="xs">
            {opponent.name}
          </Text>
          <Text fontWeight={fontWeight} textAlign={textAlign} color="grey" fontSize="xs">
            {reverse
              ? `(${opponent.coachName}) ${prettyPrint(opponent.race)}`
              : `${prettyPrint(opponent.race)} (${opponent.coachName})`}
          </Text>
        </Td>,
      ]
    : [
        <Td key={opponent.coachName}>
          <Text fontWeight={fontWeight} textAlign={textAlign}>
            {reverse
              ? `(${opponent.coachName}) ${prettyPrint(opponent.race)}`
              : `${prettyPrint(opponent.race)} (${opponent.coachName})`}
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
              src={`${imageUrls.logo(opponent.logo)}`}
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
