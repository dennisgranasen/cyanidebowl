import React from "react";
import {QuestionOutlineIcon} from "@chakra-ui/icons";
import {Box, Image, Td, Text,} from '@chakra-ui/react'
import ImageUrls from "../ImageUrls";

const boxSize = "32px";

const Boxes = (opponent, reverse, winnerTeamUuid) => {
    const winner = winnerTeamUuid && opponent.id === winnerTeamUuid;
    const fontWeight = 'normal'; // winner ? 'bold' : 'normal';
    const textAlign = !reverse ? 'right' : 'left';
    return [<Td><Text fontWeight={fontWeight} textAlign={textAlign}>{opponent.coachName}</Text></Td>,
        <Td><Text fontWeight={fontWeight} textAlign={textAlign}>{opponent.name}</Text></Td>,
        <Td><Box align={textAlign}>
            <Image align={textAlign} src={`${ImageUrls.logo(opponent.logo)}`}
                   boxSize={boxSize}
                   fallback={<QuestionOutlineIcon
                       boxSize={boxSize}/>}
                   objectFit="scale-down"/>
        </Box>
        </Td>];

}

function Opponent({opponent, reverse, winnerTeamUuid}) {
    return <>{reverse ? Boxes(opponent, reverse, winnerTeamUuid).reverse() : Boxes(opponent)}</>
}

export default Opponent;
