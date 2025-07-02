import React, {useState} from "react";
import WarpScoresApiService from "../../WarpScoresApiService";
import DelayedIconTooltip from "../common/DelayedIconTooltip";
import {WarningIcon} from "@chakra-ui/icons";
import {Button} from "@chakra-ui/react";

function NafExportButton({
                             competitionId,
                             checkPermissions,
                             authenticationReady,
                             isAuthenticated,
                             getAccessTokenSilently,
                             getAccessTokenWithPopup
                         }) {
    const [exporting, setExporting] = useState(false);
    const [error, setError] = useState(null);
    const exportNafReport = (competitionId, getAccessTokenSilently, getAccessTokenWithPopup, requestToken) => {
        setExporting(true);
        WarpScoresApiService
            .exportNafXml(competitionId, getAccessTokenSilently, getAccessTokenWithPopup, requestToken)
            .catch((reason) => setError({type: 'error', message: reason.toLocaleString()}))
            .finally(() => setExporting(false));
    }

    return <DelayedIconTooltip label={error ? error.message : 'Create NAF style report data XML file.'}
                               icon={error ? WarningIcon : null} shouldWrapChildren>
        <Button colorScheme={error ? 'red' : null}
                onClick={() => exportNafReport(competitionId, getAccessTokenSilently, getAccessTokenWithPopup, !checkPermissions || (authenticationReady && isAuthenticated))}
                isLoading={exporting}
                loadingText={exporting && 'Exporting'}
                isDisabled={error || exporting}>
            NAF-Export
        </Button>
    </DelayedIconTooltip>
}

export default NafExportButton;
