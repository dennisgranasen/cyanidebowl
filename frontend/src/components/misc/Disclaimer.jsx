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
        <Text fontSize={textSize}>
          <a href="https://www.buymeacoffee.com/d.rock"><img src="https://img.buymeacoffee.com/button-api/?text=Sponsra blaskan!&emoji=&slug=d.rock&button_colour=40DCA5&font_colour=ffffff&font_family=Bree&outline_colour=000000&coffee_colour=FFDD00" /></a>
        </Text>
        <Text>
          <script data-name="BMC-Widget" data-cfasync="false" src="https://cdnjs.buymeacoffee.com/1.0.0/widget.prod.min.js" data-id="d.rock" data-description="Support me on Buy me a coffee!" data-message="Tack för kaffet!" data-color="#40DCA5" data-position="Right" data-x_margin="18" data-y_margin="18"></script>
        </Text>
      </VStack>
    </Box>
  );
}

export default Disclaimer;
