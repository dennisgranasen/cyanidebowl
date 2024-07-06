import React, { useEffect, useState } from 'react';
import { 
  Box, 
  Button,
  ButtonGroup,
  FormControl,
  Input,
  List,
  ListItem,
  OrderedList,
  UnorderedList,
  FormLabel,
  Heading,
  Select,
  Spinner,
  Center,
  Tab,
  Table,
  TableContainer,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
  Tbody,
  Tfoot,
  Th,
  Thead,
  Tr,
  useMediaQuery,
  Wrap,
  WrapItem,
  VStack, 
  Checkbox} from '@chakra-ui/react';
import { Link as RouteLink, useParams } from 'react-router-dom';
import WarpScoresApiService from '../WarpScoresApiService';
import prettyPrint from '../util/PrettyPrint';
import Navigation from '../components/misc/Navigation';
import CircuitLeg from '../components/circuit/CircuitLeg';
import HeaderCard from '../components/common/HeaderCard';
import axios from 'axios';


function CircuitPage() {
  const [smallscreen] = useMediaQuery('(max-width: 768px)');
  const platforms = [
    "bb1.pc",
    "bb2.pc",
    "bb2.ps",
    "bb2.xbox",
    "bb3.cross",
    "bb3.pc",
    "bb3.ps",
    "bb3.switch",
    "bb3.xbox",
    /*
    "fumbbl.lrb6",
    "fumbbl.2020",
    "tt.lrb6",
    "tt.2020"*/
  ]
  const legTypes = [
    "League",
    "Competition"
  ]
  const { circuitId } = useParams();
  const [circuit, setCircuit] = useState();
  const [platform, setPlatform] = useState("bb3.cross");
  const [competitionId, setCompetitionId] = useState(null);
  const [legType, setLegType] = useState("Competition");
  const [customLabel, setCustomLabel] = useState();
  const [isKnockout, setIsKnockout] = useState(false);
  const [isCollect, setIsCollect] = useState(true);

  const changePlatform = (e) => {
    const platformId = e.target.value;
    setPlatform(platformId);
  };

  const changeLegType = (e) => {
    const compType = e.target.value;
    setLegType(compType);
  };

  const onCompetitionIdChanged = (e) => {
    setCompetitionId(e.target.value);
  };

  const onCustomLabelChanged = (e) => {
    setCustomLabel(e.target.value);
  };

  const compareCC = (a,b) => {
    return a.label.localeCompare(b.label);
  }

  const fetchCircuit = (id) => {
    WarpScoresApiService.circuit(id)
    .then((res) => {
        if (res.circuitLegs == null)
          res.circuitLegs = [];
        else
          res.circuitLegs.sort(compareCC);
        setCircuit(res);
      }
    )
    .catch((err) => console.log(err));
  };

  const onCollectChanged = (e) => {
    setIsCollect(!isCollect);
  };

  const onKnockoutChanged = (e) => {
    setIsKnockout(!isKnockout);
  };

  const onAddLegClicked = (e) => {    
    const platformParts = platform.split('.');
    const game = platformParts[0].toUpperCase();
    const p = platformParts[1].toUpperCase();
    
    WarpScoresApiService.addLegToCircuit(circuit.circuitId, competitionId, legType, customLabel, game, p, isCollect, isKnockout)
      .then((data) => fetchCircuit(circuitId))
      .catch((err) => console.log(err));
  };

  function TableColumns(smallscreen) {
    return (
      <Tr>
        <Th>
        </Th>
        <Th>
          <Center>Label</Center>
        </Th>
        <Th>
          <Center>League/Competition</Center>
        </Th>
        <Th>
          <Center>LegType</Center>
        </Th>
        <Th>
          <Center>Game version</Center>
        </Th>
        <Th>
          <Center>Platform</Center>
        </Th>
        <Th>
          <Center>Knockout?</Center>
        </Th>
        <Th>
          <Center>Collect?</Center>
        </Th>
      </Tr>
    );
  }
  

  useEffect( () => {
    fetchCircuit(circuitId);
   
  }, []);

  return (
    <VStack align="left">
      <Box>
        <Navigation
          currentPage="Circuits"
        />
      </Box>
        { circuit && ( 
          <>
            <HeaderCard
              smallscreen={smallscreen ? 'smallscreen' : undefined}
              heading={circuit.circuitName}
              detailsHeading="Circuit details"
            >
            </HeaderCard>
            <Box>
              <FormControl>
                <FormLabel>Circuit legs</FormLabel>
                <TableContainer>
                  <Table variant="simpleClickable" size="sm">
                    <Thead>{TableColumns(smallscreen)}</Thead>
                    <Tbody>
                      {circuit.circuitLegs                        
                        .map((cl) => {
                          return (
                            <CircuitLeg
                              smallscreen={smallscreen ? 'smallscreen' : undefined}
                              key={cl.label} circuitLeg={cl}
                            />
                          );
                        })}
                    </Tbody>
                    <Tfoot>{TableColumns(smallscreen)}</Tfoot>
                  </Table>
                </TableContainer>
                <FormLabel marginTop={2}>Add leagues:</FormLabel>
                <Wrap margin={1} borderWidth={1} padding={2} borderRadius={5}>
                  <WrapItem>
                    <Input placeholder="Enter Cyanide league/comp. Id" onChange={onCompetitionIdChanged} width={300}/>
                  </WrapItem>
                  <WrapItem>
                    <Select 
                        variant="outlined"
                        placeholder="Select leg type"
                        onChange={changeLegType}
                        value={legType ? legType : undefined}
                    >
                        {legTypes.map((p) => (
                        <option value={p} key={p}>
                            {p}
                        </option>
                        ))}
                    </Select>
                  </WrapItem>

                  <WrapItem>
                    <Select 
                        variant="outlined"
                        placeholder="Select platform"
                        onChange={changePlatform}
                        value={platform ? platform : undefined}
                    >
                        {platforms.map((p) => (
                        <option value={p} key={p}>
                            {p.replace("."," ")}
                        </option>
                        ))}
                    </Select>
                  </WrapItem>
                  <WrapItem>
                    <Input placeholder="Enter custom label" onChange={onCustomLabelChanged} />
                  </WrapItem>
                  <WrapItem>
                    <Checkbox name="Knockout" onChange={onKnockoutChanged} readOnly={false}/>
                    <FormLabel>Knockout?</FormLabel>
                  </WrapItem>
                  <WrapItem>
                    <Checkbox name="Collect?" defaultChecked={true} onChange={onCollectChanged} readOnly={false}/>
                    <FormLabel>Collect?</FormLabel>
                  </WrapItem>
                  <WrapItem>
                    <Button onClick={onAddLegClicked}>Add leg</Button>
                  </WrapItem>
                </Wrap>
              </FormControl>
            </Box>
          </>)}
    </VStack>
  );
}

export default CircuitPage;
