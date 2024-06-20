import React, { useEffect, useState } from 'react';
import {
  Avatar,
  AvatarBadge,
  Box,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  HStack,
  Link,
  Spacer,
  Spinner,
  useDisclosure,
  VStack,
} from '@chakra-ui/react';
import { FaTriangleExclamation } from 'react-icons/fa6';
import { Link as RouteLink } from 'react-router-dom';
import { HamburgerIcon, Icon } from '@chakra-ui/icons';
import CyanideApiService from '../../CyanideApiService';
import config from '../../config';
import formatter from '../../util/Formatter';
import NewsList from './NewsList';
import SocialLinks from './SocialLinks';
import Disclaimer from './Disclaimer';
import Version from './Version';
import Status from './Status';
import StatusIcon from './StatusIcon';
import timeUtil from '../../util/TimeUtil';
import ImageUrls from '../../ImageUrls';
import DelayedIconTooltip from '../common/DelayedIconTooltip';

function LastCheck({ status, textSize, statusOutdated }) {
  return (
    <Box align="left" pt={2} fontSize={textSize}>
      <HStack spacing={2} align="left" w="full">
        <Box>Last check:</Box>
        <Box>
          {status ? (
            `${status.lastCheck ? formatter.formatAsDate(status.lastCheck) : 'unknown'}`
          ) : (
            <Spinner size={textSize} color="orange" />
          )}
        </Box>
        <Spacer />
        <Box align="right">
          {statusOutdated && (
            <DelayedIconTooltip label="Outdated" placement="left-start" shouldWrapChildren>
              <Icon as={FaTriangleExclamation} color="yellow" size={textSize} />
            </DelayedIconTooltip>
          )}
        </Box>
      </HStack>
    </Box>
  );
}

function Menu() {
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [status, setStatus] = useState(null);
  const [statusOutdated, setStatusOutdated] = useState(false);

  const fetchStatus = () => {
    CyanideApiService.status()
      .then((data) => {
        setStatus(data);
      })
      .catch((reason) => {
        setStatus(reason.toLocaleString(config.locale));
      });
  };

  useEffect(() => {
    fetchStatus();
  }, []);

  useEffect(() => {
    const outdated = status && timeUtil.durationInMillis(status.lastCheck) > config.MAX_AGE_FOR_STATUS_IN_MILLIS;
    setStatusOutdated(outdated);
  }, [status]);

  return (
    <>
      <Link onClick={onOpen}>
        <Avatar borderRadius={4} boxSize={12} icon={<HamburgerIcon />} src={ImageUrls.warpscoresLogoPng('medium')}>
          <AvatarBadge boxSize="24px" bg="black">
            <StatusIcon status={status} statusOutdated={statusOutdated} />
          </AvatarBadge>
        </Avatar>
      </Link>
      <Drawer isOpen={isOpen} placement="right" onClose={onClose}>
        <DrawerOverlay />
        <DrawerContent>
          <DrawerHeader borderBottomWidth="1px">Menu</DrawerHeader>
          <DrawerCloseButton />
          <DrawerBody
            display="block"
            backgroundImage={ImageUrls.warpscoresLogoPng()}
            backgroundRepeat="no-repeat"
            backgroundSize="cover"
            backgroundColor="gray.700"
            backgroundBlendMode="multiply"
            borderBottomWidth="1px"
          >
            <VStack h="full" align="left">
              <VStack align="left" h="full">
                <Box>
                  <Link as={RouteLink} to="/" onClick={() => onClose()}>
                    Home
                  </Link>
                </Box>
                {/*              <Box>
                <Link as={RouteLink} to="/admin" onClick={() => onClose()}>
                  Admin
                </Link>
              </Box>
              <Box>
                <Link as={RouteLink} to="/coachPage" onClick={() => onClose()}>
                  Coach-Page
                </Link>
              </Box>
              <Box>
                <Link as={RouteLink} to="/statistics" onClick={() => onClose()}>
                  Statistics
                </Link>
              </Box>
              <Box>
                <Link as={RouteLink} to="/login" onClick={() => onClose()}>
                  Login
                </Link>
              </Box>
*/}
                <Box>
                  <Link as={RouteLink} to="/about" onClick={() => onClose()}>
                    About
                  </Link>
                </Box>
              </VStack>
              <VStack align="left">
                <NewsList news={status?.news} headerSize="sm" textSize="xs" mt={2} color="grey" />
                <SocialLinks socialLinks={status?.socialLinks} headerSize="sm" iconSize="sm" mt={2} color="grey" />
                <Disclaimer mt={2} headerSize="sm" textSize="xs" color="grey" />
              </VStack>
            </VStack>
          </DrawerBody>
          <DrawerFooter mt={0} pt={0} align="left" color="grey">
            <VStack m={0} p={0} align="left" w="full">
              <Version mt={2} textSize="xs" color="grey" />
              <Status status={status} headerSize="md" textSize="sm" mt={2} />
              <LastCheck status={status} textSize="sm" statusOutdated={statusOutdated} />
            </VStack>
          </DrawerFooter>
        </DrawerContent>
      </Drawer>
    </>
  );
}

export default Menu;
