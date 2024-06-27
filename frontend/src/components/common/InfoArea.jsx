import React from 'react';
import { Flex } from '@chakra-ui/react';

function InfoArea({ infoItems, ...props }) {
  return (
    <Flex
      flex="1"
      align="left"
      justify={{ base: 'center', md: 'space-between' }}
      direction={{ base: 'column', md: 'row' }}
      gap="0.5rem"
      wrap="wrap"
      {...props}
    >
      {infoItems
        ? infoItems.map((infoItem) => {
            return infoItem;
          })
        : null}
    </Flex>
  );
}

export default InfoArea;
