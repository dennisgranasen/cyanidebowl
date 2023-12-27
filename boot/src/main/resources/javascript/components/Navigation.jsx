import React from "react";
import {Breadcrumb, BreadcrumbItem, BreadcrumbLink} from "@chakra-ui/react";
import {Link as RouteLink} from "react-router-dom";

function Navigation({currentPage, status}) {

    const isPage = (pageName, currentPage) => {
        return pageName === currentPage;
    }
    const isNotPage = (pageName, currentPage) => {
        return pageName !== currentPage;
    }

    return <Breadcrumb>
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
}

export default Navigation;
