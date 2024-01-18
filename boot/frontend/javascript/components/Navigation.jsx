import React from "react";
import {Breadcrumb, BreadcrumbItem, BreadcrumbLink, Flex, Spacer} from "@chakra-ui/react";
import {Link as RouteLink} from "react-router-dom";
import Status from "./Status";

function Navigation({currentPage, league, competition, team}) {

    const leagueLink = league ? `/${league[0]}` : "/";
    const competitionLink = competition ? `/competition/${competition[0]}` : "";
    const teamLink = team ? `/team/${team[0]}` : "";
    const isPage = (pageName, currentPage) => {
        return pageName === currentPage;
    }
    const isNotPage = (pageName, currentPage) => {
        return pageName !== currentPage;
    }

    return <Flex>
        <Breadcrumb>
            <BreadcrumbItem isCurrentPage={isPage("home", currentPage)}>
                <BreadcrumbLink as={RouteLink} to="/">
                    Home
                </BreadcrumbLink>
            </BreadcrumbItem>
            {league ? <BreadcrumbItem>
                    <BreadcrumbLink as={RouteLink} to={leagueLink}>
                        {league[1]}
                    </BreadcrumbLink>
                </BreadcrumbItem>
                : ""}
            {competition ?
                <BreadcrumbItem isCurrentPage={isPage("competition", currentPage)}>
                    <BreadcrumbLink as={RouteLink} to={competitionLink}>
                        {competition[1]}
                    </BreadcrumbLink>
                </BreadcrumbItem>
                : ""
            }
            {team ?
                <BreadcrumbItem isCurrentPage={isPage("team", currentPage)}>
                    <BreadcrumbLink as={RouteLink} to={teamLink}>
                        {team[1]}
                    </BreadcrumbLink>
                </BreadcrumbItem>
                : ""}
        </Breadcrumb>
        <Spacer/>
        <Status/>
    </Flex>
}

export default Navigation;
