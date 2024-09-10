import React from 'react';
import { Tag, TagLabel, TagLeftIcon, Tooltip } from '@chakra-ui/react';

function IconLabel({ icon, label, ...props }) {
  return (
    <Tag variant="subtle" size="md" backgroundColor="warpScoresTooltipBackground" {...props}>
      {icon ? <TagLeftIcon as={icon} /> : null}
      <TagLabel>{label}</TagLabel>
    </Tag>
  );
}

function DelayedIconTooltip({ label, icon, shouldWrapChildren, children, ...props }) {
  return (
    <Tooltip
      background="none"
      borderWidth="0"
      shadow="none"
      borderRadius="md"
      shouldWrapChildren={shouldWrapChildren ? 'shouldWrapChildren' : null}
      placement="bottom-start"
      closeDelay={300}
      openDelay={500}
      label={<IconLabel icon={icon} label={label} {...props} />}
    >
      {children || null}
    </Tooltip>
  );
}

export default DelayedIconTooltip;
