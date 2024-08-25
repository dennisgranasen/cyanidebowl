import {Heading} from '@chakra-ui/react';
import React from 'react';
import Ranks from './Ranks';
import TabbedContests from '../contest/TabbedContests';
import Contests from "../contest/Contests";

function isLadder(competition) {
    return competition?.format.toLowerCase() === 'ladder';
}

function NoneKnockoutCompetition({ranks, contests, competition, ranksLoading, contestsLoading, competitionLoading}) {
    return (
        <>
            <Heading size="md">Ranking</Heading>
            <Ranks loading={ranksLoading} ranks={ranks}/>
            <Heading size="md">Contests</Heading>
            {isLadder(competition) ?
                (
                    <Contests
                        contestsLoading={contestsLoading}
                        contests={contests}
                        competitionLoading={competitionLoading}
                        competition={competition}
                    />
                ) :
                (<TabbedContests
                        contestsLoading={contestsLoading}
                        contests={contests}
                        competitionLoading={competitionLoading}
                        competition={competition}
                    />
                )
            }
        </>
    );
}

export default NoneKnockoutCompetition;
