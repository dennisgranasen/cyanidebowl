import React from 'react';
import formatter from '../../util/formatter';
import InfoArea from '../common/InfoArea';
import InfoItem from '../common/InfoItem';

function LeagueInfo({ league, competitionCountByStatus }) {
  console.log('Rendering LeagueInfo with league:', league);
  console.log('LI cc by status:', competitionCountByStatus);
  return (
    league && (
      <InfoArea w="100%">
        <InfoItem key="teams" label="Teams" info={league?.teamCount} />
        <InfoItem
          key="inProgress"
          label="Active Competitions"
          info={competitionCountByStatus?.InProgress || '-'}
        />
        <InfoItem
          key="registration"
          label="Competitions in registration"
          info={competitionCountByStatus?.Registration || '-'}
        />
        <InfoItem
          key="finished"
          label="Finished competitions"
          info={competitionCountByStatus?.Finished || '-'}
        />
        {competitionCountByStatus?.Unknown && <InfoItem
          key="unknown"
          label="Unknown competitions"
          info={competitionCountByStatus?.Unknown || '-'}
        />}
        <InfoItem key="lastMatch" label="Last match" info={formatter.formatAsDate(league.dateLastMatch, '-')} />
      </InfoArea>
    )
  );
}

export default LeagueInfo;
