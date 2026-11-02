import React, { useEffect, useState } from 'react';
import { Alert, Box, Heading, VStack } from '@chakra-ui/react';
import { useNavigate } from 'react-router-dom';
import { useIntl } from 'react-intl';
import Navigation from '../components/misc/Navigation';
import EditorialArticleReviewQueue from '../components/ai-reporters/EditorialArticleReviewQueue';
import RejectedMatchArticlesAdmin from '../components/admin/RejectedMatchArticlesAdmin';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';

export default function AdminEditorialPage() {
  const {
    authenticationReady,
    userPermissions,
    getAccessTokenSilently,
  } = useAuth0WithUserPermissions();
  const intl = useIntl();
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const canReview = Boolean(
    userPermissions.writeSiteAdmin
    || userPermissions.writeEditor
    || userPermissions.editorForLeagueSystems?.length
  );

  useEffect(() => {
    if (authenticationReady && !canReview) navigate('/admin');
  }, [authenticationReady, canReview, navigate]);

  return (
    <VStack align="stretch" spacing={5} p={{ base: 3, md: 6 }}>
      <Navigation currentPage="admin" parentPage="admin" />
      <Box>
        <Heading>{intl.formatMessage({ id: 'admin.editorial.heading' })}</Heading>
      </Box>
      {error && <Alert status="error">{error.message || String(error)}</Alert>}
      {canReview && (
        <EditorialArticleReviewQueue
          userPermissions={userPermissions}
          getAccessTokenSilently={getAccessTokenSilently}
        />
      )}
      {userPermissions.writeSiteAdmin && (
        <RejectedMatchArticlesAdmin
          getAccessTokenSilently={getAccessTokenSilently}
          onError={setError}
        />
      )}
    </VStack>
  );
}