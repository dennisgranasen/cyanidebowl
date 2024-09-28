import React, {useEffect, useState} from 'react';
import {Box, VStack} from '@chakra-ui/react';
import {Link as RouteLink, useParams} from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import Navigation from '../components/misc/Navigation';
import comparators from '../util/Comparators';
import ImageUrls from '../ImageUrls';
import prettyPrint from '../util/PrettyPrint';
import CompetitionProgress from '../components/competition/CompetitionProgress';
import InfoArea from '../components/common/InfoArea';
import InfoItem from '../components/common/InfoItem';
import HeaderCard from '../components/common/HeaderCard';
import formatter from '../util/Formatter';
import NoneKnockoutCompetition from '../components/competition/NoneKnockoutCompetition';
import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';
import KnockoutCompetition from '../components/competition/KnockoutCompetition';
import config from '../config';
import {useAuth0WithUserPermissions} from "../hooks/useAuth0WithUserPermissions";
import NafExportButton from "../components/misc/NafExportButton";

function isKnockout(competition) {
    return competition?.format.toLowerCase() === 'knockout';
}


function CompetitionPage() {
    const {
        authenticationReady,
        isAuthenticated,
        checkPermissions,
        userPermissions,
        getAccessTokenSilently,
        getAccessTokenWithPopup
    } = useAuth0WithUserPermissions();
    const {competitionUuid} = useParams();
    const [competition, setCompetition] = useState(null);
    const [ranks, setRanks] = useState([]);
    const [contests, setContests] = useState([]);
    const [ranksLoading, setRanksLoading] = useState(false);
    const [contestsLoading, setContestsLoading] = useState(false);
    const [competitionLoading, setCompetitionLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchCompetition = () => {
            setCompetitionLoading(true);
            WarpScoresApiService.competition(competitionUuid)
                .then((data) => {
                    setCompetition(data);
                })
                .catch((reason) => setError({type: 'error', message: reason.toLocaleString()}))
                .finally(() => setCompetitionLoading(false));
        };

        fetchCompetition();
    }, []);

    useEffect(() => {
        const fetchContests = () => {
            setContestsLoading(true);
            WarpScoresApiService.competitionContests(competitionUuid)
                .then((data) => {
                    data.sort((compA, compB) => comparators.compareAsDates(compA.matchDate, compB.matchDate));
                    setContests(data);
                })
                .catch((reason) => setError({type: 'error', message: reason.toLocaleString()}))
                .finally(() => setContestsLoading(false));
        };
        fetchContests();
    }, [competition]);

    useEffect(() => {
        const fetchRanks = () => {
            setRanksLoading(true);
            WarpScoresApiService.competitionRanks(competitionUuid)
                .then((data) => {
                    data.sort((rankA, rankB) => rankA.rank - rankB.rank);
                    setRanks(data);
                })
                .catch((reason) => setError({type: 'error', message: reason.toLocaleString()}))
                .finally(() => setRanksLoading(false));
        };
        if (competition) {
            fetchRanks();
        }
    }, [competition]);

    return (
        <VStack align="left">
            <Box>
                <Navigation
                    currentPage="competition"
                    league={competition ? [competition.leagueId, competition.leagueName] : []}
                    competition={[competitionUuid, competition ? competition.name : '']}
                />
            </Box>
            <LoadingOrErrorWrapper loading={competitionLoading} error={error}>
                {competition && (
                    <HeaderCard
                        heading={competition.name}
                        subHeading={<RouteLink
                            to={`/${competition.leagueId}`}>League: {competition.leagueName}</RouteLink>}
                        detailsHeading="Competition details"
                        mainImageSrc={ImageUrls.logo(competition.leagueLogo)}
                        additionalImageSrc={ImageUrls.logo(competition.logo)}
                    >
                        <InfoArea>
                            <InfoItem key="Created" label="Created"
                                      info={formatter.formatAsDate(competition.dateCreated)}/>
                            <InfoItem key="Format" label="Format" info={prettyPrint(competition.format)}/>
                            <InfoItem
                                key="Progress"
                                label="Progress"
                                info={
                                    <CompetitionProgress
                                        status={competition.status}
                                        format={competition.format}
                                        currentRound={competition.currentRound}
                                        totalRounds={competition.totalRounds}
                                        totalMatches={competition.totalMatches}
                                        playedMatches={competition.playedMatches}
                                        notValidatedMatches={competition.notValidatedMatches}
                                        liveMatches={competition.liveMatches}
                                        withPadding
                                    />
                                }
                            />
                            <InfoItem key="Teams" label="Teams" info={formatter.formatAsNumber(competition.teamsMax)}/>
                            <InfoItem
                                key="TimeSettings"
                                label="Time settings"
                                info={`Turn: ${formatter.formatAsNumber(competition.turnDuration / 60)}m`}
                                additionalInfo={`Bonus: ${formatter.formatAsNumber(competition.timeBonusDuration / 60)}m`}
                            />
                            {
                                'Finished' === competition.status && (!checkPermissions || (authenticationReady && userPermissions.writeLeagueAdmin)) && (
                                    <InfoItem
                                        key="NafDataExport"
                                        label="Export"
                                        info={<NafExportButton authenticationReady={authenticationReady}
                                                               checkPermissions={checkPermissions}
                                                               isAuthenticated={isAuthenticated}
                                                               getAccessTokenSilently={getAccessTokenSilently}
                                                               getAccessTokenWithPopup={getAccessTokenWithPopup}
                                                               competitionUuid={competition.uuid}/>}/>
                                )
                            }
                        </InfoArea>
                    </HeaderCard>
                )}
                {isKnockout(competition) ? (
                    <KnockoutCompetition
                        ranks={ranks}
                        ranksLoading={competitionLoading || ranksLoading}
                        contests={contests}
                        contestsLoading={contestsLoading}
                        competition={competition}
                        competitionLoading={competitionLoading}
                    />
                ) : (
                    <NoneKnockoutCompetition
                        ranks={ranks}
                        ranksLoading={competitionLoading || ranksLoading}
                        contests={contests}
                        contestsLoading={contestsLoading}
                        competition={competition}
                        competitionLoading={competitionLoading}
                    />
                )}
            </LoadingOrErrorWrapper>
        </VStack>
    );
}

export default CompetitionPage;
