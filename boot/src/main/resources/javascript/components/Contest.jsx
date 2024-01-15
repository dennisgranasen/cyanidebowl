import React from 'react';
import {Center, Spinner, Td, Tr,} from '@chakra-ui/react'
import MatchStatus from "./MatchStatus";
import Opponent from "./Opponent";

function Contest({contest}) {
    return (
        contest !== null ?
            <Tr>
                <Td><Center>{contest.round}</Center></Td>
                <Td><Center><MatchStatus status={contest.status} matchDate={contest.matchDate}
                                         stadium={contest.stadium}/></Center></Td>
                <Opponent opponent={contest.opponents[0]}
                          winnerTeamUuid={contest.winner ? contest.winner.team.id : null}
                          key={contest.opponents[0].id} reverse={false}/>
                <Td><Center>{contest.opponents[0].score} - {contest.opponents[1].score}</Center></Td>
                <Opponent opponent={contest.opponents[1]}
                          winnerTeamUuid={contest.winner ? contest.winner.team.id : null}
                          key={contest.opponents[1].id} reverse={true}/>
            </Tr> :
            <Spinner/>
    )
        ;
}

export default Contest;
