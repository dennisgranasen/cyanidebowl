const gameTypes = {
  bb1: { name: 'Blood Bowl 1', ruleset: ['LRB6'], platforms: ['PC'] },
  bb2: { name: 'Blood Bowl 2', ruleset: ['LRB6'], platforms: ['PC', 'Playstation', 'Xbox'] },
  bb3: { name: 'Blood Bowl 3', ruleset: ['BB2020'], platforms: ['PC', 'Playstation', 'Xbox', 'Switch'] },
  bloodbowl: { name: 'Blood Bowl', rulesets: ['LRB6', 'BB2016', 'BB2020', 'Other'], defaultRuleset: 'BB2020', platforms: ['Tabletop', 'Fumbbl', 'TTS', 'Other'] },
  sevens: { name: 'Blood Bowl 7s', rulesets: ['LRB6', 'BB2016', 'BB2020', 'Other'], defaultRuleset: 'BB2020', platforms: ['Tabletop', 'TTS'] },
  dungeonbowl: { name: 'Dungeon Bowl', ruleset: ['DB5', 'DB2021', 'Other'], defaultRuleset: 'DB2021', platforms: ['Tabletop', 'TTS'] },
  gutterbowl: { name: 'Gutter Bowl', ruleset: ['GB2023', 'Other'], platforms: ['Tabletop', 'TTS'] }
};

export default gameTypes;