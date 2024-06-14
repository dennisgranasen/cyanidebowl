import { Box, Link, Text, VStack } from '@chakra-ui/react';
import React from 'react';

function Disclaimer({ headerSize, textSize, ...props }) {
  return (
    <Box fontSize={headerSize} {...props}>
      <Text fontStyle="italic">Disclaimer</Text>
      <VStack align="left">
        <Text fontSize={textSize}>
          This site is completely unofficial and not affiliated with Cyanide, Nacon or Games Workshop.
        </Text>
        <Text fontSize={textSize}>
          Blood Bowl, BB3 and probably a lot more names are trademarks of their respective owners. Used without
          permission. No challenge to their status intended.
        </Text>
        <Text fontSize={textSize}>
          Page maintained by{' '}
          <Link href="mailto:naytsyrhc@gmx.org" isExternal>
            Naytsyrhc
          </Link>
        </Text>
      </VStack>
    </Box>
  );
}

export default Disclaimer;
