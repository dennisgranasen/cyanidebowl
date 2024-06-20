import React from 'react';
import { Tag, TagLabel, TagLeftIcon, Tooltip } from '@chakra-ui/react';

function IconLabel({ icon, label }) {
  return (
    <Tag variant="subtle" size="md" bg="grey" colorScheme="black">
      {icon ? <TagLeftIcon as={icon} /> : null}
      <TagLabel>{label}</TagLabel>
    </Tag>
  );
}

function DelayedIconTooltip({ label, icon, children, ...props }) {
  return (
    <Tooltip
      placement="bottom-start"
      bg="none"
      closeDelay={300}
      openDelay={500}
      {...props}
      label={<IconLabel icon={icon} label={label} />}
    >
      {children || null}
    </Tooltip>
  );
}

export default DelayedIconTooltip;
