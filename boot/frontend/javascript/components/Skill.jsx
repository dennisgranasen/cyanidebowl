import React from 'react';
import {Box, Image, Tooltip} from "@chakra-ui/react";
import prettyPrint from "../util/PrettyPrint";
import {QuestionOutlineIcon} from "@chakra-ui/icons";
import config from "../config";
import ImageUrls from "../ImageUrls";

const boxSize= "24px";

function Skill({skill}) {
    return <Tooltip label={prettyPrint(skill)}><Box boxSize={boxSize}><Image src={`${ImageUrls.skill(skill)}`}
           alt={prettyPrint(skill)} objectFit="cover" fallback={<QuestionOutlineIcon boxSize={boxSize}/>}/></Box>
    </Tooltip>
}

export default Skill;

