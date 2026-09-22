export function isMatchSelected(match, source) {
  if ((source.excludedMatchIds || []).includes(match.key)) return false;
  if ((source.includedMatchIds || []).includes(match.key)) return true;
  return match.automaticSelected;
}

export function toggleMatch(source, key, checked) {
  return {
    ...source,
    includedMatchIds: [...(source.includedMatchIds || []).filter((id) => id !== key), ...(checked ? [key] : [])],
    excludedMatchIds: [...(source.excludedMatchIds || []).filter((id) => id !== key), ...(!checked ? [key] : [])],
  };
}

export function matchDuration(start, finish) {
  if (!start || !finish) return 'Unknown';
  const seconds = Math.floor((new Date(finish) - new Date(start)) / 1000);
  // Legacy imports sometimes use the same timestamp for both fields.
  if (!Number.isFinite(seconds) || seconds <= 0) return 'Unknown';
  return `${Math.floor(seconds / 3600)}h ${Math.floor((seconds % 3600) / 60)}m`;
}
