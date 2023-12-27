import React from 'react';
import {Image, Tag, TagLabel, Tooltip} from "@chakra-ui/react";
import prettyPrint from "../util/PrettyPrint";
import {QuestionOutlineIcon} from "@chakra-ui/icons";

const boxSize = "24px";

const imageOrIconFor = (injury) => {
    let imageName = "";
    switch (injury.toLowerCase()) {
        case "groinstrain":
        case "brokenjaw":
            imageName = "seriouslyHurt";
            break;
        case "fracturedleg":
            imageName = "smashedKnee";
            break;
        case "fracturedarm":
            imageName = "brokenArm";
            break;
        default:
            imageName = injury.toLowerCase();
            break;
    }
    return <Image src={`/img/${(imageName)}.png`} alt={injury} boxSize={boxSize} fallback={<QuestionOutlineIcon/>}/>
}

function Injury({injury, count}) {
    return <Tooltip label={prettyPrint(injury)}>
        <Tag size={boxSize} borderRadius="full" ml={-1} mr={2}>
            {imageOrIconFor(injury)}
            {count > 1 ? <TagLabel>{count}</TagLabel> : "" }
        </Tag>
    </Tooltip>
}

export default Injury;

