import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Alert, Box, Button, Checkbox, FormControl, FormLabel, Heading, HStack, Input, Select, Text, Textarea, VStack } from '@chakra-ui/react';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import { useEditor } from '@tiptap/react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import Api from '../EditorialCommunityApi';
import ArticleSubjectPicker from '../components/community/ArticleSubjectPicker';
import ArticleImageGenerator from '../components/community/ArticleImageGenerator';
import { articleEditorExtensions } from '../components/community/articleEditorExtensions';
import ArticleRichTextEditor from '../components/community/ArticleRichTextEditor';
import { useIntl } from 'react-intl';
import { pasteArticleImages, dropArticleImages, validArticleImage } from '../util/articleImages';
import { articleIsGlobal, initialArticleContext } from '../util/articleContext';

const blank = { title: '', slug: '', excerpt: '', coverImageUrl: '', status: 'DRAFT', featured: false };
const validUrl = value => /^(https?:\/\/|\/(?!\/))/i.test(value);
function ArticleEditorPage() {
  const intl = useIntl();
  const t = id => intl.formatMessage({ id });
  const { getAccessTokenSilently: token } = useAuth0WithUserPermissions();
  const navigate = useNavigate();
  const location = useLocation();
  const { articleId } = useParams();
  const [form, setForm] = useState(blank);
  const [links, setLinks] = useState(() => initialArticleContext(location.search));
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(Boolean(articleId));
  const uploadRef = useRef(null);
  const [mine, setMine] = useState([]);
  const [capabilities, setCapabilities] = useState(null);
  const [brief, setBrief] = useState('');
  const [reporters, setReporters] = useState([]);
  const [reporterId, setReporterId] = useState('');
  const [queue, setQueue] = useState([]);
  const [autoAccept, setAutoAccept] = useState(false);
  const system = links.find(l => l.type === 'LEAGUE_SYSTEM')?.id;
  const mentionTypes = ['TEAM', 'PLAYER', 'COACH', 'STAR_PLAYER', 'FAN', 'STAFF'];
  const searchMentions = useCallback(async query => {
    if (!query?.trim()) return [];
    const groups = await Promise.all(mentionTypes.map(async type => {
      const rows = await Api.associationOptions(type, query.trim());
      return rows.map(option => ({ ...option, type }));
    }));
    const normalized = query.trim().toLocaleLowerCase();
    return groups.flat()
      .sort((a, b) => {
        const aLabel = (a.label || '').toLocaleLowerCase();
        const bLabel = (b.label || '').toLocaleLowerCase();
        const aStarts = aLabel.startsWith(normalized) ? 0 : 1;
        const bStarts = bLabel.startsWith(normalized) ? 0 : 1;
        return aStarts - bStarts || aLabel.localeCompare(bLabel);
      })
      .slice(0, 24);
  }, []);
  const addMentionAssociation = useCallback(option => {
    setLinks(old => old.some(link => link.type === option.type && link.id === option.id)
      ? old : [...old, { type: option.type, id: option.id, label: option.label }]);
  }, []);
  const editor = useEditor({
    extensions: articleEditorExtensions(),
    content: '',
    editorProps: {
      attributes: { role: 'textbox', 'aria-multiline': 'true', 'aria-label': t('news.articleBody') },
      handlePaste: (_view, event) => pasteArticleImages(event, files => uploadRef.current?.(files)),
      handleDrop: (view, event, _slice, moved) => dropArticleImages(view, event, moved, (files, position) => uploadRef.current?.(files, position)),
    },
  });
  useEffect(() => { editor?.setEditable(!busy && !loading, false); }, [editor, busy, loading]);
  const set = (key, value) => setForm(old => ({ ...old, [key]: value }));
  const showError = e => setError(e.response?.data?.message || e.message || t('news.error'));
  const loadArticle = article => {
    setForm({ ...blank, ...article });
    const associations = [...(article.associations || [])];
    const add = (type, id) => { if (id && !associations.some(l => l.type === type && l.id === id)) associations.push({ type, id }); };
    add('LEAGUE_SYSTEM', article.leagueSystemId); add('SEASON', article.seasonId);
    (article.teamIds || []).forEach(id => add('TEAM', id));
    setLinks(associations); editor?.commands.setContent(article.bodyHtml || '');
  };
  useEffect(() => {
    if (!editor) return undefined;
    let active = true;
    setError(''); setNotice('');
    setLoading(Boolean(articleId));
    if (articleId) {
      Api.editorArticle(articleId, token).then(a => { if (active) loadArticle(a); }).catch(e => { if (active) showError(e); }).finally(() => { if (active) setLoading(false); });
    } else { setForm(blank); setLinks(initialArticleContext(location.search)); editor.commands.setContent(''); }
    return () => { active = false; };
  }, [articleId, editor, location.search]);
  const associationKey = JSON.stringify(links.map(({ type, id }) => ({ type, id })));
  useEffect(() => {
    let active = true;
    setCapabilities(null); setQueue([]); setReporters([]);
    if (loading) return () => { active = false; };
    const data = { associations: JSON.parse(associationKey), channels: ['news'] };
    Api.articleCapabilities(articleId, data, token).then(async permissions => {
      if (!active) return;
      setCapabilities(permissions);
      if (permissions.canUseAiWriter) {
        const [people, policy] = await Promise.all([Api.reporters(system, token), Api.articlePolicy(system, token)]);
        if (active) { setReporters(people); setAutoAccept(policy.autoAccept); }
      }
      if (permissions.canReview) {
        const pending = await Api.reviewQueue(system, token);
        if (active) setQueue(pending);
      }
    }).catch(e => { if (active) showError(e); });
    return () => { active = false; };
  }, [system, associationKey, articleId, loading]);
  useEffect(() => {
    let active = true;
    Api.myArticles(token).then(data => { if (active) setMine(data); }).catch(e => { if (active) showError(e); });
    return () => { active = false; };
  }, [articleId]);
  const run = async action => {
    if (loading || busy) return;
    setBusy(true); setError(''); setNotice('');
    try { await action(); } catch (e) { showError(e); } finally { setBusy(false); }
  };
  const payload = (status = form.status, confirmGlobal = false) => ({
    title: form.title, slug: form.slug, excerpt: form.excerpt, coverImageUrl: form.coverImageUrl,
    status, featured: form.featured, bodyHtml: editor?.getHTML() || '',
    associations: links.map(({ type, id }) => ({ type, id })),
    leagueSystemId: null, seasonId: null, teamIds: [], channels: ['news'], tags: [], confirmGlobal,
  });
  const confirmPublication = () => !articleIsGlobal(links) || window.confirm(t('news.globalConfirm'));
  const save = (status = form.status) => {
    const publishing = capabilities?.canPublishDirect && (status === 'PUBLISHED' || (!capabilities.canReview && status === 'PENDING_REVIEW'));
    if (publishing && !confirmPublication()) return;
    run(async () => {
      const data = payload(status, Boolean(publishing));
      const article = articleId ? await Api.updateArticle(articleId, data, token) : await Api.createArticle(data, token);
      loadArticle(article);
      setMine(old => [article, ...old.filter(a => a.id !== article.id)]);
      if (article.status === 'PUBLISHED') navigate(`/article/${article.slug}`);
      else { navigate(`/editor/articles/${article.id}`); setNotice(t(article.status === 'PENDING_REVIEW' ? 'news.submitted' : 'news.saved')); }
    });
  };
  const insertUrl = (value, text) => {
    if (!validUrl(value)) { setError(t('news.invalidUrl')); return; }
    if (editor.state.selection.empty) editor.chain().focus().insertContent({ type: 'text', text: text || value, marks: [{ type: 'link', attrs: { href: value } }] }).run();
    else editor.chain().focus().setLink({ href: value }).run();
  };
  const insertImage = result => editor.chain().focus().setImage({
    src: Api.assetUrl(result.url), editorialImageId: result.imageId || null,
  }).run();
  const uploadImages = (files, position) => run(async () => {
    if (position != null) editor.chain().focus().setTextSelection(position).run();
    for (const file of files) {
      if (!validArticleImage(file)) throw new Error(t('news.imageSizeError'));
      insertImage(await Api.uploadArticleImage(file, system, token, links));
    }
  });
  uploadRef.current = uploadImages;
  const review = accept => {
    if (accept && !confirmPublication()) return;
    run(async () => {
      await Api.updateArticle(articleId, payload('PENDING_REVIEW'), token);
      const article = await Api.reviewArticle(articleId, accept, accept, token);
      loadArticle(article); setQueue(old => old.filter(a => a.id !== article.id));
      if (accept) navigate(`/article/${article.slug}`);
    });
  };
  return <VStack align="stretch"><Navigation />
    <Box maxW="1000px" mx="auto" w="full" p={4}>
      <VStack align="stretch" spacing={4}>
        <Heading size="md">{t('news.editor')}</Heading>
        {error && <Alert status="error" role="alert">{error}</Alert>}
        {notice && <Alert status="success">{notice}</Alert>}
        {!!mine.length && <Box as="details" borderWidth="1px" p={3}><Box as="summary" cursor="pointer">{t('news.myArticles')} ({mine.length})</Box><Box maxH="240px" overflowY="auto">
          {mine.map(a => <HStack key={a.id} my={2}><Button variant="link" onClick={() => navigate(`/editor/articles/${a.id}`)}>{a.title}</Button><Text fontSize="sm">{t(`news.status.${a.status}`)}</Text></HStack>)}
        </Box></Box>}
        {!!queue.length && <Box borderWidth="1px" p={3}><Heading size="sm">{t('news.reviewQueue')}</Heading>
          {queue.map(a => <Button key={a.id} variant="link" display="block" my={2} onClick={() => navigate(`/editor/articles/${a.id}`)}>{a.title}</Button>)}
        </Box>}
        {['title', 'slug', 'excerpt', 'coverImageUrl'].map(key => <FormControl key={key} isRequired={key === 'title'}>
          <FormLabel>{t(key === 'coverImageUrl' ? 'articleEditor.coverImage' : `articleEditor.${key}`)}</FormLabel>
          <Input value={form[key]} onChange={e => set(key, e.target.value)} />
        </FormControl>)}
        <Box borderWidth="1px" p={3} borderRadius="md">
          <Heading size="sm" mb={2}>{t('news.associations')}</Heading>
          <Text fontSize="sm" mb={2}>{t('news.scopeHelp')}</Text>
          <ArticleSubjectPicker value={links} onChange={setLinks} disabled={busy || loading} onError={showError} onInsertLink={insertUrl} />
        </Box>
        {articleIsGlobal(links) && <Alert status="warning">{t('news.globalWarning')}</Alert>}
        <ArticleRichTextEditor editor={editor} disabled={busy || loading} onUpload={uploadImages} onError={showError}
          onMentionSearch={searchMentions} onMentionSelect={addMentionAssociation} />
        <ArticleImageGenerator key={articleId || location.search} editor={editor} title={form.title} associations={payload().associations}
          disabled={busy || loading} onError={showError} onBusyChange={setBusy} />
        <Text fontSize="sm">{t(`news.status.${form.status}`)}</Text>
        <Alert status="info">{t(capabilities?.canPublishDirect ? 'news.directPublication' : 'news.reviewRequired')}</Alert>
        {capabilities?.canReview ? <>
          <FormControl><FormLabel>{t('articleEditor.status')}</FormLabel><Select value={form.status} onChange={e => set('status', e.target.value)}>
            {['DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'ARCHIVED'].map(status => <option key={status} value={status}>{t(`news.status.${status}`)}</option>)}
          </Select></FormControl>
          <Button isLoading={busy} isDisabled={loading || !form.title.trim() || !editor} onClick={() => save()}>{t('articleEditor.save')}</Button>
        </> : <HStack>
          <Button isDisabled={busy || loading || !form.title.trim()} onClick={() => save('DRAFT')}>{t('news.saveDraft')}</Button>
          <Button colorScheme="blue" isLoading={busy} isDisabled={loading || !capabilities || !form.title.trim()} onClick={() => save('PENDING_REVIEW')}>{t(capabilities?.canPublishDirect ? 'news.publish' : 'news.submitReview')}</Button>
        </HStack>}
        {articleId && capabilities?.canReview && form.status === 'PENDING_REVIEW' && <HStack><Button isDisabled={busy} colorScheme="green" onClick={() => review(true)}>{t('news.accept')}</Button><Button isDisabled={busy} colorScheme="red" onClick={() => review(false)}>{t('news.reject')}</Button></HStack>}
        {capabilities?.canUseAiWriter && <Box borderWidth="1px" borderRadius="md" p={3}>
          <Heading size="sm" mb={3}>{t('news.aiWriter')}</Heading>
          <Select value={reporterId} onChange={e => setReporterId(e.target.value)} placeholder={t('news.selectReporter')} aria-label={t('news.selectReporter')}>
            {reporters.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
          </Select>
          <Textarea mt={2} value={brief} onChange={e => setBrief(e.target.value)} placeholder={t('news.brief')} aria-label={t('news.brief')} />
          <Button mt={2} isDisabled={busy || !reporterId || !brief.trim()} onClick={() => {
            const confirmGlobal = autoAccept && articleIsGlobal(links);
            if (confirmGlobal && !confirmPublication()) return;
            run(async () => {
              const article = await Api.generateArticle({ reporterId, brief, article: payload('DRAFT', confirmGlobal) }, token);
              navigate(`/editor/articles/${article.id}`);
            });
          }}>{t('news.generateArticle')}</Button>
          <Checkbox mt={3} display="block" isChecked={autoAccept} isDisabled={busy} onChange={e => {
            const enabled = e.target.checked;
            if (enabled && !window.confirm(t('news.autoAcceptConfirm'))) return;
            run(async () => { const policy = await Api.setArticlePolicy(system, enabled, token); setAutoAccept(policy.autoAccept); });
          }}>{t('news.autoAccept')}</Checkbox>
          <Text fontSize="sm">{system ? t('news.policyLeague') : t('news.policyGlobal')}</Text>
        </Box>}
      </VStack>
    </Box>
  </VStack>;
}
export default ArticleEditorPage;
