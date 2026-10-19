import React, { useEffect, useRef, useState } from 'react';
import {
  Alert, AlertIcon, Badge, Box, Button, FormControl, FormLabel,
  HStack, Menu, MenuButton, MenuItem, MenuList, Progress, Select, SimpleGrid, Spinner, Text,
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

export const uniquePlayers = (players) => {
  const seen = new Set();
  return players.filter((player, index) => {
    const normalizedId = playerIdValue(player.playerId);
    const key = normalizedId || `missing-player-id-${index}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
};

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
  const [dataLoading, setDataLoading] = useState(true);
  const [dataLoaded, setDataLoaded] = useState(false);
  const [dataLoadError, setDataLoadError] = useState(null);
  const [loadElapsedSeconds, setLoadElapsedSeconds] = useState(0);
  const loadRequestRef = useRef(null);
  const supplementalLoadRequestRef = useRef(null);
  const displayedMatchKeyRef = useRef(null);
  const [caps, setCaps] = useState(null);
  const [instruction, setInstruction] = useState('');
  const [jobStatus, setJobStatus] = useState(null);
  const [message, setMessage] = useState('');
  const [fanAvailability, setFanAvailability] = useState(null);
  const [fanJobStatus, setFanJobStatus] = useState(null);
  const [fanMessage, setFanMessage] = useState('');
  const [copilotTargets, setCopilotTargets] = useState([]);
  const [selectedCopilotTarget, setSelectedCopilotTarget] = useState('');
  const [copilotBudget, setCopilotBudget] = useState(null);
  const [loadingCopilotBudget, setLoadingCopilotBudget] = useState(false);
  const [quotaUsageHistory, setQuotaUsageHistory] = useState([]);

  const [editorialDetailPlayer, setEditorialDetailPlayer] = useState(null);
  const [editorialDetailRatings, setEditorialDetailRatings] = useState([]);
  const [editorialDetailLoading, setEditorialDetailLoading] = useState(false);
  const [editorialDetailError, setEditorialDetailError] = useState('');


  const load = async () => {
    const requestKey = `${matchId}:${isAuthenticated}`;
    if (loadRequestRef.current?.key === requestKey) return loadRequestRef.current.promise;

    if (displayedMatchKeyRef.current !== requestKey) {
      displayedMatchKeyRef.current = requestKey;
      setPlayers([]);
      setOverview([]);
      setFanAvailability(null);
      setDataLoaded(false);
    }
    const requestId = Symbol(requestKey);
    supplementalLoadRequestRef.current = requestId;
    const startedAt = Date.now();
    setDataLoading(true);
    setLoadElapsedSeconds(0);
    setDataLoadError(null);
    const promise = (async () => {
      let timeoutId;
      try {
        const [p, o] = await Promise.race([
          Promise.all([
            EditorialCommunityApi.matchPlayers(matchId),
            EditorialCommunityApi.matchRatingOverview(
              matchId,
              isAuthenticated ? getAccessTokenSilently : null,
            )
          ]),
          new Promise((resolve, reject) => {
            timeoutId = window.setTimeout(
              () => reject(new Error('Inläsningen tog längre än 60 sekunder. Kontrollera anslutningen och försök igen.')),
              60000,
            );
          }),
        ]);
        if (loadRequestRef.current?.requestId !== requestId) return;
        setPlayers(p || []);
        setOverview(o || []);
        setDataLoaded(true);
        const applySupplemental = (promise, setter) => {
          const apply = value => {
            if (displayedMatchKeyRef.current === requestKey
                && supplementalLoadRequestRef.current === requestId) {
              setter(value || null);
            }
          };
          promise.then(apply).catch(() => apply(null));
        };
        if (isAuthenticated) {
          applySupplemental(
            EditorialCommunityApi.matchArticleCapabilities(
              matchId, getAccessTokenSilently),
            setCaps,
          );
        }
        applySupplemental(EditorialCommunityApi.aiPlayerRatingStatus(matchId), setJobStatus);
        applySupplemental(
          EditorialCommunityApi.fanPlayerRatingAvailability(matchId),
          setFanAvailability,
        );
        applySupplemental(
          EditorialCommunityApi.fanPlayerRatingStatus(matchId),
          setFanJobStatus,
        );
      } catch (error) {
        if (loadRequestRef.current?.requestId === requestId) setDataLoadError(error);
      } finally {
        window.clearTimeout(timeoutId);
        if (loadRequestRef.current?.requestId === requestId) {
          loadRequestRef.current = null;
          setDataLoading(false);
        }
      }
    })();
    loadRequestRef.current = { key: requestKey, requestId, promise, startedAt };
    return promise;
  };

  useEffect(() => { load(); }, [matchId, isAuthenticated]);

  useEffect(() => {
    if (!dataLoading) return undefined;
    const timer = window.setInterval(() => {
      const startedAt = loadRequestRef.current?.startedAt;
      if (startedAt) setLoadElapsedSeconds(Math.floor((Date.now() - startedAt) / 1000));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [dataLoading]);

  useEffect(() => {
    if (!isAuthenticated || !(caps?.canReview || caps?.canDeleteAny)) return undefined;
    let active = true;
    EditorialCommunityApi.aiPlayerRatingCopilotTargets(matchId, getAccessTokenSilently)
      .then(targets => {
      if (!active) return;
      setCopilotTargets(targets || []);
      setSelectedCopilotTarget(current => current || targets?.[0]?.targetId || '');
    }).catch(() => {
      if (active) {
        setCopilotTargets([]);
      }
    });
    EditorialCommunityApi.aiPlayerRatingCopilotBudget(matchId, getAccessTokenSilently)
      .then(budget => {
        if (active) setCopilotBudget(budget || null);
      })
      .catch(() => {
        if (active) setCopilotBudget({
          configured: false,
          message: 'Kvotinformationen är tillfälligt otillgänglig.',
        });
      });
    return () => { active = false; };
  }, [matchId, isAuthenticated, getAccessTokenSilently, caps?.canReview, caps?.canDeleteAny]);

  useEffect(() => {
    if (!isAuthenticated || !(caps?.canReview || caps?.canDeleteAny)) return undefined;
    let active = true;
    EditorialCommunityApi.aiPlayerRatingQuotaUsage(matchId, getAccessTokenSilently)
      .then(history => {
        if (active) setQuotaUsageHistory(history || []);
      })
      .catch(() => {
        if (active) setQuotaUsageHistory([]);
      });
    return () => { active = false; };
  }, [matchId, isAuthenticated, getAccessTokenSilently,
    caps?.canReview, caps?.canDeleteAny, jobStatus?.complete, fanJobStatus?.complete]);

  useEffect(() => {
    const pollEditorial = Boolean(jobStatus && !jobStatus.complete);
    const pollFans = Boolean(fanJobStatus && !fanJobStatus.complete);
    if (!pollEditorial && !pollFans) return undefined;
    const timer = window.setInterval(async () => {
      const [status, fanStatus] = await Promise.all([
        pollEditorial
          ? EditorialCommunityApi.aiPlayerRatingStatus(matchId).catch(() => null)
          : Promise.resolve(null),
        pollFans
          ? EditorialCommunityApi.fanPlayerRatingStatus(matchId).catch(() => null)
          : Promise.resolve(null),
      ]);
      let completed = false;
      if (status) {
        setJobStatus(status);
        completed ||= status.complete;
      }
      if (fanStatus) {
        setFanJobStatus(fanStatus);
        completed ||= fanStatus.complete;
      }
      if (completed) await load();
    }, 2000);
    return () => window.clearInterval(timer);
  }, [jobStatus?.jobId, jobStatus?.complete, fanJobStatus?.jobId,
    fanJobStatus?.complete, matchId, isAuthenticated]);

  const byPlayer = Object.fromEntries(
    overview.map((item) => [item.playerId, item]));
  const rate = async (playerId, score) => {
    if (score === '') return;
    await EditorialCommunityApi.ratePlayer(
      matchId, playerId, Number(score), getAccessTokenSilently);
    await load();
  };

  const refreshCopilotBudget = async () => {
    if (!isAuthenticated) return;
    setLoadingCopilotBudget(true);
    try {
      setCopilotBudget(await EditorialCommunityApi.aiPlayerRatingCopilotBudget(
        matchId, getAccessTokenSilently
      ));
    } catch {
      setCopilotBudget(null);
    } finally {
      setLoadingCopilotBudget(false);
    }
  };

  const generateFans = async () => {
    if (!fanAvailability?.unratedFanCount || (fanJobStatus && !fanJobStatus.complete)) return;
    setFanMessage('');
    const submit = confirmCopilotQuota => EditorialCommunityApi.generateFanPlayerRatings(
      matchId,
      {
        reporterIds: [],
        instruction: instruction.trim() || null,
        force: false,
        targetOverride: selectedCopilotTarget || null,
        confirmCopilotQuota,
      },
      getAccessTokenSilently
    );
    try {
      let result;
      try {
        result = await submit(false);
      } catch (reason) {
        const warning = reason?.response?.data;
        if (reason?.response?.status !== 409
            || warning?.code !== 'COPILOT_QUOTA_CONFIRMATION_REQUIRED') throw reason;
        if (!window.confirm(warning.message)) return;
        result = await submit(true);
      }
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
  const ratingBusy = ratingJobActive;
  const unratedFans = fanAvailability?.unratedFanCount || 0;
  const estimatedTokens = (calls) => ({ inputLow: calls * 8000, inputHigh: calls * 16000,
    outputLow: calls * 500, outputHigh: calls * 1500 });
  const fanTokenEstimate = estimatedTokens(unratedFans);

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
          <Text fontWeight="700">Granska spelarbetyg</Text>
          <Text mt={1} fontSize="sm" color="gray.500">
            Alla aktiva betygsagenter får samma fakta och betygsätter alla
            spelare i ett anrop per agent. Skalan är tre skallar (-3) till
            tre POWs (+3).
          </Text>
          <FormControl mt={3}>
            <FormLabel fontSize="sm">
              Instruktion till fan-granskningen (valfritt)
            </FormLabel>
            <Textarea
              value={instruction}
              onChange={(e) => setInstruction(e.target.value)}
              placeholder="T.ex. var extra hård mot onödiga turnovers, men håll er till replayfakta…"
            />
          </FormControl>
          <FormControl mt={3} maxW="420px">
            <FormLabel fontSize="sm">Copilot-modell</FormLabel>
            <Select
              value={selectedCopilotTarget}
              onChange={event => setSelectedCopilotTarget(event.target.value)}
              isDisabled={ratingBusy || Boolean(fanJobStatus && !fanJobStatus.complete)}
            >
              {copilotTargets.map(target => (
                <option key={target.targetId} value={target.targetId}>
                  {target.model} · GitHub Copilot
                </option>
              ))}
            </Select>
          </FormControl>
          <Box mt={3} borderWidth="1px" borderRadius="md" p={3}>
            <HStack justify="space-between">
              <Text fontWeight="700" fontSize="sm">Kostnadsöversikt före start</Text>
              <Button size="xs" variant="outline" onClick={refreshCopilotBudget}
                isLoading={loadingCopilotBudget}>
                Uppdatera kvot
              </Button>
            </HStack>
            <Text mt={1} fontSize="sm">
              Fans: {unratedFans} av {fanAvailability?.activeFanCount || 0} intresserade behöver rösta, cirka {fanTokenEstimate.inputLow.toLocaleString()}–{fanTokenEstimate.inputHigh.toLocaleString()} inputtokens + {fanTokenEstimate.outputLow.toLocaleString()}–{fanTokenEstimate.outputHigh.toLocaleString()} outputtokens.
            </Text>
            <Text mt={1} fontSize="sm" fontWeight="600">
              Planerat: {unratedFans} Copilot-anrop för fans. Kvotavdraget kan inte räknas om till tokens och beror på vald modells kvotvikt.
            </Text>
            {copilotBudget?.premiumInteractions && (
              <Text mt={1} fontSize="xs" color="gray.500">
                Aktuell Copilot-kvot: {copilotBudget.premiumInteractions.usedRequests ?? '—'} använda av {copilotBudget.premiumInteractions.entitlementRequests ?? '—'}
                {copilotBudget.premiumInteractions.remainingPercentage != null
                  ? ` (${copilotBudget.premiumInteractions.remainingPercentage}% kvar)` : ''}.
                {copilotBudget.premiumInteractions.resetDate
                  ? ` Återställs ${new Date(copilotBudget.premiumInteractions.resetDate).toLocaleString()}.` : ''}
              </Text>
            )}
            {copilotBudget && !copilotBudget.configured && (
              <Text mt={1} fontSize="xs" color="orange.300">
                Copilot-kvot kunde inte läsas: {copilotBudget.message || 'Copilot är inte konfigurerat.'}
              </Text>
            )}
            <Text mt={1} fontSize="xs" color="gray.500">
              Tokenintervallet är en uppskattning per röst; verklig mängd beror på matchfakta, kontext och svarslängd. Kvotens faktiska avdrag syns när Copilot har uppdaterat kontostatistiken.
            </Text>
            {quotaUsageHistory.length > 0 && (
              <Box mt={3} pt={2} borderTopWidth="1px">
                <Text fontSize="xs" fontWeight="700" mb={1}>Senaste kvotmätningar</Text>
                {quotaUsageHistory.slice(0, 5).map(entry => (
                  <HStack key={entry.id} justify="space-between" align="start" fontSize="xs" py={1}>
                    <Text>
                      {entry.ratingKind === 'FANS' ? 'Fans' : 'Redaktion'} · {entry.model || entry.targetId} · {entry.succeededCalls}/{entry.plannedCalls} lyckade
                      {entry.failedCalls ? ` · ${entry.failedCalls} misslyckade` : ''}
                    </Text>
                    <Text textAlign="right" color="gray.500">
                      {entry.observedUsedRequestDelta == null
                        ? 'Kvotdifferens saknas'
                        : `Observerad ändring: ${entry.observedUsedRequestDelta > 0 ? '+' : ''}${entry.observedUsedRequestDelta} använda`}
                      {entry.quotaBefore?.usedRequests != null && entry.quotaAfter?.usedRequests != null
                        ? ` (${entry.quotaBefore.usedRequests} → ${entry.quotaAfter.usedRequests})` : ''}
                    </Text>
                  </HStack>
                ))}
                <Text fontSize="xs" color="gray.500" mt={1}>
                  Differensen gäller hela Copilot-kontot under körningen och kan även innehålla annan samtidig Copilot-användning.
                </Text>
              </Box>
            )}
          </Box>
          <Button
            mt={3}
            ml={0}
            variant="outline"
            colorScheme="orange"
            onClick={generateFans}
            isDisabled={!unratedFans || Boolean(fanJobStatus && !fanJobStatus.complete) || !selectedCopilotTarget}
          >
            Låt oröstande fans betygsätta ({unratedFans})
          </Button>
          {fanAvailability && !fanAvailability.activeFanCount && (
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

      {dataLoading && (
        <Box mt={3} role="status" aria-live="polite">
          {dataLoaded ? (
            <HStack><Spinner size="sm"/><Text fontSize="sm">Uppdaterar spelartrupper och betyg… {loadElapsedSeconds}s</Text></HStack>
          ) : (
            <Box borderWidth="1px" borderRadius="md" p={3}>
              <HStack align="start">
                <Spinner size="sm" mt={1}/>
                <Box>
                  <Text fontWeight="700">Hämtar spelartrupper och betyg…</Text>
                  <Text fontSize="sm" color="gray.500">
                    Läser matchens spelare och betyg. Väntat {loadElapsedSeconds} sekunder.
                    {loadElapsedSeconds >= 15 && ' Servern arbetar fortfarande; du kan vänta kvar medan den hämtar uppgifterna.'}
                  </Text>
                </Box>
              </HStack>
              <Progress mt={3} size="xs" isIndeterminate aria-label="Hämtar spelartrupper och betyg"/>
            </Box>
          )}
        </Box>
      )}
      {dataLoadError && (
        <Alert mt={3} status={dataLoaded ? 'warning' : 'error'} alignItems="flex-start">
          <AlertIcon mt={1}/>
          <Box>
            <Text fontWeight="700">
              {dataLoaded ? 'Kunde inte uppdatera spelartrupper och betyg.' : 'Kunde inte läsa in spelartrupper och betyg.'}
            </Text>
            <Text fontSize="sm">{dataLoadError.message || 'Kontrollera anslutningen och försök igen.'}</Text>
            <Button mt={2} size="sm" variant="outline" onClick={load} isLoading={dataLoading}>
              Försök igen
            </Button>
          </Box>
        </Alert>
      )}

      {uniquePlayers(players).map((player) => {
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
