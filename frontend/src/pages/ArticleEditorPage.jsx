import React, { useState } from 'react';
import { Box, Button, Checkbox, FormControl, FormLabel, Input, Select, VStack } from '@chakra-ui/react';
import { useAuth0 } from '@auth0/auth0-react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Image from '@tiptap/extension-image';
import Link from '@tiptap/extension-link';
import { useNavigate } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import EditorialCommunityApi from '../EditorialCommunityApi';

function ArticleEditorPage() {
  const { getAccessTokenSilently } = useAuth0();
  const navigate = useNavigate();
  const [form, setForm] = useState({ title: '', slug: '', excerpt: '', coverImageUrl: '',
    leagueSystemId: '', seasonId: '', status: 'DRAFT', featured: false });
  const editor = useEditor({ extensions: [StarterKit, Image, Link.configure({ openOnClick: false })], content: '' });

  const set = (key, value) => setForm((old) => ({ ...old, [key]: value }));
  const save = async () => {
    const article = await EditorialCommunityApi.createArticle({
      ...form,
      leagueSystemId: form.leagueSystemId || null,
      seasonId: form.seasonId || null,
      bodyHtml: editor?.getHTML() || '',
      channels: ['news'],
      tags: [],
    }, getAccessTokenSilently);
    navigate(`/article/${article.slug}`);
  };

  return (
    <VStack align="stretch">
      <Navigation />
      <Box maxW="900px" mx="auto" w="full" p={4}>
        <VStack align="stretch" spacing={4}>
          <FormControl><FormLabel>Rubrik</FormLabel><Input value={form.title} onChange={(e) => set('title', e.target.value)} /></FormControl>
          <FormControl><FormLabel>Slug (valfri)</FormLabel><Input value={form.slug} onChange={(e) => set('slug', e.target.value)} /></FormControl>
          <FormControl><FormLabel>Ingress</FormLabel><Input value={form.excerpt} onChange={(e) => set('excerpt', e.target.value)} /></FormControl>
          <FormControl><FormLabel>Omslagsbild URL</FormLabel><Input value={form.coverImageUrl} onChange={(e) => set('coverImageUrl', e.target.value)} /></FormControl>
          <FormControl><FormLabel>LeagueSystem ID (tom = site-wide)</FormLabel><Input value={form.leagueSystemId} onChange={(e) => set('leagueSystemId', e.target.value)} /></FormControl>
          <FormControl><FormLabel>Season ID</FormLabel><Input value={form.seasonId} onChange={(e) => set('seasonId', e.target.value)} /></FormControl>
          <FormControl><FormLabel>Status</FormLabel><Select value={form.status} onChange={(e) => set('status', e.target.value)}>
            <option value="DRAFT">Draft</option><option value="PUBLISHED">Published</option><option value="ARCHIVED">Archived</option>
          </Select></FormControl>
          <Checkbox isChecked={form.featured} onChange={(e) => set('featured', e.target.checked)}>Featured</Checkbox>
          <Box borderWidth="1px" borderRadius="md" p={3} minH="300px"><EditorContent editor={editor} /></Box>
          <Button onClick={save}>Spara artikel</Button>
        </VStack>
      </Box>
    </VStack>
  );
}
export default ArticleEditorPage;
