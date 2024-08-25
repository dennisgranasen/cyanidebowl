import React, { useEffect, useState } from 'react';
import {
  Table,
  TableContainer,
  Tbody,
  Tfoot,
  Th,
  Thead,
  Tr,
  useBreakpointValue,
} from '@chakra-ui/react';
import Contest from './Contest';
import config from '../../config';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';

const { smallScreenBreakpointValues } = config;

function HeaderColumn({ title }) {
  return (
    <Th textAlign="center">
      {title}
    </Th>
  );
}

function SmallTableColumns() {
  return (
    <Tr>
      <HeaderColumn title="Home" />
      <HeaderColumn title="Result" />
      <HeaderColumn title="Away" />
    </Tr>
  );
}

function NormalTableColumns() {
  return (
    <Tr>
      <Th />
      <HeaderColumn title="Home" />
      <Th />
      <HeaderColumn title="Result" />
      <Th />
      <HeaderColumn title="Away" />
      <Th />
    </Tr>
  );
}

function TableColumns() {
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);

  return isSmallScreen ? <SmallTableColumns /> : <NormalTableColumns />;
}

function Contests({ contests, contestsLoading, competitionLoading }) {
  const [loading, setLoading] = useState(contestsLoading || competitionLoading);

  useEffect(() => {
    setLoading(contestsLoading || competitionLoading);
  }, [contestsLoading, competitionLoading]);

  return (
    <LoadingOrErrorWrapper loading={loading}>
      <TableContainer>
        <Table variant="simpleClickable" size="sm">
          <Thead>
            <TableColumns />
          </Thead>
          <Tbody>
            {contests.map((contest) => {
              return <Contest contest={contest} key={contest.contestUuid} />;
            })}
          </Tbody>
          <Tfoot>
            <TableColumns />
          </Tfoot>
        </Table>
      </TableContainer>
    </LoadingOrErrorWrapper>
  );
}

export default Contests;
