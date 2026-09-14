import React, { useEffect, useState } from 'react';
import {
  Alert, AlertIcon, Badge, Box, Button, FormControl, FormLabel,
  HStack, Menu, MenuButton, MenuItem, MenuList, SimpleGrid, Text,
  Textarea, VStack,
} from '@chakra-ui/react';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';
import EditorialCommunityApi from '../../EditorialCommunityApi';
import NuffleDiceGlyph, { ratingGlyphs } from '../NuffleDiceGlyph';

const playerIdValue = (id) => {
  if (!id) return id;
  const separator = id.indexOf('_');
  return separator >= 0 ? id.slice(separator + 1) : id;
};

const samePlayerId = (left, right) =>
  left === right || playerIdValue(left) === playerIdValue(right);

const RatingGlyph = ({ value, fontSize = '1.35rem' }) => {
  if (value == null) return <Text as="span" color="gray.400">—</Text>;
  return (
    <NuffleDiceGlyph
      glyph={ratingGlyphs(Math.max(-3, Math.min(3, Math.round(value))))}
      label={`Rating ${value > 0 ? `+${value}` : value}`}
      fontSize={fontSize}
    />
  );
};

const RatingValue = ({ aggregate }) => {
  if (!aggregate?.count || aggregate.average == null) {
    return <Text color="gray.400">—</Text>;
  }
  return (
    <HStack spacing={1.5}>
      <RatingGlyph value={aggregate.average}/>
      <Text fontSize="sm" fontWeight="600">{aggregate.average.toFixed(1)}</Text>
      <Text fontSize="xs" color="gray.500">({aggregate.count})</Text>
    </HStack>
  );
};

const RatingCategory = ({ label, match, season, onClick = null }) => (
  <Box
    borderWidth="1px"
    borderRadius="md"
    p={2.5}
    role={onClick ? 'button' : undefined}
    tabIndex={onClick ? 0 : undefined}
    cursor={onClick ? 'pointer' : undefined}
    _hover={onClick ? { borderColor: 'orange.400' } : undefined}
    onClick={onClick || undefined}
    onKeyDown={onClick ? (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        onClick();
      }
    } : undefined}
  >
    <HStack justify="space-between">
      <Text fontSize="xs" textTransform="uppercase" color="gray.500" fontWeight="700">
        {label}
      </Text>
      {onClick && (
        <Text fontSize="xs" color="orange.300">Visa detaljer →</Text>
      )}
    </HStack>
    <HStack mt={1} justify="space-between" align="start">
      <Box>
        <Text fontSize="xs" color="gray.500">Match</Text>
        <RatingValue aggregate={match}/>
      </Box>
      <Box>
        <Text fontSize="xs" color="gray.500">Säsong</Text>
        <RatingValue aggregate={season}/>
      </Box>
    </HStack>
  </Box>
);

function MatchPlayerRatings({ matchId, replayReady = false }) {
  const { isAuthenticated, getAccessTokenSilently } = useAuth0WithUserPermissions();
  const [players, setPlayers] = useState([]);
  const [overview, setOverview] = useState([]);
  const [caps, setCaps] = useState(null);
  const [instruction, setInstruction] = useState('');
  const [generating, setGenerating] = useState(false);
  const [jobStatus, setJobStatus] = useState(null);
  const [message, setMessage] = useState('');
  const [fanAvailability, setFanAvailability] = useState({ activeFanCount: 0 });
  const [fanJobStatus, setFanJobStatus] = useState(null);
  const [fanMessage, setFanMessage] = useState('');

  const [editorialDetailPlayer, setEditorialDetailPlayer] = useState(null);
  const [editorialDetailRatings, setEditorialDetailRatings] = useState([]);
  const [editorialDetailLoading, setEditorialDetailLoading] = useState(false);
  const [editorialDetailError, setEditorialDetailError] = useState('');


  const load = async () => {
    const [p, o, capabilityData, status, fanAvailabilityData, fanStatus] = await Promise.all([
      EditorialCommunityApi.matchPlayers(matchId),
      EditorialCommunityApi.matchRatingOverview(
        matchId,
        isAuthenticated ? getAccessTokenSilently : null,
      ),
      isAuthenticated
        ? EditorialCommunityApi.matchArticleCapabilities(
            matchId, getAccessTokenSilently).catch(() => null)
        : Promise.resolve(null),
      EditorialCommunityApi.aiPlayerRatingStatus(matchId).catch(() => null),
      EditorialCommunityApi.fanPlayerRatingAvailability(matchId).catch(() => ({ activeFanCount: 0 })),
      EditorialCommunityApi.fanPlayerRatingStatus(matchId).catch(() => null),
    ]);
    setPlayers(p || []);
    setOverview(o || []);
    setCaps(capabilityData);
    setJobStatus(status);
    setFanAvailability(fanAvailabilityData || { activeFanCount: 0 });
    setFanJobStatus(fanStatus);
  };

  useEffect(() => { load(); }, [matchId, isAuthenticated]);

  useEffect(() => {
    if (!jobStatus || jobStatus.complete) return undefined;
    const timer = window.setInterval(() => {
      load().catch(() => {});
    }, 2000);
    return () => window.clearInterval(timer);
  }, [jobStatus?.jobId, jobStatus?.complete, matchId, isAuthenticated]);

  useEffect(() => {
    if (!fanJobStatus || fanJobStatus.complete) return undefined;
    const timer = window.setInterval(() => { load().catch(() => {}); }, 2000);
    return () => window.clearInterval(timer);
  }, [fanJobStatus?.jobId, fanJobStatus?.complete, matchId, isAuthenticated]);

  const byPlayer = Object.fromEntries(
    overview.map((item) => [item.playerId, item]));
  const rate = async (playerId, score) => {
    if (score === '') return;
    await EditorialCommunityApi.ratePlayer(
      matchId, playerId, Number(score), getAccessTokenSilently);
    await load();
  };

  const generateAll = async () => {
    if (generating) return;
    setGenerating(true);
    setMessage('');
    try {
      const result = await EditorialCommunityApi.generateAiPlayerRatings(
        matchId,
        {
          reporterIds: [],
          instruction: instruction.trim() || null,
          force: true,
        },
        getAccessTokenSilently
      );
      setJobStatus(result);
      setMessage(
        `Granskningen är köad för ${result.reporterCount || 0} reportrar.`
      );
      await load();
    } catch (error) {
      setMessage(
        error?.response?.data?.message
        || error?.message
        || 'Betygsättningen misslyckades.'
      );
    } finally {
      setGenerating(false);
    }
  };

  const generateFans = async () => {
    if (!fanAvailability?.activeFanCount || (fanJobStatus && !fanJobStatus.complete)) return;
    setFanMessage('');
    try {
      const result = await EditorialCommunityApi.generateFanPlayerRatings(
        matchId,
        { reporterIds: [], instruction: instruction.trim() || null, force: true },
        getAccessTokenSilently
      );
      setFanJobStatus(result);
      setFanMessage(`Granskningen är köad för ${result.fanCount || 0} aktiva fans.`);
      await load();
    } catch (error) {
      setFanMessage(error?.response?.data?.message || error?.message || 'Fan-granskningen misslyckades.');
    }
  };

  const cancelRatingJob = async () => {
    if (!jobStatus?.jobId) return;
    try {
      const cancelled = await EditorialCommunityApi.cancelAiPlayerRatingJob(
        matchId, jobStatus.jobId, getAccessTokenSilently
      );
      setJobStatus(cancelled);
      setMessage('Granskningen avbröts.');
      await load();
    } catch (error) {
      setMessage(error?.response?.data?.message || error.message || 'Kunde inte avbryta granskningen.');
    }
  };

  const openEditorialDetail = async (player) => {
    setEditorialDetailPlayer(player);
    setEditorialDetailRatings([]);
    setEditorialDetailError('');
    setEditorialDetailLoading(true);
    try {
      const allRatings = await EditorialCommunityApi.aiPlayerRatings(matchId);
      setEditorialDetailRatings(
        (allRatings || []).filter((rating) =>
          samePlayerId(player.playerId, rating.playerId)
          && rating.sourceType !== 'FAN'
        )
      );
    } catch (error) {
      setEditorialDetailError(
        error?.response?.data?.message
        || error?.message
        || 'Kunde inte läsa redaktionens betyg.'
      );
    } finally {
      setEditorialDetailLoading(false);
    }
  };

  const closeEditorialDetail = () => {
    setEditorialDetailPlayer(null);
    setEditorialDetailRatings([]);
    setEditorialDetailError('');
  };

  const canGenerateAi = Boolean(
    replayReady && (caps?.canReview || caps?.canDeleteAny)
  );
  const ratingJobActive = Boolean(jobStatus && !jobStatus.complete);
  const ratingBusy = generating || ratingJobActive;

  if (editorialDetailPlayer) {
    return (
      <VStack align="stretch" spacing={4}>
        <HStack>
          <Button size="sm" variant="ghost" onClick={closeEditorialDetail}>
            ← Tillbaka till spelarbetyg
          </Button>
        </HStack>

        <Box>
          <Text fontSize="lg" fontWeight="700">
            Redaktionen · {editorialDetailPlayer.playerName}
          </Text>
          <Text fontSize="sm" color="gray.500">
            Individuella reporterbetyg och kommentarer för den här matchen.
          </Text>
        </Box>

        {editorialDetailLoading && (
          <Text color="gray.500">Läser redaktionens betyg…</Text>
        )}

        {editorialDetailError && (
          <Alert status="error">
            <AlertIcon/>{editorialDetailError}
          </Alert>
        )}

        {!editorialDetailLoading && !editorialDetailError && (
          <VStack align="stretch" spacing={3}>
            {editorialDetailRatings
              .slice()
              .sort((a, b) =>
                String(a.reporterId || '').localeCompare(String(b.reporterId || '')))
              .map((rating) => (
                <Box key={rating.id} borderWidth="1px" borderRadius="md" p={3}>
                  <HStack justify="space-between" align="start">
                    <Badge>{rating.reporterId}</Badge>
                    <HStack>
                      <RatingGlyph value={rating.rating}/>
                      <Text fontWeight="700">
                        {rating.rating > 0 ? `+${rating.rating}` : rating.rating}
                      </Text>
                    </HStack>
                  </HStack>
                  {rating.verdict && (
                    <Text mt={2} fontSize="sm">{rating.verdict}</Text>
                  )}
                  {(rating.providerId || rating.model) && (
                    <Text mt={2} fontSize="xs" color="gray.500">
                      {[rating.providerId, rating.model].filter(Boolean).join(' · ')}
                    </Text>
                  )}
                </Box>
              ))}

            {editorialDetailRatings.length === 0 && (
              <Text color="gray.500">
                Det finns ännu inga individuella redaktionsbetyg för spelaren.
              </Text>
            )}
          </VStack>
        )}
      </VStack>
    );
  }

  return (
    <VStack align="stretch" spacing={4}>
      {canGenerateAi && (
        <Box borderWidth="1px" borderRadius="md" p={3}>
          <Text fontWeight="700">Generera spelarbetyg</Text>
          <Text mt={1} fontSize="sm" color="gray.500">
            Alla aktiva betygsagenter får samma fakta och betygsätter alla
            spelare i ett anrop per agent. Skalan är tre skallar (-3) till
            tre POWs (+3).
          </Text>
          <FormControl mt={3}>
            <FormLabel fontSize="sm">
              Instruktion till agenterna (valfritt)
            </FormLabel>
            <Textarea
              value={instruction}
              onChange={(e) => setInstruction(e.target.value)}
              placeholder="T.ex. var extra hård mot onödiga turnovers, men håll er till replayfakta…"
            />
          </FormControl>
          <Button
            mt={3}
            colorScheme="orange"
            onClick={generateAll}
            isLoading={ratingBusy}
            isDisabled={ratingBusy}
          >
            Låt redaktionen betygsätta
          </Button>
          <Button
            mt={3}
            ml={{ base: 0, md: 2 }}
            variant="outline"
            colorScheme="orange"
            onClick={generateFans}
            isDisabled={!fanAvailability?.activeFanCount || Boolean(fanJobStatus && !fanJobStatus.complete)}
          >
            Låt aktiva fans betygsätta
            {fanAvailability?.activeFanCount > 0 ? ` (${fanAvailability.activeFanCount})` : ''}
          </Button>
          {!fanAvailability?.activeFanCount && (
            <Text mt={1} fontSize="xs" color="gray.500">Inga aktiva fans är kopplade till lagen i matchen.</Text>
          )}
          {fanMessage && <Alert mt={3} status="info"><AlertIcon/>{fanMessage}</Alert>}
          {fanJobStatus && !fanJobStatus.complete && (
            <Alert mt={3} status="info" alignItems="flex-start">
              <AlertIcon mt={1}/>
              <Box>
                <Text fontWeight="700">Community-fansen granskar matchen…</Text>
                <Text fontSize="sm">
                  {fanJobStatus.succeeded} av {fanJobStatus.fanCount} klara
                  {' · '}{fanJobStatus.running} arbetar
                  {' · '}{fanJobStatus.queued} väntar
                  {fanJobStatus.failed > 0 ? ` · ${fanJobStatus.failed} misslyckades` : ''}
                </Text>
              </Box>
            </Alert>
          )}
          {fanJobStatus?.failed > 0 && (
            <Alert mt={3} status="warning">
              <AlertIcon/>
              <Box>
                <Text fontWeight="700">{fanJobStatus.failed} fan-granskningar misslyckades.</Text>
                {Object.entries(fanJobStatus.errors || {}).map(([name, error]) => (
                  <Text key={name} fontSize="xs">{name}: {error}</Text>
                ))}
              </Box>
            </Alert>
          )}
          {ratingBusy && (
            <Alert mt={3} status="info" alignItems="flex-start">
              <AlertIcon mt={1}/>
              <Box w="full">
                <Text fontWeight="700">
                  Redaktionen granskar spelarnas prestationer…
                </Text>
                {jobStatus ? (
                  <>
                    <Text fontSize="sm">
                      {jobStatus.succeeded} av {jobStatus.reporterCount} reportrar klara
                      {' · '}{jobStatus.running} arbetar
                      {' · '}{jobStatus.queued} väntar
                      {jobStatus.failed > 0 ? ` · ${jobStatus.failed} misslyckades` : ''}
                      {jobStatus.cancelled > 0 ? ` · ${jobStatus.cancelled} avbrutna` : ''}
                    </Text>
                    <Text fontSize="xs" color="gray.500" mt={1}>
                      Betygen uppdateras automatiskt när varje reporter blir klar.
                      Du kan byta flik och fortsätta med annat under tiden.
                    </Text>
                    <Button mt={2} size="xs" variant="outline" onClick={cancelRatingJob}>
                      Avbryt granskning
                    </Button>
                  </>
                ) : (
                  <Text fontSize="sm">Granskningen startas…</Text>
                )}
              </Box>
            </Alert>
          )}
          {message && (
            <Alert mt={3} status="info">
              <AlertIcon/>{message}
            </Alert>
          )}
          {jobStatus?.failed > 0 && (
            <Alert mt={3} status="warning" alignItems="flex-start">
              <AlertIcon mt={1}/>
              <Box>
                <Text fontWeight="700">
                  {jobStatus.failed} reportergranskningar misslyckades.
                </Text>
                <VStack mt={1} align="stretch" spacing={0}>
                  {(jobStatus.tasks || [])
                    .filter((task) => task.status === 'FAILED')
                    .map((task) => (
                      <Text key={task.reporterId} fontSize="xs">
                        {task.reporterId}: {task.error}
                      </Text>
                    ))}
                </VStack>
              </Box>
            </Alert>
          )}
        </Box>
      )}

      {players.map((player) => {
        const item = byPlayer[player.playerId];
        return (
          <Box key={player.playerId} borderBottomWidth="1px" pb={4}>
            <HStack justify="space-between" align="start">
              <Box>
                <HStack>
                  <Text fontWeight="700">{player.playerName}</Text>
                  {!player.participated && <Badge>MNG</Badge>}
                </HStack>
                {item?.mine != null && (
                  <HStack mt={1} spacing={2}>
                    <Text fontSize="sm" color="gray.500">Ditt betyg:</Text>
                    <RatingGlyph value={item.mine}/>
                    <Text fontSize="sm" fontWeight="600">
                      {item.mine > 0 ? `+${item.mine}` : item.mine}
                    </Text>
                  </HStack>
                )}
              </Box>

              {isAuthenticated && player.participated && (
                <Menu>
                  <MenuButton as={Button} size="sm" variant="outline" w="190px">
                    {item?.mine != null ? 'Ändra ditt betyg' : 'Sätt ditt betyg'}
                  </MenuButton>
                  <MenuList minW="190px">
                    {[-3,-2,-1,0,1,2,3].map((n) => (
                      <MenuItem key={n} onClick={() => rate(player.playerId, n)}>
                        <HStack justify="space-between" w="full">
                          <RatingGlyph value={n}/>
                          <Text fontSize="sm">{n > 0 ? `+${n}` : n}</Text>
                        </HStack>
                      </MenuItem>
                    ))}
                  </MenuList>
                </Menu>
              )}
            </HStack>

            {item && (
              <SimpleGrid mt={3} columns={{ base: 1, md: 2, xl: 4 }} spacing={2}>
                <RatingCategory
                  label="Totalt"
                  match={item.matchTotal}
                  season={item.seasonTotal}
                />
                <RatingCategory
                  label="Community"
                  match={item.matchCommunity}
                  season={item.seasonCommunity}
                />
                <RatingCategory
                  label="Coacher"
                  match={item.matchCoaches}
                  season={item.seasonCoaches}
                />
                <RatingCategory
                  label="Redaktionen"
                  match={item.matchEditorial}
                  season={item.seasonEditorial}
                  onClick={() => openEditorialDetail(player)}
                />
              </SimpleGrid>
            )}

          </Box>
        );
      })}
    </VStack>
  );
}

export default MatchPlayerRatings;
