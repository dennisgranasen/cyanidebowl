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

export default function AiJobInputDialog({ isOpen, onClose, details, loading, error }) {
  const [copyError, setCopyError] = useState('');
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
              {copyError && <Text color="red.300">{copyError}</Text>}
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
            <Button onClick={onClose} size="sm">Close</Button>
          </HStack>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
}
