import React, { useEffect, useMemo, useState } from 'react';
import { Box, Card, CardBody, SimpleGrid, Stack, Text, VStack } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import ReactMarkdown from 'markdown-to-jsx';
import { ChakraUIRenderer } from 'chakra-ui-markdown';
import { FaRegHeart } from 'react-icons/fa6';
import { Icon } from '@chakra-ui/icons';
import DbbcCard from './aboutCards/DbbcCard';
import Navigation from '../components/misc/Navigation';
import imageUrls from '../imageUrls';
import HeaderCard from '../components/common/HeaderCard';
import logger from '../util/logger';
import DisclaimerCard from './aboutCards/DisclaimerCard';
import parseMarkdownPrefixingLinks from '../util/markdownParser';
import markDownTheme from '../theme/components/Markdown';
import { useIntl } from 'react-intl';

const readmeFile = '/README.md';

function AboutPage() {
  const intl = useIntl();
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
        heading={intl.formatMessage({ id: 'about.heading' })}
        subHeading={<RouteLink to="/">warp-scores.net</RouteLink>}
        mainImageSrc={imageUrls.blaskscoreLogoPng('medium')}
      />
      <VStack align="left">
        {!readme && (
          <Text>
            {intl.formatMessage({ id: 'about.fallback' })} <Icon as={FaRegHeart} />
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
