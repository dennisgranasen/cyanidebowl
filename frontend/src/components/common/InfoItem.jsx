import React from 'react';
import { Stat, StatHelpText, StatLabel, StatNumber } from '@chakra-ui/react';

function InfoItem({ label, info, additionalInfo }) {
  return (
    <Stat size="sm">
      <StatLabel>{label}</StatLabel>
      <StatNumber>{info}</StatNumber>
      {additionalInfo ? <StatHelpText>{additionalInfo}</StatHelpText> : null}
    </Stat>
  );
}

export default InfoItem;
