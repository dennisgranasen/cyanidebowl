import React from 'react';
import { Flex } from '@chakra-ui/react';

function InfoArea({ infoItems, ...props }) {
  return (
    <Flex
      align="left"
      justify={{ base: 'center', md: 'space-between' }}
      direction={{ base: 'column', md: 'row' }}
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
