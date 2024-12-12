import React, { useEffect, useMemo, useState } from 'react';
import { Box, Card, CardBody, SimpleGrid, Stack, Text, VStack } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import ReactMarkdown from 'markdown-to-jsx';
import { ChakraUIRenderer } from 'chakra-ui-markdown';
import { FaRegHeart } from 'react-icons/fa6';
import { Icon } from '@chakra-ui/icons';
import DbbcCard from './aboutCards/DbbcCard';
import Navigation from '../components/misc/Navigation';
import ImageUrls from '../ImageUrls';
import HeaderCard from '../components/common/HeaderCard';
import logger from '../util/Logger';
import DisclaimerCard from './aboutCards/DisclaimerCard';
import parseMarkdownPrefixingLinks from '../util/MarkdownParser';
import markDownTheme from '../theme/components/Markdown';

const readmeFile = '/README.md';

function AboutPage() {
  const renderer = useMemo(() => ChakraUIRenderer(markDownTheme, true), []);
  const [readme, setReadme] = useState();

  const handleError = (response) => {
    if (!response.ok) {
      throw Error(response.statusText);
    } else {
      return response.text();
    }
  };

  const loadReadme = () => {
    fetch(`${readmeFile}`)
      .then(handleError)
      .then((text) => parseMarkdownPrefixingLinks(text, '/#/'))
      .then(setReadme)
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
      />
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
        <SimpleGrid columns={{ base: 1, md: 2 }} spacing="1rem">
          <DbbcCard />
          <DisclaimerCard />
        </SimpleGrid>
      </VStack>
    </Stack>
  );
}

export default AboutPage;
