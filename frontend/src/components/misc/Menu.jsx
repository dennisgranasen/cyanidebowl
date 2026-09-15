import React, { useEffect, useState } from 'react';
import { useIntl } from 'react-intl';
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
import { Link as RouteLink, useLocation } from 'react-router-dom';
import { ExternalLinkIcon, HamburgerIcon, Icon } from '@chakra-ui/icons';
import WarpScoresApiService from '../../WarpScoresApiService';
import config from '../../config';
import formatter from '../../util/formatter';
import NewsList from './NewsList';
import ArticleFeed from '../community/ArticleFeed';
import { articleContext, articleEditorUrl } from '../../util/articleContext';
import SocialLinks from './SocialLinks';
import Disclaimer from './Disclaimer';
import Version from './Version';
import Status from './Status';
import StatusIcon from './StatusIcon';
import timeUtil from '../../util/timeUtil';
import imageUrls from '../../imageUrls';
import DelayedIconTooltip from '../common/DelayedIconTooltip';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';
import { leagueSystemMenuTarget } from '../../util/leagueSystemNavigation';

const { smallBoxSize, isProduction } = config;

function LastCheck({ status, textSize, statusOutdated }) {
  const intl = useIntl();
  return (
    <Box align="left" pt={2} fontSize={textSize}>
      <HStack spacing={2} align="left" w="full">
        <Box>{intl.formatMessage({ id: 'menu.lastCheck' })}</Box>
        <Box>
          {status ? (
            `${formatter.formatAsDate(status.lastCheck, intl.formatMessage({ id: 'common.unknown' }))}`
          ) : (
            <Spinner size={textSize} color="orange" />
          )}
        </Box>
        <Spacer />
        <Box align="right">
          {statusOutdated && (
            <DelayedIconTooltip label={intl.formatMessage({ id: 'menu.outdated' })} placement="left-start" shouldWrapChildren>
              <Icon as={FaTriangleExclamation} color="yellow" size={textSize} />
            </DelayedIconTooltip>
          )}
        </Box>
      </HStack>
    </Box>
  );
}

function Menu({ leagueSystems = [], selectedLeagueSystemId, selectedSeasonId, onSelectLeagueSystem }) {
  const [menuLeagueSystems, setMenuLeagueSystems] = useState([]);
  const intl = useIntl();
  const location = useLocation();
  const articleSubject = articleContext(location).filter(l => !['LEAGUE_SYSTEM', 'SEASON'].includes(l.type)).at(-1);
  const { user, authenticationReady, checkPermissions, userPermissions, isAuthenticated, loginWithRedirect, logout } =
    useAuth0WithUserPermissions();
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [status, setStatus] = useState(null);
  const [statusOutdated, setStatusOutdated] = useState(false);

  const fetchStatus = () => {
    WarpScoresApiService.status()
      .then((data) => {
        setStatus(data);
      })
      .catch((reason) => {
        setStatus(reason.toLocaleString());
      });
  };

  useEffect(() => {
    fetchStatus();
  }, []);

  useEffect(() => {
    WarpScoresApiService.publicLeagueSystems().then(setMenuLeagueSystems).catch(() => setMenuLeagueSystems([]));
  }, []);

  useEffect(() => {
    const outdated = status && timeUtil.durationInMillis(status.lastCheck) > config.MAX_AGE_FOR_STATUS_IN_MILLIS;
    setStatusOutdated(outdated);
  }, [status]);

  return (
    <>
      <Link onClick={onOpen}>
        <Avatar borderRadius={4} boxSize={12} icon={<HamburgerIcon />} src={imageUrls.blaskscoreLogoPng('medium')}>
          <AvatarBadge boxSize={smallBoxSize} bg="black">
            <StatusIcon status={status} statusOutdated={statusOutdated} />
          </AvatarBadge>
        </Avatar>
      </Link>
      <Drawer size={{ base: 'full', sm: 'xs' }} isOpen={isOpen} placement="right" onClose={onClose}>
        <DrawerOverlay />
        <DrawerContent>
          <DrawerHeader backgroundColor="warpScoresBackgroundColor">{intl.formatMessage({ id: 'menu.title' })}</DrawerHeader>
          <DrawerCloseButton />
          <DrawerBody
            p={0}
            backgroundImage={imageUrls.blaskscoreLogoPng()}
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
              <VStack align="left" h="full" spacing={0}>
                <Box pb={2}>
                  <Box fontSize="xs" color="gray.500" textTransform="uppercase" letterSpacing="wide" mb={1}>
                    {intl.formatMessage({ id: 'menu.section.start', defaultMessage: 'Start' })}
                  </Box>
                  <VStack align="left" spacing={1}>
                    <Link variant="menu" as={RouteLink} to="/" onClick={onClose}>
                      {intl.formatMessage({ id: 'menu.home' })}
                    </Link>
                  </VStack>
                </Box>

                <Box py={2}>
                  <Box fontSize="xs" color="gray.500" textTransform="uppercase" letterSpacing="wide" mb={1}>
                    {intl.formatMessage({ id: 'menu.leagueSystem' })}
                  </Box>
                  <VStack align="left" spacing={1}>
                    {(leagueSystems.length ? leagueSystems : menuLeagueSystems).map((system) => (
                      <Link key={system.id} variant="menu" as={RouteLink}
                        to={leagueSystemMenuTarget(location, system.id)}
                        fontWeight={system.id === selectedLeagueSystemId ? 'bold' : 'normal'}
                        onClick={async () => {
                          if (onSelectLeagueSystem) await onSelectLeagueSystem(system.id);
                          onClose();
                        }}>
                        {system.primary ? '★ ' : ''}{system.name || system.id}
                      </Link>
                    ))}
                    <Link variant="menu" as={RouteLink} to="/statistics" onClick={onClose}>
                      {intl.formatMessage({ id: 'menu.statistics' })}
                    </Link>
                  </VStack>
                </Box>

                <Box py={2}>
                  <Box fontSize="xs" color="gray.500" textTransform="uppercase" letterSpacing="wide" mb={1}>
                    {intl.formatMessage({ id: 'menu.section.editorial', defaultMessage: 'Editorial' })}
                  </Box>
                  <VStack align="left" spacing={1}>
                    <Link variant="menu" as={RouteLink} to="/" onClick={onClose}>
                      {intl.formatMessage({ id: 'menu.articles', defaultMessage: 'Articles' })}
                    </Link>
                    {isAuthenticated && (
                      <Link variant="menu" as={RouteLink} to={articleEditorUrl(articleContext(location, selectedLeagueSystemId, selectedSeasonId))} onClick={onClose}>
                        {intl.formatMessage({ id: 'menu.writeArticle' })}
                      </Link>
                    )}
                    <Link variant="menu" as={RouteLink} to="/staff" onClick={onClose}>
                      {intl.formatMessage({ id: 'menu.staff' })}
                    </Link>
                  </VStack>
                </Box>

                <Box py={2}>
                  <Box fontSize="xs" color="gray.500" textTransform="uppercase" letterSpacing="wide" mb={1}>
                    {intl.formatMessage({ id: 'menu.section.community', defaultMessage: 'Community' })}
                  </Box>
                  <VStack align="left" spacing={1}>
                    <Link variant="menu" as={RouteLink} to="/community" onClick={onClose}>Community</Link>
                  </VStack>
                </Box>

                {checkPermissions && (userPermissions?.writeSiteAdmin || userPermissions?.writeLeagueAdmin) && (
                  <Box py={2}>
                    <Box fontSize="xs" color="gray.500" textTransform="uppercase" letterSpacing="wide" mb={1}>
                      {intl.formatMessage({ id: 'menu.section.administration', defaultMessage: 'Administration' })}
                    </Box>
                    <VStack align="left" spacing={1}>
                      <Link variant="menu" as={RouteLink} to="/admin" onClick={onClose}>
                        {intl.formatMessage({ id: 'menu.admin' })}
                      </Link>
                      {checkPermissions && userPermissions?.writeSiteAdmin && (
                        <Link variant="menu" as={RouteLink} to="/admin/community-fans" onClick={onClose}>
                          Community admin
                        </Link>
                      )}
                      {checkPermissions && userPermissions?.writeSiteAdmin && (
                        <Link variant="menu" as={RouteLink} to="/admin/ai-reporters" onClick={onClose}>
                          {intl.formatMessage({ id: 'menu.admin.reporters' })}                          
                        </Link>
                      )}
                      {checkPermissions && userPermissions?.writeSiteAdmin && (
                        <Link variant="menu" as={RouteLink} to="/admin/ai-autonomous-work" onClick={onClose}>
                          {intl.formatMessage({ id: 'menu.admin.ai-autonomous-work' })}                          
                        </Link>
                      )}
                      {userPermissions?.writeSiteAdmin && (
                        <Link variant="menu" as={RouteLink} to="/admin/localization" onClick={onClose}>
                          {intl.formatMessage({ id: 'menu.localizationAdmin' })}
                        </Link>
                      )}
                    </VStack>
                  </Box>
                )}

                <Box py={2}>
                  <Box fontSize="xs" color="gray.500" textTransform="uppercase" letterSpacing="wide" mb={1}>
                    {intl.formatMessage({ id: 'menu.section.account', defaultMessage: 'My account' })}
                  </Box>
                  <VStack align="left" spacing={1}>
                    {checkPermissions && userPermissions?.readCurrentUser && (
                      <Link variant="menu" as={RouteLink} to="/coachPage" onClick={onClose}>
                        {intl.formatMessage({ id: 'menu.coach' })}
                      </Link>
                    )}
                    {authenticationReady && isAuthenticated && (
                      <Link variant="menu" as={RouteLink} to="/account" onClick={onClose}>
                        {intl.formatMessage({ id: 'menu.account' })}
                      </Link>
                    )}
                    <Link variant="menu" as={RouteLink} to="/language" onClick={onClose}>
                      {intl.formatMessage({ id: 'menu.language' })}
                    </Link>
                    {isProduction && authenticationReady && (
                      !isAuthenticated ? (
                        <Link variant="menu" onClick={async (e) => {
                          e.preventDefault();
                          await loginWithRedirect();
                          onClose();
                        }}>
                          {intl.formatMessage({ id: 'menu.login' })}
                        </Link>
                      ) : (
                        <Link variant="menu" onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}>
                          {intl.formatMessage({ id: 'menu.logout' }, { name: user.name })}
                        </Link>
                      )
                    )}
                  </VStack>
                </Box>

                <Box py={2}>
                  <Box fontSize="xs" color="gray.500" textTransform="uppercase" letterSpacing="wide" mb={1}>
                    {intl.formatMessage({ id: 'menu.section.other', defaultMessage: 'Other' })}
                  </Box>
                  <VStack align="left" spacing={1}>
                    <Link variant="menu" as={RouteLink} to="/about" onClick={onClose}>
                      {intl.formatMessage({ id: 'menu.about' })}
                    </Link>
                    <Link href="https://web.cyanide-studio.com/bloodbowl/" isExternal>
                      {intl.formatMessage({ id: 'menu.cyanideAdminTools' })} <ExternalLinkIcon mx={2} />
                    </Link>
                  </VStack>
                </Box>

                <Spacer />
              </VStack>
              <VStack align="left">
                <ArticleFeed type={articleSubject?.type} subjectId={articleSubject?.id} leagueSystemId={selectedLeagueSystemId || new URLSearchParams(location.search).get('leagueSystem')} seasonId={selectedSeasonId || new URLSearchParams(location.search).get('season')} compact limit={4} />
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
