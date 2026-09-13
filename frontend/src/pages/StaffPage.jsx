import React, { useEffect, useState } from 'react';
import {
  Avatar, Badge, Box, Card, CardBody, Heading, Text
} from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import AiReporterApi from '../AiReporterApi';
import StaffApi from '../StaffApi';
import { useIntl } from 'react-intl';
import { toStaffCards } from '../util/staffProfiles';

function StaffPage() {
  const intl = useIntl();
  const [reporters, setReporters] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([AiReporterApi.reporters(), StaffApi.users()])
      .then(([ai, humans]) => setReporters(toStaffCards(
        ai, humans, intl.formatMessage({ id: 'staff.human' })
      )))
      .catch(setError);
  }, [intl]);

  return (
    <Box p={{ base: 3, md: 6 }}>
      <Navigation currentPage="staff" />
      <Heading mt={6}>{intl.formatMessage({ id: 'staff.heading' })}</Heading>
      <Text mt={2} color="gray.400">
        {intl.formatMessage({ id: 'staff.description' })}
      </Text>

      {error && <Text mt={6} color="red.300">{error.message || String(error)}</Text>}
      {!reporters && !error && <Text mt={8}>{intl.formatMessage({ id: 'staff.loading' })}</Text>}

      <Box
        mt={6}
        display="grid"
        gridTemplateColumns="repeat(auto-fill, minmax(min(100%, 180px), 1fr))"
        gap={4}
        alignItems="stretch"
      >
        {(reporters || []).map((reporter) => (
          <Card key={reporter.id} overflow="hidden" minW={0}>
            <Box
              as={RouteLink}
              to={reporter.profileUrl || `/staff/${reporter.id}`}
              display="block"
              h="100%"
              _hover={{ textDecoration: 'none' }}
            >
              <CardBody
                display="flex"
                flexDirection="column"
                alignItems="center"
                textAlign="center"
                h="100%"
                px={3}
                py={4}
                transition="120ms ease"
                _hover={{ transform: 'translateY(-2px)' }}
              >
                <Avatar
                  size="xl"
                  name={reporter.displayName || reporter.alias}
                  src={reporter.avatarImage || reporter.portraitImage || undefined}
                  mb={3}
                />
                <Heading size="sm" noOfLines={2}>{reporter.displayName || reporter.alias}</Heading>
                <Box mt={2}>
                  <Badge colorScheme={reporter.profileType === 'AI' ? 'purple' : 'blue'} mr={1}>{reporter.profileType === 'AI' ? 'AI' : intl.formatMessage({ id: 'staff.human' })}</Badge>
                  {reporter.race && <Badge>{reporter.race}</Badge>}
                </Box>
                <Text mt={2} color="gray.400" fontSize="sm" noOfLines={2}>
                  {reporter.role || reporter.category}
                </Text>
                {reporter.summary && <Text mt={2} color="gray.500" fontSize="xs" noOfLines={3}>{reporter.summary}</Text>}
              </CardBody>
            </Box>
          </Card>
        ))}
      </Box>
    </Box>
  );
}

export default StaffPage;
