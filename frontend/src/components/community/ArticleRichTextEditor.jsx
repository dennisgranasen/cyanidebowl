import React, { useEffect, useRef, useState } from 'react';
import { Box, Button, FormControl, FormLabel, HStack, IconButton, Input, Modal, ModalBody, ModalCloseButton, ModalContent, ModalFooter, ModalHeader, ModalOverlay, Select, Text, Tooltip, useColorModeValue, VStack } from '@chakra-ui/react';
import { FaBold, FaItalic, FaUnderline, FaStrikethrough, FaListUl, FaListOl, FaQuoteRight, FaMinus, FaLink, FaUnlink, FaImage, FaFileImage, FaUndo, FaRedo, FaRemoveFormat, FaEye, FaEdit, FaAlignLeft, FaAlignCenter, FaAlignRight, FaAlignJustify } from 'react-icons/fa';
import { EditorContent } from '@tiptap/react';
import { useIntl } from 'react-intl';
import DOMPurify from 'dompurify';
import { articleBodyStyles } from './articleBodyStyles';

export const validArticleUrl = value => /^(https?:\/\/|\/(?!\/))/i.test(value);

export default function ArticleRichTextEditor({
  editor, disabled, onUpload, onError, onMentionSearch, onMentionSelect,
}) {
  const intl = useIntl();
  const t = id => intl.formatMessage({ id });
  const input = useRef(null);
  const toolbarBg = useColorModeValue('gray.50', 'gray.800');
  const canvasBg = useColorModeValue('white', 'gray.900');
  const [preview, setPreview] = useState(false);
  const [dialog, setDialog] = useState(null);
  const [url, setUrl] = useState('');
  const [alt, setAlt] = useState('');
  const [mention, setMention] = useState(null);
  const [mentionOptions, setMentionOptions] = useState([]);
  const [mentionIndex, setMentionIndex] = useState(0);

  useEffect(() => {
    if (!editor || !onMentionSearch) return undefined;
    const detectMention = () => {
      const { selection } = editor.state;
      if (!selection.empty) { setMention(null); return; }
      const { $from } = selection;
      const text = editor.state.doc.textBetween($from.start(), $from.pos, '\n', ' ');
      const match = text.match(/(?:^|\s)@([^\s@]{0,80})$/u);
      if (!match) { setMention(null); return; }
      const leadingSpace = match[0].startsWith('@') ? 0 : 1;
      setMention({
        from: $from.pos - match[0].length + leadingSpace,
        to: $from.pos,
        query: match[1],
      });
    };
    editor.on('update', detectMention);
    editor.on('selectionUpdate', detectMention);
    detectMention();
    return () => {
      editor.off('update', detectMention);
      editor.off('selectionUpdate', detectMention);
    };
  }, [editor, onMentionSearch]);

  useEffect(() => {
    if (mention == null || !onMentionSearch || !mention.query) {
      setMentionOptions([]);
      setMentionIndex(0);
      return undefined;
    }
    let active = true;
    const timer = setTimeout(() => {
      Promise.resolve(onMentionSearch(mention.query))
        .then(rows => {
          if (!active) return;
          setMentionOptions(rows || []);
          setMentionIndex(0);
        })
        .catch(error => { if (active) onError?.(error); });
    }, 180);
    return () => { active = false; clearTimeout(timer); };
  }, [mention?.query, onMentionSearch, onError]);

  const chooseMention = option => {
    if (!editor || !mention || !option) return;
    editor.chain().focus()
      .deleteRange({ from: mention.from, to: mention.to })
      .insertContent([
        {
          type: 'text',
          text: `@${option.label}`,
          marks: [{
            type: 'link',
            attrs: {
              href: option.url || '#',
              editorialMentionType: option.type,
              editorialMentionId: option.id,
            },
          }],
        },
        { type: 'text', text: ' ' },
      ]).run();
    onMentionSelect?.(option);
    setMention(null);
    setMentionOptions([]);
    setMentionIndex(0);
  };

  const mentionKeyDown = event => {
    if (!mention || !mentionOptions.length) return;
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setMentionIndex(index => (index + 1) % mentionOptions.length);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setMentionIndex(index => (index - 1 + mentionOptions.length) % mentionOptions.length);
    } else if (event.key === 'Enter' || event.key === 'Tab') {
      event.preventDefault();
      chooseMention(mentionOptions[mentionIndex]);
    } else if (event.key === 'Escape') {
      event.preventDefault();
      setMention(null);
      setMentionOptions([]);
    }
  };

  const open = kind => {
    const attrs = editor?.getAttributes(kind === 'image' ? 'image' : 'link') || {};
    setUrl(attrs.src || attrs.href || ''); setAlt(attrs.alt || ''); setDialog(kind);
  };
  const apply = () => {
    if (!validArticleUrl(url)) { onError(new Error(t('news.invalidUrl'))); return; }
    if (dialog === 'image') editor.chain().focus().setImage({ src: url, alt }).run();
    else if (editor.state.selection.empty && !editor.getAttributes('link').href) {
      editor.chain().focus().insertContent({ type: 'text', text: url, marks: [{ type: 'link', attrs: { href: url } }] }).run();
    } else editor.chain().focus().extendMarkRange('link').setLink({ href: url }).run();
    setDialog(null);
  };
  const control = (id, Icon, action, active = false, unavailable = false) => <Tooltip key={id} label={t(id)} hasArrow openDelay={350}>
    <IconButton type="button" size="sm" variant={active ? 'solid' : 'ghost'} colorScheme={active ? 'blue' : 'gray'}
      icon={<Icon />} aria-label={t(id)} aria-pressed={active} title={t(id)} borderRadius="sm"
      isDisabled={disabled || !editor || preview || unavailable} onMouseDown={e => e.preventDefault()} onClick={action} />
  </Tooltip>;
  const toggle = (id, name, Icon, action) => control(id, Icon, () => action(editor.chain().focus()).run(), !!editor?.isActive(name));
  const separator = key => <Box key={key} aria-hidden="true" borderLeftWidth="1px" h="24px" mx={1} />;
  const heading = [1, 2, 3].find(level => editor?.isActive('heading', { level })) || 0;
  return <Box borderWidth="1px" borderRadius="lg" overflow="hidden" boxShadow="sm" bg={canvasBg}
    _focusWithin={{ borderColor: 'blue.400', boxShadow: '0 0 0 1px var(--chakra-colors-blue-400)' }}>
    <HStack role="toolbar" aria-label={t('news.formatting')} p={2} flexWrap="wrap" borderBottomWidth="1px" spacing={0.5} bg={toolbarBg}>
      {control('news.undo', FaUndo, () => editor.chain().focus().undo().run(), false, !editor?.can().undo())}
      {control('news.redo', FaRedo, () => editor.chain().focus().redo().run(), false, !editor?.can().redo())}
      {separator('history')}
      <Select size="sm" width="140px" borderRadius="sm" value={heading} aria-label={t('news.textStyle')} isDisabled={disabled || preview || !editor} onChange={e => {
        const level = Number(e.target.value);
        if (level) editor.chain().focus().setHeading({ level }).run(); else editor.chain().focus().setParagraph().run();
      }}><option value={0}>{t('news.paragraph')}</option>{[1, 2, 3].map(n => <option key={n} value={n}>{t('news.headingStyle')} {n}</option>)}</Select>
      {separator('style')}
      {toggle('news.bold', 'bold', FaBold, c => c.toggleBold())}
      {toggle('news.italic', 'italic', FaItalic, c => c.toggleItalic())}
      {toggle('news.underline', 'underline', FaUnderline, c => c.toggleUnderline())}
      {toggle('news.strike', 'strike', FaStrikethrough, c => c.toggleStrike())}
      {separator('marks')}
      {[['left', FaAlignLeft], ['center', FaAlignCenter], ['right', FaAlignRight], ['justify', FaAlignJustify]].map(([align, Icon]) =>
        control(`news.align.${align}`, Icon, () => editor.chain().focus().setTextAlign(align).run(), !!editor?.isActive({ textAlign: align })))}
      {separator('alignment')}
      {toggle('news.bulletList', 'bulletList', FaListUl, c => c.toggleBulletList())}
      {toggle('news.orderedList', 'orderedList', FaListOl, c => c.toggleOrderedList())}
      {toggle('news.quote', 'blockquote', FaQuoteRight, c => c.toggleBlockquote())}
      {control('news.divider', FaMinus, () => editor.chain().focus().setHorizontalRule().run())}
      {separator('blocks')}
      {control('news.insertLink', FaLink, () => open('link'), !!editor?.isActive('link'))}
      {control('news.unlink', FaUnlink, () => editor.chain().focus().unsetLink().run(), false, !editor?.isActive('link'))}
      {control('news.addImage', FaImage, () => input.current?.click())}
      {control('news.imageUrl', FaFileImage, () => open('image'), !!editor?.isActive('image'))}
      {separator('insert')}
      {control('news.clearFormat', FaRemoveFormat, () => editor.chain().focus().unsetAllMarks().clearNodes().unsetTextAlign().run())}
      <Tooltip label={t(preview ? 'news.edit' : 'news.preview')}><IconButton size="sm" variant={preview ? 'solid' : 'ghost'}
        colorScheme={preview ? 'blue' : 'gray'} icon={preview ? <FaEdit /> : <FaEye />} aria-label={t(preview ? 'news.edit' : 'news.preview')}
        aria-pressed={preview} onClick={() => setPreview(v => !v)} /></Tooltip>
    </HStack>
    <Input ref={input} type="file" hidden multiple accept="image/png,image/jpeg" onChange={e => {
      const files = Array.from(e.target.files || []); e.target.value = ''; onUpload(files);
    }} aria-label={t('news.upload')} />
    {preview ? <Box p={4} minH="360px" sx={articleBodyStyles} dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(editor?.getHTML() || '') }} />
      : <Box onKeyDownCapture={mentionKeyDown}>
          <Box p={{ base: 2, md: 4 }} maxH="640px" overflowY="auto"
            sx={{
              '.tiptap': { ...articleBodyStyles, minHeight: '320px', padding: '.5em', outline: 'none' },
              '.ProseMirror-selectednode': { outline: '3px solid #4299e1' },
              '.editorial-mention': { fontWeight: 600, textDecoration: 'underline', textDecorationStyle: 'dotted' },
            }}>
            <EditorContent editor={editor} />
          </Box>
          {mention && <Box borderTopWidth="1px" bg={toolbarBg} p={2}>
            <Text fontSize="xs" opacity={0.7} mb={1}>@{mention.query}</Text>
            {!mention.query && <Text fontSize="sm">Type a name to search teams, players, coaches, star players, fans and staff.</Text>}
            {!!mention.query && !mentionOptions.length && <Text fontSize="sm">No matching subjects.</Text>}
            {mentionOptions.map((option, index) => <Button key={`${option.type}:${option.id}`} size="sm" mr={1} mb={1}
              variant={index === mentionIndex ? 'solid' : 'ghost'} colorScheme={index === mentionIndex ? 'blue' : 'gray'}
              onMouseDown={event => event.preventDefault()} onClick={() => chooseMention(option)}>
              {option.label} · {option.type.replaceAll('_', ' ').toLowerCase()}
            </Button>)}
          </Box>}
        </Box>}
    <Text fontSize="sm" p={3} opacity={0.75} borderTopWidth="1px">{t('news.imageDropHelp')}</Text>
    <Modal isOpen={!!dialog} onClose={() => setDialog(null)}><ModalOverlay /><ModalContent>
      <ModalHeader>{t(dialog === 'image' ? 'news.imageUrl' : 'news.insertLink')}</ModalHeader><ModalCloseButton />
      <ModalBody><VStack align="stretch"><FormControl><FormLabel>{t('news.linkUrl')}</FormLabel><Input autoFocus value={url} onChange={e => setUrl(e.target.value)} /></FormControl>
        {dialog === 'image' && <FormControl><FormLabel>{t('news.imageAlt')}</FormLabel><Input value={alt} onChange={e => setAlt(e.target.value)} /></FormControl>}
      </VStack></ModalBody><ModalFooter><Button colorScheme="blue" onClick={apply} isDisabled={!url.trim()}>{t('news.insert')}</Button></ModalFooter>
    </ModalContent></Modal>
  </Box>;
}
