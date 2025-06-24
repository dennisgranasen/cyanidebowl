import React from 'react';
import formatter from '../../util/formatter';
import InfoArea from '../common/InfoArea';
import InfoItem from '../common/InfoItem';

function LeagueInfo({ league }) {
  console.log('Rendering LeagueInfo with league:', league);
  return (
    league && (
      <InfoArea w="100%">
        <InfoItem key="teams" label="Teams" info={league?.teamCount} />
        <InfoItem
          key="inProgress"
          label="Active Competitions"
          info={league?.countsByCompetitionStatus?.InProgress || '-'}
        />
        <InfoItem
          key="registration"
          label="Competitions in registration"
          info={league?.countsByCompetitionStatus?.Registration || '-'}
        />
        <InfoItem
          key="finished"
          label="Finished competitions"
          info={league?.countsByCompetitionStatus?.Finished || '-'}
        />
        <InfoItem key="lastMatch" label="Last match" info={formatter.formatAsDate(league.dateLastMatch, '-')} />
      </InfoArea>
    )
  );
}

export default LeagueInfo;
