import React from 'react';
import { Stack } from '@chakra-ui/react';
import Injury from './Injury';
import hashCode from '../../util/hashCode';

function Injuries({ injuries }) {
  const injuryCountMap = [];
  if (injuries) {
    injuries.forEach((injury) => {
      injuryCountMap[injury] = (injuryCountMap[injury] || 0) + 1;
    });
  }
  return (
    <Stack direction="row" spacing="2px">
      {Object.entries(injuryCountMap).map(([injury, count]) => {
        return <Injury key={hashCode(injury)} injury={injury} count={count} />;
      })}
    </Stack>
  );
}

export default Injuries;
