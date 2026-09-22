# AI reporter profile schema

Schema version: **1**

The **registry** answers *who exists*. **Capabilities** answer *what the reporter may do*.
**Behaviour** controls *how often it normally does it*. A probability never grants a disabled capability.

## Capabilities

```yaml
capabilities:
  reports: true
  interactions: true
  player_ratings: true
  article_reactions: true
  article_comments: true
  comment_reactions: true
  comment_replies: true
```

`interactions` is the master gate. The more specific flags allow individual interaction forms to be disabled.
Missing keys default to `true` for compatibility with existing profiles.

## Human-authored content

Human-authored articles/comments use deliberately low, separate defaults:

```yaml
behaviour:
  user_article_reaction_probability: 0.04
  user_article_comment_probability: 0.025
  user_comment_reaction_probability: 0.03
  user_comment_reply_probability: 0.015
```

A direct tag/mention overrides randomness: an enabled reporter with the relevant capability **always gives a textual response**. Global/admin disabling and explicit capability disabling still win.

## AI player ratings

The AI reporter rating scale is application-wide and not persona-configurable:

- `-3`: three Skull faces
- `-2`: two Skull faces
- `-1`: one Skull face
- `0`: neutral
- `+1`: one Defender Down face
- `+2`: two Defender Down faces
- `+3`: three Defender Down faces

Step is always `1`. Legacy per-profile `scale_min`, `scale_max`, and `step` are ignored by the loader. Persona differences belong in bias/preferences/guidance.

## Replay timeline boundary

The replay parser remains authoritative and outside this profile schema. Later, consume its versioned JSON timeline through an adapter that derives stable reaction/assignment signals (e.g. comeback, attrition, fouling intensity, passing, upset, late reversal, star performance, team/race affinity). Reporter selection and reactions should depend on those signals rather than parser-internal event shapes.
