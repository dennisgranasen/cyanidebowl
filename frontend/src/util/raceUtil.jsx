const raceTable = {
  1: "Human",
  2: "Dwarf", 
  3: "Skaven",
  4: "Orc",
  5: "Lizardmen",
  6: "Goblin",
  7: "Wood Elf",
  8: "Chaos Chosen",
  9: "Dark Elf",
  10: "Shambling Undead",
  11: "Halfling",
  12: "Amazon", // default
  13: "Amazon", // currently not used in BB3 ??
  14: "Elven Union", // default
  15: "Norse", // default
  16: "Tomb Kings",
  17: "Necromantic Horror",
  18: "Nurgle",
  19: "Ogre",
  20: "Vampire",
  21: "Chaos Dwarf",
  22: "Underworld Denizen",
  23: "Khorne",
  24: "Imperial Nobility", // default
  25: "Slann",
  1000: "Black Orc",
  1001: "Chaos Renegade",
  1002: "Old World Alliance"
};

const opusSpecificRaces = {
  "1_12": "Norse",    // Amazon for BB3
  "1_13": "Amazon",    // Amazon for BB2
  "2_13": "Amazon",    // Amazon for BB2
  "3_13": "Vampire",
  "1_14": "Elf",       // Just Elf in BB1 and 2
  "2_14": "Elf",   
  "1_15": "High Elf",
  "2_15": "High Elf",
  "1_16": "Khemri",
  "2_16": "Khemri",
  "3_16": "Chaos Dwarf",
  "2_24": "Bretonnia", // Bretonnia for BB3
};

const _getRaceLogo = (raceId, opus) => {
  // TODO: Implement logic to return the race logo based on raceName or raceId
  switch (raceId) {
    case 5: return "Lizardman";
    case 8: if (opus < 3) 
      return "Chaos"; // BB3 uses Chaos Chosen
    case 10: return "Undead";
    case 17: return "Necromantic";
    case 22: return "Underworld";
    case 25: return (opus === 2) ? "Kislev" : "Slann";
    default:
  }
  return toRace(raceId, opus).replace(/\s+/g, ''); //  
};

const getRaceLogo = (raceId, opus) => {
  if (!raceId)
    return null;
  if (typeof raceId === 'string') {
    return raceId.replace(/\s+/g, '') + "_01"; // Assuming raceId is a string like "Human", "Dwarf", etc.
  }
  return _getRaceLogo(raceId, opus) + "_01";
}

const toRace = (raceId, opus) => {
  // First check for opus-specific race
  const opusKey = `${opus}_${raceId}`;
  if (opusSpecificRaces[opusKey]) {
    return opusSpecificRaces[opusKey];
  }
    // Fall back to default race
  return raceTable[raceId] || "Unknown Race";
}

// Cyanide responses and older stored matches can contain a stale textual race
// alongside the numeric id. The id is the stable source of truth; the text is
// only a fallback for records where no id was imported.
const resolveRace = (team, opus) => {
  if (team?.raceId !== null && team?.raceId !== undefined) {
    return toRace(team.raceId, opus);
  }
  return team?.race || "Unknown Race";
};

export {
  getRaceLogo,
  resolveRace,
  toRace
};
