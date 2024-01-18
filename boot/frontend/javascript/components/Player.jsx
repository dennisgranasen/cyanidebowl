import React from 'react';
import {Tr,Td,} from '@chakra-ui/react'
import Skills from "./Skills";
import prettyPrint from "../util/PrettyPrint";
import {FaBandage} from "react-icons/fa6";
import Injuries from "./Injuries";

function Player({player}) {
    return (
        <Tr>
            <Td>{player.number}</Td>
            <Td>{player.name}</Td>
            <Td>{prettyPrint(player.type, "_")}</Td>
            <Td><Skills skills={player.skills} /></Td>
            <Td><Injuries injuries={player.casualtiesStates}/></Td>
            <Td>{ player.suspendedNextMatch ? <FaBandage color="orange" size="24px"/> : "" }</Td>
            <Td>{player.attributes.ma}</Td>
            <Td>{player.attributes.st}</Td>
            <Td>{player.attributes.ag}+</Td>
            <Td>{player.attributes.pa ? player.attributes.pa + "+" : "-"}</Td>
            <Td>{player.attributes.av}+</Td>
            <Td>{player.value}</Td>
            <Td>{player.xp}</Td>
        </Tr>
    );
}

export default Player;

