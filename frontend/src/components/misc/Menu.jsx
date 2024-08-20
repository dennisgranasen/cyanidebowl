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
import { ExternalLinkIcon, HamburgerIcon, Icon } from '@chakra-ui/icons';
import { useAuth0 } from '@auth0/auth0-react';
import WarpScoresApiService from '../../WarpScoresApiService';
import config from '../../config';
import Formatter from '../../util/Formatter';
import NewsList from './NewsList';
import SocialLinks from './SocialLinks';
import Disclaimer from './Disclaimer';
import Version from './Version';
import Status from './Status';
import StatusIcon from './StatusIcon';
import timeUtil from '../../util/TimeUtil';
import ImageUrls from '../../ImageUrls';
import DelayedIconTooltip from '../common/DelayedIconTooltip';
import logger from '../../util/Logger';

const { smallBoxSize } = config;

function LastCheck({ status, textSize, statusOutdated }) {
  return (
    <Box align="left" pt={2} fontSize={textSize}>
      <HStack spacing={2} align="left" w="full">
        <Box>Last check:</Box>
        <Box>
          {status ? (
            `${Formatter.formatAsDate(status.lastCheck, 'unknown')}`
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
  const { user, isAuthenticated, isLoading, loginWithPopup, logout, getAccessTokenSilently, getAccessTokenWithPopup } =
    useAuth0();
  const [userPermissions, setUserPermissions] = useState({
    readCurrentUser: false,
    writeLeagueAdmin: false,
    writeSiteAdmin: false,
  });
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [status, setStatus] = useState(null);
  const [statusOutdated, setStatusOutdated] = useState(false);

  useEffect(() => {
    const fetchUserPermissions = () => {
      WarpScoresApiService.userPermissions(getAccessTokenSilently, getAccessTokenWithPopup)
        .catch((reason) => {
          logger.error(reason);
        })
        .then(setUserPermissions);
    };
    if (!isLoading && isAuthenticated) {
      fetchUserPermissions();
    }
  }, [isLoading, isAuthenticated]);

  const fetchStatus = () => {
    WarpScoresApiService.status()
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
          <AvatarBadge boxSize={smallBoxSize} bg="black">
            <StatusIcon status={status} statusOutdated={statusOutdated} />
          </AvatarBadge>
        </Avatar>
      </Link>
      <Drawer size={{ base: 'full', sm: 'xs' }} isOpen={isOpen} placement="right" onClose={onClose}>
        <DrawerOverlay />
        <DrawerContent>
          <DrawerHeader backgroundColor="warpScoresBackgroundColor">
            Menu
          </DrawerHeader>
          <DrawerCloseButton />
          <DrawerBody
            p={0}
            backgroundImage={ImageUrls.warpscoresLogoPng()}
            backgroundRepeat="no-repeat"
            backgroundSize="cover"
          >
            <VStack
              background="warpScoresBackgroundColor"
              h="full"
              w="full"
              align="left"
              paddingLeft="6"
              paddingRight="6"
              paddingTop="2"
              paddingBottom="2"
              opacity="0.9"
              overflowX="scroll"
            >
              <VStack align="left" h="full">
                <Box>
                  <Link variant="menu" as={RouteLink} to="/" onClick={() => onClose()}>
                    Home
                  </Link>
                </Box>
                {!isLoading && isAuthenticated && userPermissions?.readCurrentUser && (
                  <Box>
                    <Link variant="menu" as={RouteLink} to="/coachPage" onClick={() => onClose()}>
                      Coach-Page
                    </Link>
                  </Box>
                )}
                {!isLoading && isAuthenticated && userPermissions?.writeSiteAdmin && (
                  <Box>
                    <Link variant="menu" as={RouteLink} to="/admin" onClick={() => onClose()}>
                      Admin
                    </Link>
                  </Box>
                )}
                {/*
                <Box>
                  <Link variant="menu" as={RouteLink} to="/statistics" onClick={() => onClose()}>
                    Statistics
                  </Link>
                </Box>
                <Box>
                  <Link variant="menu" as={RouteLink} to="/login" onClick={() => onClose()}>
                    Login
                  </Link>
                </Box>
                */}
                {!isLoading && (
                  <Box>
                    {!isAuthenticated ? (
                      <Link variant="menu" onClick={() => loginWithPopup()}>
                        Login
                      </Link>
                    ) : (
                      <Link
                        variant="menu"
                        onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}
                      >{`Logout ${user.name}`}</Link>
                    )}
                  </Box>
                )}
                <Box>
                  <Link variant="menu" as={RouteLink} to="/about" onClick={() => onClose()}>
                    About
                  </Link>
                </Box>
                <Spacer />
                <Box>
                  <Link href="https://web.cyanide-studio.com/bloodbowl/" isExternal>
                    Cyanide Admin-Tools <ExternalLinkIcon mx={2} />
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
          <DrawerFooter backgroundColor="warpScoresBackgroundColor" color="gray" mt={0} pt={0} align="left">
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
