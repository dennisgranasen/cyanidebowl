import { CheckCircleIcon, Icon, WarningIcon } from '@chakra-ui/icons';
import React from 'react';
import { Spinner } from '@chakra-ui/react';

function StatusIcon({ status, statusOutdated }) {
  const maintenance = status?.maintenance
    ? [].concat(status.maintenance.pc, status.maintenance.microsoft, status.maintenance.sony).filter((value) => value)
    : [];
  let color;
  let icon;
  if (status?.overall && maintenance && maintenance.length === 0) {
    color = statusOutdated ? 'yellow' : 'green';
    icon = CheckCircleIcon;
  } else {
    color = status?.overall ? 'orange' : 'red';
    icon = WarningIcon;
  }
  return status ? <Icon as={icon} size="sm" color={color} /> : <Spinner size="sm" color="orange" />;
}

export default StatusIcon;
