import React from 'react';
import { Stack } from '@chakra-ui/react';
import Skill from './Skill';

const passingSkills = [
  'Accurate',
  'Cannonneer',
  'CloudBuster',
  'NervesOfSteel',
  'OnTheBall',
  'Pass',
  'RunningPass',
  'SafePass',
  'DumpOff',
  'FumbleRooskie',
  'HailMaryPass',
  'Leader',
];
const strengthSkills = [
  'ArmBar',
  'Brawler',
  'BreakTackle',
  'Grab',
  'Guard',
  'Juggernaut',
  'MightyBlow',
  'MultipleBlock',
  'PileDriver',
  'StandFirm',
  'StrongArm',
  'ThickSkull',
];
const agilitySkills = [
  'Catch',
  'Defensive',
  'DivingCatch',
  'DivingTackle',
  'Dodge',
  'JumpUp',
  'Leap',
  'SafePairOfHands',
  'SideStep',
  'SneakyGit',
  'Sprint',
  'SureFeet',
];
const generalSkills = [
  'Block',
  'Dauntless',
  'DirtyPlayer',
  'Fend',
  'Frenzy',
  'Kick',
  'Pro',
  'Shadowing',
  'Tackle',
  'Wrestle',
  'StripBall',
  'SureHands',
];
const mutationSkills = [
  'BigHand',
  'Claw',
  'DisturbingPresence',
  'ExtraArms',
  'FoulAppearence',
  'Horns',
  'IronHardSkin',
  'MonstrousMouth',
  'PrehensileTail',
  'Tentacles',
  'TwoHeads',
  'VeryLongLegs',
];
const traitSkills = [
  'AlwaysHungry',
  'AnimalSavagery',
  'Animosity',
  'BallChain',
  'Bloodlust',
  'Bombardier',
  'BoneHead',
  'Chainsaw',
  'Decay',
  'Drunkard',
  'HitAndRun',
  'HypnoticGaze',
  'Loner',
  'NoHands',
  'PickMeUp',
  'PlagueRidden',
  'PogoStick',
  'ProjectileVomit',
  'ReallyStupid',
  'Regeneration',
  'RightStuff',
  'SecretWeapon',
  'Stab',
  'Stunty',
  'Swarmming',
  'Swoop',
  'TakeRoot',
  'ThrowTeamMate',
  'Timmmber',
  'Titchy',
  'UnchannelledFury',
];

const isInList = (skill, listOfSkills) => {
  const normalizedSkill = skill.toLowerCase().replace(/[^a-z]/g, '');
  return listOfSkills.some((curr) => normalizedSkill.startsWith(curr.toLowerCase()));
};

const isMutation = (skill) => {
  return isInList(skill, mutationSkills);
};

const isTrait = (skill) => {
  return isInList(skill, traitSkills);
};

const isAgility = (skill) => {
  return isInList(skill, agilitySkills);
};

const isStrength = (skill) => {
  return isInList(skill, strengthSkills);
};

const isPassing = (skill) => {
  return isInList(skill, passingSkills);
};

const isGeneral = (skill) => {
  return isInList(skill, generalSkills);
};

const orderBySkillGroup = (skill) => {
  if (isTrait(skill)) return 0;
  if (isGeneral(skill)) return 1;
  if (isStrength(skill)) return 2;
  if (isAgility(skill)) return 3;
  if (isPassing(skill)) return 4;
  if (isMutation(skill)) return 5;
  return 6;
};

const skillComparator = (skillA, skillB) => {
  const compareResult = orderBySkillGroup(skillA) - orderBySkillGroup(skillB);
  if (compareResult !== 0) {
    return compareResult;
  }
  return skillA.localeCompare(skillB);
};

const getAsSimpleArray = (skills) => {
  let result = [];
  if (skills?.innateSkills) {
    result = result.concat(skills?.innateSkills);
    result = result.concat(skills?.acquiredSkills);
  } else if (skills?.length > 0) {
    result = result.concat(skills);
  }
  return result;
};

function Skills({ skills, opus }) {
  const mySkills = getAsSimpleArray(skills);
  mySkills.sort(skillComparator);

  return (
    <Stack direction="row" spacing="2px">
      {mySkills.map((skill) => (
        <Skill key={skill} skill={skill} opus={opus} />
      ))}
    </Stack>
  );
}

export default Skills;
