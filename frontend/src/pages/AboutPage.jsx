import React, { useEffect, useMemo, useState } from 'react';
import { Box, Card, CardBody, Flex, HStack, Image, Link, SimpleGrid, Stack, Text, VStack } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import ReactMarkdown from 'markdown-to-jsx';
import { ChakraUIRenderer } from 'chakra-ui-markdown';
import { ExternalLinkIcon, Icon, InfoOutlineIcon } from '@chakra-ui/icons';
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

  const loadReadme = () => {
    fetch(`${readmeFile}`)
      .then((response) => response.text())
      .then((text) => setReadme(text))
      .catch((reason) => {
        logger.debug('Could not load readme from file %s, %o.', readmeFile, reason);
        setReadme(null);
      });
  };

  useEffect(() => {
    loadReadme();
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
          {!readme && (
            <Text>
              This is a Spike-like (good old Spike made by poncho for BB2 <Icon as={FaRegHeart} />) facade to BB3 data
              provided by Cyanide&apos;s BB3-API.
            </Text>
          )}
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
          <SimpleGrid columns={2} spacing="1rem">
            <Card direction={{ base: 'row' }} variant="outline" overflow="hidden">
              <Image objectFit="cover" src={ImageUrls.dbbcLogoPng('small')} p="0.8rem" />
              <CardBody>
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
            <Card direction={{ base: 'row' }} variant="outline">
              <InfoOutlineIcon boxSize="100px" p="0.8rem"/>
              <CardBody>
                <Disclaimer />
              </CardBody>
            </Card>
          </SimpleGrid>
        </VStack>
      </HeaderCard>
    </Stack>
  );
}

export default AboutPage;
