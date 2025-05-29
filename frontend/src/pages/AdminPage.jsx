import React, { useEffect } from 'react';
import { Box, VStack } from '@chakra-ui/react';
import { useNavigate } from 'react-router-dom';
import Navigation from '../components/misc/Navigation';
import HeaderCard from '../components/common/HeaderCard';
import imageUrls from '../imageUrls';
import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';
import AdminCircuits from '../components/circuit/AdminCircuits';

function AdminPage() {
  const { authenticationReady, checkPermissions, userPermissions } = useAuth0WithUserPermissions();
  const navigate = useNavigate();

  useEffect(() => {
    if (authenticationReady && checkPermissions && !userPermissions.readCurrentUser) {
      navigate('/');
    }
  }, [authenticationReady, checkPermissions, userPermissions]);

  return (
    <VStack align="left">
      <Box>
        <Navigation currentPage="admin" />
      </Box>
      <HeaderCard
        mainImageSrc={imageUrls.warpscoresLogoPng('medium')}
        heading="Admin"
        subHeading="Configure circuits for collecting data..."
      />
      <Box>
        <AdminCircuits />
      </Box>
    </VStack>
  );
}

export default AdminPage;
