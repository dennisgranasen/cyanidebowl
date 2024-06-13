import { FaFacebook, FaGlobe, FaXTwitter } from 'react-icons/fa6';
import { FaDiscord } from 'react-icons/fa';
import { Link } from '@chakra-ui/react';
import { Icon } from '@chakra-ui/icons';
import React from 'react';

function SocialLink({ url, iconSize }) {
  if (!url) return null;
  const isDiscord = /\/discord\.gg/.test(url);
  const isTwitter = /twitter\.com/.test(url) || /\/x\.com/.test(url);
  const isFacebook = /\/www\.facebook/.test(url);
  let icon = FaGlobe;
  icon = isFacebook ? FaFacebook : icon;
  icon = isTwitter ? FaXTwitter : icon;
  icon = isDiscord ? FaDiscord : icon;

  return (
    <Link href={url}>
      <Icon as={icon} size={iconSize} />
    </Link>
  );
}

export default SocialLink;
