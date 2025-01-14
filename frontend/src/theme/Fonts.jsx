import React from 'react';
import { Global } from '@emotion/react';

function Fonts() {
  return (
    <Global
      styles={`
     @font-face {
        font-family: 'EmbeddedNuffle';
        src: url('/fonts/Nuffle.otf') format('truetype');
      }
      @font-face {
        font-family: 'EmbeddedNuffleItalic';
        src: url('/fonts/NuffleItalic.otf') format('truetype');
      }
      @font-face {
        font-family: 'EmbeddedNuffleDice';
        src: url('/fonts/NuffleDice.otf') format('truetype');
      }
      @font-face {
        font-family: 'EmbeddedBigStarRegular';
        src: url('/fonts/BigStarRegular.ttf') format('truetype');
      },
      @font-face {
        font-family: 'EmbeddedSportsWorldRegular';
        src: url('/fonts/SportsWorldRegular.otf') format('opentype'),
             url('/fonts/SportsWorldRegular.ttf') format('truetype');
      }
      `}
    />
  );
}

export default Fonts;
