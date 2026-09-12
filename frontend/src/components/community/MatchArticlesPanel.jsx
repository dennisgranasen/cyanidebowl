import React, { useEffect, useMemo, useState } from 'react';
import {
  Alert, AlertIcon, Avatar, Badge, Box, Button, Divider, FormControl, FormLabel,
  Heading, HStack, IconButton, Input, Select, Spinner, Text, Textarea, VStack,
} from '@chakra-ui/react';
import { DeleteIcon } from '@chakra-ui/icons';
import useAuth0WithUserPermissions from '../../hooks/useAuth0WithUserPermissions';
import EditorialCommunityApi from '../../EditorialCommunityApi';
import AiReporterApi from '../../AiReporterApi';
import CommentThread from './CommentThread';
import ReactionBar from './ReactionBar';

const statusScheme = {
  DRAFT: 'gray',
  PENDING_REVIEW: 'orange',
  PUBLISHED: 'green',
  REJECTED: 'red',
};

function ArticleCard({
  article, reporter, canReview, canEdit, canDelete,
  onSave, onPublish, onReject, onDelete,
}) {
  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState(article.title);
  const [editBody, setEditBody] = useState(article.body);
  const teamReport = article.kind === 'TEAM_REPORT';
  const coachContribution = article.kind === 'COACH_CONTRIBUTION';
  const save = async () => {
    await onSave(article.id, { title: editTitle, body: editBody });
    setEditing(false);
  };
  const remove = async () => {
    if (!window.confirm('Är du säker på att du vill ta bort artikeln?')) return;
    await onDelete(article.id);
  };
  return (
    <Box borderWidth="1px" borderRadius="md" p={4}>
      <HStack mb={2} flexWrap="wrap">
        {teamReport && <Badge colorScheme="blue">Lagrapport · {article.teamName}</Badge>}
        {coachContribution && <Badge colorScheme="purple">Coachbidrag</Badge>}
        {article.authorType === 'AI' && <Badge colorScheme="cyan">AI · {article.reporterAlias}</Badge>}
        {!teamReport && !coachContribution && <Badge>Blödareblaskan</Badge>}
        {article.status !== 'PUBLISHED' && (
          <Badge colorScheme={statusScheme[article.status]}>{article.status}</Badge>
        )}
      </HStack>
      {editing ? <>
        <Input value={editTitle} onChange={e => setEditTitle(e.target.value)}
          maxLength={250} isReadOnly={!canEdit}/>
        <Textarea mt={2} minH="220px" value={editBody} onChange={e => setEditBody(e.target.value)}
          maxLength={100000} isReadOnly={!canEdit}/>
        <HStack mt={2}>
          {canEdit && (
            <Button size="sm" onClick={save}
              isDisabled={!editTitle.trim() || !editBody.trim()}>Spara</Button>
          )}
          <Button size="sm" variant="ghost" onClick={() => setEditing(false)}>Avbryt</Button>
          {canDelete && (
            <IconButton
              size="sm"
              ml="auto"
              colorScheme="red"
              variant="ghost"
              icon={<DeleteIcon/>}
              aria-label="Ta bort artikel"
              title="Ta bort artikel"
              onClick={remove}
            />
          )}
        </HStack>
      </> : <>
        <Heading size="sm">{article.title}</Heading>
        <HStack mt={2} spacing={2}>
          {article.authorType === 'AI' && (
            <Avatar
              size="md"
              name={article.authorDisplayName || article.reporterAlias}
              src={reporter?.avatarImage || reporter?.portraitImage}
            />
          )}
          <Text fontSize="sm" color="gray.500">Av {article.authorDisplayName}</Text>
        </HStack>
        <Text mt={3} whiteSpace="pre-wrap">{article.body}</Text>
      </>}
      {article.status === 'PUBLISHED' && !editing && (
        <Box mt={4}>
          <Divider mb={3}/>
          <ReactionBar targetType="MATCH_ARTICLE" targetId={article.id}/>
          <Box mt={3}>
            <CommentThread targetType="MATCH_ARTICLE" targetId={article.id}/>
          </Box>
        </Box>
      )}
      {(canEdit || canDelete || canReview) && !editing && (
        <HStack mt={3}>
          {(canEdit || canDelete) && (
            <Button size="sm" variant="outline" onClick={() => setEditing(true)}>Redigera</Button>
          )}
          {canReview && article.status === 'PENDING_REVIEW' && <>
            <Button size="sm" colorScheme="green" onClick={() => onPublish(article.id)}>Publicera</Button>
            <Button size="sm" colorScheme="red" variant="outline" onClick={() => onReject(article.id)}>Refusera</Button>
          </>}
        </HStack>
      )}
    </Box>
  );
}

export default function MatchArticlesPanel({ matchId }) {
  const { user, isAuthenticated, getAccessTokenSilently } = useAuth0WithUserPermissions();
  const [articles, setArticles] = useState([]);
  const [caps, setCaps] = useState(null);
  const [reportersById, setReportersById] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [reporterId, setReporterId] = useState('');
  const [brief, setBrief] = useState('');
  const [busy, setBusy] = useState(false);

  const token = isAuthenticated ? getAccessTokenSilently : undefined;

  const load = async () => {
    if (!matchId) return;
    setLoading(true);
    setError('');
    try {
      const [articleData, capabilityData, reporterData] = await Promise.all([
        EditorialCommunityApi.matchArticles(matchId, token),
        EditorialCommunityApi.matchArticleCapabilities(matchId, token),
        AiReporterApi.reporters(),
      ]);
      setArticles(articleData || []);
      setCaps(capabilityData);
      setReportersById(Object.fromEntries((reporterData || []).map(r => [r.id, r])));
      if (!reporterId && capabilityData?.reporters?.length) {
        setReporterId(capabilityData.reporters[0].id);
      }
    } catch (e) {
      setError('Artiklarna kunde inte hämtas.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [matchId, isAuthenticated]);

  const sections = useMemo(() => ({
    editorial: articles.filter(a => a.kind === 'EDITORIAL'),
    team: articles.filter(a => a.kind === 'TEAM_REPORT'),
    coach: articles.filter(a => a.kind === 'COACH_CONTRIBUTION'),
  }), [articles]);

  const mutate = async (fn) => {
    setBusy(true);
    setError('');
    try {
      await fn();
      await load();
    } catch (e) {
      setError(e?.response?.data?.message || e?.message || 'Åtgärden misslyckades.');
    } finally {
      setBusy(false);
    }
  };

  const create = () => mutate(async () => {
    const created = await EditorialCommunityApi.createMatchArticle(
      matchId, { title, body }, getAccessTokenSilently);
    await EditorialCommunityApi.submitMatchArticle(matchId, created.id, getAccessTokenSilently);
    setTitle('');
    setBody('');
  });

  const requestAi = () => mutate(async () => {
    await EditorialCommunityApi.requestAiMatchArticle(
      matchId, { reporterId, editorialBrief: brief }, getAccessTokenSilently);
    setBrief('');
  });

  const articlePermissions = (article) => {
    const ownHumanArticle = article.authorType === 'HUMAN'
      && Boolean(user?.sub)
      && user.sub === article.authorSubject;
    return {
      canEdit: Boolean(caps?.canReview || ownHumanArticle),
      canDelete: Boolean(caps?.canDeleteAny || ownHumanArticle),
    };
  };

  if (loading) return <HStack><Spinner size="sm"/><Text>Hämtar artiklar…</Text></HStack>;

  return (
    <VStack align="stretch" spacing={5}>
      {error && <Alert status="error"><AlertIcon/>{error}</Alert>}

      <Box>
        <Heading size="md" mb={3}>Blödareblaskan</Heading>
        {sections.editorial.length
          ? <VStack align="stretch" spacing={3}>{sections.editorial.map(a =>
              <ArticleCard key={a.id} article={a} reporter={reportersById[a.reporterId]}
                canReview={caps?.canReview}
                canEdit={articlePermissions(a).canEdit}
                canDelete={articlePermissions(a).canDelete}
                onSave={(id, payload) => mutate(() => EditorialCommunityApi.updateMatchArticle(matchId, id, payload, getAccessTokenSilently))}
                onPublish={id => mutate(() => EditorialCommunityApi.publishMatchArticle(matchId, id, getAccessTokenSilently))}
                onReject={id => mutate(() => EditorialCommunityApi.rejectMatchArticle(matchId, id, getAccessTokenSilently))}
                onDelete={id => mutate(() => EditorialCommunityApi.deleteMatchArticle(matchId, id, getAccessTokenSilently))}/>)}</VStack>
          : <Text color="gray.500">Ingen redaktionell artikel har publicerats om matchen ännu.</Text>}
      </Box>

      {sections.team.length > 0 && <Box>
        <Heading size="md" mb={1}>Lagens matchrapporter</Heading>
        <Text fontSize="sm" color="gray.500" mb={3}>
          Dessa texter är skrivna av lagens coacher och är lagens egna rapporter, inte Blödareblaskans redaktionella material.
        </Text>
        <VStack align="stretch" spacing={3}>{sections.team.map(a =>
          <ArticleCard key={a.id} article={a} reporter={reportersById[a.reporterId]}
            canReview={caps?.canReview}
            canEdit={articlePermissions(a).canEdit}
            canDelete={articlePermissions(a).canDelete}
            onSave={(id, payload) => mutate(() => EditorialCommunityApi.updateMatchArticle(matchId, id, payload, getAccessTokenSilently))}
            onPublish={id => mutate(() => EditorialCommunityApi.publishMatchArticle(matchId, id, getAccessTokenSilently))}
            onReject={id => mutate(() => EditorialCommunityApi.rejectMatchArticle(matchId, id, getAccessTokenSilently))}
            onDelete={id => mutate(() => EditorialCommunityApi.deleteMatchArticle(matchId, id, getAccessTokenSilently))}/>)}</VStack>
      </Box>}

      {sections.coach.length > 0 && <Box>
        <Heading size="md" mb={3}>Övriga coachbidrag</Heading>
        <VStack align="stretch" spacing={3}>{sections.coach.map(a =>
          <ArticleCard key={a.id} article={a} reporter={reportersById[a.reporterId]}
            canReview={caps?.canReview}
            canEdit={articlePermissions(a).canEdit}
            canDelete={articlePermissions(a).canDelete}
            onSave={(id, payload) => mutate(() => EditorialCommunityApi.updateMatchArticle(matchId, id, payload, getAccessTokenSilently))}
            onPublish={id => mutate(() => EditorialCommunityApi.publishMatchArticle(matchId, id, getAccessTokenSilently))}
            onReject={id => mutate(() => EditorialCommunityApi.rejectMatchArticle(matchId, id, getAccessTokenSilently))}
            onDelete={id => mutate(() => EditorialCommunityApi.deleteMatchArticle(matchId, id, getAccessTokenSilently))}/>)}</VStack>
      </Box>}

      {caps?.canWrite && <>
        <Divider/>
        <Box>
          <Heading size="sm" mb={3}>
            {caps.participatingCoach && caps.teamName
              ? `Skriv ${caps.teamName}s matchrapport`
              : 'Skriv artikel om matchen'}
          </Heading>
          {caps.participatingCoach && <Alert status="info" mb={3}><AlertIcon/>
            Rapporten märks som lagets egen matchrapport och publiceras utan redaktörsgodkännande.
          </Alert>}
          {!caps.participatingCoach && !caps.canReview && <Alert status="info" mb={3}><AlertIcon/>
            Som coach utanför matchen skickas artikeln till redaktionen för godkännande.
          </Alert>}
          <FormControl mb={2}><FormLabel>Rubrik</FormLabel>
            <Input value={title} onChange={e => setTitle(e.target.value)} maxLength={250}/></FormControl>
          <FormControl><FormLabel>Text</FormLabel>
            <Textarea minH="220px" value={body} onChange={e => setBody(e.target.value)} maxLength={100000}/></FormControl>
          <Button mt={3} onClick={create} isLoading={busy} isDisabled={!title.trim() || !body.trim()}>
            {caps.canReview || caps.participatingCoach ? 'Publicera' : 'Skicka för granskning'}
          </Button>
        </Box>
      </>}

      {caps?.canReview && <>
        <Divider/>
        <Box>
          <Heading size="sm" mb={3}>Be en AI-reporter skriva</Heading>
          {!caps.replayAnalyzed ? <Alert status="warning"><AlertIcon/>
            AI-reporters kan endast tillfrågas när matchen har en analyserad replay.
          </Alert> : <>
            {(caps.reporters || []).length === 0 ? (
              <Alert status="warning"><AlertIcon/>
                Ingen AI-reporter har en körbar provider/modell-route. Kontrollera modellkonfiguration och API-nyckel.
              </Alert>
            ) : <>
              <FormControl mb={2}><FormLabel>Reporter</FormLabel>
                <Select value={reporterId} onChange={e => setReporterId(e.target.value)}>
                  {(caps.reporters || []).map(r =>
                    <option key={r.id} value={r.id}>
                      {r.alias} · {r.providerId}/{r.model}
                    </option>)}
                </Select>
              </FormControl>
              <FormControl><FormLabel>Redaktionell brief (valfritt)</FormLabel>
                <Textarea value={brief} onChange={e => setBrief(e.target.value)}
                  placeholder="Vinkel, ton eller sådant reportern särskilt ska uppmärksamma…"/></FormControl>
              <Button mt={3} onClick={requestAi} isLoading={busy}
                isDisabled={!caps.canRequestAi || !reporterId}>Beställ artikel</Button>
              <Text mt={2} fontSize="sm" color="gray.500">
                AI-artiklar publiceras aldrig automatiskt. De hamnar i redaktionell granskning.
              </Text>
            </>}
          </>}
        </Box>
      </>}
    </VStack>
  );
}
