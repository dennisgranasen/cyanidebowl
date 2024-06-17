import { CheckCircleIcon, Icon, WarningIcon } from '@chakra-ui/icons';
import React from 'react';

function StatusIcon({ status, statusOutdated }) {
  const maintenance =
    status && status.maintenance
      ? [].concat(status.maintenance.pc, status.maintenance.microsoft, status.maintenance.sony).filter((value) => value)
      : [];
  let color;
  let icon;
  if (status.overall && maintenance && maintenance.length === 0) {
    color = statusOutdated ? 'yellow' : 'green';
    icon = CheckCircleIcon;
  } else {
    color = status && status.overall ? 'orange' : 'red';
    icon = WarningIcon;
  }
  return <Icon as={icon} size="sm" color={color} />;
}

export default StatusIcon;
