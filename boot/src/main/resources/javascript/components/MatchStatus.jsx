import React from "react";
import {QuestionOutlineIcon} from "@chakra-ui/icons";
import {Avatar, Tooltip,} from '@chakra-ui/react'
import ImageUrls from "../ImageUrls";
import prettyPrint from "../util/PrettyPrint";
import formatter from "../util/Formatter";
import StatusIcon from "./StatusIcon";
import config from "../config";

const boxSize = config.boxSize;
const smallBoxSize = config.smallBoxSize;

const getIcon = (status, stadium) => {
    switch (status) {
        case 'played':
            return <Avatar src={`${ImageUrls.stadium(stadium)}`} boxSize={boxSize}
                           icon={<QuestionOutlineIcon boxSize={boxSize}/>}/>
        case 'scheduled':
        case 'live':
            return <StatusIcon status={status} boxSize={smallBoxSize}/>
        default:
            return <QuestionOutlineIcon boxSize={boxSize}/>
    }
}

function MatchStatus({status, matchDate, stadium}) {
    return <Tooltip
        label={`${prettyPrint(status)} ${formatter.formatAsDate(matchDate)}`}>{getIcon(status, stadium)}</Tooltip>
}

export default MatchStatus;
