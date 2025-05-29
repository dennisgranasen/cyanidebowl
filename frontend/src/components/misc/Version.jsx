import { Box, HStack, Spacer } from '@chakra-ui/react';
import React, { useEffect, useState } from 'react';
import WarpScoresApiService from '../../WarpScoresApiService';
import abbreviators from '../../util/abbreviators';
import logger from '../../util/logger';

const frontendVersionFile = '/version.json';

function Version({ textSize, ...props }) {
  const [backendVersion, setBackendVersion] = useState(null);
  const [frontendVersion, setFrontendVersion] = useState(null);
  const fetchBackendVersion = () => {
    WarpScoresApiService.backendVersion()
      .then((data) => {
        setBackendVersion(data);
      })
      .catch((reason) => {
        logger.debug('Could not load backend version, %o.', frontendVersionFile, reason);
        setBackendVersion(null);
      });
  };

  const handleError = (response) => {
    if (!response.ok) {
      throw Error(response.statusText);
    } else {
      return response.json();
    }
  };

  const loadFrontendVersion = () => {
    fetch(`${frontendVersionFile}`, { headers: { 'Content-Type': 'application/json', Accept: 'application/json' } })
      .then(handleError)
      .then(setFrontendVersion)
      .catch((reason) => {
        logger.debug('Could not load frontend version from file %s, %o.', frontendVersionFile, reason);
        setFrontendVersion(null);
      });
  };

  useEffect(() => {
    fetchBackendVersion();
    loadFrontendVersion();
  }, []);

  return (
    <HStack gap={2}>
      {backendVersion?.projectVersion && (
        <Box fontSize={textSize} {...props} pb={2} bottomwidth="1px">
          {`B: ${backendVersion.projectVersion} (${abbreviators.abbreviateText(backendVersion.committish, 6)})`}
        </Box>
      )}
      <Spacer />
      {frontendVersion?.projectVersion && (
        <Box fontSize={textSize} {...props} pb={2} bottomwidth="1px">
          {`F: ${frontendVersion.projectVersion} (${abbreviators.abbreviateText(frontendVersion.committish, 6)})`}
        </Box>
      )}
    </HStack>
  );
}

export default Version;
