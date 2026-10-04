import React, { useState } from 'react';
import { Button, ButtonGroup, Center, Spinner, Table, TableContainer, Tbody, Tfoot, Th, Thead, Tr } from '@chakra-ui/react';
import Player from './Player';
import { identityUtils } from '../../util/identityUtil';
import { useIntl } from 'react-intl';

function TableColumns({ view }) {
  const intl = useIntl();
  return <Tr>
    <Th>#</Th>
    <Th>{intl.formatMessage({ id: 'common.name' })}</Th>
    <Th>{intl.formatMessage({ id: 'common.type' })}</Th>
    <Th>
      <Center>{intl.formatMessage({ id: 'common.level' })}</Center>
    </Th>
    {view === 'career' ? (
      <>
        <Th><Center>{intl.formatMessage({ id: 'team.roster.games' })}</Center></Th>
        <Th><Center>{intl.formatMessage({ id: 'team.roster.sppEarned' })}</Center></Th>
        <Th><Center>{intl.formatMessage({ id: 'team.roster.touchdowns' })}</Center></Th>
        <Th><Center>{intl.formatMessage({ id: 'team.roster.casualties' })}</Center></Th>
        <Th><Center>{intl.formatMessage({ id: 'team.roster.mvps' })}</Center></Th>
        <Th><Center>{intl.formatMessage({ id: 'team.roster.passes' })}</Center></Th>
        <Th><Center>{intl.formatMessage({ id: 'team.roster.catches' })}</Center></Th>
        <Th><Center>{intl.formatMessage({ id: 'team.roster.blocks' })}</Center></Th>
        <Th><Center>{intl.formatMessage({ id: 'team.roster.knockouts' })}</Center></Th>
        <Th><Center>{intl.formatMessage({ id: 'team.roster.interceptions' })}</Center></Th>
      </>
    ) : <Th>{intl.formatMessage({ id: 'common.skills' })}</Th>}
    <Th>{intl.formatMessage({ id: 'common.injuries' })}</Th>
    <Th>
      <Center>MNG</Center>
    </Th>
    {view === 'career' ? null : <>
      <Th><Center>MA</Center></Th>
      <Th><Center>ST</Center></Th>
      <Th><Center>AG</Center></Th>
      <Th><Center>PA</Center></Th>
      <Th><Center>AV</Center></Th>
    </>}
    <Th><Center>{intl.formatMessage({ id: 'team.roster.spp' })}</Center></Th>
    <Th isNumeric>{intl.formatMessage({ id: 'common.value' })}</Th>
  </Tr>
}

function Roster({ players, careerStats }) {
  const intl = useIntl();
  const [view, setView] = useState('attributes');
  return (
    <>
      <ButtonGroup isAttached size="sm" variant="outline" mb={3} aria-label={intl.formatMessage({ id: 'team.roster.display' })}>
        <Button
          colorScheme={view === 'attributes' ? 'teal' : undefined}
          variant={view === 'attributes' ? 'solid' : 'outline'}
          onClick={() => setView('attributes')}
          aria-pressed={view === 'attributes'}
        >
          {intl.formatMessage({ id: 'team.roster.attributesAndSkills' })}
        </Button>
        <Button
          colorScheme={view === 'career' ? 'teal' : undefined}
          variant={view === 'career' ? 'solid' : 'outline'}
          onClick={() => setView('career')}
          aria-pressed={view === 'career'}
        >
          {intl.formatMessage({ id: 'team.roster.careerStats' })}
        </Button>
      </ButtonGroup>
      <TableContainer width="100%">
        <Table variant="striped" size="sm">
          <Thead><TableColumns view={view} /></Thead>
          <Tbody>
            {Array.isArray(players) ? (
              players.map((player) => {
                const id = String(player.id?.key || `${player.number}:${player.name || ''}`).toLocaleLowerCase();
                return <Player player={player} careerStats={careerStats?.get(id)} view={view}
                  key={player.id?.key || id} opus={identityUtils.opus(player.id)} />;
              })
            ) : (
              <Spinner />
            )}
          </Tbody>
          <Tfoot><TableColumns view={view} /></Tfoot>
        </Table>
      </TableContainer>
    </>
  );
}

export default Roster;
