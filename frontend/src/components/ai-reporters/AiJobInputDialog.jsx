import React, { useMemo, useState } from 'react';
import {
  Alert,
  AlertIcon,
  Box,
  Button,
  HStack,
  Link,
  Modal,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
  ModalOverlay,
  Spinner,
  Textarea,
  Text,
  VStack,
} from '@chakra-ui/react';

const providerLinks = {
  openai: ['OpenAI Playground', 'https://platform.openai.com/playground'],
  cloudflare: ['Cloudflare Workers AI Playground', 'https://playground.ai.cloudflare.com/'],
  gemini: ['Google AI Studio', 'https://aistudio.google.com/prompts/new_chat'],
  groq: ['Groq Playground', 'https://console.groq.com/playground'],
  openrouter: ['OpenRouter Chat', 'https://openrouter.ai/chat'],
  mistral: ['Mistral Le Chat', 'https://chat.mistral.ai/'],
};

export default function AiJobInputDialog({ isOpen, onClose, details, loading, error, canSubmitResult, onSubmitResult }) {
  const [copyError, setCopyError] = useState('');
  const [result, setResult] = useState('');
  const [submitError, setSubmitError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const copiedText = useMemo(() => {
    if (!details) return '';
    const header = [details.title, details.provider ? `Provider: ${details.provider}` : null]
      .filter(Boolean)
      .join('\n');
    const sections = details.sections
      ?.map(section => `${section.title}\n${section.content || 'Not recorded'}`)
      .join('\n\n') || '';
    return [header, sections].filter(Boolean).join('\n\n');
  }, [details]);
  const providerLink = details?.provider
    ? providerLinks[details.provider.toLowerCase()]
    : null;

  const copyInput = async () => {
    setCopyError('');
    try {
      await navigator.clipboard.writeText(copiedText);
    } catch (reason) {
      setCopyError(`Could not copy input: ${reason?.message || 'clipboard access is unavailable'}`);
    }
  };

  const submitResult = async () => {
    setSubmitting(true);
    setSubmitError('');
    try {
      await onSubmitResult(result);
      setResult('');
    } catch (reason) {
      setSubmitError(reason?.response?.data?.message || reason?.message || 'Could not submit the external result.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="4xl" scrollBehavior="inside">
      <ModalOverlay />
      <ModalContent>
        <ModalHeader>{details?.title || 'AI job input'}</ModalHeader>
        <ModalCloseButton />
        <ModalBody>
          {loading && <HStack><Spinner /><Text>Loading saved input…</Text></HStack>}
          {error && <Alert status="error"><AlertIcon />{error}</Alert>}
          {details && (
            <VStack align="stretch" spacing={4}>
              {details.note && (
                <Alert status={details.complete ? 'info' : 'warning'}>
                  <AlertIcon />
                  <Text>{details.note}</Text>
                </Alert>
              )}
              {details.sections?.map((section, index) => (
                <Box key={`${section.title}-${index}`}>
                  <Text fontWeight="700" mb={1}>{section.title}</Text>
                  <Box
                    as="pre"
                    p={3}
                    borderWidth="1px"
                    borderRadius="md"
                    whiteSpace="pre-wrap"
                    overflowWrap="anywhere"
                    fontSize="sm"
                    maxH="420px"
                    overflowY="auto"
                  >
                    {section.content || 'Not recorded'}
                  </Box>
                </Box>
              ))}
              {canSubmitResult && <Box>
                <Text fontWeight="700" mb={1}>External model result</Text>
                <Textarea aria-label="External model result" value={result}
                  onChange={event => setResult(event.target.value)} maxLength={200000}
                  minH="180px" resize="vertical"
                  placeholder="Paste the model response here" />
                <Text fontSize="xs" color="gray.500" mt={1}>
                  The original job validates this response before saving its result. Maximum 200,000 characters.
                </Text>
              </Box>}
              {copyError && <Text color="red.300">{copyError}</Text>}
              {submitError && <Alert status="error"><AlertIcon />{submitError}</Alert>}
            </VStack>
          )}
        </ModalBody>
        <ModalFooter justifyContent="space-between" flexWrap="wrap" gap={2}>
          <HStack flexWrap="wrap">
            <Button as={Link} href="https://chatgpt.com/" isExternal variant="outline" size="sm">
              Open ChatGPT
            </Button>
            <Button as={Link} href="https://github.com/copilot" isExternal variant="outline" size="sm">
              Open Copilot
            </Button>
            {providerLink && (
              <Button as={Link} href={providerLink[1]} isExternal variant="outline" size="sm">
                Open {providerLink[0]}
              </Button>
            )}
          </HStack>
          <HStack>
            <Button onClick={copyInput} isDisabled={!copiedText} size="sm">Copy input</Button>
            {canSubmitResult && <Button colorScheme="blue" onClick={submitResult}
              isLoading={submitting} isDisabled={!result.trim()} size="sm">
              Submit result
            </Button>}
            <Button onClick={onClose} size="sm">Close</Button>
          </HStack>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
}
