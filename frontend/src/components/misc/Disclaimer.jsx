import { Box, Link, Text, VStack } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import React from 'react';

function Disclaimer({ headerSize, textSize, ...props }) {
  return (
    <Box fontSize={headerSize} {...props}>
      <Text fontStyle="italic">Disclaimer</Text>
      <VStack align="left">
        <Text fontSize={textSize}>
          This site is completely unofficial and not affiliated with Cyanide, Nacon, Slitherine or Games Workshop.
        </Text>
        <Text fontSize={textSize}>
          Blood Bowl, BB3 and probably a lot more names are trademarks of their respective owners. Used without
          permission. No challenge to their status intended.
        </Text>
        <Text fontSize={textSize}>
          This work is based heavily on <Link href="https://warp-scores.net" isExternal>Warp-Scores</Link> by Naytsyrhc.
        </Text>
        <Text fontSize={textSize}>
          Page maintained by{' '}
          <Link href="mailto:dennis.granasen@gmail.com" isExternal>
            d-rock
          </Link>
        </Text>
        <Text fontSize={textSize}>
          Please also check{' '}
          <Link as={RouteLink} to="/terms.md">
            Terms
          </Link>{' '}
          and{' '}
          <Link as={RouteLink} to="/privacy.md">
            Privacy Policy
          </Link>
          .
        </Text>
      </VStack>
    </Box>
  );
}

export default Disclaimer;
