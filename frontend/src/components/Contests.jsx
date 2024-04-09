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
  Tfoot,
  Th,
  Thead,
  Tr,
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
  const tabData = [];
  groupedContests.forEach((value, key) => {
    tabData.push({
      round: key,
      label: `Round ${key}`,
      content: (
        <TableContainer>
          <Table variant="simple" size="sm">
            <Thead>{TableColumns}</Thead>
            {value.map((contest) => {
              return <Contest contest={contest} key={contest.contestUuid} />;
            })}
            <Tfoot>{TableColumns}</Tfoot>
          </Table>
        </TableContainer>
      ),
    });
  });

  console.log(`Current round: ${currentRound}`);

  return (
    <Tabs defaultIndex={currentRound - 1}>
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
