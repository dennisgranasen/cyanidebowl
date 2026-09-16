import { selectMembers } from './communityDirectory';
const members = [
  { id: '1', displayName: 'Zara', teamId: 'a', teamRace: 'Orc', species: 'Goblin', seasonIds: ['s1'], active: true, createdAt: '2020-01-01', commentCount: 10 },
  { id: '2', displayName: 'Anna', teamId: 'b', teamRace: 'Human', species: 'Human', seasonIds: ['s2'], active: false, createdAt: '2022-01-01', commentCount: 2 },
  { id: '3', displayName: 'Bertil', teamId: 'a', teamRace: 'Orc', species: 'Orc', seasonIds: ['s1', 's2'], active: true, createdAt: null, commentCount: 0 },
];
test('combines team, season, race, species and active filters', () => {
  expect(selectMembers(members, { team: 'a', season: 's1', race: 'Orc', species: 'Goblin', status: 'active' }).map(p => p.id)).toEqual(['1']);
  expect(selectMembers(members, { status: 'inactive' }).map(p => p.id)).toEqual(['2']);
});
test('sorts name, membership age and comment counts without mutating input', () => {
  expect(selectMembers(members, { sort: 'name' }).map(p => p.id)).toEqual(['2', '3', '1']);
  expect(selectMembers(members, { sort: 'joined', direction: 'desc' }).map(p => p.id)).toEqual(['2', '1', '3']);
  expect(selectMembers(members, { sort: 'comments', direction: 'desc' }).map(p => p.id)).toEqual(['1', '2', '3']);
  expect(members[0].id).toBe('1');
});
