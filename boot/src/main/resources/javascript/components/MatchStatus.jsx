import React from "react";
import {ArrowRightIcon, QuestionOutlineIcon, TimeIcon} from "@chakra-ui/icons";
import {Avatar, Tooltip,} from '@chakra-ui/react'
import ImageUrls from "../ImageUrls";
import prettyPrint from "../util/PrettyPrint";
import formatter from "../util/Formatter";
import {FaTowerBroadcast} from "react-icons/fa6";

const boxSize = "32px";
const smallBoxSize = "24px";

const getIcon = (status, stadium) => {
    switch (status) {
        case 'played':
            return <Avatar src={`${ImageUrls.stadium(stadium)}`} boxSize={boxSize}
                           icon={<QuestionOutlineIcon boxSize={boxSize}/>}/>
        case 'scheduled':
            return <TimeIcon boxSize={smallBoxSize}/>
        case 'live':
            return <FaTowerBroadcast boxSize={smallBoxSize}/>
        default:
            return <QuestionOutlineIcon boxSize={boxSize}/>
    }
}

function MatchStatus({status, matchDate, stadium}) {
    return <Tooltip label={`${prettyPrint(status)} ${formatter.formatAsDate(matchDate)}`}>{getIcon(status, stadium)}</Tooltip>
}

export default MatchStatus;
