import React, { useEffect, useMemo, useState } from 'react';
import {
  Badge,
  Box,
  Button,
  Card,
  CardBody,
  Container,
  Divider,
  Heading,
  HStack,
  Image,
  SimpleGrid,
  Spinner,
  Stack,
  Text,
  VStack,
} from '@chakra-ui/react';
import { EditIcon } from '@chakra-ui/icons';
import { Link as RouteLink, useParams } from 'react-router-dom';
import ReactMarkdown from 'markdown-to-jsx';
import Navigation from '../components/misc/Navigation';
import AiReporterApi from '../AiReporterApi';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';

const splitPublicProfile = (markdown = '') => {
  const sections = markdown.split(/(?=^##\s+)/m);

  let intro = '';
  const body = [];

  for (const section of sections) {
    const heading = section.match(/^##\s+(.+)\s*$/m)?.[1]?.trim().toLowerCase();

    if (heading === 'public profile') {
      intro = section
        .replace(/^##\s+Public profile\s*/im, '')
        .trim();
      continue;
    }

    // Defensive client-side filtering as well. The backend should already remove these.
    if (heading === 'llm guidance' || heading === 'portrait brief') {
      continue;
    }

    // The H1 repeats the hero title and is unnecessary in the article body.
    const cleaned = section.replace(/^#\s+.+\s*$/m, '').trim();
    if (cleaned) body.push(cleaned);
  }

  return {
    intro,
    body: body.join('\n\n'),
  };
};

const markdownOverrides = {
  h1: {
    component: (props) => <Heading as="h2" size="lg" mt={8} mb={3} {...props} />,
  },
  h2: {
    component: (props) => <Heading as="h2" size="md" mt={8} mb={3} {...props} />,
  },
  h3: {
    component: (props) => <Heading as="h3" size="sm" mt={6} mb={2} {...props} />,
  },
  p: {
    component: (props) => (
      <Text fontSize="md" lineHeight="1.8" mb={4} {...props} />
    ),
  },
  ul: {
    component: (props) => (
      <Box as="ul" pl={6} mb={5} sx={{ '& > li': { marginBottom: '0.35rem' } }} {...props} />
    ),
  },
  ol: {
    component: (props) => (
      <Box as="ol" pl={6} mb={5} sx={{ '& > li': { marginBottom: '0.35rem' } }} {...props} />
    ),
  },
  blockquote: {
    component: (props) => (
      <Box
        as="blockquote"
        borderLeftWidth="4px"
        borderLeftColor="purple.400"
        pl={5}
        py={2}
        my={5}
        fontStyle="italic"
        color="gray.600"
        _dark={{ color: 'gray.300' }}
        {...props}
      />
    ),
  },
  strong: {
    component: (props) => <Box as="strong" fontWeight="700" {...props} />,
  },
};

function ReportCard({ report }) {
  return (
    <Card variant="outline">
      <CardBody>
        <Heading size="sm" lineHeight="1.3">
          {report.headline || 'Untitled report'}
        </Heading>

        {report.excerpt && (
          <Text mt={2} fontSize="sm" color="gray.600" _dark={{ color: 'gray.300' }} noOfLines={4}>
            {report.excerpt}
          </Text>
        )}

        {report.publishedAt && (
          <Text mt={3} fontSize="xs" color="gray.500">
            {new Date(report.publishedAt).toLocaleString()}
          </Text>
        )}
      </CardBody>
    </Card>
  );
}

function ReporterProfilePage() {
  const { reporterId } = useParams();
  const [reporter, setReporter] = useState(null);
  const [reports, setReports] = useState([]);
  const [error, setError] = useState(null);

  const { authenticationReady, userPermissions } = useAuth0WithUserPermissions();
  const canEdit = authenticationReady && Boolean(userPermissions?.writeSiteAdmin);

  useEffect(() => {
    setReporter(null);
    setError(null);

    Promise.all([
      AiReporterApi.reporter(reporterId),
      AiReporterApi.reports(reporterId).catch(() => []),
    ])
      .then(([profile, articles]) => {
        setReporter(profile);
        setReports(articles || []);
      })
      .catch(setError);
  }, [reporterId]);

  const publicProfile = useMemo(
    () => splitPublicProfile(reporter?.publicMarkdown || ''),
    [reporter]
  );

  return (
    <Box>
      <Container maxW="7xl" px={{ base: 3, md: 6 }} py={{ base: 3, md: 5 }}>
        <Navigation currentPage="staff" />

        {error && (
          <Text mt={6} color="red.400">
            {error.message || String(error)}
          </Text>
        )}

        {!reporter && !error && <Spinner mt={8} />}

        {reporter && (
          <>
            <Card mt={6} overflow="hidden" variant="outline">
              <Stack direction={{ base: 'column', md: 'row' }} spacing={0}>
                <Box
                  flexShrink={0}
                  w={{ base: '100%', md: '320px', lg: '360px' }}
                  aspectRatio={{ base: '4 / 3', md: '3 / 4' }}
                  overflow="hidden"
                  bg="gray.100"
                  _dark={{ bg: 'gray.800' }}
                >
                  <Image
                    src={reporter.portraitImage || reporter.avatarImage || undefined}
                    alt={reporter.alias}
                    w="100%"
                    h="100%"
                    objectFit="cover"
                    objectPosition="top center"
                  />
                </Box>

                <CardBody
                  display="flex"
                  flexDirection="column"
                  justifyContent="center"
                  px={{ base: 5, md: 8, lg: 10 }}
                  py={{ base: 6, md: 8 }}
                  minW={0}
                >
                  <HStack justify="space-between" align="flex-start" spacing={5}>
                    <Box minW={0}>
                      <Text
                        fontSize="xs"
                        fontWeight="700"
                        letterSpacing="0.12em"
                        textTransform="uppercase"
                        color="purple.500"
                        mb={2}
                      >
                        BlaskScore staff
                      </Text>

                      <Heading
                        as="h1"
                        size={{ base: 'xl', md: '2xl' }}
                        lineHeight="1"
                        letterSpacing="-0.02em"
                      >
                        {reporter.alias}
                      </Heading>

                      <HStack mt={4} spacing={2} wrap="wrap">
                        <Badge colorScheme="purple">AI Reporter</Badge>
                        {reporter.race && <Badge>{reporter.race}</Badge>}
                        {!reporter.active && <Badge colorScheme="gray">Inactive</Badge>}
                      </HStack>
                    </Box>

                    {canEdit && (
                      <Button
                        as={RouteLink}
                        to={`/admin/ai-reporters/${reporter.id}`}
                        leftIcon={<EditIcon />}
                        colorScheme="purple"
                        size="sm"
                        flexShrink={0}
                      >
                        Edit
                      </Button>
                    )}
                  </HStack>

                  <Text
                    mt={5}
                    fontSize={{ base: 'lg', md: 'xl' }}
                    fontWeight="600"
                    lineHeight="1.45"
                  >
                    {reporter.role || reporter.category}
                  </Text>

                  {publicProfile.intro && (
                    <>
                      <Divider my={6} />
                      <Box
                        fontSize={{ base: 'md', md: 'lg' }}
                        lineHeight="1.75"
                        color="gray.600"
                        _dark={{ color: 'gray.300' }}
                        maxW="52rem"
                      >
                        <ReactMarkdown
                          options={{
                            overrides: {
                              p: {
                                component: (props) => (
                                  <Text fontSize="inherit" lineHeight="inherit" mb={0} {...props} />
                                ),
                              },
                            },
                          }}
                        >
                          {publicProfile.intro}
                        </ReactMarkdown>
                      </Box>
                    </>
                  )}
                </CardBody>
              </Stack>
            </Card>

            <SimpleGrid
              mt={{ base: 6, md: 8 }}
              columns={{ base: 1, lg: reports.length > 0 ? 2 : 1 }}
              templateColumns={{
                base: '1fr',
                lg: reports.length > 0 ? 'minmax(0, 2fr) minmax(280px, 0.8fr)' : 'minmax(0, 820px)',
              }}
              spacing={{ base: 6, lg: 10 }}
              alignItems="start"
            >
              <Box
                maxW={reports.length > 0 ? 'none' : '820px'}
                mx={reports.length > 0 ? 0 : 'auto'}
                w="100%"
              >
                <Heading size="lg" mb={1}>
                  About {reporter.alias}
                </Heading>
                <Box w="48px" borderTopWidth="4px" borderColor="purple.400" mt={3} mb={6} />

                <Box
                  sx={{
                    '& a': {
                      textDecoration: 'underline',
                      textUnderlineOffset: '3px',
                    },
                  }}
                >
                  <ReactMarkdown options={{ overrides: markdownOverrides }}>
                    {publicProfile.body}
                  </ReactMarkdown>
                </Box>
              </Box>

              {reports.length > 0 && (
                <Box
                  as="aside"
                  position={{ base: 'static', lg: 'sticky' }}
                  top={{ lg: 6 }}
                >
                  <HStack justify="space-between" mb={4}>
                    <Heading size="md">Latest reports</Heading>
                    <Badge variant="subtle">{reports.length}</Badge>
                  </HStack>

                  <VStack align="stretch" spacing={3}>
                    {reports.slice(0, 8).map((report) => (
                      <ReportCard key={report.id} report={report} />
                    ))}
                  </VStack>
                </Box>
              )}
            </SimpleGrid>

            {reports.length === 0 && (
              <Box mt={8} pt={6} borderTopWidth="1px">
                <Heading size="md">Latest reports</Heading>
                <Text mt={2} color="gray.500">
                  No published reports yet.
                </Text>
              </Box>
            )}
          </>
        )}
      </Container>
    </Box>
  );
}

export default ReporterProfilePage;
