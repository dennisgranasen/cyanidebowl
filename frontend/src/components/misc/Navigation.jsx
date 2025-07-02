import React from 'react';
import { Breadcrumb, BreadcrumbItem, BreadcrumbLink, Flex, Spacer } from '@chakra-ui/react';
import { Link as RouteLink } from 'react-router-dom';
import Menu from './Menu';
import AuthButton from './AuthButton';
import config from '../../config';
import ToggleColorModeButton from './ToggleColorModeButton';
import prettyPrint from '../../util/prettyPrint';

const { isProduction } = config;

function Navigation({ currentPage, parentPage, league, competition, circuit, team, race, coach }) {
  const isPage = (pageName, currentPageName) => {
    return pageName === currentPageName;
  };
  const leagueLink = league && league.length > 0 && league[0] ? `/league/${league[0]}` : '/';
  const competitionLink = competition && competition.length > 0 && competition[0] ? `/competition/${competition[0]}` : '';
  const teamLink = team ? `${competitionLink}/team/${team[0]}` : '';
  const circuitLink = circuit ? `${isPage('admin', parentPage) ? '/admin' : ''}/circuit/${circuit[0]}` : '';

  return (
    <Flex>
      <Breadcrumb fontFamily="bigStar" spacing={1}>
        <BreadcrumbItem isCurrentPage={isPage('home', currentPage)}>
          <BreadcrumbLink variant="menu" as={RouteLink} to="/">
            Home
          </BreadcrumbLink>
        </BreadcrumbItem>
        {(isPage('admin', currentPage) || isPage('admin', parentPage)) && (
          <BreadcrumbItem isCurrentPage={isPage('admin', currentPage)} flexWrap>
            <BreadcrumbLink variant="menu" as={RouteLink} to="/admin">
              Admin
            </BreadcrumbLink>
          </BreadcrumbItem>
        )}
        {circuit && (
          <BreadcrumbItem isCurrentPage={isPage('circuits', currentPage)} flexWrap>
            <BreadcrumbLink variant="menu" as={RouteLink} to={circuitLink}>
              {circuit[1]}
            </BreadcrumbLink>
          </BreadcrumbItem>
        )}
        {league && (
          <BreadcrumbItem isCurrentPage={isPage('league', currentPage)} flexWrap>
            <BreadcrumbLink variant="menu" as={RouteLink} to={leagueLink}>
              {league[1]}
            </BreadcrumbLink>
          </BreadcrumbItem>
        )}
        {competition && (
          <BreadcrumbItem isCurrentPage={isPage('competition', currentPage)} flexWrap>
            <BreadcrumbLink variant="menu" as={RouteLink} to={competitionLink}>
              {competition[1]}
            </BreadcrumbLink>
          </BreadcrumbItem>
        )}
        {team && (
          <BreadcrumbItem isCurrentPage={isPage('team', currentPage)} flexWrap>
            <BreadcrumbLink variant="menu" as={RouteLink} to={teamLink}>
              {team[1]}
            </BreadcrumbLink>
          </BreadcrumbItem>
        )}
        {race && (
          <BreadcrumbItem isCurrentPage={isPage('race', currentPage)} flexWrap>
            <BreadcrumbLink variant="menu">{prettyPrint(race)}</BreadcrumbLink>
          </BreadcrumbItem>
        )}
        {coach && (
          <BreadcrumbItem isCurrentPage={isPage('coach', currentPage)} flexWrap>
            <BreadcrumbLink variant="menu">{coach}</BreadcrumbLink>
          </BreadcrumbItem>
        )}
      </Breadcrumb>
      <Spacer />
      <ToggleColorModeButton />
      {isProduction && <AuthButton mr="0.5rem" />}
      <Menu />
    </Flex>
  );
}

export default Navigation;
