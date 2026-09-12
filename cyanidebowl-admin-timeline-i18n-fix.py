#!/usr/bin/env python3
from pathlib import Path
import subprocess, sys

EXPECTED_BRANCH = "dev"
OPS = []

def add(path, old, new):
    OPS.append((path, old, new))

# Add message IDs after reporter.noReports in each real locale.
catalog = {
"en": ("  'reporter.noReports': 'No published reports yet.',\n", {
"title":"Technician · Internal state","description":"Persistent memories, social relationships, runtime state and latest AI provenance.",
"memories":"Memories","relationships":"Relationships","activity":"Activity","runtime":"Runtime","memory":"Memory","references":"References",
"targetType":"Target type","referenceId":"Reference ID","displayName":"Display name","sentiment":"Sentiment","confidence":"Confidence",
"evidence":"Evidence","sources":"Sources","supersededBy":"Superseded by","updated":"Updated","generationTraces":"Generation traces",
"hardConstraints":"Hard constraints","author":"Author","taskInstructionPreview":"Task instruction preview","recentActivity":"Recent AI article activity",
"match":"Match","providerModel":"Provider/model","request":"Request","tokens":"Tokens","reporter":"Reporter","userId":"User ID",
"enabled":"Enabled","reports":"Reports","interactions":"Interactions","ratings":"Ratings","writingWeight":"Writing weight",
"effectiveLanguage":"Effective language","profileLanguage":"Profile language","casualty":"Casualty"}),
"sv": ("  'reporter.noReports': 'Inga publicerade rapporter ännu.',\n", {
"title":"Tekniker · Internt tillstånd","description":"Beständiga minnen, sociala relationer, driftläge och senaste AI-proveniens.",
"memories":"Minnen","relationships":"Relationer","activity":"Aktivitet","runtime":"Drift","memory":"Minne","references":"Referenser",
"targetType":"Måltyp","referenceId":"Referens-ID","displayName":"Visningsnamn","sentiment":"Attityd","confidence":"Säkerhet",
"evidence":"Evidens","sources":"Källor","supersededBy":"Ersatt av","updated":"Uppdaterad","generationTraces":"Genereringsspår",
"hardConstraints":"Hårda begränsningar","author":"Författare","taskInstructionPreview":"Förhandsvisning av uppgiftsinstruktion","recentActivity":"Senaste AI-artikelaktivitet",
"match":"Match","providerModel":"Leverantör/modell","request":"Förfrågan","tokens":"Token","reporter":"Reporter","userId":"Användar-ID",
"enabled":"Aktiverad","reports":"Rapporter","interactions":"Interaktioner","ratings":"Betyg","writingWeight":"Skrivvikt",
"effectiveLanguage":"Aktivt språk","profileLanguage":"Profilspråk","casualty":"Skada"}),
"es": ("  'reporter.noReports': 'Aún no hay informes publicados.',\n", {
"title":"Técnico · Estado interno","description":"Memorias persistentes, relaciones sociales, estado de ejecución y procedencia reciente de la IA.",
"memories":"Memorias","relationships":"Relaciones","activity":"Actividad","runtime":"Ejecución","memory":"Memoria","references":"Referencias",
"targetType":"Tipo de objetivo","referenceId":"ID de referencia","displayName":"Nombre visible","sentiment":"Actitud","confidence":"Confianza",
"evidence":"Evidencia","sources":"Fuentes","supersededBy":"Sustituido por","updated":"Actualizado","generationTraces":"Trazas de generación",
"hardConstraints":"Restricciones estrictas","author":"Autor","taskInstructionPreview":"Vista previa de la instrucción","recentActivity":"Actividad reciente de artículos de IA",
"match":"Partido","providerModel":"Proveedor/modelo","request":"Solicitud","tokens":"Tokens","reporter":"Reportero","userId":"ID de usuario",
"enabled":"Activado","reports":"Informes","interactions":"Interacciones","ratings":"Valoraciones","writingWeight":"Peso de escritura",
"effectiveLanguage":"Idioma efectivo","profileLanguage":"Idioma del perfil","casualty":"Baja"}),
"fi": ("  'reporter.noReports': 'Julkaistuja raportteja ei vielä ole.',\n", {
"title":"Teknikko · Sisäinen tila","description":"Pysyvät muistot, sosiaaliset suhteet, ajonaikainen tila ja viimeisin tekoälyn alkuperätieto.",
"memories":"Muistot","relationships":"Suhteet","activity":"Toiminta","runtime":"Ajonaikainen tila","memory":"Muisto","references":"Viitteet",
"targetType":"Kohdetyyppi","referenceId":"Viitetunnus","displayName":"Näyttönimi","sentiment":"Asenne","confidence":"Luottamus",
"evidence":"Näyttö","sources":"Lähteet","supersededBy":"Korvannut","updated":"Päivitetty","generationTraces":"Generointijäljet",
"hardConstraints":"Tiukat rajoitteet","author":"Kirjoittaja","taskInstructionPreview":"Tehtäväohjeen esikatselu","recentActivity":"Viimeaikainen AI-artikkelitoiminta",
"match":"Ottelu","providerModel":"Palveluntarjoaja/malli","request":"Pyyntö","tokens":"Tokenit","reporter":"Toimittaja","userId":"Käyttäjätunnus",
"enabled":"Käytössä","reports":"Raportit","interactions":"Vuorovaikutukset","ratings":"Arviot","writingWeight":"Kirjoituspaino",
"effectiveLanguage":"Käytössä oleva kieli","profileLanguage":"Profiilin kieli","casualty":"Loukkaantuminen"}),
"pl": ("  'reporter.noReports': 'Brak opublikowanych raportów.',\n", {
"title":"Technik · Stan wewnętrzny","description":"Trwałe wspomnienia, relacje społeczne, stan wykonawczy i najnowsze pochodzenie danych AI.",
"memories":"Wspomnienia","relationships":"Relacje","activity":"Aktywność","runtime":"Stan wykonawczy","memory":"Wspomnienie","references":"Odwołania",
"targetType":"Typ celu","referenceId":"ID odwołania","displayName":"Nazwa wyświetlana","sentiment":"Nastawienie","confidence":"Pewność",
"evidence":"Dowody","sources":"Źródła","supersededBy":"Zastąpione przez","updated":"Zaktualizowano","generationTraces":"Ślady generowania",
"hardConstraints":"Twarde ograniczenia","author":"Autor","taskInstructionPreview":"Podgląd instrukcji zadania","recentActivity":"Ostatnia aktywność artykułów AI",
"match":"Mecz","providerModel":"Dostawca/model","request":"Żądanie","tokens":"Tokeny","reporter":"Reporter","userId":"ID użytkownika",
"enabled":"Włączony","reports":"Raporty","interactions":"Interakcje","ratings":"Oceny","writingWeight":"Waga pisania",
"effectiveLanguage":"Efektywny język","profileLanguage":"Język profilu","casualty":"Kontuzja"}),
}
for _, (anchor, vals) in catalog.items():
    lines = [anchor.rstrip("\n")]
    for k,v in vals.items():
        msgid = "timeline.casualty" if k == "casualty" else f"reporterInspector.{k}"
        v = v.replace("\\","\\\\").replace("'","\\'")
        lines.append(f"  '{msgid}': '{v}',")
    add("frontend/src/i18n/messages.js", anchor, "\n".join(lines)+"\n")

p="frontend/src/components/ai-reporters/AiReporterInspector.jsx"
add(p,"import AiReporterApi from '../../AiReporterApi';\n","import AiReporterApi from '../../AiReporterApi';\nimport { useIntl } from 'react-intl';\n")
add(p,"function MemoryEditor({ isOpen, onClose, memory, onPrepare }) {\n  const [body, setBody] = useState('');",
      "function MemoryEditor({ isOpen, onClose, memory, onPrepare }) {\n  const intl = useIntl();\n  const [body, setBody] = useState('');")
add(p,"<FormLabel>Memory</FormLabel>","<FormLabel>{intl.formatMessage({ id: 'reporterInspector.memory' })}</FormLabel>")
add(p,"<FormLabel>Subjects</FormLabel>","<FormLabel>{intl.formatMessage({ id: 'reporterInspector.references' })}</FormLabel>")
add(p,"function RelationshipEditor({ isOpen, onClose, relationship, onPrepare }) {\n  const [subjectType, setSubjectType] = useState('TEAM');",
      "function RelationshipEditor({ isOpen, onClose, relationship, onPrepare }) {\n  const intl = useIntl();\n  const [subjectType, setSubjectType] = useState('TEAM');")
for old,key in [
("Target type","targetType"),("Subject ID","referenceId"),("Display name","displayName"),
("Sentiment (-1 … +1)","sentiment"),("Confidence (0 … 1)","confidence")]:
    suffix = " (-1 … +1)" if old.startswith("Sentiment") else (" (0 … 1)" if old.startswith("Confidence") else "")
    add(p,f"<FormLabel>{old}</FormLabel>",f"<FormLabel>{{intl.formatMessage({{ id: 'reporterInspector.{key}' }})}}{suffix}</FormLabel>")
add(p,"}) {\n  const [state, setState] = useState(null);","}) {\n  const intl = useIntl();\n  const [state, setState] = useState(null);")
repls = [
('            <Text fontWeight="700">Technician · Internal state</Text>',"            <Text fontWeight=\"700\">{intl.formatMessage({ id: 'reporterInspector.title' })}</Text>"),
("              Persistent MEMORY, social attitudes, runtime och senaste AI-provenance.","              {intl.formatMessage({ id: 'reporterInspector.description' })}"),
("<Tab>Memories</Tab>","<Tab>{intl.formatMessage({ id: 'reporterInspector.memories' })}</Tab>"),
("<Tab>Relationships</Tab>","<Tab>{intl.formatMessage({ id: 'reporterInspector.relationships' })}</Tab>"),
("<Tab>Activity</Tab>","<Tab>{intl.formatMessage({ id: 'reporterInspector.activity' })}</Tab>"),
("<Tab>Runtime</Tab>","<Tab>{intl.formatMessage({ id: 'reporterInspector.runtime' })}</Tab>"),
("Memories <Badge ml={2}>{state.memories?.length || 0}</Badge>","{intl.formatMessage({ id: 'reporterInspector.memories' })} <Badge ml={2}>{state.memories?.length || 0}</Badge>"),
("Relationships <Badge ml={2}>{state.relationships?.length || 0}</Badge>","{intl.formatMessage({ id: 'reporterInspector.relationships' })} <Badge ml={2}>{state.relationships?.length || 0}</Badge>"),
("Subjects: {(memory.subjects || []).map((s) => `${s.type}:${s.id}`).join(', ') || '—'}","{intl.formatMessage({ id: 'reporterInspector.references' })}: {(memory.subjects || []).map((s) => `${s.type}:${s.id}`).join(', ') || '—'}"),
("Sources: {(memory.sourceContentIds || []).join(', ') || '—'}","{intl.formatMessage({ id: 'reporterInspector.sources' })}: {(memory.sourceContentIds || []).join(', ') || '—'}"),
("Superseded by: {memory.supersededByMemoryId}","{intl.formatMessage({ id: 'reporterInspector.supersededBy' })}: {memory.supersededByMemoryId}"),
("Updated: {formatTime(memory.updatedAt)}","{intl.formatMessage({ id: 'reporterInspector.updated' })}: {formatTime(memory.updatedAt)}"),
('<Text fontSize="xs" color="gray.500">Sentiment</Text>',"<Text fontSize=\"xs\" color=\"gray.500\">{intl.formatMessage({ id: 'reporterInspector.sentiment' })}</Text>"),
('<Text fontSize="xs" color="gray.500">Confidence</Text>',"<Text fontSize=\"xs\" color=\"gray.500\">{intl.formatMessage({ id: 'reporterInspector.confidence' })}</Text>"),
('<Text fontSize="xs" color="gray.500">Evidence</Text>',"<Text fontSize=\"xs\" color=\"gray.500\">{intl.formatMessage({ id: 'reporterInspector.evidence' })}</Text>"),
('<Text fontSize="xs" fontWeight="700" mb={1}>Evidence</Text>',"<Text fontSize=\"xs\" fontWeight=\"700\" mb={1}>{intl.formatMessage({ id: 'reporterInspector.evidence' })}</Text>"),
("Generation traces <Badge ml={2}>{state.traces?.length || 0}</Badge>","{intl.formatMessage({ id: 'reporterInspector.generationTraces' })} <Badge ml={2}>{state.traces?.length || 0}</Badge>"),
('<Text fontSize="xs" fontWeight="700" mb={1}>Hard constraints</Text>',"<Text fontSize=\"xs\" fontWeight=\"700\" mb={1}>{intl.formatMessage({ id: 'reporterInspector.hardConstraints' })}</Text>"),
("Author: {item.authorDisplayName}","{intl.formatMessage({ id: 'reporterInspector.author' })}: {item.authorDisplayName}"),
("Subjects: {(item.subjects || [])","{intl.formatMessage({ id: 'reporterInspector.references' })}: {(item.subjects || [])"),
('<Text fontSize="xs" fontWeight="700">Task instruction preview</Text>',"<Text fontSize=\"xs\" fontWeight=\"700\">{intl.formatMessage({ id: 'reporterInspector.taskInstructionPreview' })}</Text>"),
("Recent AI article activity <Badge ml={2}>{state.activity?.length || 0}</Badge>","{intl.formatMessage({ id: 'reporterInspector.recentActivity' })} <Badge ml={2}>{state.activity?.length || 0}</Badge>"),
("Match: {activity.matchId}","{intl.formatMessage({ id: 'reporterInspector.match' })}: {activity.matchId}"),
("Provider/model: {activity.providerId || '—'} / {activity.model || '—'}","{intl.formatMessage({ id: 'reporterInspector.providerModel' })}: {activity.providerId || '—'} / {activity.model || '—'}"),
("Request: {activity.providerRequestId || '—'}","{intl.formatMessage({ id: 'reporterInspector.request' })}: {activity.providerRequestId || '—'}"),
("Tokens: {activity.inputTokens ?? '—'} in / {activity.outputTokens ?? '—'} out","{intl.formatMessage({ id: 'reporterInspector.tokens' })}: {activity.inputTokens ?? '—'} in / {activity.outputTokens ?? '—'} out"),
("Updated: {formatTime(activity.updatedAt)}","{intl.formatMessage({ id: 'reporterInspector.updated' })}: {formatTime(activity.updatedAt)}"),
]
for a,b in repls: add(p,a,b)
runtime = {
"Reporter":"reporter","User ID":"userId","Enabled":"enabled","Reports":"reports","Interactions":"interactions","Ratings":"ratings",
"Writing weight":"writingWeight","Effective language":"effectiveLanguage","Profile language":"profileLanguage"}
runtime_expr = {
"Reporter":"runtime.alias","User ID":"state.userId","Enabled":"String(runtime.enabled)","Reports":"String(runtime.reportsEnabled)",
"Interactions":"String(runtime.interactionsEnabled)","Ratings":"String(runtime.playerRatingsEnabled)","Writing weight":"runtime.writingWeight",
"Effective language":"runtime.primaryLanguage || '—'","Profile language":"runtime.profileLanguage || '—'"}
for label,key in runtime.items():
    expr=runtime_expr[label]
    add(p,f"<Text><b>{label}:</b> {{{expr}}}</Text>",f"<Text><b>{{intl.formatMessage({{ id: 'reporterInspector.{key}' }})}}:</b> {{{expr}}}</Text>")

p="frontend/src/components/contest/MatchTimelineBar.jsx"
add(p,"import TimelineIcon from './TimelineIcon';\n","import TimelineIcon from './TimelineIcon';\nimport { useIntl } from 'react-intl';\n")
add(p,"  casualty: 'CASUALTY',\n  injury: 'INJURY',","  casualty: 'CASUALTY',\n  damage: 'CASUALTY',\n  injury: 'INJURY',")
add(p,
"""export default function MatchTimelineBar({ timeline, events = [], match }) {
  const [logOpen, setLogOpen] = React.useState(false);
  const sourceEvents = timeline?.format === 'pybb3-narrative-timeline'
    ? narrativeDisplayEvents(timeline)
    : events;
  if (!sourceEvents.length) return null;""",
"""export default function MatchTimelineBar({ timeline, events = [], match }) {
  const intl = useIntl();
  const [logOpen, setLogOpen] = React.useState(false);
  const rawSourceEvents = timeline?.format === 'pybb3-narrative-timeline'
    ? narrativeDisplayEvents(timeline)
    : events;
  const sourceEvents = rawSourceEvents.map((event) =>
    event?.type === 'CASUALTY'
      ? { ...event, title: intl.formatMessage({ id: 'timeline.casualty' }) }
      : event
  );
  if (!sourceEvents.length) return null;""")

def branch():
    return subprocess.check_output(["git","branch","--show-current"],text=True).strip()

def main():
    b=branch()
    if b!=EXPECTED_BRANCH:
        raise RuntimeError(f"Expected branch {EXPECTED_BRANCH!r}, current branch is {b!r}")
    staged={}
    indexes={}
    for path,old,new in OPS:
        if path not in staged:
            p=Path(path)
            if not p.exists(): raise RuntimeError(f"Missing file: {path}")
            staged[path]=p.read_text(encoding="utf-8-sig")
            indexes[path]=0
        indexes[path]+=1
        n=staged[path].count(old)
        if n!=1:
            raise RuntimeError(f"{path} replacement {indexes[path]} expected exactly one match, found {n}. No files were written.")
        staged[path]=staged[path].replace(old,new,1)
    for path,text in staged.items():
        Path(path).write_text(text,encoding="utf-8",newline="\n")
    print(f"Applied {len(OPS)} replacements across {len(staged)} files.")
    for path in staged: print(f"  {path}")

if __name__=="__main__":
    try: main()
    except Exception as e:
        print(e,file=sys.stderr); sys.exit(1)
