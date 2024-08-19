import { Box, useColorMode } from '@chakra-ui/react';
import { Icon } from '@chakra-ui/icons';
import { FaMoon, FaSun } from 'react-icons/fa6';
import React from 'react';
import DelayedIconTooltip from '../common/DelayedIconTooltip';

function ToggleColorModeButton() {
  const { colorMode, toggleColorMode } = useColorMode();

  return (
    <Box textAlign="right" mr="0.5rem">
      <DelayedIconTooltip shouldWrapChildren label={`Change color mode to ${colorMode === 'dark' ? 'light' : 'dark'}`}>
        <Icon as={colorMode === 'dark' ? FaSun : FaMoon} boxSize="1rem" onClick={toggleColorMode} cursor="pointer" />
      </DelayedIconTooltip>
    </Box>
  );
}

export default ToggleColorModeButton;
