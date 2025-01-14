import React from 'react';
import prettyPrint from '../../util/prettyPrint';

function Race({ race }) {
  return <>{prettyPrint(race)}</>;
}

export default Race;
