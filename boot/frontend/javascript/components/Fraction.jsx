import React from 'react';
import prettyPrint from "../util/PrettyPrint";
function Fraction( { fraction } ) {

    return <>{prettyPrint(fraction)}</>
}

export default Fraction;


