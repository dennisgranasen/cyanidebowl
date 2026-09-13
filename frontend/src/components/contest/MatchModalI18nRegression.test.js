const fs = require('fs');
const path = require('path');

const source = fs.readFileSync(path.join(__dirname, 'MatchModalWithRosters.jsx'), 'utf8');

describe('MatchModalWithRosters localization regression', () => {
  test('does not embed formatMessage expressions inside JavaScript identifiers', () => {
    expect(source).not.toMatch(/[A-Za-z0-9_$]\{intl\.formatMessage\(/);
  });

  test('does not reintroduce known hard-coded labels', () => {
    [
      '<Th>Spelare</Th>', '<Th>Nivå</Th>', '<Th>Bollplock</Th>', '<Th>Mottag</Th>',
      '<Th>Passning</Th>', '<Th>Utförda tackl.</Th>', '<Th>Mottag tackl.</Th>',
      '<Th>Utförda fouls</Th>', '<Th>Mottagna fouls</Th>', 'Replaystrukturen är bevarad',
      '>Download {'
    ].forEach((literal) => expect(source).not.toContain(literal));
  });

  test('keeps critical replay-analysis identifiers intact', () => {
    [
      'analysis?.specialEvents', 'analysis?.resourceEvents', 'analysis.checkpointCount',
      'analysis.eventCount', 'analysis.stepCount', 'analysis.dieValueCounts'
    ].forEach((identifier) => expect(source).toContain(identifier));
  });
});
