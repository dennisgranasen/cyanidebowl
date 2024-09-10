import React, {useEffect} from 'react';
import { Box, VStack } from '@chakra-ui/react';
import Navigation from '../components/misc/Navigation';
import HeaderCard from '../components/common/HeaderCard';
import Circuits from '../components/Circuits';
import ImageUrls from '../ImageUrls';
import {useAuth0WithUserPermissions} from "../hooks/useAuth0WithUserPermissions";
import {useNavigate} from "react-router-dom";

function AdminPage() {
    const {
        authenticationReady,
        checkPermissions,
        userPermissions,
    } = useAuth0WithUserPermissions();
    const navigate = useNavigate()

    useEffect(() => {
        if ( authenticationReady && checkPermissions && !userPermissions.readCurrentUser)
        {
            navigate("/");
        }
    }, [authenticationReady, checkPermissions, userPermissions]);

  return (
    <VStack align="left">
      <Box>
        <Navigation currentPage="admin" />
      </Box>
      <HeaderCard
        mainImageSrc={ImageUrls.warpscoresLogoPng('medium')}
        heading="Admin"
        subHeading="Configure circuits for collecting data..."
      />
      <Box>
        <Circuits />
      </Box>
    </VStack>
  );
}

export default AdminPage;
