import React, { useRef, useState } from 'react';
import { Box, Button, FormControl, FormLabel, HStack, Input, Modal, ModalBody, ModalCloseButton, ModalContent, ModalFooter, ModalHeader, ModalOverlay, Select, Text, VStack } from '@chakra-ui/react';
import { EditorContent } from '@tiptap/react';
import { useIntl } from 'react-intl';
import DOMPurify from 'dompurify';
import { articleBodyStyles } from './articleBodyStyles';

export const validArticleUrl = value => /^(https?:\/\/|\/(?!\/))/i.test(value);

export default function ArticleRichTextEditor({ editor, disabled, onUpload, onError }) {
  const intl = useIntl();
  const t = id => intl.formatMessage({ id });
  const input = useRef(null);
  const [preview, setPreview] = useState(false);
  const [dialog, setDialog] = useState(null);
  const [url, setUrl] = useState('');
  const [alt, setAlt] = useState('');
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
  const toggle = (id, name, action) => <Button key={id} size="sm" variant={editor?.isActive(name) ? 'solid' : 'ghost'}
    colorScheme={editor?.isActive(name) ? 'blue' : 'gray'} aria-pressed={!!editor?.isActive(name)}
    isDisabled={disabled || preview} onClick={() => action(editor.chain().focus()).run()}>{t(id)}</Button>;
  const heading = [1, 2, 3].find(level => editor?.isActive('heading', { level })) || 0;
  return <Box borderWidth="1px" borderRadius="md" overflow="hidden">
    <HStack role="toolbar" aria-label={t('news.formatting')} p={2} flexWrap="wrap" borderBottomWidth="1px" spacing={1}>
      <Select size="sm" width="auto" value={heading} aria-label={t('news.textStyle')} isDisabled={disabled || preview} onChange={e => {
        const level = Number(e.target.value);
        if (level) editor.chain().focus().setHeading({ level }).run(); else editor.chain().focus().setParagraph().run();
      }}><option value={0}>{t('news.paragraph')}</option>{[1, 2, 3].map(n => <option key={n} value={n}>{t('news.headingStyle')} {n}</option>)}</Select>
      {toggle('news.bold', 'bold', c => c.toggleBold())}
      {toggle('news.italic', 'italic', c => c.toggleItalic())}
      {toggle('news.strike', 'strike', c => c.toggleStrike())}
      {toggle('news.bulletList', 'bulletList', c => c.toggleBulletList())}
      {toggle('news.orderedList', 'orderedList', c => c.toggleOrderedList())}
      {toggle('news.quote', 'blockquote', c => c.toggleBlockquote())}
      <Button size="sm" variant="ghost" isDisabled={disabled || preview} onClick={() => editor.chain().focus().setHorizontalRule().run()}>{t('news.divider')}</Button>
      <Button size="sm" variant="ghost" isDisabled={disabled || preview} onClick={() => open('link')}>{t('news.insertLink')}</Button>
      <Button size="sm" variant="ghost" isDisabled={disabled || preview || !editor?.isActive('link')} onClick={() => editor.chain().focus().unsetLink().run()}>{t('news.unlink')}</Button>
      <Button size="sm" colorScheme="blue" isDisabled={disabled || preview} onClick={() => input.current?.click()}>{t('news.addImage')}</Button>
      <Button size="sm" variant="ghost" isDisabled={disabled || preview} onClick={() => open('image')}>{t('news.imageUrl')}</Button>
      <Button size="sm" variant="ghost" isDisabled={disabled || preview || !editor?.can().undo()} onClick={() => editor.chain().focus().undo().run()}>{t('news.undo')}</Button>
      <Button size="sm" variant="ghost" isDisabled={disabled || preview || !editor?.can().redo()} onClick={() => editor.chain().focus().redo().run()}>{t('news.redo')}</Button>
      <Button size="sm" variant="ghost" isDisabled={disabled || preview} onClick={() => editor.chain().focus().unsetAllMarks().clearNodes().run()}>{t('news.clearFormat')}</Button>
      <Button size="sm" aria-pressed={preview} onClick={() => setPreview(v => !v)}>{t(preview ? 'news.edit' : 'news.preview')}</Button>
    </HStack>
    <Input ref={input} type="file" hidden multiple accept="image/png,image/jpeg" onChange={e => {
      const files = Array.from(e.target.files || []); e.target.value = ''; onUpload(files);
    }} aria-label={t('news.upload')} />
    {preview ? <Box p={4} minH="360px" sx={articleBodyStyles} dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(editor?.getHTML() || '') }} />
      : <Box p={4} sx={{ '.tiptap': { ...articleBodyStyles, minHeight: '360px', outline: 'none' }, '.ProseMirror-selectednode': { outline: '3px solid #4299e1' } }}><EditorContent editor={editor} /></Box>}
    <Text fontSize="sm" p={3} opacity={0.75} borderTopWidth="1px">{t('news.imageDropHelp')}</Text>
    <Modal isOpen={!!dialog} onClose={() => setDialog(null)}><ModalOverlay /><ModalContent>
      <ModalHeader>{t(dialog === 'image' ? 'news.imageUrl' : 'news.insertLink')}</ModalHeader><ModalCloseButton />
      <ModalBody><VStack align="stretch"><FormControl><FormLabel>{t('news.linkUrl')}</FormLabel><Input autoFocus value={url} onChange={e => setUrl(e.target.value)} /></FormControl>
        {dialog === 'image' && <FormControl><FormLabel>{t('news.imageAlt')}</FormLabel><Input value={alt} onChange={e => setAlt(e.target.value)} /></FormControl>}
      </VStack></ModalBody><ModalFooter><Button colorScheme="blue" onClick={apply} isDisabled={!url.trim()}>{t('news.insert')}</Button></ModalFooter>
    </ModalContent></Modal>
  </Box>;
}
