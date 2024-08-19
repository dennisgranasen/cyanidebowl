import { defineStyle, defineStyleConfig } from '@chakra-ui/styled-system';

const baseStyle = defineStyle({
  textColor: 'green.600',
  _hover: {
    textDecoration: 'underline',
    textDecorationStyle: 'dotted',
  },
});

const menuStyle = defineStyle({
  textColor: 'warpScoresMenuTextColor',
  textDecoration: 'none',
});

const linkTheme = defineStyleConfig({
  baseStyle,
  variants: {
    menu: menuStyle,
  },
});

export default linkTheme;
