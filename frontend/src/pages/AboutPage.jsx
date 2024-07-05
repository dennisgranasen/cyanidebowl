import React, { useEffect, useMemo, useState } from 'react';
import { Box, Card, CardBody, HStack, Image, Link, Stack, Text, VStack } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import ReactMarkdown from 'markdown-to-jsx';
import { ChakraUIRenderer } from 'chakra-ui-markdown';
import { ExternalLinkIcon, Icon } from '@chakra-ui/icons';
import { FaRegHeart } from 'react-icons/fa6';
import Navigation from '../components/misc/Navigation';
import Disclaimer from '../components/misc/Disclaimer';
import ImageUrls from '../ImageUrls';
import HeaderCard from '../components/common/HeaderCard';
import logger from '../util/Logger';

const readmeFile = '/README.md';

function AboutPage() {
  const renderer = useMemo(() => ChakraUIRenderer(), []);
  const [readme, setReadme] = useState();

  useEffect(() => {
    import(`${readmeFile}`)
      .then((fileResponse) => {
        fetch(fileResponse.default)
          .then((response) => response.text())
          .then((text) => setReadme(text))
          .catch(() => logger.debug('Could not parse %s.', readmeFile));
      })
      .catch(() => logger.debug('Could not load %s.', readmeFile));
  }, []);

  return (
    <Stack>
      <Box>
        <Navigation currentPage="home" />
      </Box>
      <HeaderCard
        heading="About"
        subHeading={<RouteLink to="/">warp-scores.net</RouteLink>}
        mainImageSrc={ImageUrls.warpscoresLogoPng('medium')}
      >
        <VStack align="left">
          <Text>
            This is a Spike-like (good old Spike made by poncho for BB2 <Icon as={FaRegHeart} />) facade to BB3 data
            provided by Cyanide&apos;s BB3-API.
          </Text>
          <HStack spacing="1rem">
            <Card direction={{ base: 'row' }} overflow="hidden" size="sm">
              <Image objectFit="cover" src={ImageUrls.dbbcLogoPng('small')} flexWrap="left" />
              <CardBody maxW="sm">
                It was initially coded to support the{' '}
                <Link href="http://dbbcev.de" isExternal>
                  Deutsche Blood Bowl Community <ExternalLinkIcon mx="2x" />
                </Link>{' '}
                (German Blood Bowl Community) organising it&apos;s League &quot;
                <Link as={RouteLink} to="/94dd6ae4-83fa-11ee-b910-02000090a64f">
                  Deutsche Blood Bowl Liga
                </Link>
                &quot; but is about to open to support other private leagues as well.
              </CardBody>
            </Card>
          </HStack>
          {readme && (
            <Card variant="outline">
              <CardBody>
                <ReactMarkdown
                  options={{
                    forceBlock: true,
                    overrides: renderer,
                  }}
                >
                  {readme}
                </ReactMarkdown>
              </CardBody>
            </Card>
          )}
          <Disclaimer />
        </VStack>
      </HeaderCard>
    </Stack>
  );
}

export default AboutPage;
