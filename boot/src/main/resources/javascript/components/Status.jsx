import React, {useEffect, useState} from "react";
import {CheckCircleIcon, WarningIcon} from "@chakra-ui/icons";
import {Box, Spinner, Tooltip} from "@chakra-ui/react";
import CyanideApiService from "../CyanideApiService";
import config from "../config";

function Status() {
    const [status, setStatus] = useState(null);
    useEffect(() => {
        const fetchStatus = () => {
            setStatus(null);
            CyanideApiService.status().then((data) => {
                setStatus(data)
            })
                .catch((reason) => {
                    setError(reason.toLocaleString(config.locale));
                })
        }
        setInterval(fetchStatus, 30_000);
        fetchStatus();
    }, []);

    return <Box align="right">
        <Tooltip label={`Last check: ${status !== null ? status.lastCheck : '-'}`}>
            <Box>
                {status === null ? <Spinner size="sm" color="orange"/> : (status.gameServerDatabase ?
                    <CheckCircleIcon size="sm" color="green"/> :
                    <WarningIcon size="sm" color="red"/>)}
                {status === null ? <Spinner size="sm" color="orange"/> : (status.gameServerAddressDirectory ?
                    <CheckCircleIcon size="sm" color="green"/> : <WarningIcon size="sm" color="red"/>)}
            </Box>
        </Tooltip>
    </Box>
}

export default Status;
