import React from 'react';
import {Center, Image, Spinner, Td, Tr,} from '@chakra-ui/react'
import {Link as RouteLink} from 'react-router-dom'
import Fraction from './Fraction'
import {QuestionOutlineIcon} from "@chakra-ui/icons";
import Formatter from "../util/Formatter";
import ImageUrls from "../ImageUrls";

const boxSize = "32px";

function Rank({rank}) {
    return (
        rank !== null ?
            <Tr>
                <Td><RouteLink to={`/team/${rank.team.id}`}>{rank.team.name}</RouteLink></Td>
                <Td><RouteLink to={`/team/${rank.team.id}`}><Image src={`${ImageUrls.logo(rank.team.logo)}`}
                                                                   boxSize={boxSize}
                                                                   fallback={<QuestionOutlineIcon boxSize={boxSize}/>}
                                                                   objectFit="scale-down"/></RouteLink></Td>
                <Td>{rank.team.coachName}</Td>
                <Td><Fraction fraction={rank.team.fraction}/></Td>
                <Td><Center>{Formatter.formatAsNumber(rank.gamesPlayed)}</Center></Td>
                <Td><Center>{Formatter.formatAsNumber(rank.score)}</Center></Td>
                <Td><Center>{Formatter.formatAsNumber(rank.gamesWon)}</Center></Td>
                <Td><Center>{Formatter.formatAsNumber(rank.gamesDrawn)}</Center></Td>
                <Td><Center>{Formatter.formatAsNumber(rank.gamesLost)}</Center></Td>
                <Td><Center>{Formatter.formatAsNumber(rank.inflictedTouchdowns)}</Center></Td>
                <Td><Center>{Formatter.formatAsNumber(rank.sustainedTouchdowns)}</Center></Td>
                <Td><Center>{Formatter.formatAsNumber(rank.inflictedTouchdowns - rank.sustainedTouchdowns)}</Center></Td>
                <Td><Center>{Formatter.formatAsNumber(rank.inflictedCasualties)}</Center></Td>
                <Td><Center>{Formatter.formatAsNumber(rank.sustainedCasualties)}</Center></Td>
                <Td><Center>{Formatter.formatAsNumber(rank.inflictedCasualties - rank.sustainedCasualties)}</Center></Td>
            </Tr> : <Spinner/>
    );
}

export default Rank;
