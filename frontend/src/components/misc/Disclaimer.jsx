import { Box, Link, Text, VStack } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import React from 'react';
import { useIntl } from 'react-intl';

function Disclaimer({ headerSize, textSize, ...props }) {
  const intl = useIntl();
  return (
    <Box fontSize={headerSize} {...props}>
      <Text fontStyle="italic">{intl.formatMessage({ id: 'disclaimer.heading' })}</Text>
      <VStack align="left">
        <Text fontSize={textSize}>{intl.formatMessage({ id: 'disclaimer.unofficial' })}</Text>
        <Text fontSize={textSize}>{intl.formatMessage({ id: 'disclaimer.trademarks' })}</Text>
        <Text fontSize={textSize}>
          {intl.formatMessage({ id: 'disclaimer.basedOnPrefix' })}{' '}
          <Link href="https://warp-scores.net" isExternal>Warp-Scores</Link>{' '}
          {intl.formatMessage({ id: 'disclaimer.basedOnSuffix' })}
        </Text>
        <Text fontSize={textSize}>
          {intl.formatMessage({ id: 'disclaimer.maintainedPrefix' })}{' '}
          <Link href="mailto:dennis.granasen@gmail.com" isExternal>d-rock</Link>.
        </Text>
        <Text fontSize={textSize}>
          {intl.formatMessage({ id: 'disclaimer.pleaseCheck' })}{' '}
          <Link as={RouteLink} to="/terms.md">{intl.formatMessage({ id: 'disclaimer.terms' })}</Link>{' '}
          {intl.formatMessage({ id: 'disclaimer.and' })}{' '}
          <Link as={RouteLink} to="/privacy.md">{intl.formatMessage({ id: 'disclaimer.privacy' })}</Link>.
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
