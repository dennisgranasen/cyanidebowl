import { Box } from '@chakra-ui/react';
import React, { useEffect, useState } from 'react';
import WarpScoresApiService from '../../WarpScoresApiService';
import config from '../../config';
import abbreviators from '../../util/Abbreviators';

function Version({ textSize, ...props }) {
  const [backendVersion, setBackendVersion] = useState(null);
  const fetchBackendVersion = () => {
    WarpScoresApiService.backendVersion()
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

  return backendVersion?.projectVersion ? (
    <Box fontSize={textSize} {...props} pb={2} bottomWidth="1px">
      {`B: ${backendVersion.projectVersion} (${abbreviators.abbreviateText(backendVersion.committish, 6)})`}
    </Box>
  ) : null;
}

export default Version;
