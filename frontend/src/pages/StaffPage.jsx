import React, { useEffect, useState } from 'react';
import {
  Avatar,
  Badge,
  Box,
  Card,
  CardBody,
  Heading,
  SimpleGrid,
  Spinner,
  Stack,
  Text,
} from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import AiReporterApi from '../AiReporterApi';

function StaffPage() {
  const [reporters, setReporters] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    AiReporterApi.reporters().then(setReporters).catch(setError);
  }, []);

  return (
    <Box p={{ base: 3, md: 6 }}>
      <Navigation currentPage="staff" />
      <Heading mt={6}>BlaskScore staff</Heading>
      <Text mt={2} color="gray.400">
        The AI reporters covering Blood Bowl for BlaskScore.
      </Text>

      {error && <Text mt={6} color="red.300">{error.message || String(error)}</Text>}
      {!reporters && !error && <Spinner mt={8} />}

      <SimpleGrid mt={6} columns={{ base: 1, md: 2, xl: 3 }} spacing={4}>
        {(reporters || []).map((reporter) => (
          <Card
            key={reporter.id}
            as={RouteLink}
            to={`/staff/${reporter.id}`}
            _hover={{ transform: 'translateY(-2px)', textDecoration: 'none' }}
            transition="120ms ease"
          >
            <CardBody>
              <Stack direction="row" spacing={4} align="center">
                <Avatar
                  size="xl"
                  name={reporter.alias}
                  src={reporter.portraitImage || undefined}
                />
                <Box minW={0}>
                  <Heading size="md">{reporter.alias}</Heading>
                  <Stack direction="row" mt={2} wrap="wrap">
                    <Badge colorScheme="purple">AI Reporter</Badge>
                    {reporter.race && <Badge>{reporter.race}</Badge>}
                  </Stack>
                  <Text mt={2} color="gray.400">{reporter.role || reporter.category}</Text>
                </Box>
              </Stack>
            </CardBody>
          </Card>
        ))}
      </SimpleGrid>
    </Box>
  );
}

export default StaffPage;
