import React, { useEffect, useState } from 'react';
import { CheckCircleIcon, Icon, WarningIcon } from '@chakra-ui/icons';
import {
  Box,
  HStack,
  Link,
  Popover,
  PopoverArrow,
  PopoverBody,
  PopoverCloseButton,
  PopoverContent,
  PopoverFooter,
  PopoverHeader,
  PopoverTrigger,
  Spinner,
  VStack,
} from '@chakra-ui/react';
import { FaDatabase, FaDesktop, FaPlaystation, FaRegAddressBook, FaTriangleExclamation, FaXbox } from 'react-icons/fa6';
import CyanideApiService from '../../CyanideApiService';
import config from '../../config';
import formatter from '../../util/Formatter';
import timeUtil from '../../util/TimeUtil';
import NewsList from './NewsList';
import SocialLinks from './SocialLinks';
import Disclaimer from './Disclaimer';

const MAX_AGE_FOR_STATUS_IN_MILLIS = 20 * 60 * 1_000; // 20 Minutes

const isMaintenance = (maintenanceStatus) => {
  return maintenanceStatus && maintenanceStatus.length > 0;
};

function StatusIcon({ status, maintenance, statusOutdated }) {
  let color;
  let icon;
  if (status && maintenance && maintenance.length === 0) {
    color = statusOutdated ? 'yellow' : 'green';
    icon = CheckCircleIcon;
  } else {
    color = status ? 'orange' : 'red';
    icon = WarningIcon;
  }
  return <Icon as={icon} size="sm" color={color} />;
}

function PlatformIcon({ codename, status, maintenance }) {
  let color;
  let icon;

  const isPc = /pc/i.test(codename);
  const isXbox = /microsoft/i.test(codename);
  const isPlaystation = /sony/i.test(codename);

  color = status ? 'green' : 'red';
  if (maintenance) color = 'orange';

  icon = isPc ? FaDesktop : null;
  icon = isXbox ? FaXbox : icon;
  icon = isPlaystation ? FaPlaystation : icon;

  return <Icon as={icon} color={color} />;
}

function Status() {
  const [status, setStatus] = useState(null);
  useEffect(() => {
    const fetchStatus = () => {
      setStatus(null);
      CyanideApiService.status()
        .then((data) => {
          setStatus(data);
        })
        .catch((reason) => {
          setStatus(reason.toLocaleString(config.locale));
        });
    };
    fetchStatus();
  }, []);

  const maintenance =
    status && status.maintenance
      ? [].concat(status.maintenance.pc, status.maintenance.microsoft, status.maintenance.sony).filter((value) => value)
      : [];

  const statusOutdated = status && timeUtil.durationInMillis(status.lastCheck) > MAX_AGE_FOR_STATUS_IN_MILLIS;

  return status === null ? (
    <Spinner size="sm" color="orange" />
  ) : (
    <Popover>
      <PopoverTrigger>
        <Link>
          <StatusIcon status={status.overall} maintenance={maintenance} statusOutdated={statusOutdated} />
        </Link>
      </PopoverTrigger>
      <PopoverContent>
        <PopoverArrow />
        <PopoverCloseButton />
        <PopoverHeader>Cyanide Api-Status</PopoverHeader>
        <PopoverBody>
          <VStack spacing={2} align="left">
            <HStack spacing={2} align="left">
              <Box>Overall:</Box>
              <StatusIcon status={status.overall} maintenance={maintenance} />
            </HStack>
            <HStack spacing={2} align="left">
              <Box>Game Server:</Box>
              <Icon as={FaDatabase} color={status.serviceStatuses.game_server_database ? 'green' : 'red'} />
              <Icon
                as={FaRegAddressBook}
                color={status.serviceStatuses.game_server_address_directory ? 'green' : 'red'}
              />
            </HStack>
            <HStack spacing={2} align="left">
              <Box>Platforms:</Box>
              {status.platforms &&
                status.platforms.map((platform) => (
                  <PlatformIcon
                    key={platform.codename}
                    codename={platform.codename}
                    status={platform.ok}
                    maintenance={isMaintenance(status.maintenance[platform.codename])}
                  />
                ))}
            </HStack>
            {status && <NewsList news={status.news} headerSize="sm" textSize="xs" mt={2} color="grey" />}
            {status && (
              <SocialLinks socialLinks={status.socialLinks} headerSize="sm" iconSize="sm" mt={2} color="grey" />
            )}
            <Disclaimer mt={2} headerSize="sm" textSize="xs" color="grey" />
          </VStack>
        </PopoverBody>
        <PopoverFooter>
          <HStack spacing={2}>
            <Box>{`Last check: ${
              status && status.lastCheck ? formatter.formatAsDate(status.lastCheck) : 'unknown'
            } `}</Box>
            {statusOutdated && <Icon as={FaTriangleExclamation} color="yellow" size="xs" />}
            {statusOutdated && (
              <Box fontSize="xs" color="yellow">
                Outdated
              </Box>
            )}
          </HStack>
        </PopoverFooter>
      </PopoverContent>
    </Popover>
  );
}

export default Status;
