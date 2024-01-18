import React from 'react';
import Injury from "./Injury";
import {Stack} from "@chakra-ui/react";

function Injuries({injuries}) {
    const injuryCountMap = [];
    if (injuries) {
        injuries.forEach(
            (injury) => {
                injuryCountMap[injury] = (injuryCountMap[injury] || 0) + 1;
        });
    }
    return <Stack direction='row' spacing="2px">
        {
            Object.entries(injuryCountMap).map(([injury, count], index) => {
                return <Injury key={index} injury={injury} count={count}/>
                }
            )
        }
    </Stack>;
}

export default Injuries;

