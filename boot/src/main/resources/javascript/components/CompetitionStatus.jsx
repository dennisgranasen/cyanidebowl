import React from "react";
import prettyPrint from "../util/PrettyPrint";
import config from "../config";
import {FaAddressCard, FaFlagCheckered, FaSpinner} from "react-icons/fa6";
import {QuestionOutlineIcon} from "@chakra-ui/icons";
import {Box, Tooltip} from "@chakra-ui/react";

const boxSize = config.smallBoxSize;
const Icon = ({status}) => {
    switch (status) {
        case 'InProgress':
            return <FaSpinner boxSize={boxSize}/>
        case 'Finished':
            return <FaFlagCheckered boxSize={boxSize}/>
        case 'Registration':
            return <FaAddressCard boxSize={boxSize}/>
        default:
            return <QuestionOutlineIcon boxSize={boxSize}/>
    }
}

function CompetitionStatus({status}) {
    return <Tooltip
        label={prettyPrint(status)}><Box><Icon status={status}/></Box></Tooltip>
}

export default CompetitionStatus;
