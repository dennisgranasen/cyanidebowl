import { staffProfilePath, staffProfileUpdatePayload, toStaffCards } from './staffProfiles';

describe('staff profile helpers', () => {
  test('builds canonical human and AI deep links', () => {
    expect(staffProfilePath({ id: '42', profileType: 'HUMAN' })).toBe('/staff/user/42');
    expect(staffProfilePath({ id: 'lady putridia', profileType: 'AI' })).toBe('/staff/lady%20putridia');
  });

  test('builds own-profile payload without auth fields', () => {
    expect(staffProfileUpdatePayload({
      id: 7, eligible: true, displayName: 'Editor', avatarUrl: 'https://example.test/a.png',
      portraitUrl: '', bio: 'Bio', authSubject: 'must-not-leak',
    })).toEqual({ displayName: 'Editor', avatarUrl: 'https://example.test/a.png', portraitUrl: '', bio: 'Bio' });
  });

  test('normalizes AI and HUMAN card fallbacks', () => {
    const cards = toStaffCards(
      [{ id: 'bot', alias: 'Bot', category: 'Columnist', portraitImage: '/portrait.png' }],
      [{ id: 9, displayName: 'Human', bio: 'Human bio', avatarUrl: '/avatar.png' }],
      'Staff');
    expect(cards[0]).toMatchObject({ profileType: 'AI', displayName: 'Bot', role: 'Columnist', avatarImage: '/portrait.png', profileUrl: '/staff/bot' });
    expect(cards[1]).toMatchObject({ profileType: 'HUMAN', displayName: 'Human', role: 'Staff', summary: 'Human bio', profileUrl: '/staff/user/9' });
  });
});
