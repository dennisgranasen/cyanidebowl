import { prepareNarrativeOverviewEvents } from './timelineOverview';

const bloodlust = (id, outcome = 'failed') => ({
  id,
  type: 'negatrait_check',
  actor: { kind: 'player', id: 1 },
  outcome,
  details: { trait: 'bloodlust' },
});

describe('prepareNarrativeOverviewEvents', () => {
  test('hides Hypnotic Gaze without changing the narrative source', () => {
    const events = [
      { id: 1, type: 'hypnotic_gaze', outcome: 'passed' },
      { id: 2, type: 'touchdown' },
    ];
    const original = JSON.parse(JSON.stringify(events));

    expect(prepareNarrativeOverviewEvents(events)).toEqual([
      { id: 2, type: 'touchdown' },
    ]);
    expect(events).toEqual(original);
  });

  test('hides successful Bloodlust even when the narrative contains reroll data', () => {
    const event = {
      ...bloodlust(10, 'passed'),
      checks: [{
        type: 'bloodlust',
        outcome: 'passed',
        reroll_used: true,
        attempts: [
          { dice: [1], outcome: 'failed' },
          { dice: [4], outcome: 'passed', reroll: 'team' },
        ],
      }],
    };

    expect(prepareNarrativeOverviewEvents([event])).toEqual([]);
  });

  test('keeps failed Bloodlust when there is no bite resolution', () => {
    const event = bloodlust(20);
    expect(prepareNarrativeOverviewEvents([event])).toEqual([event]);
  });

  test('groups a linked bite into the failed Bloodlust event by caused_by', () => {
    const check = bloodlust(30);
    const unrelated = { id: 31, type: 'move', actor: { kind: 'player', id: 1 } };
    const bite = {
      id: 32,
      type: 'bloodlust_bite',
      actor: { kind: 'player', id: 1 },
      target: { kind: 'player', id: 2 },
      outcome: 'teammate_bitten',
      caused_by: 30,
      effects: [{
        type: 'injury',
        subject: { kind: 'player', id: 2 },
        outcome: 'stunned',
      }],
    };

    const projected = prepareNarrativeOverviewEvents([check, unrelated, bite]);

    expect(projected).toHaveLength(2);
    expect(projected[0]).toMatchObject({
      id: 30,
      type: 'negatrait_check',
      outcome: 'failed',
      target: { kind: 'player', id: 2 },
      details: {
        grouped_event_ids: [32],
        bloodlust_bite: {
          id: 32,
          type: 'bloodlust_bite',
          outcome: 'teammate_bitten',
        },
      },
    });
    expect(projected[0].effects).toEqual(bite.effects);
    expect(projected[0].details.resolution).toEqual([
      expect.objectContaining({ id: 32, type: 'bloodlust_bite' }),
    ]);
    expect(projected[1]).toBe(unrelated);
  });

  test('groups no-victim Bloodlust resolution and preserves its effect', () => {
    const check = bloodlust(40);
    const bite = {
      id: 41,
      type: 'bloodlust_bite',
      actor: { kind: 'player', id: 1 },
      outcome: 'no_victim',
      caused_by: 40,
      effects: [{
        type: 'lost_tackle_zone',
        subject: { kind: 'player', id: 1 },
        outcome: true,
      }],
    };

    const [projected] = prepareNarrativeOverviewEvents([check, bite]);

    expect(projected.details.bloodlust_bite.outcome).toBe('no_victim');
    expect(projected.effects).toEqual(bite.effects);
  });

  test('keeps an orphaned bite visible instead of discarding it', () => {
    const bite = {
      id: 51,
      type: 'bloodlust_bite',
      caused_by: 999,
      outcome: 'teammate_bitten',
    };

    expect(prepareNarrativeOverviewEvents([bite])).toEqual([bite]);
  });
});
