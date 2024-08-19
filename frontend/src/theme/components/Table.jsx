import { defineStyleConfig } from '@chakra-ui/styled-system';

const tableTheme = defineStyleConfig({
  variants: {
    simpleClickable: {
      tbody: {
        tr: {
          borderBlock: 'thin solid',
          borderColor: 'warpScoresBorderColor',
          cursor: 'pointer',
          _hover: {
            background: 'warpScoresHoverColor',
          },
        },
      },
    },
    stripedClickable: {
      tbody: {
        tr: {
          cursor: 'pointer',
          _odd: {
            background: 'warpScoresAlternativeBackgroundColor',
          },
          _hover: {
            background: 'warpScoresHoverColor',
          },
        },
      },
    },
  },
});

export default tableTheme;
