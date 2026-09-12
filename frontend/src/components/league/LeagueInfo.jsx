import React from 'react';
import formatter from '../../util/formatter';
import InfoArea from '../common/InfoArea';
import InfoItem from '../common/InfoItem';
import { useIntl } from 'react-intl';

function LeagueInfo({ league, competitionCountByStatus }) {
  const intl = useIntl();
  return (
    league && (
      <InfoArea w="100%">
        <InfoItem key="teams" label={intl.formatMessage({ id: 'common.teams' })} info={league?.teamCount} />
        <InfoItem
          key="inProgress"
          label={intl.formatMessage({ id: 'league.activeCompetitions' })}
          info={competitionCountByStatus?.InProgress || '-'}
        />
        <InfoItem
          key="registration"
          label={intl.formatMessage({ id: 'league.registrationCompetitions' })}
          info={competitionCountByStatus?.Registration || '-'}
        />
        <InfoItem
          key="finished"
          label={intl.formatMessage({ id: 'league.finishedCompetitions' })}
          info={competitionCountByStatus?.Finished || '-'}
        />
        {competitionCountByStatus?.Unknown && <InfoItem
          key="unknown"
          label={intl.formatMessage({ id: 'league.unknownCompetitions' })}
          info={competitionCountByStatus?.Unknown || '-'}
        />}
        <InfoItem key="lastMatch" label={intl.formatMessage({ id: 'league.lastMatch' })} info={formatter.formatAsDate(league.dateLastMatch, '-')} />
      </InfoArea>
    )
  );
}

export default LeagueInfo;
