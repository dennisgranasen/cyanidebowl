# Match narrative context

BlaskScore uses pybb3 as the canonical replay timeline parser.

For BB3, `pybb3-service` calls the same API as:

```text
python tools/replay_timeline.py replay.xml --narrative
```

That is:

```python
Replay.from_xml(xml).timeline().to_narrative_dict()
```

The resulting `pybb3-narrative-timeline` object is persisted on
`ReplayAnalysis.timeline` and is the shared source for:

- the timeline shown in the match/replay card;
- AI reporter narrative facts;
- deterministic narrative signals.

The frontend only projects canonical participants/events into visual lanes and
labels. It does not parse Cyanide replay protocol messages.

`DefaultMatchNarrativeContextBuilder` adds deterministic importance and
aggregate signals to pybb3's already compact narrative projection. Reporter
voice and opinions are applied after this layer.

Existing stored BB3 analyses using the old `matchEvents` timeline remain
readable as a compatibility fallback, but must be reanalysed before they have
the canonical pybb3 narrative timeline.
