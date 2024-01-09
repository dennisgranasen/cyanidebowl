import React from "react";
import {Breadcrumb, BreadcrumbItem, BreadcrumbLink, Flex, Spacer} from "@chakra-ui/react";
import {Link as RouteLink} from "react-router-dom";
import Status from "./Status";

function Navigation({currentPage, status}) {

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
            {isPage("team", currentPage) ?
                <BreadcrumbItem isCurrentPage={isPage("team", currentPage)}>
                    <BreadcrumbLink>Team</BreadcrumbLink>
                </BreadcrumbItem>
                : ""}
            {isPage("coach", currentPage) ?
                <BreadcrumbItem isCurrentPage={isPage("coach", currentPage)}>
                    <BreadcrumbLink>Coach</BreadcrumbLink>
                </BreadcrumbItem>
                : ""}
        </Breadcrumb>
        <Spacer/>
        <Status/>
    </Flex>
}

export default Navigation;
