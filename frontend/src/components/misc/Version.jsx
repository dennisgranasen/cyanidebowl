import { Box } from '@chakra-ui/react';
import React, { useEffect, useState } from 'react';
import CyanideApiService from '../../CyanideApiService';
import config from '../../config';
import abbreviators from '../../util/Abbreviators';

function Version({ headerSize, textSize, ...props }) {
  const [backendVersion, setBackendVersion] = useState(null);
  const fetchBackendVersion = () => {
    CyanideApiService.backendVersion()
      .then((data) => {
        setBackendVersion(data);
      })
      .catch((reason) => {
        setBackendVersion(reason.toLocaleString(config.locale));
      });
  };

  useEffect(() => {
    fetchBackendVersion();
  }, []);

  return (
    <Box fontSize={headerSize} {...props}>
      <Box fontSize={textSize}>
        {backendVersion && backendVersion.projectVersion &&
          `B: ${backendVersion.projectVersion} (${abbreviators.abbreviateText(backendVersion.committish, 6)})`}
      </Box>
    </Box>
  );
}

export default Version;
