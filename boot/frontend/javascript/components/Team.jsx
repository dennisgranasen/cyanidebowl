import React from 'react';
import {Image, Spinner, Td, Tr,} from '@chakra-ui/react'
import {Link as RouteLink} from 'react-router-dom'
import Fraction from './Fraction'
import {QuestionOutlineIcon} from "@chakra-ui/icons";
import Formatter from "../util/Formatter";
import ImageUrls from "../ImageUrls";

const boxSize = "32px";

function Team({team}) {
    return (
        team !== null ?
            <Tr>
                <Td><RouteLink to={`/team/${team.id}`}>{team.name}</RouteLink></Td>
                <Td><RouteLink to={`/team/${team.id}`}><Image src={`${ImageUrls.logo(team.logo)}`}
                                                              boxSize={boxSize}
                                                              fallback={<QuestionOutlineIcon boxSize={boxSize}/>}
                                                              objectFit="scale-down"/></RouteLink></Td>
                <Td>{team.coachName}</Td>
                <Td><Fraction fraction={team.fraction}/></Td>
                <Td isNumeric>{Formatter.formatAsNumber(team.value)}</Td>
                <Td isNumeric>{Formatter.formatAsNumber(team.cash)}</Td>
            </Tr> : <Spinner/>
    );
}

export default Team;
