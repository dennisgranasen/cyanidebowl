import { participantLogoUrl } from './KnockoutCompetition';

jest.mock('../../config', () => ({ backendUrl: 'http://backend' }));

test('uses the participant game version when requesting a playoff team logo', () => {
  expect(participantLogoUrl({ picture: 'Logo_Human_01', id: { value: 'team-1' }, opus: 2 }))
    .toBe('http://backend/img/logo/Logo_Human_01?opus=2');
});

test('falls back to the participant identity version for legacy brackets', () => {
  expect(participantLogoUrl({ picture: 'Logo_Human_01', id: { value: 'team-1', opus: 2 } }))
    .toBe('http://backend/img/logo/Logo_Human_01?opus=2');
});
