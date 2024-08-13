import { defineStyleConfig } from '@chakra-ui/styled-system';

const tableTheme = defineStyleConfig({
  variants: {
    simpleClickable: {
      tbody: {
        tr: {
          borderBlock: 'thin solid',
          borderColor: 'gray.700',
          cursor: 'pointer',
          _hover: {
            background: 'gray.600',
          },
        },
      },
    },
    stripedClickable: {
      tbody: {
        tr: {
          cursor: 'pointer',
          _odd: {
            background: 'gray.700',
          },
          _hover: {
            background: 'gray.600',
          },
        },
      },
    },
  },
});

export default tableTheme;
