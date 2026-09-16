export function selectMembers(members, filters, locale = 'sv') {
  const compare = (a, b) => String(a || '').localeCompare(String(b || ''), locale, { sensitivity: 'base', numeric: true });
  const result = members.filter(p => (!filters.team || p.teamId === filters.team)
    && (!filters.season || p.seasonIds?.includes(filters.season))
    && (!filters.race || p.teamRace === filters.race)
    && (!filters.species || p.species === filters.species)
    && (!filters.status || (filters.status === 'active' ? p.active : !p.active)));
  return result.sort((a, b) => {
    let order;
    switch (filters.sort) {
      case 'joined': {
        const first = Date.parse(a.createdAt), second = Date.parse(b.createdAt);
        if (!Number.isFinite(first) || !Number.isFinite(second)) return Number.isFinite(first) ? -1 : Number.isFinite(second) ? 1 : compare(a.id, b.id);
        order = first - second; break;
      }
      case 'comments': order = (a.commentCount || 0) - (b.commentCount || 0); break;
      case 'team': order = compare(a.teamName, b.teamName); break;
      case 'race': order = compare(a.teamRace, b.teamRace); break;
      case 'season': order = compare(a.seasonNames?.join(', '), b.seasonNames?.join(', ')); break;
      case 'status': order = Number(b.active) - Number(a.active); break;
      default: order = compare(a.displayName, b.displayName);
    }
    return (filters.direction === 'desc' ? -order : order) || compare(a.displayName, b.displayName) || compare(a.id, b.id);
  });
}
