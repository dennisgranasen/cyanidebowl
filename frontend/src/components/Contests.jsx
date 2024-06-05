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
  Tr,
  useMediaQuery,
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

function getDateFromUUID(uuid) {
  const timestampHex = `${uuid.substr(15, 3)}${uuid.substr(9, 4)}${uuid.substr(0, 8)}`;
  const timestamp = parseInt(timestampHex, 16);
  let seconds = timestamp / (10 * 1000 * 1000);
  // we can convert this to unix time by subtracting the number of seconds between that date and January 1, 1970
  seconds -= 141427 * 24 * 60 * 60;
  return new Date(seconds * 1000);
}

function getRobinFrom(contests) {
  if (contests[0].format !== 'RoundRobin') return null;

  contests.sort((contestA, contestB) => getDateFromUUID(contestA.contestUuid) > getDateFromUUID(contestB.contestUuid));
  const { coachName } = contests[0].opponents[0];
  return coachName;
}

function Contests({ contests, currentRound }) {
  const groupedContests = Map.groupBy(contests, (contest) => contest.round);
  const [isSmallScreen] = useMediaQuery('(max-width: 768px)');
  const tabData = [];
  const robin = getRobinFrom(groupedContests.get(1));
  groupedContests.forEach((value, key) => {
    value.sort((contestA, contestB) => {
      let comparison = 0;
      if (robin) {
        comparison =
          robin === contestA.opponents[0].coachName
            ? -1
            : contestA.opponents[0].coachName.localeCompare(contestB.opponents[0].coachName);
      } else if (contestA.matchDate) {
        if (contestB.matchDate) {
          comparison = contestA.matchDate - contestB.matchDate;
        } else {
          comparison = -1;
        }
      } else {
        comparison = getDateFromUUID(contestA.contestUuid) - getDateFromUUID(contestB.contestUuid);
      }
      return comparison;
    });
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
