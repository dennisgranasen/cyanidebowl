import React from 'react';
import {Spinner, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr,} from '@chakra-ui/react'
import Team from "./Team";


const TableColumns = <Tr>
    <Th>Team-Name</Th>
    <Th></Th>
    <Th>Competition</Th>
    <Th>Coach-Name</Th>
    <Th>Fraction</Th>
    <Th>CTV</Th>
    <Th>Cash</Th>
</Tr>

function Teams({teams}) {

    return (
        <TableContainer>
            <Table variant='striped' size="sm">
                <Thead>
                    {TableColumns}
                </Thead>
                <Tbody>
                    {
                        teams ?
                            teams.map(({team, coach}) => {
                                return <Team team={team} key={team.id} coach={coach}/>
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

export default Teams;

