import React from 'react';
import {
  Center,
  Tab,
  Table,
  TableContainer,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
  Tbody,
  Tfoot,
  Th,
  Thead,
  Tr, useMediaQuery,
} from '@chakra-ui/react';
import Contest from './Contest';

const TableColumns = (
  <Tr>
    <Th />
    <Th>
      <Center>Home</Center>
    </Th>
    <Th />
    <Th>
      <Center>Result</Center>
    </Th>
    <Th />
    <Th>
      <Center>Away</Center>
    </Th>
    <Th />
  </Tr>
);

function Contests({ contests, currentRound }) {
  const groupedContests = Map.groupBy(contests, (contest) => contest.round);
  const [isSmallScreen] = useMediaQuery('(max-width: 768px)');
  const tabData = [];
  groupedContests.forEach((value, key) => {
    tabData.push({
      round: key,
      label: isSmallScreen ? `${key}` : `Round ${key}`,
      content: (
        <TableContainer>
          <Table variant="simpleClickable" size="sm">
            <Thead>{TableColumns}</Thead>
            <Tbody>
              {value.map((contest) => {
                return <Contest contest={contest} key={contest.contestUuid} />;
              })}
            </Tbody>
            <Tfoot>{TableColumns}</Tfoot>
          </Table>
        </TableContainer>
      ),
    });
  });

  return (
    <Tabs isFitted defaultIndex={currentRound - 1}>
      <TabList>
        {tabData.map((tab) => (
          <Tab key={tab.round}>{tab.label}</Tab>
        ))}
      </TabList>
      <TabPanels>
        {tabData.map((tab) => (
          <TabPanel key={tab.round}>{tab.content}</TabPanel>
        ))}
      </TabPanels>
    </Tabs>
  );
}

export default Contests;
