import React from 'react';
import {Spinner, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr,} from '@chakra-ui/react'
import Team from "./Team";
import Competition from "./Competition";


const TableColumns = <Tr>
    <Th>League</Th>
    <Th></Th>
    <Th>Competition</Th>
    <Th>Format</Th>
    <Th>Status</Th>
    <Th isNumeric>Teams</Th>
</Tr>

function Competitions({competitions}) {

    return (
        <TableContainer>
            <Table variant='striped' size="sm">
                <Thead>
                    {TableColumns}
                </Thead>
                <Tbody>
                    {
                        competitions ?
                            competitions.map(competition => {
                                return <Competition competition={competition} key={competition.uuid}/>
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

export default Competitions;

