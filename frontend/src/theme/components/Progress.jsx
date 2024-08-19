import { defineStyle, defineStyleConfig } from '@chakra-ui/styled-system';

const baseStyle = defineStyle({
  track: {
    backgroundColor: 'warpScoresProgressBackgroundColor',
  },
  filledTrack: {
    backgroundColor: 'warpScoresProgressColor',
  },
});

const validationNeededStyle = defineStyle({
  filledTrack: {
    backgroundColor: 'warpScoresValidationNeededProgressColor',
  },
});

const progressTheme = defineStyleConfig({
  baseStyle,
  variants: {
    validationNeeded: validationNeededStyle,
  },
});

export default progressTheme;
