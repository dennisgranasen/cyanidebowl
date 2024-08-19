import { useAuth0 } from '@auth0/auth0-react';
import { Avatar, AvatarBadge, Spinner } from '@chakra-ui/react';
import React, { useEffect, useState } from 'react';
import config from '../../config';
import DelayedIconTooltip from '../common/DelayedIconTooltip';

const { smallBoxSize } = config;

function AuthButton({ ...props }) {
  const { user, isAuthenticated, isLoading } = useAuth0();
  const [name, setName] = useState();
  const [image, setImage] = useState();
  const [status, setStatus] = useState();

  useEffect(() => {
    if (isLoading) {
      setStatus('Initializing authentication provider.');
    } else {
      setName(isAuthenticated ? user.name : null);
      setImage(isAuthenticated ? user.picture : null);
      setStatus(isAuthenticated ? `Authenticated as ${user.name}.` : 'Not authenticated.');
    }
  }, [isLoading, isAuthenticated]);

  return (
    <DelayedIconTooltip shouldWrapChildren label={status}>
      <Avatar {...props} name={name} src={image}>
        {isLoading && (
          <AvatarBadge boxSize={smallBoxSize} bg="black">
            <Spinner size="sm" color="grey" />
          </AvatarBadge>
        )}
      </Avatar>
    </DelayedIconTooltip>
  );
}

export default AuthButton;
