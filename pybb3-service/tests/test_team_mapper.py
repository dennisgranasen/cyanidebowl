import base64
import xml.etree.ElementTree as ET
from app.services.team_mapper import teams_response

def encoded(value): return base64.b64encode(value.encode()).decode()

def test_maps_owned_team_list_without_raw_xml():
    root=ET.fromstring(f"""<ResponseGetTeams><Total>1</Total><Teams><Team>
      <Id>{encoded('team-1')}</Id><Name>{encoded('The Team')}</Name><Race>13</Race>
      <TeamValue>1230000</TeamValue><IsCustom>false</IsCustom><IsTemplate>false</IsTemplate>
    </Team></Teams></ResponseGetTeams>""")
    result=teams_response(root,start=0,size=50)
    assert result["items"][0] == {"id":"team-1","name":"The Team","raceId":13,"teamValue":1230000,
                                  "logoId":None,"isCustom":False,"isTemplate":False}
    assert result["total"] == 1 and result["hasMore"] is False
