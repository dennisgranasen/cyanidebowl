import React from 'react';
import { Breadcrumb, BreadcrumbItem, BreadcrumbLink, Flex, Spacer } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import Menu from './Menu';

function Navigation({ currentPage, league, competition, circuit, team }) {
  const leagueLink = league ? `/${league[0]}` : '/';
  const competitionLink = competition ? `/competition/${competition[0]}` : '';
  const teamLink = team ? `${competitionLink}/team/${team[0]}` : '';
  const circuitLink = circuit ? `/admin/circuit/${circuit[0]}` : '';
  const isPage = (pageName, currentPageName) => {
    return pageName === currentPageName;
  };

  return (
    <Flex>
      <Breadcrumb spacing={1}>
        <BreadcrumbItem isCurrentPage={isPage('home', currentPage)}>
          <BreadcrumbLink as={RouteLink} to="/">
            Home
          </BreadcrumbLink>
        </BreadcrumbItem>
        {(isPage('admin', currentPage) || isPage('circuits', currentPage)) && (
          <BreadcrumbItem isCurrentPage={isPage('admin', currentPage)} flexWrap>
            <BreadcrumbLink as={RouteLink} to="/admin">
              Admin
            </BreadcrumbLink>
          </BreadcrumbItem>
        )}
        {circuit && (
          <BreadcrumbItem isCurrentPage={isPage('circuits', currentPage)} flexWrap>
            <BreadcrumbLink as={RouteLink} to={circuitLink}>
              {circuit[1]}
            </BreadcrumbLink>
          </BreadcrumbItem>
        )}
        {league && (
          <BreadcrumbItem isCurrentPage={isPage('league', currentPage)} flexWrap>
            <BreadcrumbLink as={RouteLink} to={leagueLink}>
              {league[1]}
            </BreadcrumbLink>
          </BreadcrumbItem>
        )}
        {competition && (
          <BreadcrumbItem isCurrentPage={isPage('competition', currentPage)} flexWrap>
            <BreadcrumbLink as={RouteLink} to={competitionLink}>
              {competition[1]}
            </BreadcrumbLink>
          </BreadcrumbItem>
        )}
        {team && (
          <BreadcrumbItem isCurrentPage={isPage('team', currentPage)} flexWrap>
            <BreadcrumbLink as={RouteLink} to={teamLink}>
              {team[1]}
            </BreadcrumbLink>
          </BreadcrumbItem>
        )}
      </Breadcrumb>
      <Spacer />
      <Menu />
    </Flex>
  );
}

export default Navigation;
