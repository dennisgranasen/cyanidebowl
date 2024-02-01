import React, { useEffect, useState } from 'react';
import { CheckCircleIcon, WarningIcon } from '@chakra-ui/icons';
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
} from '@chakra-ui/react';
import { FaDatabase, FaDesktop, FaPlaystation, FaSitemap, FaXbox } from 'react-icons/fa6';
import CyanideApiService from '../CyanideApiService';
import config from '../config';
import DelayedIconTooltip from './DelayedIconTooltip';

function StatusIcon({ status, maintenance }) {
  if (status && maintenance && maintenance.length === 0) {
    return <CheckCircleIcon size="sm" color="green" />;
  }
  const color = status ? 'orange' : 'red';
  return <WarningIcon size="sm" color={color} />;
}

const getColor = (status) => {
  return status ? 'green' : 'red';
};

const getMaintenanceColor = (maintenanceStatus) => {
  return maintenanceStatus.length === 0 ? 'grey' : 'orange';
};

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

  return status === null ? (
    <Spinner size="sm" color="orange" />
  ) : (
    <Popover>
      <PopoverTrigger>
        <Link>
          <StatusIcon
            status={status.overall}
            maintenance={
              status.maintenance
                ? [].concat(status.maintenance.pc, status.maintenance.microsoft, status.maintenance.sony)
                : []
            }
          />
        </Link>
      </PopoverTrigger>
      <PopoverContent>
        <PopoverArrow />
        <PopoverCloseButton />
        <PopoverHeader>Cyanide Api-Status</PopoverHeader>
        <PopoverBody>
          <HStack spacing={2}>
            <Box>Overall:</Box>
              <HStack spacing={2}>
                <StatusIcon
                  status={status.overall}
                  maintenance={
                    status.maintenance
                      ? [].concat(status.maintenance.pc, status.maintenance.microsoft, status.maintenance.sony)
                      : []
                  }
                />
              </HStack>
          </HStack>
          <HStack spacing={2}>
            <Box>Maintenance:</Box>
            <HStack spacing={2}>
              {status.maintenance ? (
                <>
                  <FaDesktop color={getMaintenanceColor(status.maintenance.pc)} />
                  <FaXbox color={getMaintenanceColor(status.maintenance.microsoft)} />
                  <FaPlaystation color={getMaintenanceColor(status.maintenance.sony)} />
                </>
              ) : null}
            </HStack>
          </HStack>
        </PopoverBody>
        <PopoverFooter>Last check: {status && status.lastCheck ? status.lastCheck : status}</PopoverFooter>
      </PopoverContent>
    </Popover>
  );
}

export default Status;
