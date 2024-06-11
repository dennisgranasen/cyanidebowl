import React, { useEffect, useState } from 'react';
import { CheckCircleIcon, ExternalLinkIcon, Icon, WarningIcon } from '@chakra-ui/icons';
import {
  Box,
  HStack,
  Link,
  List,
  ListItem,
  Popover,
  PopoverArrow,
  PopoverBody,
  PopoverCloseButton,
  PopoverContent,
  PopoverFooter,
  PopoverHeader,
  PopoverTrigger,
  Spinner,
  Text,
  VStack,
} from '@chakra-ui/react';
import { FaDesktop, FaFacebook, FaGlobe, FaPlaystation, FaXbox, FaXTwitter } from 'react-icons/fa6';
import { FaDiscord } from 'react-icons/fa';
import CyanideApiService from '../CyanideApiService';
import config from '../config';
import hashCode from '../util/HashCode';

function NewsItem({ title, message }) {
  const hasStringTitle = typeof title === 'string' && title.length > 0;
  const hasStringMessage = typeof message === 'string' && message.length > 0;
  const color = hasStringMessage && message.match(/maintenance/i) ? 'orange' : null;
  let text;
  if (hasStringTitle) {
    text = title;
  } else if (hasStringMessage && !message.match(/^http/)) text = message;
  const url = hasStringMessage && message.match(/^http/) ? message : null;
  return (
    <List fontSize="xs" color={color}>
      <ListItem>
        {url ? (
          <Link href={url} isExternal>
            <>
              {text}
              <ExternalLinkIcon mx="2px" />
            </>
          </Link>
        ) : (
          text
        )}
      </ListItem>
    </List>
  );
}

function StatusIcon({ status, maintenance }) {
  if (status && maintenance && maintenance.length === 0) {
    return <CheckCircleIcon size="sm" color="green" />;
  }
  const color = status ? 'orange' : 'red';
  return <WarningIcon size="sm" color={color} />;
}

const getMaintenanceColor = (maintenanceStatus) => {
  return maintenanceStatus.length === 0 ? 'grey' : 'orange';
};

function SocialLink({ url }) {
  const isDiscord = /\/discord\.gg/.test(url);
  const isTwitter = /twitter\.com/.test(url) || /\/x\.com/.test(url);
  const isFacebook = /\/www\.facebook/.test(url);
  let icon = FaGlobe;
  icon = isFacebook ? FaFacebook : icon;
  icon = isTwitter ? FaXTwitter : icon;
  icon = isDiscord ? FaDiscord : icon;

  return (
    <Link href={url}>
      <Icon as={icon} size="sm" />
    </Link>
  );
}

function Status() {
  const [status, setStatus] = useState(null);
  useEffect(() => {
    const fetchStatus = () => {
      setStatus(null);
      CyanideApiService.status()
        .then((data) => {
          setStatus(data);
        })
        .catch((reason) => {
          setStatus(reason.toLocaleString(config.locale));
        });
    };
    fetchStatus();
  }, []);

  return status === null ? (
    <Spinner size="sm" color="orange" />
  ) : (
    <Popover>
      <PopoverTrigger>
        <Link>
          <StatusIcon
            status={status.overall}
            maintenance={
              status.maintenance
                ? [].concat(status.maintenance.pc, status.maintenance.microsoft, status.maintenance.sony)
                : []
            }
          />
        </Link>
      </PopoverTrigger>
      <PopoverContent>
        <PopoverArrow />
        <PopoverCloseButton />
        <PopoverHeader>Cyanide Api-Status</PopoverHeader>
        <PopoverBody>
          <HStack spacing={2}>
            <Box>Overall:</Box>
            <HStack spacing={2}>
              <StatusIcon
                status={status.overall}
                maintenance={
                  status.maintenance
                    ? [].concat(status.maintenance.pc, status.maintenance.microsoft, status.maintenance.sony)
                    : []
                }
              />
            </HStack>
          </HStack>
          <HStack spacing={2}>
            <Box>Maintenance:</Box>
            <HStack spacing={2}>
              {status.maintenance ? (
                <>
                  <FaDesktop color={getMaintenanceColor(status.maintenance.pc)} />
                  <FaXbox color={getMaintenanceColor(status.maintenance.microsoft)} />
                  <FaPlaystation color={getMaintenanceColor(status.maintenance.sony)} />
                </>
              ) : null}
            </HStack>
          </HStack>
          {status.news.length > 0 && (
            <Box mt={2} fontSize="sm" color="grey">
              <Text fontStyle="italic">Latest BB3 news</Text>
              <List>
                {status.news
                  .sort((news1, news2) => {
                    if (!news1.title) return -1;
                    if (!news2.title) return 1;
                    return 0;
                  })
                  .map((newsItem) => (
                    <NewsItem
                      key={hashCode(newsItem.title, newsItem.message)}
                      title={newsItem.title}
                      message={newsItem.message}
                    />
                  ))}
              </List>
            </Box>
          )}
          {status.socialLinks.length > 0 && (
            <Box mt={2} fontSize="sm" color="grey">
              <Text fontStyle="italic">BB3 social links</Text>
              <HStack>
                {status.socialLinks.map((socialLink) => (
                  <SocialLink url={socialLink} key={socialLink} />
                ))}
              </HStack>
            </Box>
          )}
          <Box mt={2} fontSize="sm" color="grey">
            <Text fontStyle="italic">Disclaimer</Text>
            <VStack>
              <Text fontSize="xs">
                This site is completely unofficial and not affiliated with Cyanide, Nacon or Games Workshop.
              </Text>
              <Text fontSize="xs">
                Blood Bowl, BB3 and probably a lot more names are trademarks of their respective owners. Used without permission. No challenge to their status intended.
              </Text>
              <Text fontSize="xs">
                Page maintained by{' '}
                <Link href="mailto:naytsyrhc@gmx.org" isExternal>
                  Naytsyrhc
                </Link>
              </Text>
            </VStack>
          </Box>
        </PopoverBody>
        <PopoverFooter>Last check: {status && status.lastCheck ? status.lastCheck : status}</PopoverFooter>
      </PopoverContent>
    </Popover>
  );
}

export default Status;
