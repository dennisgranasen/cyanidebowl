import React, { useEffect, useRef, useState } from 'react';
import { VStack } from '@chakra-ui/react';
import { useEditor } from '@tiptap/react';
import { useIntl } from 'react-intl';
import Api from '../../EditorialCommunityApi';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';
import ArticleRichTextEditor from './ArticleRichTextEditor';
import ArticleImageGenerator from './ArticleImageGenerator';
import { articleEditorExtensions } from './articleEditorExtensions';
import { pasteArticleImages, dropArticleImages, validArticleImage } from '../../util/articleImages';
import { plainTextToArticleHtml } from '../../util/articleContent';

export default function ArticleComposer({ value, onChange, title, matchId, reporterId, disabled,
  contextAvailable, onError, onBusyChange }) {
  const intl = useIntl();
  const { getAccessTokenSilently } = useAuth0WithUserPermissions();
  const [working, setWorking] = useState(false);
  const uploadRef = useRef(null);
  const content = value.bodyHtml ?? plainTextToArticleHtml(value.body || '');
  const editor = useEditor({
    extensions: articleEditorExtensions(), content,
    onUpdate: ({ editor }) => onChange({ body: editor.getText(), bodyHtml: editor.getHTML() }),
    editorProps: {
      attributes: { role: 'textbox', 'aria-multiline': 'true', 'aria-label': intl.formatMessage({ id: 'news.articleBody' }) },
      handlePaste: (_view, event) => pasteArticleImages(event, files => uploadRef.current?.(files)),
      handleDrop: (view, event, _slice, moved) => dropArticleImages(view, event, moved, (files, pos) => uploadRef.current?.(files, pos)),
    },
  });
  useEffect(() => { if (editor && editor.getHTML() !== content) editor.commands.setContent(content, false); }, [editor, content]);
  useEffect(() => { editor?.setEditable(!disabled && !working, false); }, [editor, disabled, working]);
  const busy = value => { setWorking(value); onBusyChange?.(value); };
  uploadRef.current = async (files, position) => {
    if (!editor || disabled || working) return;
    busy(true);
    try {
      if (position != null) editor.chain().focus().setTextSelection(position).run();
      for (const file of files) {
        if (!validArticleImage(file)) throw new Error(intl.formatMessage({ id: 'news.imageSizeError' }));
        const image = await Api.uploadArticleImage(file, null, getAccessTokenSilently);
        if (!editor.isDestroyed) editor.chain().focus().setImage({ src: Api.assetUrl(image.url) }).run();
      }
    } catch (error) { onError(error); }
    finally { busy(false); }
  };
  return <VStack align="stretch" spacing={3}>
    <ArticleRichTextEditor editor={editor} disabled={disabled || working} onUpload={files => uploadRef.current?.(files)} onError={onError} />
    <ArticleImageGenerator editor={editor} title={title} matchId={matchId} reporterId={reporterId}
      disabled={disabled || working} contextAvailable={contextAvailable} onError={onError} onBusyChange={busy} />
  </VStack>;
}
