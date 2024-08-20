import React from 'react';
import Formatter from '../../util/Formatter';
import InfoArea from '../common/InfoArea';
import InfoItem from '../common/InfoItem';

function LeagueInfo({ league }) {
  return (
    league && (
      <InfoArea w="100%">
        <InfoItem key="teams" label="Teams" info={league.teamCount} />
        <InfoItem
          key="inProgress"
          label="Active Competitions"
          info={league.countsByCompetitionStatus.InProgress || 0}
        />
        <InfoItem
          key="registration"
          label="Competitions in registration"
          info={league.countsByCompetitionStatus.Registration || 0}
        />
        <InfoItem key="finished" label="Finished competitions" info={league.countsByCompetitionStatus.Finished || 0} />
        <InfoItem key="lastMatch" label="Last match" info={Formatter.formatAsDate(league.dateLastMatch, '-')} />
      </InfoArea>
    )
  );
}

export default LeagueInfo;
