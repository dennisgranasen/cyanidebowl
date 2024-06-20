import { Box, HStack, Text } from '@chakra-ui/react';
import React from 'react';
import SocialLink from './SocialLink';

function SocialLinks({ socialLinks, headerSize, iconSize, ...props }) {
  return (
    socialLinks?.length > 0 && (
      <Box fontSize={headerSize} {...props}>
        <Text fontStyle="italic">BB3 social links</Text>
        <HStack>
          {socialLinks.map((socialLink) => (
            <SocialLink url={socialLink} key={socialLink} iconSize={iconSize} />
          ))}
        </HStack>
      </Box>
    )
  );
}

export default SocialLinks;
