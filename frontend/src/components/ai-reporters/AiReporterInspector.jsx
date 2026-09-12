import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  AlertDialog,
  AlertDialogBody,
  AlertDialogContent,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogOverlay,
  AlertIcon,
  Badge,
  Box,
  Button,
  Code,
  Divider,
  FormControl,
  FormLabel,
  HStack,
  Input,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  NumberInput,
  NumberInputField,
  Select,
  Spinner,
  Stack,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
  Text,
  Textarea,
  VStack,
  useDisclosure,
} from '@chakra-ui/react';
import AiReporterApi from '../../AiReporterApi';

const parseSubjects = (text) => text
  .split('\n')
  .map((line) => line.trim())
  .filter(Boolean)
  .map((line) => {
    const split = line.indexOf(':');
    if (split < 1 || split === line.length - 1) {
      throw new Error(`Ogiltigt subject: ${line}. Använd TYPE:id.`);
    }
    return {
      type: line.slice(0, split).trim().toUpperCase(),
      id: line.slice(split + 1).trim(),
    };
  });

const subjectsToText = (subjects = []) =>
  subjects.map((subject) => `${subject.type}:${subject.id}`).join('\n');

const formatTime = (value) =>
  value ? new Date(value).toLocaleString() : '—';

function ConfirmDialog({ state, onClose, onConfirm, busy }) {
  const cancelRef = useRef();

  return (
    <AlertDialog
      isOpen={Boolean(state)}
      leastDestructiveRef={cancelRef}
      onClose={busy ? () => {} : onClose}
      isCentered
    >
      <AlertDialogOverlay>
        <AlertDialogContent>
          <AlertDialogHeader fontSize="lg" fontWeight="700">
            {state?.title || 'Bekräfta ändring'}
          </AlertDialogHeader>
          <AlertDialogBody>
            <Text>{state?.message}</Text>
            {state?.detail && (
              <Box mt={3} p={3} borderWidth="1px" borderRadius="md">
                <Text fontSize="sm" whiteSpace="pre-wrap">{state.detail}</Text>
              </Box>
            )}
            <Alert status="warning" mt={4}>
              <AlertIcon />
              Detta ändrar reporterns persistenta AI-state och kan påverka framtida texter.
            </Alert>
          </AlertDialogBody>
          <AlertDialogFooter>
            <Button ref={cancelRef} onClick={onClose} isDisabled={busy}>
              Avbryt
            </Button>
            <Button
              colorScheme="red"
              ml={3}
              onClick={onConfirm}
              isLoading={busy}
            >
              Bekräfta ändring
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialogOverlay>
    </AlertDialog>
  );
}

function MemoryEditor({ isOpen, onClose, memory, onPrepare }) {
  const [body, setBody] = useState('');
  const [subjects, setSubjects] = useState('');

  useEffect(() => {
    if (!isOpen) return;
    setBody(memory?.body || '');
    setSubjects(subjectsToText(memory?.subjects || []));
  }, [isOpen, memory]);

  const prepare = () => {
    let parsed;
    try {
      parsed = parseSubjects(subjects);
    } catch (error) {
      onPrepare({ error: error.message });
      return;
    }
    if (!body.trim()) {
      onPrepare({ error: 'Minnestext krävs.' });
      return;
    }
    onPrepare({
      payload: {
        body: body.trim(),
        subjects: parsed,
        active: memory?.active ?? true,
      },
      detail:
        `Minnestext:\n${body.trim()}\n\nSubjects:\n`
        + (parsed.length ? parsed.map((s) => `${s.type}:${s.id}`).join('\n') : '(inga)'),
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="xl">
      <ModalOverlay />
      <ModalContent>
        <ModalHeader>{memory ? 'Redigera minne' : 'Injicera minne'}</ModalHeader>
        <ModalCloseButton />
        <ModalBody>
          <FormControl>
            <FormLabel>Memory</FormLabel>
            <Textarea
              minH="180px"
              value={body}
              onChange={(e) => setBody(e.target.value)}
              maxLength={4000}
            />
          </FormControl>
          <FormControl mt={4}>
            <FormLabel>Subjects</FormLabel>
            <Textarea
              minH="120px"
              value={subjects}
              onChange={(e) => setSubjects(e.target.value)}
              placeholder={'TEAM:team-id\nCOACH_IDENTITY:coach-id\nCOMPETITION:competition-id'}
            />
            <Text mt={1} fontSize="xs" color="gray.500">
              Ett subject per rad i formatet TYPE:id. Tomt är tillåtet för fristående minnen.
            </Text>
          </FormControl>
        </ModalBody>
        <ModalFooter>
          <Button variant="ghost" onClick={onClose}>Avbryt</Button>
          <Button ml={3} colorScheme="orange" onClick={prepare}>
            Förbered ändring
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
}

function RelationshipEditor({ isOpen, onClose, relationship, onPrepare }) {
  const [subjectType, setSubjectType] = useState('TEAM');
  const [subjectId, setSubjectId] = useState('');
  const [subjectDisplayName, setSubjectDisplayName] = useState('');
  const [sentiment, setSentiment] = useState(0);
  const [confidence, setConfidence] = useState(1);
  const [rationale, setRationale] = useState('');

  useEffect(() => {
    if (!isOpen) return;
    setSubjectType(relationship?.subjectType || 'TEAM');
    setSubjectId(relationship?.subjectId || '');
    setSubjectDisplayName(relationship?.subjectDisplayName || '');
    setSentiment(Number.isFinite(relationship?.sentiment) ? relationship.sentiment : 0);
    setConfidence(1);
    setRationale(relationship?.rationale || '');
  }, [isOpen, relationship]);

  const prepare = () => {
    if (!subjectId.trim() || !rationale.trim()) {
      onPrepare({ error: 'Subject ID och rationale krävs.' });
      return;
    }
    const payload = {
      subjectType,
      subjectId: subjectId.trim(),
      subjectDisplayName: subjectDisplayName.trim() || null,
      sentiment,
      confidence,
      rationale: rationale.trim(),
    };
    onPrepare({
      payload,
      detail:
        `${subjectType}:${payload.subjectId}`
        + `${payload.subjectDisplayName ? ` (${payload.subjectDisplayName})` : ''}\n`
        + `Sentiment: ${sentiment}\nConfidence: ${confidence}\n`
        + `Rationale: ${payload.rationale}`,
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="lg">
      <ModalOverlay />
      <ModalContent>
        <ModalHeader>
          {relationship ? 'Justera relation' : 'Lägg till manuell relation'}
        </ModalHeader>
        <ModalCloseButton />
        <ModalBody>
          <Stack spacing={4}>
            <FormControl>
              <FormLabel>Target type</FormLabel>
              <Select
                value={subjectType}
                onChange={(e) => setSubjectType(e.target.value)}
                isDisabled={Boolean(relationship)}
              >
                <option value="TEAM">TEAM</option>
                <option value="COACH_IDENTITY">COACH_IDENTITY</option>
              </Select>
            </FormControl>
            <FormControl>
              <FormLabel>Subject ID</FormLabel>
              <Input
                value={subjectId}
                onChange={(e) => setSubjectId(e.target.value)}
                isReadOnly={Boolean(relationship)}
              />
            </FormControl>
            <FormControl>
              <FormLabel>Display name</FormLabel>
              <Input
                value={subjectDisplayName}
                onChange={(e) => setSubjectDisplayName(e.target.value)}
              />
            </FormControl>
            <FormControl>
              <FormLabel>Sentiment (-1 … +1)</FormLabel>
              <NumberInput
                min={-1}
                max={1}
                step={0.05}
                precision={2}
                value={sentiment}
                onChange={(_, value) => Number.isFinite(value) && setSentiment(value)}
              >
                <NumberInputField />
              </NumberInput>
            </FormControl>
            <FormControl>
              <FormLabel>Confidence (0 … 1)</FormLabel>
              <NumberInput
                min={0}
                max={1}
                step={0.05}
                precision={2}
                value={confidence}
                onChange={(_, value) => Number.isFinite(value) && setConfidence(value)}
              >
                <NumberInputField />
              </NumberInput>
            </FormControl>
            <FormControl>
              <FormLabel>Rationale</FormLabel>
              <Textarea
                value={rationale}
                onChange={(e) => setRationale(e.target.value)}
                maxLength={500}
              />
            </FormControl>
          </Stack>
        </ModalBody>
        <ModalFooter>
          <Button variant="ghost" onClick={onClose}>Avbryt</Button>
          <Button ml={3} colorScheme="orange" onClick={prepare}>
            Förbered ändring
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
}

export default function AiReporterInspector({
  reporterId,
  getAccessTokenSilently,
  getAccessTokenWithPopup,
}) {
  const [state, setState] = useState(null);
  const [runtime, setRuntime] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [editingMemory, setEditingMemory] = useState(null);
  const [editingRelationship, setEditingRelationship] = useState(null);
  const [confirm, setConfirm] = useState(null);

  const memoryModal = useDisclosure();
  const relationshipModal = useDisclosure();
  const auth = useMemo(
    () => [getAccessTokenSilently, getAccessTokenWithPopup],
    [getAccessTokenSilently, getAccessTokenWithPopup]
  );

  const load = async () => {
    setError(null);
    try {
      const [loadedState, loadedRuntime] = await Promise.all([
        AiReporterApi.inspectorState(reporterId, ...auth),
        AiReporterApi.adminReporter(reporterId, ...auth),
      ]);
      setState(loadedState);
      setRuntime(loadedRuntime);
    } catch (reason) {
      setError(reason);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reporterId]);

  const runConfirmed = async () => {
    if (!confirm?.action) return;
    setBusy(true);
    setError(null);
    try {
      await confirm.action();
      setConfirm(null);
      memoryModal.onClose();
      relationshipModal.onClose();
      setEditingMemory(null);
      setEditingRelationship(null);
      await load();
    } catch (reason) {
      setError(reason);
    } finally {
      setBusy(false);
    }
  };

  const prepareMemory = ({ payload, detail, error: formError }) => {
    if (formError) {
      setError(new Error(formError));
      return;
    }
    const current = editingMemory;
    setConfirm({
      title: current ? 'Bekräfta ändring av minne' : 'Bekräfta injicering av minne',
      message: current
        ? 'Du är på väg att skriva om ett persistent minne.'
        : 'Du är på väg att injicera ett nytt persistent minne.',
      detail,
      action: () => current
        ? AiReporterApi.updateMemory(reporterId, current.id, payload, ...auth)
        : AiReporterApi.injectMemory(reporterId, payload, ...auth),
    });
  };

  const prepareDeactivateMemory = (memory) => {
    setConfirm({
      title: 'Deaktivera minne?',
      message: 'Minnet tas inte bort men kommer inte längre användas som aktiv MEMORY-context.',
      detail: memory.body,
      action: () => AiReporterApi.updateMemory(
        reporterId,
        memory.id,
        { body: memory.body, subjects: memory.subjects || [], active: false },
        ...auth
      ),
    });
  };

  const prepareRelationship = ({ payload, detail, error: formError }) => {
    if (formError) {
      setError(new Error(formError));
      return;
    }
    setConfirm({
      title: 'Bekräfta manuell relationsändring',
      message:
        'Ändringen läggs som en explicit Technician-observation och vägs ihop med automatisk evidens.',
      detail,
      action: () => AiReporterApi.setManualRelationship(reporterId, payload, ...auth),
    });
  };

  const prepareClearManualRelationship = (relationship) => {
    setConfirm({
      title: 'Ta bort manuell relationspåverkan?',
      message:
        'Endast Technician-observationen tas bort. Automatisk evidens från publicerade artiklar behålls.',
      detail: `${relationship.subjectType}:${relationship.subjectId}`,
      action: () => AiReporterApi.clearManualRelationship(
        reporterId,
        relationship.subjectType,
        relationship.subjectId,
        ...auth
      ),
    });
  };

  if (!state && !error) return <Spinner size="sm" />;
  if (error && !state) {
    return <Alert status="error"><AlertIcon />{error.message || String(error)}</Alert>;
  }

  return (
    <Box mt={8} borderWidth="1px" borderRadius="lg" overflow="hidden">
      <Box px={4} py={3} bg="gray.50" _dark={{ bg: 'gray.800' }}>
        <HStack justify="space-between">
          <Box>
            <Text fontWeight="700">Technician · Internal state</Text>
            <Text fontSize="xs" color="gray.500">
              Persistent MEMORY, social attitudes, runtime och senaste AI-provenance.
            </Text>
          </Box>
          <Button size="xs" variant="outline" onClick={load}>Uppdatera</Button>
        </HStack>
      </Box>

      {error && (
        <Alert status="error">
          <AlertIcon />{error.message || String(error)}
        </Alert>
      )}

      <Tabs variant="enclosed" size="sm">
        <TabList px={3} pt={3} overflowX="auto">
          <Tab>Memories</Tab>
          <Tab>Relationships</Tab>
          <Tab>Activity</Tab>
          <Tab>Runtime</Tab>
        </TabList>
        <TabPanels>
          <TabPanel>
            <HStack justify="space-between" mb={4}>
              <Text fontWeight="700">
                Memories <Badge ml={2}>{state.memories?.length || 0}</Badge>
              </Text>
              <HStack>
                <Button
                  size="sm"
                  variant="outline"
                  colorScheme="orange"
                  onClick={() => setConfirm({
                    title: 'Rekonsolidera publicerad historik?',
                    message:
                      'Upp till 100 publicerade AI-matchartiklar körs om genom nuvarande memory/relationship-policy. '
                      + 'Det kan ändra minnen och relationer och gör LLM-anrop.',
                    detail:
                      'Detta är avsiktligt ett Technician-verktyg för migration/rebuild efter policyändringar.',
                    action: () => AiReporterApi.reconsolidateReporter(
                      reporterId, { limit: 100 }, ...auth),
                  })}
                >
                  Rekonsolidera historik
                </Button>
                <Button
                  size="sm"
                  colorScheme="orange"
                  onClick={() => {
                    setEditingMemory(null);
                    memoryModal.onOpen();
                  }}
                >
                  Injicera minne
                </Button>
              </HStack>
            </HStack>
            <VStack align="stretch" spacing={3}>
              {(state.memories || []).map((memory) => (
                <Box key={memory.id} p={3} borderWidth="1px" borderRadius="md">
                  <HStack justify="space-between" align="start">
                    <Box>
                      <HStack>
                        <Badge colorScheme={memory.active ? 'green' : 'gray'}>
                          {memory.active ? 'ACTIVE' : 'INACTIVE'}
                        </Badge>
                        {memory.manual && <Badge colorScheme="orange">MANUAL</Badge>}
                      </HStack>
                      <Text mt={2} whiteSpace="pre-wrap">{memory.body}</Text>
                    </Box>
                    <HStack>
                      <Button
                        size="xs"
                        variant="outline"
                        onClick={() => {
                          setEditingMemory(memory);
                          memoryModal.onOpen();
                        }}
                      >
                        Redigera
                      </Button>
                      {memory.active && (
                        <Button
                          size="xs"
                          colorScheme="red"
                          variant="outline"
                          onClick={() => prepareDeactivateMemory(memory)}
                        >
                          Deaktivera
                        </Button>
                      )}
                    </HStack>
                  </HStack>
                  <Divider my={3} />
                  <Text fontSize="xs" color="gray.500">
                    Subjects: {(memory.subjects || []).map((s) => `${s.type}:${s.id}`).join(', ') || '—'}
                  </Text>
                  <Text fontSize="xs" color="gray.500">
                    Sources: {(memory.sourceContentIds || []).join(', ') || '—'}
                  </Text>
                  {memory.supersededByMemoryId && (
                    <Text fontSize="xs" color="orange.400">
                      Superseded by: {memory.supersededByMemoryId}
                      {' · '}{formatTime(memory.supersededAt)}
                    </Text>
                  )}
                  <Text fontSize="xs" color="gray.500">
                    Updated: {formatTime(memory.updatedAt)}
                  </Text>
                </Box>
              ))}
              {(state.memories || []).length === 0 && (
                <Text color="gray.500">Inga persistenta minnen ännu.</Text>
              )}
            </VStack>
          </TabPanel>

          <TabPanel>
            <HStack justify="space-between" mb={4}>
              <Text fontWeight="700">
                Relationships <Badge ml={2}>{state.relationships?.length || 0}</Badge>
              </Text>
              <Button
                size="sm"
                colorScheme="orange"
                onClick={() => {
                  setEditingRelationship(null);
                  relationshipModal.onOpen();
                }}
              >
                Lägg till relation
              </Button>
            </HStack>
            <VStack align="stretch" spacing={3}>
              {(state.relationships || []).map((relationship) => (
                <Box key={relationship.id} p={3} borderWidth="1px" borderRadius="md">
                  <HStack justify="space-between" align="start">
                    <Box>
                      <HStack wrap="wrap">
                        <Badge>{relationship.type}</Badge>
                        <Badge colorScheme={relationship.active ? 'green' : 'gray'}>
                          {relationship.active ? 'ACTIVE' : 'INACTIVE'}
                        </Badge>
                        {relationship.hasManualOverride && (
                          <Badge colorScheme="orange">MANUAL OVERRIDE</Badge>
                        )}
                      </HStack>
                      <Text mt={2} fontWeight="700">
                        {relationship.subjectDisplayName || relationship.subjectId}
                      </Text>
                      <Text fontSize="sm" color="gray.500">
                        {relationship.subjectType}:{relationship.subjectId}
                      </Text>
                    </Box>
                    <HStack>
                      <Button
                        size="xs"
                        variant="outline"
                        onClick={() => {
                          setEditingRelationship(relationship);
                          relationshipModal.onOpen();
                        }}
                      >
                        Justera
                      </Button>
                      {relationship.hasManualOverride && (
                        <Button
                          size="xs"
                          colorScheme="red"
                          variant="outline"
                          onClick={() => prepareClearManualRelationship(relationship)}
                        >
                          Ta bort override
                        </Button>
                      )}
                    </HStack>
                  </HStack>
                  <HStack mt={3} spacing={6}>
                    <Box>
                      <Text fontSize="xs" color="gray.500">Sentiment</Text>
                      <Text fontWeight="700">
                        {relationship.sentiment == null
                          ? '—' : relationship.sentiment.toFixed(2)}
                      </Text>
                    </Box>
                    <Box>
                      <Text fontSize="xs" color="gray.500">Confidence</Text>
                      <Text fontWeight="700">
                        {relationship.confidence == null
                          ? '—' : relationship.confidence.toFixed(2)}
                      </Text>
                    </Box>
                    <Box>
                      <Text fontSize="xs" color="gray.500">Evidence</Text>
                      <Text fontWeight="700">{relationship.evidenceCount ?? 0}</Text>
                    </Box>
                  </HStack>
                  {relationship.rationale && (
                    <Text mt={3} fontSize="sm">{relationship.rationale}</Text>
                  )}
                  {(relationship.evidence || []).length > 0 && (
                    <Box mt={3}>
                      <Text fontSize="xs" fontWeight="700" mb={1}>Evidence</Text>
                      {(relationship.evidence || []).map((evidence, index) => (
                        <Text key={`${evidence.sourceContentId}-${index}`} fontSize="xs" color="gray.500">
                          {evidence.manual ? '[MANUAL] ' : ''}
                          {evidence.sourceContentId} · s={evidence.sentiment?.toFixed?.(2) ?? evidence.sentiment}
                          {' · '}c={evidence.confidence?.toFixed?.(2) ?? evidence.confidence}
                          {evidence.rationale ? ` · ${evidence.rationale}` : ''}
                        </Text>
                      ))}
                    </Box>
                  )}
                </Box>
              ))}
              {(state.relationships || []).length === 0 && (
                <Text color="gray.500">
                  Inga relationer ännu. Det är normalt tills publicerade artiklar uttryckt
                  en tydlig attityd eller en Technician har lagt till en.
                </Text>
              )}
            </VStack>
          </TabPanel>

          <TabPanel>
            <Text fontWeight="700" mb={4}>
              Generation traces <Badge ml={2}>{state.traces?.length || 0}</Badge>
            </Text>
            <VStack align="stretch" spacing={3}>
              {(state.traces || []).map((trace) => {
                const sectionCounts = (trace.contextItems || []).reduce((acc, item) => {
                  acc[item.section] = (acc[item.section] || 0) + 1;
                  return acc;
                }, {});
                return (
                  <Box key={trace.id} p={3} borderWidth="1px" borderRadius="md">
                    <HStack justify="space-between" align="start">
                      <Box>
                        <HStack wrap="wrap">
                          <Badge colorScheme={trace.status === 'SUCCESS' ? 'green' : 'red'}>
                            {trace.status}
                          </Badge>
                          <Badge>{trace.taskType}</Badge>
                          <Badge variant="outline">{trace.providerId}</Badge>
                          <Badge variant="outline">{trace.model}</Badge>
                        </HStack>
                        <Text mt={2} fontSize="xs" color="gray.500">
                          {formatTime(trace.createdAt)} · {trace.durationMs ?? '—'} ms
                        </Text>
                      </Box>
                      <Text fontSize="xs" color="gray.500">
                        {trace.inputTokens ?? '—'} in / {trace.outputTokens ?? '—'} out
                      </Text>
                    </HStack>

                    {trace.status === 'FAILED' && (
                      <Alert status="error" mt={3}>
                        <AlertIcon />
                        <Box>
                          <Text fontSize="sm" fontWeight="700">
                            {trace.failureKind || 'FAILED'}
                            {trace.failureStatusCode ? ` · HTTP ${trace.failureStatusCode}` : ''}
                          </Text>
                          {trace.failureMessage && (
                            <Text fontSize="xs">{trace.failureMessage}</Text>
                          )}
                        </Box>
                      </Alert>
                    )}

                    <HStack mt={3} spacing={4} wrap="wrap">
                      {Object.entries(sectionCounts).map(([section, count]) => (
                        <Badge key={section} variant="subtle">
                          {section}: {count}
                        </Badge>
                      ))}
                    </HStack>

                    <Text mt={3} fontSize="xs" color="gray.500">
                      Context estimate: {trace.estimatedContextTokens ?? '—'} tokens ·
                      dropped: {trace.droppedContextItems ?? 0} ·
                      instruction: {trace.taskInstructionChars ?? '—'} chars
                      {trace.taskInstructionTruncated ? ' (preview truncated)' : ''}
                    </Text>

                    <Box as="details" mt={3}>
                      <Box as="summary" cursor="pointer" fontSize="sm" fontWeight="700">
                        Visa context snapshot
                      </Box>
                      <Box mt={3}>
                        {(trace.hardConstraints || []).length > 0 && (
                          <Box mb={4}>
                            <Text fontSize="xs" fontWeight="700" mb={1}>Hard constraints</Text>
                            {(trace.hardConstraints || []).map((constraint, index) => (
                              <Text key={index} fontSize="xs" color="gray.500">
                                • {constraint}
                              </Text>
                            ))}
                          </Box>
                        )}

                        {(trace.contextItems || []).map((item, index) => (
                          <Box
                            key={`${item.section}-${item.id}-${index}`}
                            mb={3}
                            p={2}
                            borderWidth="1px"
                            borderRadius="md"
                          >
                            <HStack wrap="wrap">
                              <Badge>{item.section}</Badge>
                              <Badge variant="outline">{item.contentType}</Badge>
                              <Badge variant="outline">{item.source}</Badge>
                              <Badge variant="outline">{item.authority}</Badge>
                            </HStack>
                            <Code mt={2} fontSize="xs">{item.id}</Code>
                            {item.title && (
                              <Text mt={2} fontSize="sm" fontWeight="700">{item.title}</Text>
                            )}
                            {item.authorDisplayName && (
                              <Text fontSize="xs" color="gray.500">
                                Author: {item.authorDisplayName}
                              </Text>
                            )}
                            {(item.subjects || []).length > 0 && (
                              <Text fontSize="xs" color="gray.500">
                                Subjects: {(item.subjects || [])
                                  .map((subject) => `${subject.type}:${subject.id}`)
                                  .join(', ')}
                              </Text>
                            )}
                            {item.body && (
                              <Text mt={2} fontSize="xs" whiteSpace="pre-wrap">
                                {item.body}
                                {item.bodyTruncated ? '\n[…truncated…]' : ''}
                              </Text>
                            )}
                          </Box>
                        ))}

                        <Divider my={3} />
                        <Text fontSize="xs" fontWeight="700">Task instruction preview</Text>
                        <Text mt={1} fontSize="xs" color="gray.500">
                          SHA-256: {trace.taskInstructionSha256 || '—'}
                        </Text>
                        <Box
                          mt={2}
                          p={2}
                          borderWidth="1px"
                          borderRadius="md"
                          maxH="360px"
                          overflow="auto"
                        >
                          <Text fontSize="xs" whiteSpace="pre-wrap">
                            {trace.taskInstructionPreview || '—'}
                            {trace.taskInstructionTruncated ? '\n[…truncated…]' : ''}
                          </Text>
                        </Box>
                      </Box>
                    </Box>
                  </Box>
                );
              })}
              {(state.traces || []).length === 0 && (
                <Text color="gray.500">
                  Inga generation traces ännu. Nya AI-anrop sparas här automatiskt.
                </Text>
              )}
            </VStack>

            <Divider my={6} />

            <Text fontWeight="700" mb={4}>
              Recent AI article activity <Badge ml={2}>{state.activity?.length || 0}</Badge>
            </Text>
            <VStack align="stretch" spacing={3}>
              {(state.activity || []).map((activity) => (
                <Box key={activity.id} p={3} borderWidth="1px" borderRadius="md">
                  <HStack justify="space-between">
                    <Text fontWeight="700">{activity.title || '(utan rubrik)'}</Text>
                    <Badge>{activity.status}</Badge>
                  </HStack>
                  <Text mt={1} fontSize="xs" color="gray.500">
                    Match: {activity.matchId}
                  </Text>
                  <Text fontSize="xs" color="gray.500">
                    Provider/model: {activity.providerId || '—'} / {activity.model || '—'}
                  </Text>
                  <Text fontSize="xs" color="gray.500">
                    Request: {activity.providerRequestId || '—'}
                  </Text>
                  <Text fontSize="xs" color="gray.500">
                    Tokens: {activity.inputTokens ?? '—'} in / {activity.outputTokens ?? '—'} out
                  </Text>
                  <Text fontSize="xs" color="gray.500">
                    Updated: {formatTime(activity.updatedAt)}
                  </Text>
                </Box>
              ))}
              {(state.activity || []).length === 0 && (
                <Text color="gray.500">Ingen AI-artikelaktivitet ännu.</Text>
              )}
            </VStack>
          </TabPanel>

          <TabPanel>
            {!runtime ? <Spinner size="sm" /> : (
              <Stack spacing={2}>
                <Text><b>Reporter:</b> {runtime.alias}</Text>
                <Text><b>User ID:</b> {state.userId}</Text>
                <Text><b>Enabled:</b> {String(runtime.enabled)}</Text>
                <Text><b>Reports:</b> {String(runtime.reportsEnabled)}</Text>
                <Text><b>Interactions:</b> {String(runtime.interactionsEnabled)}</Text>
                <Text><b>Ratings:</b> {String(runtime.playerRatingsEnabled)}</Text>
                <Text><b>Writing weight:</b> {runtime.writingWeight}</Text>
                <Text><b>Effective language:</b> {runtime.primaryLanguage || '—'}</Text>
                <Text><b>Profile language:</b> {runtime.profileLanguage || '—'}</Text>
                <Divider />
                <Text fontSize="xs" color="gray.500">
                  Runtime overrides kan fortsatt ändras via kugghjulet på reporterprofilen.
                </Text>
              </Stack>
            )}
          </TabPanel>
        </TabPanels>
      </Tabs>

      <MemoryEditor
        isOpen={memoryModal.isOpen}
        onClose={memoryModal.onClose}
        memory={editingMemory}
        onPrepare={prepareMemory}
      />
      <RelationshipEditor
        isOpen={relationshipModal.isOpen}
        onClose={relationshipModal.onClose}
        relationship={editingRelationship}
        onPrepare={prepareRelationship}
      />
      <ConfirmDialog
        state={confirm}
        onClose={() => setConfirm(null)}
        onConfirm={runConfirmed}
        busy={busy}
      />
    </Box>
  );
}
