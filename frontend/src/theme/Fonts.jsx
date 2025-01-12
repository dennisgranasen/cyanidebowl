import React from 'react';
import { Global } from '@emotion/react';

function Fonts() {
  return (
    <Global
      styles={`
     @font-face {
        font-family: 'EmbeddedNuffle';
        font-style: normal;
        src: url('/fonts/Nuffle.otf') format('truetype');
      }
      @font-face {
        font-family: 'EmbeddedNuffleItalic';
        font-style: normal;
        src: url('/fonts/NuffleItalic.otf') format('truetype');
      }
      @font-face {
        font-family: 'EmbeddedNuffleDice';
        font-style: normal;
        src: url('/fonts/NuffleDice.otf') format('truetype');
      }
      @font-face {
        font-family: 'EmbeddedBigStarRegular';
        font-style: normal;
        src: url('/fonts/BigStarRegular.otf') format('opentype');
      },
      @font-face {
        font-family: 'EmbeddedSportsWorldRegular';
        font-style: normal;
        src: url('/fonts/SportsWorldRegular.otf') format('opentype');
      },
      @font-face {
        font-family: 'EmbeddedSportsWorldRegular2';
        font-style: normal;
        src: url('/fonts/SportsWorldRegular.ttf') format('truetype');
      }
      `}
    />
  );
}
export default Fonts;
