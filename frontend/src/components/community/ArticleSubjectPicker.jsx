import React, { useEffect, useState } from 'react';
import { Box, Button, HStack, Input, Select, Text } from '@chakra-ui/react';
import { useIntl } from 'react-intl';
import Api from '../../EditorialCommunityApi';
import { articleTypes } from '../../util/articleContext';

// Shared concepts and search for editorial writing and individual illustrations.
export default function ArticleSubjectPicker({ value, onChange, disabled, onError, onInsertLink }) {
  const intl = useIntl();
  const t = id => intl.formatMessage({ id });
  const [type, setType] = useState('SEASON');
  const [query, setQuery] = useState('');
  const [options, setOptions] = useState([]);
  const [labels, setLabels] = useState({});
  const system = value.find(l => l.type === 'LEAGUE_SYSTEM')?.id;
  const key = JSON.stringify(value.map(({ type, id }) => ({ type, id })));
  useEffect(() => {
    let active = true;
    Promise.all(value.filter(l => !l.label).map(async link => {
      const option = await Api.resolveAssociation(link.type, link.id);
      return [`${link.type}:${link.id}`, option.label];
    })).then(entries => { if (active) setLabels(Object.fromEntries(entries)); })
      .catch(error => { if (active) onError?.(error); });
    return () => { active = false; };
  }, [key]);
  useEffect(() => {
    let active = true;
    setOptions([]);
    const timer = setTimeout(() => Api.associationOptions(type, query, type === 'SEASON' ? system : null)
      .then(rows => { if (active) setOptions(rows); }).catch(error => { if (active) onError?.(error); }), 250);
    return () => { active = false; clearTimeout(timer); };
  }, [type, query, system]);
  const add = option => {
    const next = [...value];
    const insert = (type, id, label) => {
      if (id && !next.some(l => l.type === type && l.id === id)) next.push({ type, id, label });
    };
    insert(type, option.id, option.label);
    if (type === 'SEASON') insert('LEAGUE_SYSTEM', option.leagueSystemId);
    onChange(next);
  };
  return <Box>
    {value.map((link, i) => <HStack key={`${link.type}:${link.id}`} mb={1}>
      <Text>{t(`news.type.${link.type}`)}: {link.label || labels[`${link.type}:${link.id}`] || link.id}</Text>
      <Button size="xs" isDisabled={disabled} onClick={() => onChange(value.filter((_, n) => n !== i))}>{t('news.remove')}</Button>
    </HStack>)}
    <HStack mt={2}>
      <Select value={type} isDisabled={disabled} onChange={e => setType(e.target.value)} aria-label={t('news.associations')}>
        {articleTypes.map(type => <option key={type} value={type}>{t(`news.type.${type}`)}</option>)}
      </Select>
      <Input value={query} isDisabled={disabled} onChange={e => setQuery(e.target.value)} placeholder={t('news.search')} aria-label={t('news.search')} />
    </HStack>
    <Box maxH="160px" overflowY="auto">
      {options.map(option => <HStack key={option.id} mt={2} justify="space-between">
        <Text>{option.label}</Text><HStack>
          <Button size="xs" isDisabled={disabled || value.some(l => l.type === type && l.id === option.id)} onClick={() => add(option)}>{t('news.add')}</Button>
          {onInsertLink && option.url && <Button size="xs" isDisabled={disabled} onClick={() => onInsertLink(option.url, option.label)}>{t('news.insertLink')}</Button>}
        </HStack>
      </HStack>)}
    </Box>
  </Box>;
}
