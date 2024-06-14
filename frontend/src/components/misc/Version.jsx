import { Box, HStack, VStack } from '@chakra-ui/react';
import React, { useEffect, useState } from 'react';
import CyanideApiService from '../../CyanideApiService';
import config from '../../config';
import version from '../../version.json';
import abbreviators from '../../util/Abbreviators';

function Version({ headerSize, textSize, ...props }) {
  const [backendVersion, setBackendVersion] = useState(null);
  const [frontendVersion, setFrontendVersion] = useState(null);
  const fetchBackendVersion = () => {
    CyanideApiService.backendVersion()
      .then((data) => {
        setBackendVersion(data);
      })
      .catch((reason) => {
        setBackendVersion(reason.toLocaleString(config.locale));
      });
  };
  const fetchFrontendVersion = () => {
    setFrontendVersion(version);
  };

  useEffect(() => {
    fetchBackendVersion();
    fetchFrontendVersion();
  }, []);

  return (
    <Box fontSize={headerSize} {...props}>
      <HStack align="left">
        <Box fontSize={textSize}>
          {backendVersion &&
            `B: ${backendVersion.projectVersion} (${abbreviators.abbreviateText(backendVersion.committish, 6)})`}
        </Box>
        <Box fontSize={textSize}>
          {frontendVersion &&
            `F: ${frontendVersion.projectVersion} (${abbreviators.abbreviateText(
              frontendVersion.committish,
              6
            )})`}
        </Box>
      </HStack>
    </Box>
  );
}

export default Version;
