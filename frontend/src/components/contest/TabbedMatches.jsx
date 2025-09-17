import React, { useEffect, useState } from 'react';
import { Tab, TabList, TabPanel, TabPanels, Tabs, useBreakpointValue } from '@chakra-ui/react';
import comparators from '../../util/comparators';
import config from '../../config';
import Matches from './Matches';
import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';

const { smallScreenBreakpointValues } = config;

function getRobinFrom(matches) {
  if (!matches || matches[0].format !== 'RoundRobin' || matches[0].format !== 'Ladder') return null;
  matches.sort(comparators.compareContestsByDateAsc);
  const { coachName } = contests[0].opponents[0];
  return coachName;
}

function toTabData(matches, isSmallScreen) {
  matches.forEach((match) => {
    console.log("sorting match",match);
    match.teams && match.teams.forEach((team) => {
      if (team.players) {
        team.players = team.players.sort((playerA, playerB) => playerA.number - playerB.number);
        console.log('toTabData sorted players', team.players);
      }
    });
  });
  const groupedMatches = matches ? Map.groupBy(matches, (match) => match.round) : null;
  const tabData = [];
  const robin = groupedMatches ? getRobinFrom(groupedMatches.get(1)) : null;

  groupedMatches?.forEach((matchesForGroup, key) => {
    matchesForGroup.sort((matchA, matchB) => {
      let comparison;
      if (robin) {
        comparison =
          robin === matchA.opponents[0].coachName
            ? -1
            : matchA.opponents[0].coachName.localeCompare(matchB.opponents[0].coachName);
      } else {
        comparison = comparators.compareMatchesByDateAsc(matchA, matchB);
      }
      return comparison;
    });

    tabData.push({
      round: key,
      label: isSmallScreen ? `${key}` : `Round ${key}`,
      content: <Matches matches={matchesForGroup} />,
    });

  });
  tabData.sort((tab1, tab2) => tab1.round - tab2.round);
  return tabData;
}

function TabbedMatches({ matches, currentRound, loading, error }) {
  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);
  const [tabData, setTabData] = useState([]);
  const [activatedTab, setActivatedTab] = useState(currentRound ? currentRound - 1 : 0);
  const handleTabsChange = (index) => {
    setActivatedTab(index);
  };

  useEffect(() => {
    if (matches) {
      const data = toTabData(matches, isSmallScreen);
      setTabData(data);
      console.log('TabbedMatches useEffect setTabData', data);
    }
  }, [matches, isSmallScreen]);

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

export default TabbedMatches;
