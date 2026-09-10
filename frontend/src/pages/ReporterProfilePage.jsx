import React, { useEffect, useState } from 'react';
import {
  Badge, Box, Button, Card, CardBody, Heading, HStack, Image,
  Spinner, Stack, Text, VStack
} from '@chakra-ui/react';
import { EditIcon } from '@chakra-ui/icons';
import { Link as RouteLink, useParams } from 'react-router-dom';
import ReactMarkdown from 'markdown-to-jsx';
import Navigation from '../components/misc/Navigation';
import AiReporterApi from '../AiReporterApi';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';

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
      .then(([p, articles]) => {
        setReporter(p);
        setReports(articles || []);
      })
      .catch(setError);
  }, [reporterId]);

  return (
    <Box p={{ base: 3, md: 6 }}>
      <Navigation currentPage="staff" />
      {error && <Text mt={6} color="red.300">{error.message || String(error)}</Text>}
      {!reporter && !error && <Spinner mt={8} />}

      {reporter && (
        <>
          <Stack mt={6} direction={{ base: 'column', md: 'row' }} spacing={6} align="flex-start">
            <Image
              src={reporter.portraitImage || reporter.avatarImage || undefined}
              alt={reporter.alias}
              objectFit="cover"
              objectPosition="top center"
              borderRadius="lg"
              boxShadow="lg"
              w={{ base: '100%', sm: '22rem', md: '20rem' }}
              maxH={{ base: '34rem', md: '32rem' }}
            />

            <Box flex="1">
              <HStack justify="space-between" align="start">
                <Box>
                  <Heading>{reporter.alias}</Heading>
                  <HStack mt={2} wrap="wrap">
                    <Badge colorScheme="purple">AI Reporter</Badge>
                    {reporter.race && <Badge>{reporter.race}</Badge>}
                    {!reporter.active && <Badge colorScheme="gray">Inactive</Badge>}
                  </HStack>
                  <Text mt={3} fontSize="lg">{reporter.role || reporter.category}</Text>
                </Box>
                {canEdit && (
                  <Button
                    as={RouteLink}
                    to={`/admin/ai-reporters/${reporter.id}`}
                    leftIcon={<EditIcon />}
                    colorScheme="purple"
                    size="sm"
                  >
                    Edit
                  </Button>
                )}
              </HStack>
            </Box>
          </Stack>

          <Card mt={6}>
            <CardBody>
              <Box className="reporter-markdown">
                <ReactMarkdown>{reporter.publicMarkdown || ''}</ReactMarkdown>
              </Box>
            </CardBody>
          </Card>

          <Heading size="md" mt={8}>Latest reports</Heading>
          <VStack align="stretch" spacing={3} mt={3}>
            {reports.slice(0, 10).map((report) => (
              <Card key={report.id}>
                <CardBody>
                  <Heading size="sm">{report.headline || 'Untitled report'}</Heading>
                  {report.excerpt && <Text mt={2}>{report.excerpt}</Text>}
                  {report.publishedAt && (
                    <Text mt={2} fontSize="sm" color="gray.500">
                      {new Date(report.publishedAt).toLocaleString()}
                    </Text>
                  )}
                </CardBody>
              </Card>
            ))}
            {reports.length === 0 && <Text color="gray.500">No published reports yet.</Text>}
          </VStack>
        </>
      )}
    </Box>
  );
}

export default ReporterProfilePage;
