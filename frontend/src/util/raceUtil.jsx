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
  12: "Norse", // default
  13: "Amazon", // default
  14: "Elven Union",
  15: "High Elf", // default
  16: "Tomb Kings",
  17: "Necromantic Horror",
  18: "Nurgle",
  19: "Ogre",
  20: "Vampire",
  21: "Chaos Dwarf",
  22: "Underworld Denizen",
  23: "Khorne",
  24: "Bretonnia", // default
  25: "Slann",
  1000: "Black Orc",
  1001: "Chaos Renegade",
  1002: "Old World Alliance"
};

const opusSpecificRaces = {
  "3_12": "Amazon",    // Amazon for opus 3, race 12
  "2_13": "Amazon",    // Amazon for opus 2, race 13
  "2_15": "High Elf",  // High Elf for opus 2, race 15
  "3_15": "Norse",     // Norse for opus 3, race 15
  "3_24": "Imperial Nobility" // Imperial Nobility for opus 3, race 24
};

const _getRaceLogo = (raceId, opus) => {
  // TODO: Implement logic to return the race logo based on raceName or raceId
  switch (raceId) {
    case 5: return "Lizardman";
    case 10: return "Undead";
    case 17: return "Necromantic";
    case 22: return "Underworld";
    case 25: return (opus === 2) ? "Kislev" : "Slann";
    default:
      return toRace(raceId, opus).replace(/\s+/g, ''); //  
  }
};

const getRaceLogo = (raceId, opus) => {
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

export {
  getRaceLogo,
  toRace
};
