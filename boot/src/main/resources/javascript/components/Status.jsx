import React from "react";
import {CheckCircleIcon, WarningIcon} from "@chakra-ui/icons";
import {Box} from "@chakra-ui/react";

function Status({status}) {

    return <Box align="right">
        {status && status.gameServerDatabase ? <CheckCircleIcon color="green"/> : <WarningIcon color="red"/>}
        {status && status.gameServerAddressDirectory ? <CheckCircleIcon color="green"/> : <WarningIcon color="red"/>}
    </Box>
}

export default Status;
