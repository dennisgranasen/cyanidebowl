import { extendTheme, theme as base } from '@chakra-ui/react';
import linkTheme from './components/Link';
import tableTheme from './components/Table';
import progressTheme from './components/Progress';

const warpScoresTheme = extendTheme({
  fonts: {
    nuffle: `'EmbeddedNuffle', sans-serif`,
    nuffleDice: `'EmbeddedNuffleDice', sans-serif`,
    bigStar: `'EmbeddedBigStarRegular', sans-serif`,
    sportsWorld: `'EmbeddedSportsWorldRegular', sans-serif`,
    heading: `'EmbeddedSportsWorldRegular', 'EmbeddedBigStarRegular',${base.fonts?.heading}, sans-serif`,
    body: `${base.fonts?.body}, sans-serif`,
  },
  semanticTokens: {
    colors: {
      warpScoresMenuTextColor: {
        default: 'black',
        _dark: 'white',
      },
      warpScoresSecondaryColor: {
        default: 'gray.500',
        _dark: 'gray.500',
      },
      warpScoresBackgroundColor: {
        default: 'gray.100',
        _dark: 'gray.700',
      },
      warpScoresBorderColor: {
        default: 'gray.100',
        _dark: 'gray.700',
      },
      warpScoresHoverColor: {
        default: 'green.100',
        _dark: 'green.700',
      },
      warpScoresTooltipBackground: {
        default: 'green.200',
        _dark: 'green.800',
      },
      warpScoresAlternativeBackgroundColor: {
        default: 'gray.100',
        _dark: 'gray.700',
      },
      warpScoresProgressBackgroundColor: {
        default: 'blackAlpha.300',
        _dark: 'whiteAlpha.300',
      },
      warpScoresProgressColor: {
        default: 'blue.500',
        _dark: 'blue.200',
      },
      warpScoresValidationNeededProgressColor: {
        default: 'orange.400',
        _dark: 'orange.200',
      },
    },
  },
  components: {
    Link: linkTheme,
    RouteLink: linkTheme,
    Table: tableTheme,
    Progress: progressTheme,
  },
});

export default warpScoresTheme;
