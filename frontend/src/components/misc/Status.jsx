import { Box, HStack, Spacer, Text, VStack } from '@chakra-ui/react';
import { Icon } from '@chakra-ui/icons';
import { FaDatabase, FaDesktop, FaPlaystation, FaRegAddressBook, FaXbox } from 'react-icons/fa6';
import React from 'react';
import StatusIcon from './StatusIcon';

const isMaintenance = (maintenanceStatus) => {
  return maintenanceStatus && maintenanceStatus.length > 0;
};

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

function Status({ status, headerFontSize, fontSize, color }) {
  return (
    <VStack align="left">
      <Text fontStyle="italic" fontSize={headerFontSize} color={color}>
        Cyanide Api Status
      </Text>
      <HStack align="left">
        <Box fontSize={fontSize} color={color}>
          Overall:
        </Box>
        <Spacer />
        <StatusIcon status={status} />
      </HStack>
      {status?.serviceStatuses && (
        <HStack spacing={2} align="left">
          <Box fontSize={fontSize} color={color}>
            Game Server:
          </Box>
          <Spacer />
          <Icon as={FaDatabase} color={status.serviceStatuses.game_server_database ? 'green' : 'red'} />
          <Icon as={FaRegAddressBook} color={status.serviceStatuses.game_server_address_directory ? 'green' : 'red'} />
        </HStack>
      )}
      {status && (
        <HStack spacing={2} align="left">
          <Box fontSize={fontSize} color={color}>
            Platforms:
          </Box>
          <Spacer />
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
      )}
    </VStack>
  );
}

export default Status;
