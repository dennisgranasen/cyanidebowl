import React from 'react';
import {Center, Spinner, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr,} from '@chakra-ui/react'
import Contest from "./Contest";


const TableColumns = <Tr>
    <Th><Center>Round</Center></Th>
    <Th><Center>Match Status</Center></Th>
    <Th/>
    <Th><Center>Home</Center></Th>
    <Th/>
    <Th><Center>Result</Center></Th>
    <Th/>
    <Th><Center>Away</Center></Th>
    <Th/>
</Tr>
function Contests({contests}) {
    return (
        <TableContainer>
            <Table variant='simple' size="sm">
                <Thead>
                    {TableColumns}
                </Thead>
                <Tbody>
                    {
                        contests ?
                            contests.map(contest => {
                                return <Contest contest={contest} key={contest.contestUuid}/>
                            })
                            : <Spinner/>
                    }
                </Tbody>
                <Tfoot>
                    {TableColumns}
                </Tfoot>
            </Table>
        </TableContainer>
    );
}

export default Contests;

