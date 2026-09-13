const normalizedType = (event) => String(event?.type || '').trim().toLowerCase();

const normalizedTrait = (event) => String(
  event?.details?.trait || event?.trait || '',
).trim().toLowerCase();

const normalizedOutcome = (event) => String(event?.outcome || '').trim().toLowerCase();

const isBloodlustCheck = (event) =>
  normalizedType(event) === 'negatrait_check' && normalizedTrait(event) === 'bloodlust';

const isFailedBloodlust = (event) =>
  isBloodlustCheck(event) && normalizedOutcome(event) === 'failed';

const compactResolution = (event) => {
  const result = {};
  [
    'id',
    'type',
    'actor',
    'target',
    'outcome',
    'checks',
    'effects',
  ].forEach((key) => {
    const value = event?.[key];
    if (value == null) return;
    if (Array.isArray(value) && value.length === 0) return;
    result[key] = value;
  });
  return result;
};

/**
 * Build the compact presentation projection used by MatchTimelineBar.
 *
 * The pybb3 narrative timeline remains untouched and semantically complete for
 * AI/reporting consumers. This function only decides what the overview bar
 * presents and how closely related narrative events are visually collapsed.
 */
export const prepareNarrativeOverviewEvents = (events = []) => {
  const source = Array.isArray(events) ? events : [];
  const byId = new Map(
    source
      .filter((event) => event?.id != null)
      .map((event) => [String(event.id), event]),
  );
  const bitesByCause = new Map();

  source.forEach((event) => {
    if (normalizedType(event) !== 'bloodlust_bite' || event?.caused_by == null) return;
    const cause = byId.get(String(event.caused_by));
    if (isFailedBloodlust(cause)) bitesByCause.set(String(event.caused_by), event);
  });

  return source.flatMap((event) => {
    const type = normalizedType(event);

    // Hypnotic Gaze is useful semantic evidence for analysis/reporting, but it
    // is too frequent and low-signal for the compact match overview.
    if (type === 'hypnotic_gaze') return [];

    if (isBloodlustCheck(event)) {
      // Keep successful Bloodlust in the narrative data, not on the overview.
      // Failed Bloodlust changed play and remains visible.
      if (!isFailedBloodlust(event)) return [];

      const bite = bitesByCause.get(String(event.id));
      if (!bite) return [event];

      const existingResolution = Array.isArray(event?.details?.resolution)
        ? event.details.resolution
        : [];
      const existingGroupedIds = Array.isArray(event?.details?.grouped_event_ids)
        ? event.details.grouped_event_ids
        : [];
      const groupedIds = [...existingGroupedIds, bite.id]
        .filter((value, index, values) =>
          value != null && values.findIndex((candidate) => String(candidate) === String(value)) === index);

      return [{
        ...event,
        target: event.target || bite.target,
        effects: [
          ...(Array.isArray(event.effects) ? event.effects : []),
          ...(Array.isArray(bite.effects) ? bite.effects : []),
        ],
        details: {
          ...(event.details || {}),
          grouped_event_ids: groupedIds,
          resolution: [...existingResolution, compactResolution(bite)],
          bloodlust_bite: compactResolution(bite),
        },
      }];
    }

    // A linked bite is rendered as part of its failed Bloodlust marker above.
    // Keep orphaned bite events visible rather than silently losing evidence.
    if (type === 'bloodlust_bite' && event?.caused_by != null) {
      const cause = byId.get(String(event.caused_by));
      if (isFailedBloodlust(cause)) return [];
    }

    return [event];
  });
};
