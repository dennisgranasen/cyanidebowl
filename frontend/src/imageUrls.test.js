import imageUrls from './imageUrls';

describe('imageUrls.race', () => {
  test.each([null, undefined, '', 'null'])('omits a race image URL for %s', (race) => {
    expect(imageUrls.race(race, 3)).toBeUndefined();
  });

  test('creates a URL for a known race', () => {
    expect(imageUrls.race('human', 3)).toContain('/img/race/human?opus=3');
  });
});