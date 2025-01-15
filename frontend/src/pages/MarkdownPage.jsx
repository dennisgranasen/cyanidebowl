import React, { useEffect, useMemo, useState } from 'react';
import { Box, Card, CardBody, Stack, VStack } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import ReactMarkdown from 'markdown-to-jsx';
import { ChakraUIRenderer } from 'chakra-ui-markdown';
import Navigation from '../components/misc/Navigation';
import imageUrls from '../imageUrls';
import HeaderCard from '../components/common/HeaderCard';
import logger from '../util/logger';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import markDownTheme from '../theme/components/Markdown';

const getContentAdjustingMarkdownLinks = (text) => {
  const markdownLinkRegex = /(\[[^\]]*\])\((?:https?:\/\/){0}([^:)]*)\)/g;
  return text.replace(markdownLinkRegex, '$1(/#/$2)');
};

const handleError = (response) => {
  if (!response.ok) {
    throw Error(response.statusText);
  } else {
    return response.text();
  }
};

function MarkdownPage({ markdownDocument, title }) {
  const renderer = useMemo(() => ChakraUIRenderer(markDownTheme, true), []);
  const [content, setContent] = useState();
  const [error, setError] = useState(null);

  const loadMarkdownDocument = () => {
    fetch(`${markdownDocument}`)
      .then(handleError)
      .then(getContentAdjustingMarkdownLinks)
      .then(setContent)
      .catch((reason) => {
        logger.error('Could not load document from file %s, %o.', markdownDocument, reason);
        setContent(null);
        setError({ type: 'error', message: `Error loading content for ${title}.` });
      });
  };

  useEffect(() => {
    if (markdownDocument) {
      loadMarkdownDocument();
    }
  }, [markdownDocument]);

  return (
    <Stack>
      <Box>
        <Navigation currentPage="home" />
      </Box>
      <HeaderCard
        heading={title}
        subHeading={<RouteLink to="/">warp-scores.net</RouteLink>}
        mainImageSrc={imageUrls.warpscoresLogoPng('medium')}
      />
      <VStack align="left">
        <LoadingOrErrorWrapper loading={!markdownDocument || !content} error={error}>
          <Card variant="outline">
            <CardBody>
              <ReactMarkdown
                options={{
                  forceBlock: true,
                  overrides: renderer,
                }}
              >
                {content}
              </ReactMarkdown>
            </CardBody>
          </Card>
        </LoadingOrErrorWrapper>
      </VStack>
    </Stack>
  );
}

export default MarkdownPage;
