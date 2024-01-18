import React from "react";
import {TimeIcon} from "@chakra-ui/icons";
import {FaTowerBroadcast} from "react-icons/fa6";

const MatchStatusIcon = ({status, boxSize}) => {
    switch (status) {
        case 'scheduled':
            return <TimeIcon boxSize={boxSize}/>
        case 'live':
            return <FaTowerBroadcast boxSize={boxSize}/>
        default:
            return <></>
    }
}

export default MatchStatusIcon;
