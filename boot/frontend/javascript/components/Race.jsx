import React from 'react';
import prettyPrint from '../util/PrettyPrint';

function Race({ race }) {
  return <>{prettyPrint(race)}</>;
}

export default Race;
