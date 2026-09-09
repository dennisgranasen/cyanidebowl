import React, { useEffect, useState } from 'react';
import {
  Avatar,
  Badge,
  Box,
  Card,
  CardBody,
  Divider,
  Heading,
  HStack,
  Spinner,
  Stack,
  Text,
  VStack,
} from '@chakra-ui/react';
import { useParams } from 'react-router-dom';
import ReactMarkdown from 'markdown-to-jsx';
import Navigation from '../components/misc/Navigation';
import AiReporterApi from '../AiReporterApi';

function ReporterProfilePage() {
  const { reporterId } = useParams();
  const [reporter, setReporter] = useState(null);
  const [reports, setReports] = useState([]);
  const [error, setError] = useState(null);

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

  return (
    <Box p={{ base: 3, md: 6 }}>
      <Navigation currentPage="staff" />
      {error && <Text mt={6} color="red.300">{error.message || String(error)}</Text>}
      {!reporter && !error && <Spinner mt={8} />}

      {reporter && (
        <>
          <Stack
            mt={6}
            direction={{ base: 'column', md: 'row' }}
            spacing={6}
            align={{ base: 'flex-start', md: 'center' }}
          >
            <Avatar
              size="2xl"
              name={reporter.alias}
              src={reporter.portraitImage || undefined}
            />
            <Box>
              <Heading>{reporter.alias}</Heading>
              <HStack mt={2} wrap="wrap">
                <Badge colorScheme="purple">AI Reporter</Badge>
                {reporter.race && <Badge>{reporter.race}</Badge>}
                {!reporter.active && <Badge colorScheme="gray">Inactive</Badge>}
              </HStack>
              <Text mt={3} fontSize="lg">{reporter.role || reporter.category}</Text>
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
