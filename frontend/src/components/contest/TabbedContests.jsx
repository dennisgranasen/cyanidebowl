import React, { useEffect, useState } from 'react';
import { Tab, TabList, TabPanel, TabPanels, Tabs, useBreakpointValue } from '@chakra-ui/react';
import comparators from '../../util/Comparators';
import config from '../../config';
import Contests from './Contests';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';

const { smallScreenBreakpointValues } = config;

function getDateFromUUID(uuid) {
  const timestampHex = `${uuid.substr(15, 3)}${uuid.substr(9, 4)}${uuid.substr(0, 8)}`;
  const timestamp = parseInt(timestampHex, 16);
  let seconds = timestamp / (10 * 1000 * 1000);
  // we can convert this to unix time by subtracting the number of seconds between that date and January 1, 1970
  seconds -= 141427 * 24 * 60 * 60;
  return new Date(seconds * 1000);
}

function getRobinFrom(contests) {
  if (!contests || contests[0].format !== 'RoundRobin' || contests[0].format !== 'Ladder') return null;
  contests.sort((contestA, contestB) =>
    comparators.compareAsDatesAsc(getDateFromUUID(contestA.contestUuid), getDateFromUUID(contestB.contestUuid))
  );
  const { coachName } = contests[0].opponents[0];
  return coachName;
}

function toTabData(contests, isSmallScreen) {
  const groupedContests = contests ? Map.groupBy(contests, (contest) => contest.round) : null;
  const tabData = [];
  const robin = groupedContests ? getRobinFrom(groupedContests.get(1)) : null;
  groupedContests?.forEach((contestsForGroup, key) => {
    contestsForGroup.sort((contestA, contestB) => {
      let comparison;
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
      content: <Contests contests={contestsForGroup} />,
    });
  });
  tabData.sort((tab1, tab2) => tab1.round - tab2.round);
  return tabData;
}

function TabbedContests({ contests, currentRound, loading, error }) {
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);
  const [tabData, setTabData] = useState([]);
  const [activatedTab, setActivatedTab] = useState(currentRound ? currentRound - 1 : 0);
  const handleTabsChange = (index) => {
    setActivatedTab(index);
  };

  useEffect(() => {
    if (contests) {
      const data = toTabData(contests, isSmallScreen);
      setTabData(data);
    }
  }, [contests, isSmallScreen]);

  return (
    <LoadingOrErrorWrapper loading={loading} error={error}>
      <Tabs isFitted align="center" index={activatedTab} onChange={handleTabsChange}>
        <TabList>
          {tabData?.map((tab) => (
            <Tab key={tab.round}>{tab.label}</Tab>
          ))}
        </TabList>
        <TabPanels>
          {tabData?.map((tab) => (
            <TabPanel key={tab.round}>{tab.content}</TabPanel>
          ))}
        </TabPanels>
      </Tabs>
    </LoadingOrErrorWrapper>
  );
}

export default TabbedContests;
