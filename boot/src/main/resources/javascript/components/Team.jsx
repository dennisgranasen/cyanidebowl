import React from 'react';
import {Image, Spinner, Td, Tr,} from '@chakra-ui/react'
import {Link as RouteLink} from 'react-router-dom'
import Fraction from './Fraction'
import {QuestionOutlineIcon} from "@chakra-ui/icons";

const boxSize= "32px";

function Team({team, coach}) {
    return (
        team !== null ?
        <Tr>
            <Td><RouteLink to={`/team/${team.id}`}>{team.name}</RouteLink></Td>
            <Td><RouteLink to={`/team/${team.id}`}><Image src={`http://localhost:8080/img/logo/${team.logo}`}
                                                          boxSize={boxSize} fallback={<QuestionOutlineIcon boxSize={boxSize}/>} objectFit="scale-down"/></RouteLink></Td>
            <Td>{ team.competitionName }</Td>
            <Td>{ coach !== null ? coach.name : <Spinner/>}</Td>
            <Td><Fraction fraction={team.fraction}/></Td>
            <Td>{team.value !== null ? team.value.toLocaleString("en-UK") : ""}</Td>
            <Td>{team.cash !== null ? team.cash.toLocaleString("en-UK") : ""}</Td>
        </Tr> : <Spinner />
    );
}

export default Team;
