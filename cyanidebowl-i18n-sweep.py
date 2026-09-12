#!/usr/bin/env python3
"""Apply the BlaskScore UI localization sweep to a cyanidebowl checkout.

Run from the repository root on the current dev branch. The patcher is
transactional per file: it validates every expected source fragment before
writing anything, so drift causes a clean abort instead of a partial edit.
"""
from pathlib import Path
import sys

EXPECTED_BRANCH = "dev"

OPERATIONS = [('frontend/src/i18n/messages.js',
  "  'localizationAdmin.saved': 'Default language saved.',\n"
  "  'localizationAdmin.saveError': 'Could not save the default language.',\n"
  "  'common.loading': 'Loading…',\n"
  "  'common.save': 'Save',\n",
  "  'localizationAdmin.saved': 'Default language saved.',\n"
  "  'localizationAdmin.saveError': 'Could not save the default language.',\n"
  '  \'home.tagline\': "Blödareblaskan\'s incorruptible(?) results service",\n'
  "  'common.unknown': 'Unknown',\n"
  "  'common.unknownPlayer': 'Unknown player',\n"
  "  'common.unknownTeam': 'Unknown team',\n"
  "  'common.previous': 'Previous',\n"
  "  'common.next': 'Next',\n"
  "  'common.cancel': 'Cancel',\n"
  "  'common.continue': 'Continue',\n"
  "  'common.date': 'Date',\n"
  "  'common.competition': 'Competition',\n"
  "  'common.home': 'Home',\n"
  "  'common.away': 'Away',\n"
  "  'common.result': 'Result',\n"
  "  'common.rank': 'Rank',\n"
  "  'common.player': 'Player',\n"
  "  'common.team': 'Team',\n"
  "  'common.coach': 'Coach',\n"
  "  'common.position': 'Position',\n"
  "  'common.games': 'Games',\n"
  "  'common.skills': 'Skills',\n"
  "  'common.score': 'Score',\n"
  "  'common.name': 'Name',\n"
  "  'common.type': 'Type',\n"
  "  'common.level': 'Level',\n"
  "  'common.injuries': 'Injuries',\n"
  "  'common.value': 'Value',\n"
  "  'common.players': 'Players',\n"
  "  'common.teams': 'Teams',\n"
  "  'common.matches': 'Matches',\n"
  "  'menu.lastCheck': 'Last check:',\n"
  "  'menu.outdated': 'Outdated',\n"
  "  'menu.cyanideAdminTools': 'Cyanide admin tools',\n"
  "  'league.details': 'League details',\n"
  "  'league.noSelection': 'No league selected.',\n"
  "  'league.activeCompetitions': 'Active competitions',\n"
  "  'league.registrationCompetitions': 'Competitions in registration',\n"
  "  'league.finishedCompetitions': 'Finished competitions',\n"
  "  'league.unknownCompetitions': 'Unknown competitions',\n"
  "  'league.lastMatch': 'Last match',\n"
  "  'competition.league': 'League: {name}',\n"
  "  'competition.details': 'Competition details',\n"
  "  'competition.created': 'Created',\n"
  "  'competition.format': 'Format',\n"
  "  'competition.progress': 'Progress',\n"
  "  'competition.timeSettings': 'Time settings',\n"
  "  'competition.turnMinutes': 'Turn: {minutes}m',\n"
  "  'competition.bonusMinutes': 'Bonus: {minutes}m',\n"
  "  'competition.export': 'Export',\n"
  "  'competition.statistics': 'Statistics',\n"
  "  'competition.round': 'Round {round}',\n"
  "  'competition.ofRounds': 'of {total}',\n"
  "  'competition.playedMatches': '{count, plural, one {# played match} other {# played matches}}',\n"
  "  'competition.notValidated': '{count, plural, one {# not yet validated} other {# not yet validated}}',\n"
  "  'competition.finishedMatches': 'Finished {finished}{total, select, none {} other { out of {total}}} matches{validation}',\n"
  "  'team.coach': 'Coach: {name}',\n"
  "  'team.details': 'Team details',\n"
  "  'team.race': 'Race',\n"
  "  'team.rerolls': 'Rerolls',\n"
  "  'team.dedicatedFans': 'Dedicated fans',\n"
  "  'team.cheerleaders': 'Cheerleaders',\n"
  "  'team.assistantCoaches': 'Assistant coaches',\n"
  "  'team.apothecary': 'Apothecary',\n"
  "  'team.cash': 'Cash',\n"
  "  'statistics.title': 'Statistics',\n"
  "  'statistics.season': 'Season',\n"
  "  'statistics.marathon': 'Marathon',\n"
  "  'statistics.coachVersus': 'Coach versus',\n"
  "  'statistics.seasonNumber': 'Season {number}',\n"
  "  'statistics.myTeamsOnly': 'My teams only',\n"
  "  'statistics.myPlayersOnly': 'My players only',\n"
  "  'statistics.allEditions': 'All editions',\n"
  "  'statistics.mergeTeams': 'Merge teams with the same name',\n"
  "  'statistics.comparableOnly': 'Only player statistics comparable across rulesets are shown.',\n"
  "  'statistics.page': 'Page {page} of {pages}',\n"
  "  'statistics.noClaimedCoaches': 'No coaches claimed yet. Claim your BB1/BB2/BB3 coach names under Account.',\n"
  "  'statistics.noData': 'No statistics are available for this selection.',\n"
  "  'statistics.noOpponents': 'No opponents found for the mapped coach IDs.',\n"
  "  'statistics.myPlayer': 'My player',\n"
  "  'statistics.myTeam': 'My team',\n"
  "  'statistics.teamCoach': 'Team / coach',\n"
  "  'statistics.teamName': 'Team name',\n"
  "  'statistics.coachName': 'Coach name',\n"
  "  'matches.latest': 'Latest matches',\n"
  "  'matches.live': 'Live matches',\n"
  "  'matches.nonePlayed': 'No matches played yet.',\n"
  "  'matches.noneLive': 'No matches are live currently.',\n"
  "  'matches.lastWas': 'Last match was {date}',\n"
  "  'coachPage.title': 'Coach page',\n"
  "  'coachPage.authenticatedAs': 'Authenticated as {name}',\n"
  "  'coachPage.devUser': 'No real user in the development environment.',\n"
  "  'coachPage.readPermission': 'User read permission',\n"
  "  'coachPage.registerLeaguePermission': 'Permission to register leagues',\n"
  "  'coachPage.leagueAdminPermission': 'League admin permission',\n"
  "  'coachPage.siteAdminPermission': 'Site admin permission',\n"
  "  'articleEditor.title': 'Title',\n"
  "  'articleEditor.slug': 'Slug (optional)',\n"
  "  'articleEditor.excerpt': 'Excerpt',\n"
  "  'articleEditor.coverImage': 'Cover image URL',\n"
  "  'articleEditor.leagueSystem': 'LeagueSystem ID (empty = site-wide)',\n"
  "  'articleEditor.season': 'Season ID',\n"
  "  'articleEditor.status': 'Status',\n"
  "  'articleEditor.draft': 'Draft',\n"
  "  'articleEditor.published': 'Published',\n"
  "  'articleEditor.archived': 'Archived',\n"
  "  'articleEditor.featured': 'Featured',\n"
  "  'articleEditor.save': 'Save article',\n"
  "  'disclaimer.heading': 'Disclaimer',\n"
  "  'disclaimer.unofficial': 'This site is completely unofficial and not affiliated with Cyanide, Nacon, Slitherine or Games Workshop.',\n"
  "  'disclaimer.trademarks': 'Blood Bowl, BB3 and probably many more names are trademarks of their respective owners. Used without permission. No challenge "
  "to their status intended.',\n"
  "  'disclaimer.basedOn': 'This work is based heavily on Warp-Scores by Naytsyrhc.',\n"
  "  'disclaimer.maintainedBy': 'Page maintained by {name}.',\n"
  "  'disclaimer.checkTerms': 'Please also check Terms and Privacy Policy.',\n"
  "  'common.loading': 'Loading…',\n"
  "  'common.save': 'Save',\n"),
 ('frontend/src/i18n/messages.js',
  "  'localizationAdmin.saved': 'Standardspråket har sparats.',\n"
  "  'localizationAdmin.saveError': 'Det gick inte att spara standardspråket.',\n"
  "  'common.loading': 'Laddar…',\n"
  "  'common.save': 'Spara',\n",
  "  'localizationAdmin.saved': 'Standardspråket har sparats.',\n"
  "  'localizationAdmin.saveError': 'Det gick inte att spara standardspråket.',\n"
  "  'home.tagline': 'Blödareblaskans omutliga(?) resultatförmedlingstjänst',\n"
  "  'common.unknown': 'Okänd',\n"
  "  'common.unknownPlayer': 'Okänd spelare',\n"
  "  'common.unknownTeam': 'Okänt lag',\n"
  "  'common.previous': 'Föregående',\n"
  "  'common.next': 'Nästa',\n"
  "  'common.cancel': 'Avbryt',\n"
  "  'common.continue': 'Fortsätt',\n"
  "  'common.date': 'Datum',\n"
  "  'common.competition': 'Tävling',\n"
  "  'common.home': 'Hemma',\n"
  "  'common.away': 'Borta',\n"
  "  'common.result': 'Resultat',\n"
  "  'common.rank': 'Placering',\n"
  "  'common.player': 'Spelare',\n"
  "  'common.team': 'Lag',\n"
  "  'common.coach': 'Coach',\n"
  "  'common.position': 'Position',\n"
  "  'common.games': 'Matcher',\n"
  "  'common.skills': 'Färdigheter',\n"
  "  'common.score': 'Poäng',\n"
  "  'common.name': 'Namn',\n"
  "  'common.type': 'Typ',\n"
  "  'common.level': 'Nivå',\n"
  "  'common.injuries': 'Skador',\n"
  "  'common.value': 'Värde',\n"
  "  'common.players': 'Spelare',\n"
  "  'common.teams': 'Lag',\n"
  "  'common.matches': 'Matcher',\n"
  "  'menu.lastCheck': 'Senaste kontroll:',\n"
  "  'menu.outdated': 'Inaktuellt',\n"
  "  'menu.cyanideAdminTools': 'Cyanides adminverktyg',\n"
  "  'league.details': 'Ligadetaljer',\n"
  "  'league.noSelection': 'Ingen liga vald.',\n"
  "  'league.activeCompetitions': 'Aktiva tävlingar',\n"
  "  'league.registrationCompetitions': 'Tävlingar med öppen registrering',\n"
  "  'league.finishedCompetitions': 'Avslutade tävlingar',\n"
  "  'league.unknownCompetitions': 'Tävlingar med okänd status',\n"
  "  'league.lastMatch': 'Senaste match',\n"
  "  'competition.league': 'Liga: {name}',\n"
  "  'competition.details': 'Tävlingsdetaljer',\n"
  "  'competition.created': 'Skapad',\n"
  "  'competition.format': 'Format',\n"
  "  'competition.progress': 'Förlopp',\n"
  "  'competition.timeSettings': 'Tidsinställningar',\n"
  "  'competition.turnMinutes': 'Tur: {minutes} min',\n"
  "  'competition.bonusMinutes': 'Bonus: {minutes} min',\n"
  "  'competition.export': 'Export',\n"
  "  'competition.statistics': 'Statistik',\n"
  "  'competition.round': 'Omgång {round}',\n"
  "  'competition.ofRounds': 'av {total}',\n"
  "  'competition.playedMatches': '{count, plural, one {# spelad match} other {# spelade matcher}}',\n"
  "  'competition.notValidated': '{count, plural, one {# ännu inte validerad} other {# ännu inte validerade}}',\n"
  "  'competition.finishedMatches': 'Avslutade {finished}{total, select, none {} other { av {total}}} matcher{validation}',\n"
  "  'team.coach': 'Coach: {name}',\n"
  "  'team.details': 'Lagdetaljer',\n"
  "  'team.race': 'Ras',\n"
  "  'team.rerolls': 'Rerolls',\n"
  "  'team.dedicatedFans': 'Hängivna fans',\n"
  "  'team.cheerleaders': 'Cheerleaders',\n"
  "  'team.assistantCoaches': 'Assisterande coacher',\n"
  "  'team.apothecary': 'Apotekare',\n"
  "  'team.cash': 'Kassa',\n"
  "  'statistics.title': 'Statistik',\n"
  "  'statistics.season': 'Säsong',\n"
  "  'statistics.marathon': 'Maraton',\n"
  "  'statistics.coachVersus': 'Coach mot coach',\n"
  "  'statistics.seasonNumber': 'Säsong {number}',\n"
  "  'statistics.myTeamsOnly': 'Endast mina lag',\n"
  "  'statistics.myPlayersOnly': 'Endast mina spelare',\n"
  "  'statistics.allEditions': 'Alla utgåvor',\n"
  "  'statistics.mergeTeams': 'Slå ihop lag med samma namn',\n"
  "  'statistics.comparableOnly': 'Endast spelarstatistik som är jämförbar mellan regelverk visas.',\n"
  "  'statistics.page': 'Sida {page} av {pages}',\n"
  "  'statistics.noClaimedCoaches': 'Inga coacher är kopplade ännu. Koppla dina BB1/BB2/BB3-coachnamn under Konto.',\n"
  "  'statistics.noData': 'Ingen statistik finns för det här urvalet.',\n"
  "  'statistics.noOpponents': 'Inga motståndare hittades för de kopplade coach-ID:na.',\n"
  "  'statistics.myPlayer': 'Min spelare',\n"
  "  'statistics.myTeam': 'Mitt lag',\n"
  "  'statistics.teamCoach': 'Lag / coach',\n"
  "  'statistics.teamName': 'Lagnamn',\n"
  "  'statistics.coachName': 'Coachnamn',\n"
  "  'matches.latest': 'Senaste matcher',\n"
  "  'matches.live': 'Livematcher',\n"
  "  'matches.nonePlayed': 'Inga matcher spelade ännu.',\n"
  "  'matches.noneLive': 'Inga matcher pågår just nu.',\n"
  "  'matches.lastWas': 'Senaste matchen var {date}',\n"
  "  'coachPage.title': 'Coach-sida',\n"
  "  'coachPage.authenticatedAs': 'Inloggad som {name}',\n"
  "  'coachPage.devUser': 'Ingen riktig användare i utvecklingsmiljön.',\n"
  "  'coachPage.readPermission': 'Behörighet att läsa användardata',\n"
  "  'coachPage.registerLeaguePermission': 'Behörighet att registrera ligor',\n"
  "  'coachPage.leagueAdminPermission': 'Ligaadminbehörighet',\n"
  "  'coachPage.siteAdminPermission': 'Siteadminbehörighet',\n"
  "  'articleEditor.title': 'Rubrik',\n"
  "  'articleEditor.slug': 'Slug (valfri)',\n"
  "  'articleEditor.excerpt': 'Ingress',\n"
  "  'articleEditor.coverImage': 'URL till omslagsbild',\n"
  "  'articleEditor.leagueSystem': 'LeagueSystem-ID (tomt = hela sajten)',\n"
  "  'articleEditor.season': 'Säsongs-ID',\n"
  "  'articleEditor.status': 'Status',\n"
  "  'articleEditor.draft': 'Utkast',\n"
  "  'articleEditor.published': 'Publicerad',\n"
  "  'articleEditor.archived': 'Arkiverad',\n"
  "  'articleEditor.featured': 'Utvald',\n"
  "  'articleEditor.save': 'Spara artikel',\n"
  "  'disclaimer.heading': 'Ansvarsfriskrivning',\n"
  "  'disclaimer.unofficial': 'Den här sajten är helt inofficiell och är inte knuten till Cyanide, Nacon, Slitherine eller Games Workshop.',\n"
  "  'disclaimer.trademarks': 'Blood Bowl, BB3 och sannolikt många fler namn är varumärken som tillhör respektive ägare. Används utan tillstånd och utan "
  "avsikt att ifrågasätta deras status.',\n"
  "  'disclaimer.basedOn': 'Det här arbetet bygger i hög grad på Warp-Scores av Naytsyrhc.',\n"
  "  'disclaimer.maintainedBy': 'Sidan underhålls av {name}.',\n"
  "  'disclaimer.checkTerms': 'Läs även Villkor och Integritetspolicy.',\n"
  "  'common.loading': 'Laddar…',\n"
  "  'common.save': 'Spara',\n"),
 ('frontend/src/i18n/messages.js',
  "  'localizationAdmin.saved': 'Idioma predeterminado guardado.',\n"
  "  'localizationAdmin.saveError': 'No se pudo guardar el idioma predeterminado.',\n"
  "  'common.loading': 'Cargando…',\n"
  "  'common.save': 'Guardar',\n",
  "  'localizationAdmin.saved': 'Idioma predeterminado guardado.',\n"
  "  'localizationAdmin.saveError': 'No se pudo guardar el idioma predeterminado.',\n"
  "  'home.tagline': 'El servicio de resultados incorruptible(?) de Blödareblaskan',\n"
  "  'common.unknown': 'Desconocido',\n"
  "  'common.unknownPlayer': 'Jugador desconocido',\n"
  "  'common.unknownTeam': 'Equipo desconocido',\n"
  "  'common.previous': 'Anterior',\n"
  "  'common.next': 'Siguiente',\n"
  "  'common.cancel': 'Cancelar',\n"
  "  'common.continue': 'Continuar',\n"
  "  'common.date': 'Fecha',\n"
  "  'common.competition': 'Competición',\n"
  "  'common.home': 'Local',\n"
  "  'common.away': 'Visitante',\n"
  "  'common.result': 'Resultado',\n"
  "  'common.rank': 'Posición',\n"
  "  'common.player': 'Jugador',\n"
  "  'common.team': 'Equipo',\n"
  "  'common.coach': 'Entrenador',\n"
  "  'common.position': 'Posición',\n"
  "  'common.games': 'Partidos',\n"
  "  'common.skills': 'Habilidades',\n"
  "  'common.score': 'Puntuación',\n"
  "  'common.name': 'Nombre',\n"
  "  'common.type': 'Tipo',\n"
  "  'common.level': 'Nivel',\n"
  "  'common.injuries': 'Lesiones',\n"
  "  'common.value': 'Valor',\n"
  "  'common.players': 'Jugadores',\n"
  "  'common.teams': 'Equipos',\n"
  "  'common.matches': 'Partidos',\n"
  "  'menu.lastCheck': 'Última comprobación:',\n"
  "  'menu.outdated': 'Desactualizado',\n"
  "  'menu.cyanideAdminTools': 'Herramientas de administración de Cyanide',\n"
  "  'league.details': 'Detalles de la liga',\n"
  "  'league.noSelection': 'No se ha seleccionado ninguna liga.',\n"
  "  'league.activeCompetitions': 'Competiciones activas',\n"
  "  'league.registrationCompetitions': 'Competiciones en inscripción',\n"
  "  'league.finishedCompetitions': 'Competiciones finalizadas',\n"
  "  'league.unknownCompetitions': 'Competiciones con estado desconocido',\n"
  "  'league.lastMatch': 'Último partido',\n"
  "  'competition.league': 'Liga: {name}',\n"
  "  'competition.details': 'Detalles de la competición',\n"
  "  'competition.created': 'Creada',\n"
  "  'competition.format': 'Formato',\n"
  "  'competition.progress': 'Progreso',\n"
  "  'competition.timeSettings': 'Ajustes de tiempo',\n"
  "  'competition.turnMinutes': 'Turno: {minutes} min',\n"
  "  'competition.bonusMinutes': 'Bono: {minutes} min',\n"
  "  'competition.export': 'Exportar',\n"
  "  'competition.statistics': 'Estadísticas',\n"
  "  'competition.round': 'Ronda {round}',\n"
  "  'competition.ofRounds': 'de {total}',\n"
  "  'competition.playedMatches': '{count, plural, one {# partido jugado} other {# partidos jugados}}',\n"
  "  'competition.notValidated': '{count, plural, one {# aún sin validar} other {# aún sin validar}}',\n"
  "  'competition.finishedMatches': 'Finalizados {finished}{total, select, none {} other { de {total}}} partidos{validation}',\n"
  "  'team.coach': 'Entrenador: {name}',\n"
  "  'team.details': 'Detalles del equipo',\n"
  "  'team.race': 'Raza',\n"
  "  'team.rerolls': 'Segundas oportunidades',\n"
  "  'team.dedicatedFans': 'Aficionados fieles',\n"
  "  'team.cheerleaders': 'Animadoras',\n"
  "  'team.assistantCoaches': 'Entrenadores asistentes',\n"
  "  'team.apothecary': 'Apotecario',\n"
  "  'team.cash': 'Tesorería',\n"
  "  'statistics.title': 'Estadísticas',\n"
  "  'statistics.season': 'Temporada',\n"
  "  'statistics.marathon': 'Maratón',\n"
  "  'statistics.coachVersus': 'Entrenador contra',\n"
  "  'statistics.seasonNumber': 'Temporada {number}',\n"
  "  'statistics.myTeamsOnly': 'Solo mis equipos',\n"
  "  'statistics.myPlayersOnly': 'Solo mis jugadores',\n"
  "  'statistics.allEditions': 'Todas las ediciones',\n"
  "  'statistics.mergeTeams': 'Combinar equipos con el mismo nombre',\n"
  "  'statistics.comparableOnly': 'Solo se muestran estadísticas de jugadores comparables entre reglamentos.',\n"
  "  'statistics.page': 'Página {page} de {pages}',\n"
  "  'statistics.noClaimedCoaches': 'Aún no has reclamado entrenadores. Reclama tus nombres de BB1/BB2/BB3 en Cuenta.',\n"
  "  'statistics.noData': 'No hay estadísticas disponibles para esta selección.',\n"
  "  'statistics.noOpponents': 'No se encontraron rivales para los identificadores de entrenador asociados.',\n"
  "  'statistics.myPlayer': 'Mi jugador',\n"
  "  'statistics.myTeam': 'Mi equipo',\n"
  "  'statistics.teamCoach': 'Equipo / entrenador',\n"
  "  'statistics.teamName': 'Nombre del equipo',\n"
  "  'statistics.coachName': 'Nombre del entrenador',\n"
  "  'matches.latest': 'Últimos partidos',\n"
  "  'matches.live': 'Partidos en directo',\n"
  "  'matches.nonePlayed': 'Todavía no se han jugado partidos.',\n"
  "  'matches.noneLive': 'No hay partidos en directo ahora mismo.',\n"
  "  'matches.lastWas': 'El último partido fue {date}',\n"
  "  'coachPage.title': 'Página del entrenador',\n"
  "  'coachPage.authenticatedAs': 'Sesión iniciada como {name}',\n"
  "  'coachPage.devUser': 'No hay un usuario real en el entorno de desarrollo.',\n"
  "  'coachPage.readPermission': 'Permiso de lectura del usuario',\n"
  "  'coachPage.registerLeaguePermission': 'Permiso para registrar ligas',\n"
  "  'coachPage.leagueAdminPermission': 'Permiso de administrador de liga',\n"
  "  'coachPage.siteAdminPermission': 'Permiso de administrador del sitio',\n"
  "  'articleEditor.title': 'Título',\n"
  "  'articleEditor.slug': 'Slug (opcional)',\n"
  "  'articleEditor.excerpt': 'Entradilla',\n"
  "  'articleEditor.coverImage': 'URL de imagen de portada',\n"
  "  'articleEditor.leagueSystem': 'ID de LeagueSystem (vacío = todo el sitio)',\n"
  "  'articleEditor.season': 'ID de temporada',\n"
  "  'articleEditor.status': 'Estado',\n"
  "  'articleEditor.draft': 'Borrador',\n"
  "  'articleEditor.published': 'Publicado',\n"
  "  'articleEditor.archived': 'Archivado',\n"
  "  'articleEditor.featured': 'Destacado',\n"
  "  'articleEditor.save': 'Guardar artículo',\n"
  "  'disclaimer.heading': 'Aviso legal',\n"
  "  'disclaimer.unofficial': 'Este sitio es completamente extraoficial y no está afiliado con Cyanide, Nacon, Slitherine ni Games Workshop.',\n"
  "  'disclaimer.trademarks': 'Blood Bowl, BB3 y probablemente muchos más nombres son marcas de sus respectivos propietarios. Se usan sin permiso y sin "
  "intención de cuestionar su condición.',\n"
  "  'disclaimer.basedOn': 'Este trabajo se basa en gran medida en Warp-Scores de Naytsyrhc.',\n"
  "  'disclaimer.maintainedBy': 'Página mantenida por {name}.',\n"
  "  'disclaimer.checkTerms': 'Consulta también los Términos y la Política de privacidad.',\n"
  "  'common.loading': 'Cargando…',\n"
  "  'common.save': 'Guardar',\n"),
 ('frontend/src/i18n/messages.js',
  "  'localizationAdmin.saved': 'Oletuskieli tallennettu.',\n"
  "  'localizationAdmin.saveError': 'Oletuskieltä ei voitu tallentaa.',\n"
  "  'common.loading': 'Ladataan…',\n"
  "  'common.save': 'Tallenna',\n",
  "  'localizationAdmin.saved': 'Oletuskieli tallennettu.',\n"
  "  'localizationAdmin.saveError': 'Oletuskieltä ei voitu tallentaa.',\n"
  "  'home.tagline': 'Blödareblaskanin lahjomaton(?) tulospalvelu',\n"
  "  'common.unknown': 'Tuntematon',\n"
  "  'common.unknownPlayer': 'Tuntematon pelaaja',\n"
  "  'common.unknownTeam': 'Tuntematon joukkue',\n"
  "  'common.previous': 'Edellinen',\n"
  "  'common.next': 'Seuraava',\n"
  "  'common.cancel': 'Peruuta',\n"
  "  'common.continue': 'Jatka',\n"
  "  'common.date': 'Päivä',\n"
  "  'common.competition': 'Kilpailu',\n"
  "  'common.home': 'Koti',\n"
  "  'common.away': 'Vieras',\n"
  "  'common.result': 'Tulos',\n"
  "  'common.rank': 'Sijoitus',\n"
  "  'common.player': 'Pelaaja',\n"
  "  'common.team': 'Joukkue',\n"
  "  'common.coach': 'Valmentaja',\n"
  "  'common.position': 'Pelipaikka',\n"
  "  'common.games': 'Ottelut',\n"
  "  'common.skills': 'Taidot',\n"
  "  'common.score': 'Pisteet',\n"
  "  'common.name': 'Nimi',\n"
  "  'common.type': 'Tyyppi',\n"
  "  'common.level': 'Taso',\n"
  "  'common.injuries': 'Vammat',\n"
  "  'common.value': 'Arvo',\n"
  "  'common.players': 'Pelaajat',\n"
  "  'common.teams': 'Joukkueet',\n"
  "  'common.matches': 'Ottelut',\n"
  "  'menu.lastCheck': 'Viimeisin tarkistus:',\n"
  "  'menu.outdated': 'Vanhentunut',\n"
  "  'menu.cyanideAdminTools': 'Cyaniden ylläpitotyökalut',\n"
  "  'league.details': 'Liigan tiedot',\n"
  "  'league.noSelection': 'Liigaa ei ole valittu.',\n"
  "  'league.activeCompetitions': 'Aktiiviset kilpailut',\n"
  "  'league.registrationCompetitions': 'Ilmoittautumisvaiheessa olevat kilpailut',\n"
  "  'league.finishedCompetitions': 'Päättyneet kilpailut',\n"
  "  'league.unknownCompetitions': 'Tuntemattoman tilan kilpailut',\n"
  "  'league.lastMatch': 'Viimeisin ottelu',\n"
  "  'competition.league': 'Liiga: {name}',\n"
  "  'competition.details': 'Kilpailun tiedot',\n"
  "  'competition.created': 'Luotu',\n"
  "  'competition.format': 'Muoto',\n"
  "  'competition.progress': 'Eteneminen',\n"
  "  'competition.timeSettings': 'Aika-asetukset',\n"
  "  'competition.turnMinutes': 'Vuoro: {minutes} min',\n"
  "  'competition.bonusMinutes': 'Bonus: {minutes} min',\n"
  "  'competition.export': 'Vie',\n"
  "  'competition.statistics': 'Tilastot',\n"
  "  'competition.round': 'Kierros {round}',\n"
  "  'competition.ofRounds': '/ {total}',\n"
  "  'competition.playedMatches': '{count, plural, one {# pelattu ottelu} other {# pelattua ottelua}}',\n"
  "  'competition.notValidated': '{count, plural, one {# ei vielä vahvistettu} other {# ei vielä vahvistettu}}',\n"
  "  'competition.finishedMatches': 'Päättyneitä {finished}{total, select, none {} other { / {total}}} ottelua{validation}',\n"
  "  'team.coach': 'Valmentaja: {name}',\n"
  "  'team.details': 'Joukkueen tiedot',\n"
  "  'team.race': 'Rotu',\n"
  "  'team.rerolls': 'Uusintaheitot',\n"
  "  'team.dedicatedFans': 'Uskolliset fanit',\n"
  "  'team.cheerleaders': 'Kannustajat',\n"
  "  'team.assistantCoaches': 'Apuvalmentajat',\n"
  "  'team.apothecary': 'Apteekkari',\n"
  "  'team.cash': 'Kassa',\n"
  "  'statistics.title': 'Tilastot',\n"
  "  'statistics.season': 'Kausi',\n"
  "  'statistics.marathon': 'Maraton',\n"
  "  'statistics.coachVersus': 'Valmentaja vastaan',\n"
  "  'statistics.seasonNumber': 'Kausi {number}',\n"
  "  'statistics.myTeamsOnly': 'Vain omat joukkueeni',\n"
  "  'statistics.myPlayersOnly': 'Vain omat pelaajani',\n"
  "  'statistics.allEditions': 'Kaikki versiot',\n"
  "  'statistics.mergeTeams': 'Yhdistä samannimiset joukkueet',\n"
  "  'statistics.comparableOnly': 'Vain sääntöversioiden välillä vertailukelpoinen pelaajatilasto näytetään.',\n"
  "  'statistics.page': 'Sivu {page} / {pages}',\n"
  "  'statistics.noClaimedCoaches': 'Valmentajia ei ole vielä liitetty. Liitä BB1/BB2/BB3-valmentajanimet Tili-sivulla.',\n"
  "  'statistics.noData': 'Tälle valinnalle ei ole tilastoja.',\n"
  "  'statistics.noOpponents': 'Liitetyille valmentajatunnuksille ei löytynyt vastustajia.',\n"
  "  'statistics.myPlayer': 'Oma pelaaja',\n"
  "  'statistics.myTeam': 'Oma joukkue',\n"
  "  'statistics.teamCoach': 'Joukkue / valmentaja',\n"
  "  'statistics.teamName': 'Joukkueen nimi',\n"
  "  'statistics.coachName': 'Valmentajan nimi',\n"
  "  'matches.latest': 'Viimeisimmät ottelut',\n"
  "  'matches.live': 'Live-ottelut',\n"
  "  'matches.nonePlayed': 'Otteluita ei ole vielä pelattu.',\n"
  "  'matches.noneLive': 'Yhtään ottelua ei ole juuri nyt käynnissä.',\n"
  "  'matches.lastWas': 'Viimeisin ottelu oli {date}',\n"
  "  'coachPage.title': 'Valmentajasivu',\n"
  "  'coachPage.authenticatedAs': 'Kirjautuneena käyttäjänä {name}',\n"
  "  'coachPage.devUser': 'Kehitysympäristössä ei ole oikeaa käyttäjää.',\n"
  "  'coachPage.readPermission': 'Käyttäjän lukuoikeus',\n"
  "  'coachPage.registerLeaguePermission': 'Oikeus rekisteröidä liigoja',\n"
  "  'coachPage.leagueAdminPermission': 'Liigan ylläpito-oikeus',\n"
  "  'coachPage.siteAdminPermission': 'Sivuston ylläpito-oikeus',\n"
  "  'articleEditor.title': 'Otsikko',\n"
  "  'articleEditor.slug': 'Slug (valinnainen)',\n"
  "  'articleEditor.excerpt': 'Ingressi',\n"
  "  'articleEditor.coverImage': 'Kansikuvan URL',\n"
  "  'articleEditor.leagueSystem': 'LeagueSystem-tunnus (tyhjä = koko sivusto)',\n"
  "  'articleEditor.season': 'Kauden tunnus',\n"
  "  'articleEditor.status': 'Tila',\n"
  "  'articleEditor.draft': 'Luonnos',\n"
  "  'articleEditor.published': 'Julkaistu',\n"
  "  'articleEditor.archived': 'Arkistoitu',\n"
  "  'articleEditor.featured': 'Nostettu',\n"
  "  'articleEditor.save': 'Tallenna artikkeli',\n"
  "  'disclaimer.heading': 'Vastuuvapauslauseke',\n"
  "  'disclaimer.unofficial': 'Tämä sivusto on täysin epävirallinen eikä sillä ole yhteyttä Cyanideen, Naconiin, Slitherineen tai Games Workshopiin.',\n"
  "  'disclaimer.trademarks': 'Blood Bowl, BB3 ja todennäköisesti monet muut nimet ovat omistajiensa tavaramerkkejä. Niitä käytetään ilman lupaa eikä niiden "
  "asemaa ole tarkoitus kyseenalaistaa.',\n"
  "  'disclaimer.basedOn': 'Tämä työ perustuu vahvasti Naytsyrhcin Warp-Scores-palveluun.',\n"
  "  'disclaimer.maintainedBy': 'Sivua ylläpitää {name}.',\n"
  "  'disclaimer.checkTerms': 'Tutustu myös käyttöehtoihin ja tietosuojakäytäntöön.',\n"
  "  'common.loading': 'Ladataan…',\n"
  "  'common.save': 'Tallenna',\n"),
 ('frontend/src/i18n/messages.js',
  "  'localizationAdmin.saved': 'Domyślny język został zapisany.',\n"
  "  'localizationAdmin.saveError': 'Nie udało się zapisać domyślnego języka.',\n"
  "  'common.loading': 'Ładowanie…',\n"
  "  'common.save': 'Zapisz',\n",
  "  'localizationAdmin.saved': 'Domyślny język został zapisany.',\n"
  "  'localizationAdmin.saveError': 'Nie udało się zapisać domyślnego języka.',\n"
  "  'home.tagline': 'Nieprzekupny(?) serwis wyników Blödareblaskan',\n"
  "  'common.unknown': 'Nieznane',\n"
  "  'common.unknownPlayer': 'Nieznany zawodnik',\n"
  "  'common.unknownTeam': 'Nieznana drużyna',\n"
  "  'common.previous': 'Poprzednia',\n"
  "  'common.next': 'Następna',\n"
  "  'common.cancel': 'Anuluj',\n"
  "  'common.continue': 'Kontynuuj',\n"
  "  'common.date': 'Data',\n"
  "  'common.competition': 'Rozgrywki',\n"
  "  'common.home': 'Gospodarz',\n"
  "  'common.away': 'Gość',\n"
  "  'common.result': 'Wynik',\n"
  "  'common.rank': 'Miejsce',\n"
  "  'common.player': 'Zawodnik',\n"
  "  'common.team': 'Drużyna',\n"
  "  'common.coach': 'Trener',\n"
  "  'common.position': 'Pozycja',\n"
  "  'common.games': 'Mecze',\n"
  "  'common.skills': 'Umiejętności',\n"
  "  'common.score': 'Punkty',\n"
  "  'common.name': 'Nazwa',\n"
  "  'common.type': 'Typ',\n"
  "  'common.level': 'Poziom',\n"
  "  'common.injuries': 'Kontuzje',\n"
  "  'common.value': 'Wartość',\n"
  "  'common.players': 'Zawodnicy',\n"
  "  'common.teams': 'Drużyny',\n"
  "  'common.matches': 'Mecze',\n"
  "  'menu.lastCheck': 'Ostatnie sprawdzenie:',\n"
  "  'menu.outdated': 'Nieaktualne',\n"
  "  'menu.cyanideAdminTools': 'Narzędzia administracyjne Cyanide',\n"
  "  'league.details': 'Szczegóły ligi',\n"
  "  'league.noSelection': 'Nie wybrano ligi.',\n"
  "  'league.activeCompetitions': 'Aktywne rozgrywki',\n"
  "  'league.registrationCompetitions': 'Rozgrywki w fazie rejestracji',\n"
  "  'league.finishedCompetitions': 'Zakończone rozgrywki',\n"
  "  'league.unknownCompetitions': 'Rozgrywki o nieznanym statusie',\n"
  "  'league.lastMatch': 'Ostatni mecz',\n"
  "  'competition.league': 'Liga: {name}',\n"
  "  'competition.details': 'Szczegóły rozgrywek',\n"
  "  'competition.created': 'Utworzono',\n"
  "  'competition.format': 'Format',\n"
  "  'competition.progress': 'Postęp',\n"
  "  'competition.timeSettings': 'Ustawienia czasu',\n"
  "  'competition.turnMinutes': 'Tura: {minutes} min',\n"
  "  'competition.bonusMinutes': 'Bonus: {minutes} min',\n"
  "  'competition.export': 'Eksport',\n"
  "  'competition.statistics': 'Statystyki',\n"
  "  'competition.round': 'Runda {round}',\n"
  "  'competition.ofRounds': 'z {total}',\n"
  "  'competition.playedMatches': '{count, plural, one {# rozegrany mecz} few {# rozegrane mecze} other {# rozegranych meczów}}',\n"
  "  'competition.notValidated': '{count, plural, one {# jeszcze niezweryfikowany} few {# jeszcze niezweryfikowane} other {# jeszcze niezweryfikowanych}}',\n"
  "  'competition.finishedMatches': 'Zakończono {finished}{total, select, none {} other { z {total}}} meczów{validation}',\n"
  "  'team.coach': 'Trener: {name}',\n"
  "  'team.details': 'Szczegóły drużyny',\n"
  "  'team.race': 'Rasa',\n"
  "  'team.rerolls': 'Przerzuty',\n"
  "  'team.dedicatedFans': 'Oddani kibice',\n"
  "  'team.cheerleaders': 'Cheerleaderki',\n"
  "  'team.assistantCoaches': 'Asystenci trenera',\n"
  "  'team.apothecary': 'Aptekarz',\n"
  "  'team.cash': 'Kasa',\n"
  "  'statistics.title': 'Statystyki',\n"
  "  'statistics.season': 'Sezon',\n"
  "  'statistics.marathon': 'Maraton',\n"
  "  'statistics.coachVersus': 'Trener kontra',\n"
  "  'statistics.seasonNumber': 'Sezon {number}',\n"
  "  'statistics.myTeamsOnly': 'Tylko moje drużyny',\n"
  "  'statistics.myPlayersOnly': 'Tylko moi zawodnicy',\n"
  "  'statistics.allEditions': 'Wszystkie edycje',\n"
  "  'statistics.mergeTeams': 'Połącz drużyny o tej samej nazwie',\n"
  "  'statistics.comparableOnly': 'Pokazywane są tylko statystyki zawodników porównywalne między zestawami zasad.',\n"
  "  'statistics.page': 'Strona {page} z {pages}',\n"
  "  'statistics.noClaimedCoaches': 'Nie przypisano jeszcze trenerów. Przypisz swoje nazwy trenerów BB1/BB2/BB3 na stronie Konto.',\n"
  "  'statistics.noData': 'Brak statystyk dla tego wyboru.',\n"
  "  'statistics.noOpponents': 'Nie znaleziono przeciwników dla przypisanych identyfikatorów trenerów.',\n"
  "  'statistics.myPlayer': 'Mój zawodnik',\n"
  "  'statistics.myTeam': 'Moja drużyna',\n"
  "  'statistics.teamCoach': 'Drużyna / trener',\n"
  "  'statistics.teamName': 'Nazwa drużyny',\n"
  "  'statistics.coachName': 'Nazwa trenera',\n"
  "  'matches.latest': 'Ostatnie mecze',\n"
  "  'matches.live': 'Mecze na żywo',\n"
  "  'matches.nonePlayed': 'Nie rozegrano jeszcze żadnych meczów.',\n"
  "  'matches.noneLive': 'Obecnie żaden mecz nie jest rozgrywany.',\n"
  "  'matches.lastWas': 'Ostatni mecz: {date}',\n"
  "  'coachPage.title': 'Strona trenera',\n"
  "  'coachPage.authenticatedAs': 'Zalogowano jako {name}',\n"
  "  'coachPage.devUser': 'W środowisku deweloperskim nie ma prawdziwego użytkownika.',\n"
  "  'coachPage.readPermission': 'Uprawnienie do odczytu danych użytkownika',\n"
  "  'coachPage.registerLeaguePermission': 'Uprawnienie do rejestrowania lig',\n"
  "  'coachPage.leagueAdminPermission': 'Uprawnienie administratora ligi',\n"
  "  'coachPage.siteAdminPermission': 'Uprawnienie administratora serwisu',\n"
  "  'articleEditor.title': 'Tytuł',\n"
  "  'articleEditor.slug': 'Slug (opcjonalny)',\n"
  "  'articleEditor.excerpt': 'Zajawka',\n"
  "  'articleEditor.coverImage': 'URL obrazu okładkowego',\n"
  "  'articleEditor.leagueSystem': 'ID LeagueSystem (puste = cały serwis)',\n"
  "  'articleEditor.season': 'ID sezonu',\n"
  "  'articleEditor.status': 'Status',\n"
  "  'articleEditor.draft': 'Szkic',\n"
  "  'articleEditor.published': 'Opublikowany',\n"
  "  'articleEditor.archived': 'Zarchiwizowany',\n"
  "  'articleEditor.featured': 'Wyróżniony',\n"
  "  'articleEditor.save': 'Zapisz artykuł',\n"
  "  'disclaimer.heading': 'Zastrzeżenie',\n"
  "  'disclaimer.unofficial': 'Ten serwis jest całkowicie nieoficjalny i nie jest powiązany z Cyanide, Nacon, Slitherine ani Games Workshop.',\n"
  "  'disclaimer.trademarks': 'Blood Bowl, BB3 i prawdopodobnie wiele innych nazw to znaki towarowe ich właścicieli. Używane bez pozwolenia i bez zamiaru "
  "podważania ich statusu.',\n"
  "  'disclaimer.basedOn': 'Ta praca w dużej mierze opiera się na Warp-Scores autorstwa Naytsyrhc.',\n"
  "  'disclaimer.maintainedBy': 'Stronę utrzymuje {name}.',\n"
  "  'disclaimer.checkTerms': 'Zapoznaj się również z Warunkami i Polityką prywatności.',\n"
  "  'common.loading': 'Ładowanie…',\n"
  "  'common.save': 'Zapisz',\n"),
 ('frontend/src/components/misc/Menu.jsx',
  'function LastCheck({ status, textSize, statusOutdated }) {\n  return (\n',
  'function LastCheck({ status, textSize, statusOutdated }) {\n  const intl = useIntl();\n  return (\n'),
 ('frontend/src/components/misc/Menu.jsx', '        <Box>Last check:</Box>\n', "        <Box>{intl.formatMessage({ id: 'menu.lastCheck' })}</Box>\n"),
 ('frontend/src/components/misc/Menu.jsx',
  "            `${formatter.formatAsDate(status.lastCheck, 'unknown')}`\n",
  "            `${formatter.formatAsDate(status.lastCheck, intl.formatMessage({ id: 'common.unknown' }))}`\n"),
 ('frontend/src/components/misc/Menu.jsx',
  '            <DelayedIconTooltip label="Outdated" placement="left-start" shouldWrapChildren>\n',
  '            <DelayedIconTooltip label={intl.formatMessage({ id: \'menu.outdated\' })} placement="left-start" shouldWrapChildren>\n'),
 ('frontend/src/components/misc/Menu.jsx',
  '                    Cyanide Admin-Tools <ExternalLinkIcon mx={2} />\n',
  "                    {intl.formatMessage({ id: 'menu.cyanideAdminTools' })} <ExternalLinkIcon mx={2} />\n"),
 ('frontend/src/pages/WarpScores.jsx',
  "import ArticleFeed from '../components/community/ArticleFeed';\n",
  "import ArticleFeed from '../components/community/ArticleFeed';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/WarpScores.jsx', 'function WarpScores() {\n', 'function WarpScores() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/WarpScores.jsx',
  "          mainImageSrc={imageUrls.blaskscoreLogoPng('medium')}\n"
  '          heading="BlaskScore"\n'
  '          subHeading="Blödareblaskans omutliga(?) resultatförmedlingstjänst"\n',
  "          mainImageSrc={imageUrls.blaskscoreLogoPng('medium')}\n"
  '          heading="BlaskScore"\n'
  "          subHeading={intl.formatMessage({ id: 'home.tagline' })}\n"),
 ('frontend/src/pages/LeaguePage.jsx',
  "import LeagueInfo from '../components/league/LeagueInfo';\n",
  "import LeagueInfo from '../components/league/LeagueInfo';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/LeaguePage.jsx', 'function LeaguePage() {\n', 'function LeaguePage() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/LeaguePage.jsx',
  "        setError({ type: 'info', message: 'No League selected.' });\n",
  "        setError({ type: 'info', message: intl.formatMessage({ id: 'league.noSelection' }) });\n"),
 ('frontend/src/pages/LeaguePage.jsx',
  '        <HeaderCard heading={league.name} detailsHeading="League details" mainImageSrc={imageUrls.logo(league.logo,league?.id?.opus)}>\n',
  "        <HeaderCard heading={league.name} detailsHeading={intl.formatMessage({ id: 'league.details' })} "
  'mainImageSrc={imageUrls.logo(league.logo,league?.id?.opus)}>\n'),
 ('frontend/src/components/league/LeagueInfo.jsx',
  "import InfoItem from '../common/InfoItem';\n",
  "import InfoItem from '../common/InfoItem';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/league/LeagueInfo.jsx',
  'function LeagueInfo({ league, competitionCountByStatus }) {\n',
  'function LeagueInfo({ league, competitionCountByStatus }) {\n  const intl = useIntl();\n'),
 ('frontend/src/components/league/LeagueInfo.jsx',
  '        <InfoItem key="teams" label="Teams" info={league?.teamCount} />\n',
  '        <InfoItem key="teams" label={intl.formatMessage({ id: \'common.teams\' })} info={league?.teamCount} />\n'),
 ('frontend/src/components/league/LeagueInfo.jsx',
  '          label="Active Competitions"\n',
  "          label={intl.formatMessage({ id: 'league.activeCompetitions' })}\n"),
 ('frontend/src/components/league/LeagueInfo.jsx',
  '          label="Competitions in registration"\n',
  "          label={intl.formatMessage({ id: 'league.registrationCompetitions' })}\n"),
 ('frontend/src/components/league/LeagueInfo.jsx',
  '          label="Finished competitions"\n',
  "          label={intl.formatMessage({ id: 'league.finishedCompetitions' })}\n"),
 ('frontend/src/components/league/LeagueInfo.jsx',
  '          label="Unknown competitions"\n',
  "          label={intl.formatMessage({ id: 'league.unknownCompetitions' })}\n"),
 ('frontend/src/components/league/LeagueInfo.jsx',
  '        <InfoItem key="lastMatch" label="Last match" info={formatter.formatAsDate(league.dateLastMatch, \'-\')} />\n',
  '        <InfoItem key="lastMatch" label={intl.formatMessage({ id: \'league.lastMatch\' })} info={formatter.formatAsDate(league.dateLastMatch, \'-\')} />\n'),
 ('frontend/src/pages/CompetitionPage.jsx',
  "import LadderCompetition from '../components/competition/LadderCompetition';\n",
  "import LadderCompetition from '../components/competition/LadderCompetition';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/CompetitionPage.jsx', 'function CompetitionPage() {\n', 'function CompetitionPage() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/CompetitionPage.jsx',
  '          subHeading={<RouteLink to={`/${competition?.leagueId}`}>League: {competition?.leagueName}</RouteLink>}\n'
  '          detailsHeading="Competition details"\n',
  "          subHeading={<RouteLink to={`/${competition?.leagueId}`}>{intl.formatMessage({ id: 'competition.league' }, { name: competition?.leagueName "
  '})}</RouteLink>}\n'
  "          detailsHeading={intl.formatMessage({ id: 'competition.details' })}\n"),
 ('frontend/src/pages/CompetitionPage.jsx',
  '            <InfoItem key="Created" label="Created" info={formatter.formatAsDate(competition?.dateCreated)} />\n'
  '            <InfoItem key="Format" label="Format" info={prettyPrint(competition?.format)} />\n',
  '            <InfoItem key="Created" label={intl.formatMessage({ id: \'competition.created\' })} info={formatter.formatAsDate(competition?.dateCreated)} />\n'
  '            <InfoItem key="Format" label={intl.formatMessage({ id: \'competition.format\' })} info={prettyPrint(competition?.format)} />\n'),
 ('frontend/src/pages/CompetitionPage.jsx', '              label="Progress"\n', "              label={intl.formatMessage({ id: 'competition.progress' })}\n"),
 ('frontend/src/pages/CompetitionPage.jsx',
  '            <InfoItem key="Teams" label="Teams" info={formatter.formatAsNumber(getTeamsFor(competition))} />\n',
  '            <InfoItem key="Teams" label={intl.formatMessage({ id: \'common.teams\' })} info={formatter.formatAsNumber(getTeamsFor(competition))} />\n'),
 ('frontend/src/pages/CompetitionPage.jsx',
  '              label="Time settings"\n'
  '              info={`Turn: ${formatter.formatAsNumber((competition?.turnDuration ?? 0) / 60)}m`}\n'
  '              additionalInfo={`Bonus: ${formatter.formatAsNumber((competition?.timeBonusDuration ?? 0) / 60)}m`}\n',
  "              label={intl.formatMessage({ id: 'competition.timeSettings' })}\n"
  "              info={intl.formatMessage({ id: 'competition.turnMinutes' }, { minutes: formatter.formatAsNumber((competition?.turnDuration ?? 0) / 60) })}\n"
  "              additionalInfo={intl.formatMessage({ id: 'competition.bonusMinutes' }, { minutes: formatter.formatAsNumber((competition?.timeBonusDuration ?? "
  '0) / 60) })}\n'),
 ('frontend/src/pages/CompetitionPage.jsx',
  '                  label="Export"\n',
  "                  label={intl.formatMessage({ id: 'competition.export' })}\n"),
 ('frontend/src/pages/CompetitionPage.jsx',
  '            <Button size="xs">Statistics</Button>\n',
  '            <Button size="xs">{intl.formatMessage({ id: \'competition.statistics\' })}</Button>\n'),
 ('frontend/src/components/competition/CompetitionProgress.jsx',
  "import prettyPrint from '../../util/prettyPrint';\n",
  "import prettyPrint from '../../util/prettyPrint';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/competition/CompetitionProgress.jsx', '}) {\n', '}) {\n  const intl = useIntl();\n'),
 ('frontend/src/components/competition/CompetitionProgress.jsx',
  "      return `${playedMatches || 0} played match${playedMatches !== 1 ? 'es' : ''}`;\n",
  "      return intl.formatMessage({ id: 'competition.playedMatches' }, { count: playedMatches || 0 });\n"),
 ('frontend/src/components/competition/CompetitionProgress.jsx',
  '}) {\n'
  "  const currentRoundText = currentRound ? `, Round ${currentRound}` : '';\n"
  "  const totalRoundsText = currentRound && totalRounds ? `of ${totalRounds}` : '';\n",
  '}) {\n'
  '  const intl = useIntl();\n'
  "  const currentRoundText = currentRound ? `, ${intl.formatMessage({ id: 'competition.round' }, { round: currentRound })}` : '';\n"
  "  const totalRoundsText = currentRound && totalRounds ? intl.formatMessage({ id: 'competition.ofRounds' }, { total: totalRounds }) : '';\n"),
 ('frontend/src/components/competition/CompetitionProgress.jsx',
  "  const notYetValidatedMatches = notValidatedMatches > 0 ? ` (${notValidatedMatches} not yet validated)` : '';\n"
  "  const outOfTotalMatchesText = totalMatches ? ` out of ${totalMatches}` : '';\n"
  '  const progressAdditionalText = playedMatches\n'
  '    ? `Finished ${finishedMatches}${outOfTotalMatchesText} matches${notYetValidatedMatches}`\n'
  '    : undefined;\n',
  '  const notYetValidatedMatches = notValidatedMatches > 0\n'
  "    ? ` (${intl.formatMessage({ id: 'competition.notValidated' }, { count: notValidatedMatches })})`\n"
  "    : '';\n"
  '  const progressAdditionalText = playedMatches\n'
  '    ? intl.formatMessage(\n'
  "        { id: 'competition.finishedMatches' },\n"
  "        { finished: finishedMatches, total: totalMatches || 'none', validation: notYetValidatedMatches }\n"
  '      )\n'
  '    : undefined;\n'),
 ('frontend/src/pages/TeamPage.jsx',
  "import { identityUtils } from '../util/identityUtil';\n",
  "import { identityUtils } from '../util/identityUtil';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/TeamPage.jsx', 'function TeamPage() {\n', 'function TeamPage() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/TeamPage.jsx',
  '                subHeading={`Coach: ${team?.coachName}`}\n                detailsHeading="Team details"\n',
  "                subHeading={intl.formatMessage({ id: 'team.coach' }, { name: team?.coachName })}\n"
  "                detailsHeading={intl.formatMessage({ id: 'team.details' })}\n"),
 ('frontend/src/pages/TeamPage.jsx',
  '                  <InfoItem key="race" label="Race" info={prettyPrint(team.race)} />\n'
  '                  <InfoItem key="players" label="Players" info={players !== null ? players.length : \'-\'} />\n'
  '                  <InfoItem key="rerolls" label="Rerolls" info={team.rerolls} />\n'
  '                  <InfoItem key="dedicatedFans" label="Dedicated Fans" info={team.dedicatedFans} />\n'
  '                  <InfoItem key="cheerleaders" label="Cheerleaders" info={team.cheerleaders} />\n'
  '                  <InfoItem key="assistantCoaches" label="Assistant coaches" info={team.coachAssistants} />\n'
  '                  <InfoItem key="apothecary" label="Apothecary" info={team.apothecary} />\n'
  '                  <InfoItem key="cash" label="Cash" info={formatter.formatAsNumber(team.cash)} />\n'
  '                  <InfoItem key="value" label="Value" info={formatter.formatAsNumber(team.value)} />\n'
  '                  <InfoItem key="matches" label="Matches"\n',
  '                  <InfoItem key="race" label={intl.formatMessage({ id: \'team.race\' })} info={prettyPrint(team.race)} />\n'
  '                  <InfoItem key="players" label={intl.formatMessage({ id: \'common.players\' })} info={players !== null ? players.length : \'-\'} />\n'
  '                  <InfoItem key="rerolls" label={intl.formatMessage({ id: \'team.rerolls\' })} info={team.rerolls} />\n'
  '                  <InfoItem key="dedicatedFans" label={intl.formatMessage({ id: \'team.dedicatedFans\' })} info={team.dedicatedFans} />\n'
  '                  <InfoItem key="cheerleaders" label={intl.formatMessage({ id: \'team.cheerleaders\' })} info={team.cheerleaders} />\n'
  '                  <InfoItem key="assistantCoaches" label={intl.formatMessage({ id: \'team.assistantCoaches\' })} info={team.coachAssistants} />\n'
  '                  <InfoItem key="apothecary" label={intl.formatMessage({ id: \'team.apothecary\' })} info={team.apothecary} />\n'
  '                  <InfoItem key="cash" label={intl.formatMessage({ id: \'team.cash\' })} info={formatter.formatAsNumber(team.cash)} />\n'
  '                  <InfoItem key="value" label={intl.formatMessage({ id: \'common.value\' })} info={formatter.formatAsNumber(team.value)} />\n'
  '                  <InfoItem key="matches" label={intl.formatMessage({ id: \'common.matches\' })}\n'),
 ('frontend/src/pages/TeamPage.jsx',
  '        <Heading size="md">Matches</Heading>\n',
  '        <Heading size="md">{intl.formatMessage({ id: \'common.matches\' })}</Heading>\n'),
 ('frontend/src/components/team/Roster.jsx',
  "import { identityUtils } from '../../util/identityUtil';\n"
  'const TableColumns = (\n'
  '  <Tr>\n'
  '    <Th>#</Th>\n'
  '    <Th>Name</Th>\n'
  '    <Th>Type</Th>\n'
  '    <Th>\n'
  '      <Center>Level</Center>\n'
  '    </Th>\n'
  '    <Th>Skills</Th>\n'
  '    <Th>Injuries</Th>\n',
  "import { identityUtils } from '../../util/identityUtil';\n"
  "import { useIntl } from 'react-intl';\n"
  'function TableColumns() {\n'
  '  const intl = useIntl();\n'
  '  return <Tr>\n'
  '    <Th>#</Th>\n'
  "    <Th>{intl.formatMessage({ id: 'common.name' })}</Th>\n"
  "    <Th>{intl.formatMessage({ id: 'common.type' })}</Th>\n"
  '    <Th>\n'
  "      <Center>{intl.formatMessage({ id: 'common.level' })}</Center>\n"
  '    </Th>\n'
  "    <Th>{intl.formatMessage({ id: 'common.skills' })}</Th>\n"
  "    <Th>{intl.formatMessage({ id: 'common.injuries' })}</Th>\n"),
 ('frontend/src/components/team/Roster.jsx',
  '    <Th isNumeric>Value</Th>\n  </Tr>\n);\n',
  "    <Th isNumeric>{intl.formatMessage({ id: 'common.value' })}</Th>\n  </Tr>\n}\n"),
 ('frontend/src/components/team/Roster.jsx', '        <Thead>{TableColumns}</Thead>\n', '        <Thead><TableColumns /></Thead>\n'),
 ('frontend/src/components/team/Roster.jsx', '        <Tfoot>{TableColumns}</Tfoot>\n', '        <Tfoot><TableColumns /></Tfoot>\n'),
 ('frontend/src/components/contest/Matches.jsx',
  "import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';\n",
  "import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/contest/Matches.jsx',
  'function TableColumns() {\n'
  '  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);\n'
  '  return (\n'
  '    <Tr>\n'
  '      {!isSmallScreen && <Th>Date</Th>}\n'
  '      {!isSmallScreen && <Th>Competition</Th>}\n'
  '      <Th textAlign="center">Home</Th>\n'
  '      <Th textAlign="center">Result</Th>\n'
  '      <Th textAlign="center">Away</Th>\n',
  'function TableColumns() {\n'
  '  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);\n'
  '  const intl = useIntl();\n'
  '  return (\n'
  '    <Tr>\n'
  "      {!isSmallScreen && <Th>{intl.formatMessage({ id: 'common.date' })}</Th>}\n"
  "      {!isSmallScreen && <Th>{intl.formatMessage({ id: 'common.competition' })}</Th>}\n"
  '      <Th textAlign="center">{intl.formatMessage({ id: \'common.home\' })}</Th>\n'
  '      <Th textAlign="center">{intl.formatMessage({ id: \'common.result\' })}</Th>\n'
  '      <Th textAlign="center">{intl.formatMessage({ id: \'common.away\' })}</Th>\n'),
 ('frontend/src/components/statistics/StatisticsTables.jsx',
  "import { useMyTeams } from '../../context/MyTeamsContext';\n",
  "import { useMyTeams } from '../../context/MyTeamsContext';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/statistics/StatisticsTables.jsx',
  'function PlayerDisplayName({ name }) {\n',
  'function PlayerDisplayName({ name }) {\n  const intl = useIntl();\n'),
 ('frontend/src/components/statistics/StatisticsTables.jsx',
  "    {starPlayer && '⭐ '}{name ? getStarPlayerDisplayName(name) : 'Unknown player'}\n",
  "    {starPlayer && '⭐ '}{name ? getStarPlayerDisplayName(name) : intl.formatMessage({ id: 'common.unknownPlayer' })}\n"),
 ('frontend/src/components/statistics/StatisticsTables.jsx',
  'function PlayerColumns({ label, compact }) {\n'
  "  return <Tr><Th><Center>{compact ? 'R' : 'Rank'}</Center></Th><Th>Player</Th><Th>Team</Th>\n"
  '    {!compact && <Th />}<Th><Center>{label}</Center></Th>\n'
  '    {!compact && <><Th>Coach</Th><Th>Position</Th><Th><Center>Games</Center></Th><Th><Center>SPP</Center></Th><Th>Skills</Th></>}</Tr>;\n',
  'function PlayerColumns({ label, compact }) {\n'
  '  const intl = useIntl();\n'
  "  return <Tr><Th><Center>{compact ? 'R' : intl.formatMessage({ id: 'common.rank' })}</Center></Th><Th>{intl.formatMessage({ id: 'common.player' "
  "})}</Th><Th>{intl.formatMessage({ id: 'common.team' })}</Th>\n"
  '    {!compact && <Th />}<Th><Center>{label}</Center></Th>\n'
  "    {!compact && <><Th>{intl.formatMessage({ id: 'common.coach' })}</Th><Th>{intl.formatMessage({ id: 'common.position' "
  "})}</Th><Th><Center>{intl.formatMessage({ id: 'common.games' })}</Center></Th><Th><Center>SPP</Center></Th><Th>{intl.formatMessage({ id: 'common.skills' "
  '})}</Th></>}</Tr>;\n'),
 ('frontend/src/components/statistics/StatisticsTables.jsx',
  'export function PlayerTable({ category, mineOnly = false }) {\n',
  'export function PlayerTable({ category, mineOnly = false }) {\n  const intl = useIntl();\n'),
 ('frontend/src/components/statistics/StatisticsTables.jsx',
  '      <Td><HStack><Text>{player.teamName || \'Unknown team\'}</Text>{mine && <Badge colorScheme="green">My player</Badge>}</HStack>\n',
  "      <Td><HStack><Text>{player.teamName || intl.formatMessage({ id: 'common.unknownTeam' })}</Text>{mine && <Badge "
  'colorScheme="green">{intl.formatMessage({ id: \'statistics.myPlayer\' })}</Badge>}</HStack>\n'),
 ('frontend/src/components/statistics/StatisticsTables.jsx',
  'function TeamColumns({ label, compact }) {\n'
  "  return <Tr><Th><Center>{compact ? 'R' : 'Rank'}</Center></Th><Th>{compact ? 'Team / coach' : 'Team-name'}</Th><Th />\n"
  '    {!compact && <Th>Coach-name</Th>}<Th><Center>{label}</Center></Th>\n'
  "    <Th><Center>W</Center></Th><Th><Center>D</Center></Th><Th><Center>L</Center></Th><Th><Center>{compact ? 'GP' : 'Games'}</Center></Th>\n",
  'function TeamColumns({ label, compact }) {\n'
  '  const intl = useIntl();\n'
  "  return <Tr><Th><Center>{compact ? 'R' : intl.formatMessage({ id: 'common.rank' })}</Center></Th><Th>{compact ? intl.formatMessage({ id: "
  "'statistics.teamCoach' }) : intl.formatMessage({ id: 'statistics.teamName' })}</Th><Th />\n"
  "    {!compact && <Th>{intl.formatMessage({ id: 'statistics.coachName' })}</Th>}<Th><Center>{label}</Center></Th>\n"
  "    <Th><Center>W</Center></Th><Th><Center>D</Center></Th><Th><Center>L</Center></Th><Th><Center>{compact ? 'GP' : intl.formatMessage({ id: 'common.games' "
  '})}</Center></Th>\n'),
 ('frontend/src/components/statistics/StatisticsTables.jsx',
  'export function TeamTable({ category, entries, mineOnly = false }) {\n',
  'export function TeamTable({ category, entries, mineOnly = false }) {\n  const intl = useIntl();\n'),
 ('frontend/src/components/statistics/StatisticsTables.jsx',
  "  const label = category?.label || 'Score';\n",
  "  const label = category?.label || intl.formatMessage({ id: 'common.score' });\n"),
 ('frontend/src/components/statistics/StatisticsTables.jsx',
  '      <Td><HStack><Text fontWeight="semibold">{team.name}</Text>{mine && <Badge colorScheme="green">My team</Badge>}{team.editions?.length > 1 && '
  "<Badge>{team.editions.join(' + ')}</Badge>}</HStack>\n",
  '      <Td><HStack><Text fontWeight="semibold">{team.name}</Text>{mine && <Badge colorScheme="green">{intl.formatMessage({ id: \'statistics.myTeam\' '
  "})}</Badge>}{team.editions?.length > 1 && <Badge>{team.editions.join(' + ')}</Badge>}</HStack>\n"),
 ('frontend/src/components/statistics/StatisticsTables.jsx',
  'export function CategoryTabs({ categories, type, mineOnly = false }) {\n'
  '  if (!categories?.length) return <Text color="gray.500">No statistics are available for this selection.</Text>;\n',
  'export function CategoryTabs({ categories, type, mineOnly = false }) {\n'
  '  const intl = useIntl();\n'
  '  if (!categories?.length) return <Text color="gray.500">{intl.formatMessage({ id: \'statistics.noData\' })}</Text>;\n'),
 ('frontend/src/components/statistics/StatisticsTables.jsx',
  'export function VersusTable({ rows }) {\n'
  '  if (!rows?.length) return <Text color="gray.500">No opponents found for the mapped coach IDs.</Text>;\n'
  '  return <TableContainer borderWidth="1px" borderRadius="md"><Table variant="stripedClickable" size="sm">\n'
  '    <Thead><Tr><Th>Coach</Th><Th isNumeric>G</Th><Th isNumeric>W-D-L</Th><Th isNumeric>TD</Th><Th isNumeric>CAS</Th></Tr></Thead>\n',
  'export function VersusTable({ rows }) {\n'
  '  const intl = useIntl();\n'
  '  if (!rows?.length) return <Text color="gray.500">{intl.formatMessage({ id: \'statistics.noOpponents\' })}</Text>;\n'
  '  return <TableContainer borderWidth="1px" borderRadius="md"><Table variant="stripedClickable" size="sm">\n'
  "    <Thead><Tr><Th>{intl.formatMessage({ id: 'common.coach' })}</Th><Th isNumeric>G</Th><Th isNumeric>W-D-L</Th><Th isNumeric>TD</Th><Th "
  'isNumeric>CAS</Th></Tr></Thead>\n'),
 ('frontend/src/pages/StatisticsPage.jsx',
  "import {CategoryTabs,TeamTable,VersusTable} from '../components/statistics/StatisticsTables';\n"
  'export default function StatisticsPage(){const '
  "auth=useAuth0WithUserPermissions(),[systems,setSystems]=useState([]),[systemId,setSystemId]=useState(''),[overview,setOverview]=useState(null),[seasonId,setSeasonId]=useState(''),[season,setSeason]=useState(null),[marathon,setMarathon]=useState(null),[edition,setEdition]=useState('ALL'),[merge,setMerge]=useState(false),[page,setPage]=useState(0),[myTeamsOnly,setMyTeamsOnly]=useState(false),[myPlayersOnly,setMyPlayersOnly]=useState(false),[personal,setPersonal]=useState(null),[loading,setLoading]=useState(true),[error,setError]=useState('');\n",
  "import {CategoryTabs,TeamTable,VersusTable} from '../components/statistics/StatisticsTables';\n"
  "import {useIntl} from 'react-intl';\n"
  'export default function StatisticsPage(){const '
  "intl=useIntl(),auth=useAuth0WithUserPermissions(),[systems,setSystems]=useState([]),[systemId,setSystemId]=useState(''),[overview,setOverview]=useState(null),[seasonId,setSeasonId]=useState(''),[season,setSeason]=useState(null),[marathon,setMarathon]=useState(null),[edition,setEdition]=useState('ALL'),[merge,setMerge]=useState(false),[page,setPage]=useState(0),[myTeamsOnly,setMyTeamsOnly]=useState(false),[myPlayersOnly,setMyPlayersOnly]=useState(false),[personal,setPersonal]=useState(null),[loading,setLoading]=useState(true),[error,setError]=useState('');\n"),
 ('frontend/src/pages/StatisticsPage.jsx',
  "const mineToggle=(type)=><Checkbox mb={3} isChecked={type==='team'?myTeamsOnly:myPlayersOnly} "
  "onChange={e=>type==='team'?setMyTeamsOnly(e.target.checked):setMyPlayersOnly(e.target.checked)}>{type==='team'?'My teams only':'My players "
  "only'}</Checkbox>;\n"
  'return <Stack><Navigation currentPage="statistics"/><Heading>Statistics</Heading><HStack wrap="wrap"><Select maxW="20rem" value={systemId} '
  "onChange={e=>{setSystemId(e.target.value);setSeasonId('');setPage(0)}}>{systems.map(s=><option key={s.id} "
  'value={s.id}>{s.name}</option>)}</Select>{loading&&<Spinner size="sm"/>}</HStack>{error&&<Text color="red.500">{error}</Text>}<Tabs variant="soft-rounded" '
  'isLazy><TabList><Tab>Season</Tab><Tab>Marathon</Tab>{auth.authenticationReady&&auth.isAuthenticated&&<Tab>Coach versus</Tab>}</TabList><TabPanels><TabPanel '
  'px={0}><Select mb={4} maxW="20rem" value={seasonId} onChange={e=>setSeasonId(e.target.value)}>{overview?.seasons?.map(s=><option key={s.id} '
  'value={s.id}>{s.name||`Season ${s.number}`}</option>)}</Select><Tabs isFitted '
  'variant="enclosed"><TabList><Tab>Players</Tab><Tab>Teams</Tab></TabList><TabPanels><TabPanel px={0}>{mineToggle(\'player\')}<CategoryTabs type="player" '
  'categories={season?.players} mineOnly={myPlayersOnly}/></TabPanel><TabPanel px={0}>{mineToggle(\'team\')}<CategoryTabs type="team" '
  "categories={season?.teams} mineOnly={myTeamsOnly}/></TabPanel></TabPanels></Tabs></TabPanel><TabPanel px={0}><Stack direction={{base:'column',md:'row'}} "
  'mb={4}><Select maxW="14rem" value={edition} onChange={e=>{setEdition(e.target.value);setPage(0)}}><option value="ALL">All '
  'editions</option>{marathon?.availableEditions?.map(e=><option key={e}>{e}</option>)}</Select><Checkbox isChecked={merge} '
  'onChange={e=>{setMerge(e.target.checked);setPage(0)}}>Merge teams with the same name</Checkbox></Stack>{edition===\'ALL\'&&<Text fontSize="sm" '
  'color="gray.500" mb={3}>Only player statistics comparable across rulesets are shown.</Text>}<Tabs isFitted '
  'variant="enclosed"><TabList><Tab>Teams</Tab><Tab>Players</Tab></TabList><TabPanels><TabPanel px={0}>{mineToggle(\'team\')}<TeamTable '
  'entries={marathon?.teams?.content||[]} mineOnly={myTeamsOnly}/><HStack justify="space-between" mt={3}><Button isDisabled={!page} '
  'onClick={()=>setPage(p=>p-1)}>Previous</Button><Text>Page {page+1} of {marathon?.teams?.totalPages||1}</Text><Button '
  'isDisabled={page+1>=(marathon?.teams?.totalPages||1)} onClick={()=>setPage(p=>p+1)}>Next</Button></HStack></TabPanel><TabPanel '
  'px={0}>{mineToggle(\'player\')}<CategoryTabs type="player" categories={marathon?.players} '
  'mineOnly={myPlayersOnly}/></TabPanel></TabPanels></Tabs></TabPanel>{auth.authenticationReady&&auth.isAuthenticated&&<TabPanel '
  'px={0}>{!personal?.coaches?.length?<Text>No coaches claimed yet. Claim your BB1/BB2/BB3 coach names under Account.</Text>:<VersusTable '
  'rows={personal?.versus}/>}</TabPanel>}</TabPanels></Tabs></Stack>}\n',
  "const mineToggle=(type)=><Checkbox mb={3} isChecked={type==='team'?myTeamsOnly:myPlayersOnly} "
  "onChange={e=>type==='team'?setMyTeamsOnly(e.target.checked):setMyPlayersOnly(e.target.checked)}>{intl.formatMessage({id:type==='team'?'statistics.myTeamsOnly':'statistics.myPlayersOnly'})}</Checkbox>;\n"
  'return <Stack><Navigation currentPage="statistics"/><Heading>{intl.formatMessage({id:\'statistics.title\'})}</Heading><HStack wrap="wrap"><Select '
  'maxW="20rem" value={systemId} onChange={e=>{setSystemId(e.target.value);setSeasonId(\'\');setPage(0)}}>{systems.map(s=><option key={s.id} '
  'value={s.id}>{s.name}</option>)}</Select>{loading&&<Spinner size="sm"/>}</HStack>{error&&<Text color="red.500">{error}</Text>}<Tabs variant="soft-rounded" '
  "isLazy><TabList><Tab>{intl.formatMessage({id:'statistics.season'})}</Tab><Tab>{intl.formatMessage({id:'statistics.marathon'})}</Tab>{auth.authenticationReady&&auth.isAuthenticated&&<Tab>{intl.formatMessage({id:'statistics.coachVersus'})}</Tab>}</TabList><TabPanels><TabPanel "
  'px={0}><Select mb={4} maxW="20rem" value={seasonId} onChange={e=>setSeasonId(e.target.value)}>{overview?.seasons?.map(s=><option key={s.id} '
  "value={s.id}>{s.name||intl.formatMessage({id:'statistics.seasonNumber'},{number:s.number})}</option>)}</Select><Tabs isFitted "
  'variant="enclosed"><TabList><Tab>{intl.formatMessage({id:\'common.players\'})}</Tab><Tab>{intl.formatMessage({id:\'common.teams\'})}</Tab></TabList><TabPanels><TabPanel '
  'px={0}>{mineToggle(\'player\')}<CategoryTabs type="player" categories={season?.players} mineOnly={myPlayersOnly}/></TabPanel><TabPanel '
  'px={0}>{mineToggle(\'team\')}<CategoryTabs type="team" categories={season?.teams} '
  "mineOnly={myTeamsOnly}/></TabPanel></TabPanels></Tabs></TabPanel><TabPanel px={0}><Stack direction={{base:'column',md:'row'}} mb={4}><Select "
  'maxW="14rem" value={edition} onChange={e=>{setEdition(e.target.value);setPage(0)}}><option '
  'value="ALL">{intl.formatMessage({id:\'statistics.allEditions\'})}</option>{marathon?.availableEditions?.map(e=><option '
  'key={e}>{e}</option>)}</Select><Checkbox isChecked={merge} '
  "onChange={e=>{setMerge(e.target.checked);setPage(0)}}>{intl.formatMessage({id:'statistics.mergeTeams'})}</Checkbox></Stack>{edition==='ALL'&&<Text "
  'fontSize="sm" color="gray.500" mb={3}>{intl.formatMessage({id:\'statistics.comparableOnly\'})}</Text>}<Tabs isFitted '
  'variant="enclosed"><TabList><Tab>{intl.formatMessage({id:\'common.teams\'})}</Tab><Tab>{intl.formatMessage({id:\'common.players\'})}</Tab></TabList><TabPanels><TabPanel '
  'px={0}>{mineToggle(\'team\')}<TeamTable entries={marathon?.teams?.content||[]} mineOnly={myTeamsOnly}/><HStack justify="space-between" mt={3}><Button '
  'isDisabled={!page} '
  "onClick={()=>setPage(p=>p-1)}>{intl.formatMessage({id:'common.previous'})}</Button><Text>{intl.formatMessage({id:'statistics.page'},{page:page+1,pages:marathon?.teams?.totalPages||1})}</Text><Button "
  'isDisabled={page+1>=(marathon?.teams?.totalPages||1)} '
  "onClick={()=>setPage(p=>p+1)}>{intl.formatMessage({id:'common.next'})}</Button></HStack></TabPanel><TabPanel px={0}>{mineToggle('player')}<CategoryTabs "
  'type="player" categories={marathon?.players} '
  'mineOnly={myPlayersOnly}/></TabPanel></TabPanels></Tabs></TabPanel>{auth.authenticationReady&&auth.isAuthenticated&&<TabPanel '
  "px={0}>{!personal?.coaches?.length?<Text>{intl.formatMessage({id:'statistics.noClaimedCoaches'})}</Text>:<VersusTable "
  'rows={personal?.versus}/>}</TabPanel>}</TabPanels></Tabs></Stack>}\n'),
 ('frontend/src/components/contest/LatestMatches.jsx',
  "import { identityUtils } from '../../util/identityUtil';\n",
  "import { identityUtils } from '../../util/identityUtil';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/contest/LatestMatches.jsx',
  'function LatestMatches({ league, competition, embeddable, limit }) {\n',
  'function LatestMatches({ league, competition, embeddable, limit }) {\n  const intl = useIntl();\n'),
 ('frontend/src/components/contest/LatestMatches.jsx',
  '      {!embeddable && <Heading size="md">Latest matches</Heading>}\n',
  '      {!embeddable && <Heading size="md">{intl.formatMessage({ id: \'matches.latest\' })}</Heading>}\n'),
 ('frontend/src/components/contest/LatestMatches.jsx',
  '          noContentHeading="No matches played (yet?)..."\n'
  "          noContentText={league ? `Last match was ${formatter.formatAsDate(league.dateLastMatch, '-')}` : null}\n",
  "          noContentHeading={intl.formatMessage({ id: 'matches.nonePlayed' })}\n"
  "          noContentText={league ? intl.formatMessage({ id: 'matches.lastWas' }, { date: formatter.formatAsDate(league.dateLastMatch, '-') }) : null}\n"),
 ('frontend/src/components/contest/LiveContests.jsx',
  "import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';\n",
  "import LoadingOrErrorWrapper from '../common/LoadingOrErrorWrapper';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/contest/LiveContests.jsx',
  'function LiveContests({ league, competition, embeddable, limit }) {\n',
  'function LiveContests({ league, competition, embeddable, limit }) {\n  const intl = useIntl();\n'),
 ('frontend/src/components/contest/LiveContests.jsx',
  '      {!embeddable && <Heading size="md">Live matches</Heading>}\n',
  '      {!embeddable && <Heading size="md">{intl.formatMessage({ id: \'matches.live\' })}</Heading>}\n'),
 ('frontend/src/components/contest/LiveContests.jsx',
  '          noContentHeading="No matches live currently..."\n'
  "          noContentText={league ? `Last match was ${formatter.formatAsDate(league.dateLastMatch, '-')}` : null}\n",
  "          noContentHeading={intl.formatMessage({ id: 'matches.noneLive' })}\n"
  "          noContentText={league ? intl.formatMessage({ id: 'matches.lastWas' }, { date: formatter.formatAsDate(league.dateLastMatch, '-') }) : null}\n"),
 ('frontend/src/pages/CoachPage.jsx',
  "import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';\n",
  "import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/CoachPage.jsx', 'function CoachPage() {\n', 'function CoachPage() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/CoachPage.jsx', '          heading="Coach-Page"\n', "          heading={intl.formatMessage({ id: 'coachPage.title' })}\n"),
 ('frontend/src/pages/CoachPage.jsx',
  "              ? authenticationReady && user && `Authenticated as ${user.name}`\n              : 'No real User in dev environment.'\n",
  "              ? authenticationReady && user && intl.formatMessage({ id: 'coachPage.authenticatedAs' }, { name: user.name })\n"
  "              : intl.formatMessage({ id: 'coachPage.devUser' })\n"),
 ('frontend/src/pages/CoachPage.jsx',
  '          <PermissionIcon granted={userPermissions?.readCurrentUser} /> User read permissions\n',
  "          <PermissionIcon granted={userPermissions?.readCurrentUser} /> {intl.formatMessage({ id: 'coachPage.readPermission' })}\n"),
 ('frontend/src/pages/CoachPage.jsx',
  '          <PermissionIcon granted={userPermissions?.writeRegisterLeague} /> Permission to register leagues\n',
  "          <PermissionIcon granted={userPermissions?.writeRegisterLeague} /> {intl.formatMessage({ id: 'coachPage.registerLeaguePermission' })}\n"),
 ('frontend/src/pages/CoachPage.jsx',
  '          <PermissionIcon granted={userPermissions?.writeLeagueAdmin} /> League admin permissions\n',
  "          <PermissionIcon granted={userPermissions?.writeLeagueAdmin} /> {intl.formatMessage({ id: 'coachPage.leagueAdminPermission' })}\n"),
 ('frontend/src/pages/CoachPage.jsx',
  '          <PermissionIcon granted={userPermissions?.writeSiteAdmin} /> Site admin permissions\n',
  "          <PermissionIcon granted={userPermissions?.writeSiteAdmin} /> {intl.formatMessage({ id: 'coachPage.siteAdminPermission' })}\n"),
 ('frontend/src/pages/ArticleEditorPage.jsx',
  "import EditorialCommunityApi from '../EditorialCommunityApi';\n",
  "import EditorialCommunityApi from '../EditorialCommunityApi';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/ArticleEditorPage.jsx', 'function ArticleEditorPage() {\n', 'function ArticleEditorPage() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/ArticleEditorPage.jsx',
  "          <FormControl><FormLabel>Rubrik</FormLabel><Input value={form.title} onChange={(e) => set('title', e.target.value)} /></FormControl>\n"
  "          <FormControl><FormLabel>Slug (valfri)</FormLabel><Input value={form.slug} onChange={(e) => set('slug', e.target.value)} /></FormControl>\n"
  "          <FormControl><FormLabel>Ingress</FormLabel><Input value={form.excerpt} onChange={(e) => set('excerpt', e.target.value)} /></FormControl>\n"
  "          <FormControl><FormLabel>Omslagsbild URL</FormLabel><Input value={form.coverImageUrl} onChange={(e) => set('coverImageUrl', e.target.value)} "
  '/></FormControl>\n'
  "          <FormControl><FormLabel>LeagueSystem ID (tom = site-wide)</FormLabel><Input value={form.leagueSystemId} onChange={(e) => set('leagueSystemId', "
  'e.target.value)} /></FormControl>\n'
  "          <FormControl><FormLabel>Season ID</FormLabel><Input value={form.seasonId} onChange={(e) => set('seasonId', e.target.value)} /></FormControl>\n"
  "          <FormControl><FormLabel>Status</FormLabel><Select value={form.status} onChange={(e) => set('status', e.target.value)}>\n"
  '            <option value="DRAFT">Draft</option><option value="PUBLISHED">Published</option><option value="ARCHIVED">Archived</option>\n'
  '          </Select></FormControl>\n'
  "          <Checkbox isChecked={form.featured} onChange={(e) => set('featured', e.target.checked)}>Featured</Checkbox>\n",
  "          <FormControl><FormLabel>{intl.formatMessage({ id: 'articleEditor.title' })}</FormLabel><Input value={form.title} onChange={(e) => set('title', "
  'e.target.value)} /></FormControl>\n'
  "          <FormControl><FormLabel>{intl.formatMessage({ id: 'articleEditor.slug' })}</FormLabel><Input value={form.slug} onChange={(e) => set('slug', "
  'e.target.value)} /></FormControl>\n'
  "          <FormControl><FormLabel>{intl.formatMessage({ id: 'articleEditor.excerpt' })}</FormLabel><Input value={form.excerpt} onChange={(e) => "
  "set('excerpt', e.target.value)} /></FormControl>\n"
  "          <FormControl><FormLabel>{intl.formatMessage({ id: 'articleEditor.coverImage' })}</FormLabel><Input value={form.coverImageUrl} onChange={(e) => "
  "set('coverImageUrl', e.target.value)} /></FormControl>\n"
  "          <FormControl><FormLabel>{intl.formatMessage({ id: 'articleEditor.leagueSystem' })}</FormLabel><Input value={form.leagueSystemId} onChange={(e) => "
  "set('leagueSystemId', e.target.value)} /></FormControl>\n"
  "          <FormControl><FormLabel>{intl.formatMessage({ id: 'articleEditor.season' })}</FormLabel><Input value={form.seasonId} onChange={(e) => "
  "set('seasonId', e.target.value)} /></FormControl>\n"
  "          <FormControl><FormLabel>{intl.formatMessage({ id: 'articleEditor.status' })}</FormLabel><Select value={form.status} onChange={(e) => "
  "set('status', e.target.value)}>\n"
  '            <option value="DRAFT">{intl.formatMessage({ id: \'articleEditor.draft\' })}</option><option value="PUBLISHED">{intl.formatMessage({ id: '
  '\'articleEditor.published\' })}</option><option value="ARCHIVED">{intl.formatMessage({ id: \'articleEditor.archived\' })}</option>\n'
  '          </Select></FormControl>\n'
  "          <Checkbox isChecked={form.featured} onChange={(e) => set('featured', e.target.checked)}>{intl.formatMessage({ id: 'articleEditor.featured' "
  '})}</Checkbox>\n'),
 ('frontend/src/pages/ArticleEditorPage.jsx',
  '          <Button onClick={save}>Spara artikel</Button>\n',
  "          <Button onClick={save}>{intl.formatMessage({ id: 'articleEditor.save' })}</Button>\n"),
 ('frontend/src/components/misc/Disclaimer.jsx', "import React from 'react';\n", "import React from 'react';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/misc/Disclaimer.jsx',
  'function Disclaimer({ headerSize, textSize, ...props }) {\n'
  '  return (\n'
  '    <Box fontSize={headerSize} {...props}>\n'
  '      <Text fontStyle="italic">Disclaimer</Text>\n'
  '      <VStack align="left">\n'
  '        <Text fontSize={textSize}>\n'
  '          This site is completely unofficial and not affiliated with Cyanide, Nacon, Slitherine or Games Workshop.\n'
  '        </Text>\n'
  '        <Text fontSize={textSize}>\n'
  '          Blood Bowl, BB3 and probably a lot more names are trademarks of their respective owners. Used without\n'
  '          permission. No challenge to their status intended.\n'
  '        </Text>\n'
  '        <Text fontSize={textSize}>\n'
  '          This work is based heavily on <Link href="https://warp-scores.net" isExternal>Warp-Scores</Link> by Naytsyrhc.\n'
  '        </Text>\n'
  '        <Text fontSize={textSize}>\n'
  "          Page maintained by{' '}\n"
  '          <Link href="mailto:dennis.granasen@gmail.com" isExternal>\n'
  '            d-rock\n'
  '          </Link>\n'
  '        </Text>\n'
  '        <Text fontSize={textSize}>\n'
  "          Please also check{' '}\n"
  '          <Link as={RouteLink} to="/terms.md">\n'
  '            Terms\n'
  "          </Link>{' '}\n"
  "          and{' '}\n"
  '          <Link as={RouteLink} to="/privacy.md">\n'
  '            Privacy Policy\n'
  '          </Link>\n'
  '          .\n'
  '        </Text>\n',
  'function Disclaimer({ headerSize, textSize, ...props }) {\n'
  '  const intl = useIntl();\n'
  '  return (\n'
  '    <Box fontSize={headerSize} {...props}>\n'
  '      <Text fontStyle="italic">{intl.formatMessage({ id: \'disclaimer.heading\' })}</Text>\n'
  '      <VStack align="left">\n'
  "        <Text fontSize={textSize}>{intl.formatMessage({ id: 'disclaimer.unofficial' })}</Text>\n"
  "        <Text fontSize={textSize}>{intl.formatMessage({ id: 'disclaimer.trademarks' })}</Text>\n"
  '        <Text fontSize={textSize}>\n'
  "          {intl.formatMessage({ id: 'disclaimer.basedOn' })}\n"
  '        </Text>\n'
  '        <Text fontSize={textSize}>\n'
  '          <Link href="mailto:dennis.granasen@gmail.com" isExternal>\n'
  "            {intl.formatMessage({ id: 'disclaimer.maintainedBy' }, { name: 'd-rock' })}\n"
  '          </Link>\n'
  '        </Text>\n'
  '        <Text fontSize={textSize}>\n'
  '          <Link as={RouteLink} to="/terms.md">\n'
  "            {intl.formatMessage({ id: 'disclaimer.checkTerms' })}\n"
  "          </Link>{' '}\n"
  '          <Link as={RouteLink} to="/privacy.md">\n'
  '            Privacy\n'
  '          </Link>\n'
  '        </Text>\n')]


EXTRA_OPERATIONS = [
    ("frontend/src/i18n/messages.js",
     "  'disclaimer.checkTerms': 'Please also check Terms and Privacy Policy.',\n",
     "  'disclaimer.checkTerms': 'Please also check Terms and Privacy Policy.',\n"
     "  'status.Registration': 'Registration',\n"
     "  'status.InProgress': 'In progress',\n"
     "  'status.Finished': 'Finished',\n"
     "  'status.Unknown': 'Unknown',\n"
     "  'format.RoundRobin': 'Round robin',\n"
     "  'format.Wissen': 'Swiss',\n"
     "  'format.Knockout': 'Knockout',\n"
     "  'format.Ladder': 'Ladder',\n"
     "  'format.Arena': 'Arena',\n"
     "  'account.heading': 'BlaskScore account',\n"
     "  'account.steamOptional': 'Steam is optional. Coach claims determine which historical teams and players are yours.',\n"
     "  'account.myCoaches': 'My coaches',\n"
     "  'account.noCoaches': 'No coaches claimed yet.',\n"
     "  'account.claimedFromSteam': 'claimed from Steam',\n"
     "  'account.manuallyClaimed': 'manually claimed',\n"
     "  'account.releaseClaim': 'Release claim',\n"
     "  'account.bbVersion': 'Blood Bowl version',\n"
     "  'account.selectCoachNames': 'Select your coach names. Internal coach IDs are deliberately hidden.',\n"
     "  'account.teamCount': '{count, plural, one {# team} other {# teams}}',\n"
     "  'account.claimSelected': 'Claim selected coaches',\n"
     "  'account.connectedAs': 'Connected as {name}',\n"
     "  'account.autoClaim': 'The BB3 coach exposed by this Steam session is claimed automatically if unclaimed.',\n"
     "  'account.myTeams': 'My teams',\n"
     "  'account.loadingTeams': 'Loading teams…',\n"
     "  'account.noTeams': 'No BB3 teams found.',\n"
     "  'account.raceTv': 'Race ID: {race} · TV: {tv}',\n"
     "  'account.disconnect': 'Disconnect session',\n"
     "  'account.approveSteam': 'Approve the sign-in in the Steam app, then confirm below.',\n"
     "  'account.enterGuard': 'Enter the Steam Guard code{hint, select, none {} other { sent to {hint}}}.',\n"
     "  'account.guardCode': 'Steam Guard code',\n"
     "  'account.credentialsPrivacy': 'Your Steam username is remembered. Passwords and Guard codes are sent only for this login and are never stored by BlaskScore.',\n"
     "  'account.steamUsername': 'Steam username',\n"
     "  'account.steamPassword': 'Steam password',\n"
     "  'account.connectSteam': 'Connect Steam',\n"
     "  'account.claimAdmin': 'Coach claims administration',\n"
     "  'account.claimAdminHelp': 'Remove a bad claim; the correct user can then claim that coach.',\n"
     "  'account.removeClaim': 'Remove claim',\n"
     "  'account.authFailed': 'Authentication failed',\n"),
    ("frontend/src/i18n/messages.js",
     "  'disclaimer.checkTerms': 'Läs även Villkor och Integritetspolicy.',\n",
     "  'disclaimer.checkTerms': 'Läs även Villkor och Integritetspolicy.',\n"
     "  'status.Registration': 'Registrering',\n"
     "  'status.InProgress': 'Pågår',\n"
     "  'status.Finished': 'Avslutad',\n"
     "  'status.Unknown': 'Okänd',\n"
     "  'format.RoundRobin': 'Alla möter alla',\n"
     "  'format.Wissen': 'Schweizer',\n"
     "  'format.Knockout': 'Slutspel',\n"
     "  'format.Ladder': 'Stege',\n"
     "  'format.Arena': 'Arena',\n"
     "  'account.heading': 'BlaskScore-konto',\n"
     "  'account.steamOptional': 'Steam är valfritt. Coachkopplingar avgör vilka historiska lag och spelare som är dina.',\n"
     "  'account.myCoaches': 'Mina coacher',\n"
     "  'account.noCoaches': 'Inga coacher är kopplade ännu.',\n"
     "  'account.claimedFromSteam': 'kopplad via Steam',\n"
     "  'account.manuallyClaimed': 'manuellt kopplad',\n"
     "  'account.releaseClaim': 'Ta bort koppling',\n"
     "  'account.bbVersion': 'Blood Bowl-version',\n"
     "  'account.selectCoachNames': 'Välj dina coachnamn. Interna coach-ID:n visas avsiktligt inte.',\n"
     "  'account.teamCount': '{count, plural, one {# lag} other {# lag}}',\n"
     "  'account.claimSelected': 'Koppla valda coacher',\n"
     "  'account.connectedAs': 'Ansluten som {name}',\n"
     "  'account.autoClaim': 'BB3-coachen som Steam-sessionen visar kopplas automatiskt om den inte redan är kopplad.',\n"
     "  'account.myTeams': 'Mina lag',\n"
     "  'account.loadingTeams': 'Laddar lag…',\n"
     "  'account.noTeams': 'Inga BB3-lag hittades.',\n"
     "  'account.raceTv': 'Ras-ID: {race} · TV: {tv}',\n"
     "  'account.disconnect': 'Koppla från session',\n"
     "  'account.approveSteam': 'Godkänn inloggningen i Steam-appen och bekräfta sedan nedan.',\n"
     "  'account.enterGuard': 'Ange Steam Guard-koden{hint, select, none {} other { som skickades till {hint}}}.',\n"
     "  'account.guardCode': 'Steam Guard-kod',\n"
     "  'account.credentialsPrivacy': 'Ditt Steam-användarnamn sparas. Lösenord och Guard-koder skickas endast för den här inloggningen och lagras aldrig av BlaskScore.',\n"
     "  'account.steamUsername': 'Steam-användarnamn',\n"
     "  'account.steamPassword': 'Steam-lösenord',\n"
     "  'account.connectSteam': 'Anslut Steam',\n"
     "  'account.claimAdmin': 'Administration av coachkopplingar',\n"
     "  'account.claimAdminHelp': 'Ta bort en felaktig koppling så att rätt användare kan koppla coachen.',\n"
     "  'account.removeClaim': 'Ta bort koppling',\n"
     "  'account.authFailed': 'Autentiseringen misslyckades',\n"),
    ("frontend/src/i18n/messages.js",
     "  'disclaimer.checkTerms': 'Consulta también los Términos y la Política de privacidad.',\n",
     "  'disclaimer.checkTerms': 'Consulta también los Términos y la Política de privacidad.',\n"
     "  'status.Registration': 'Inscripción',\n"
     "  'status.InProgress': 'En curso',\n"
     "  'status.Finished': 'Finalizada',\n"
     "  'status.Unknown': 'Desconocido',\n"
     "  'format.RoundRobin': 'Todos contra todos',\n"
     "  'format.Wissen': 'Suizo',\n"
     "  'format.Knockout': 'Eliminatoria',\n"
     "  'format.Ladder': 'Clasificación',\n"
     "  'format.Arena': 'Arena',\n"
     "  'account.heading': 'Cuenta de BlaskScore',\n"
     "  'account.steamOptional': 'Steam es opcional. Las reclamaciones de entrenador determinan qué equipos y jugadores históricos son tuyos.',\n"
     "  'account.myCoaches': 'Mis entrenadores',\n"
     "  'account.noCoaches': 'Aún no has reclamado entrenadores.',\n"
     "  'account.claimedFromSteam': 'reclamado desde Steam',\n"
     "  'account.manuallyClaimed': 'reclamado manualmente',\n"
     "  'account.releaseClaim': 'Liberar reclamación',\n"
     "  'account.bbVersion': 'Versión de Blood Bowl',\n"
     "  'account.selectCoachNames': 'Selecciona tus nombres de entrenador. Los ID internos se ocultan deliberadamente.',\n"
     "  'account.teamCount': '{count, plural, one {# equipo} other {# equipos}}',\n"
     "  'account.claimSelected': 'Reclamar entrenadores seleccionados',\n"
     "  'account.connectedAs': 'Conectado como {name}',\n"
     "  'account.autoClaim': 'El entrenador BB3 expuesto por esta sesión de Steam se reclama automáticamente si está libre.',\n"
     "  'account.myTeams': 'Mis equipos',\n"
     "  'account.loadingTeams': 'Cargando equipos…',\n"
     "  'account.noTeams': 'No se encontraron equipos de BB3.',\n"
     "  'account.raceTv': 'ID de raza: {race} · TV: {tv}',\n"
     "  'account.disconnect': 'Desconectar sesión',\n"
     "  'account.approveSteam': 'Aprueba el inicio de sesión en la aplicación de Steam y confirma abajo.',\n"
     "  'account.enterGuard': 'Introduce el código de Steam Guard{hint, select, none {} other { enviado a {hint}}}.',\n"
     "  'account.guardCode': 'Código de Steam Guard',\n"
     "  'account.credentialsPrivacy': 'Se recuerda tu nombre de usuario de Steam. Las contraseñas y códigos Guard solo se envían para este inicio de sesión y BlaskScore nunca los almacena.',\n"
     "  'account.steamUsername': 'Usuario de Steam',\n"
     "  'account.steamPassword': 'Contraseña de Steam',\n"
     "  'account.connectSteam': 'Conectar Steam',\n"
     "  'account.claimAdmin': 'Administración de reclamaciones de entrenador',\n"
     "  'account.claimAdminHelp': 'Elimina una reclamación incorrecta para que el usuario correcto pueda reclamar ese entrenador.',\n"
     "  'account.removeClaim': 'Eliminar reclamación',\n"
     "  'account.authFailed': 'Error de autenticación',\n"),
    ("frontend/src/i18n/messages.js",
     "  'disclaimer.checkTerms': 'Tutustu myös käyttöehtoihin ja tietosuojakäytäntöön.',\n",
     "  'disclaimer.checkTerms': 'Tutustu myös käyttöehtoihin ja tietosuojakäytäntöön.',\n"
     "  'status.Registration': 'Ilmoittautuminen',\n"
     "  'status.InProgress': 'Käynnissä',\n"
     "  'status.Finished': 'Päättynyt',\n"
     "  'status.Unknown': 'Tuntematon',\n"
     "  'format.RoundRobin': 'Sarja',\n"
     "  'format.Wissen': 'Sveitsiläinen',\n"
     "  'format.Knockout': 'Pudotuspeli',\n"
     "  'format.Ladder': 'Ladder',\n"
     "  'format.Arena': 'Arena',\n"
     "  'account.heading': 'BlaskScore-tili',\n"
     "  'account.steamOptional': 'Steam on valinnainen. Valmentajaliitokset määrittävät, mitkä historialliset joukkueet ja pelaajat ovat sinun.',\n"
     "  'account.myCoaches': 'Omat valmentajani',\n"
     "  'account.noCoaches': 'Valmentajia ei ole vielä liitetty.',\n"
     "  'account.claimedFromSteam': 'liitetty Steamista',\n"
     "  'account.manuallyClaimed': 'liitetty käsin',\n"
     "  'account.releaseClaim': 'Poista liitos',\n"
     "  'account.bbVersion': 'Blood Bowl -versio',\n"
     "  'account.selectCoachNames': 'Valitse valmentajanimet. Sisäiset valmentajatunnukset piilotetaan tarkoituksella.',\n"
     "  'account.teamCount': '{count, plural, one {# joukkue} other {# joukkuetta}}',\n"
     "  'account.claimSelected': 'Liitä valitut valmentajat',\n"
     "  'account.connectedAs': 'Yhdistetty käyttäjänä {name}',\n"
     "  'account.autoClaim': 'Tämän Steam-istunnon BB3-valmentaja liitetään automaattisesti, jos sitä ei ole jo liitetty.',\n"
     "  'account.myTeams': 'Omat joukkueeni',\n"
     "  'account.loadingTeams': 'Ladataan joukkueita…',\n"
     "  'account.noTeams': 'BB3-joukkueita ei löytynyt.',\n"
     "  'account.raceTv': 'Rotutunnus: {race} · TV: {tv}',\n"
     "  'account.disconnect': 'Katkaise istunto',\n"
     "  'account.approveSteam': 'Hyväksy kirjautuminen Steam-sovelluksessa ja vahvista sitten alla.',\n"
     "  'account.enterGuard': 'Syötä Steam Guard -koodi{hint, select, none {} other {, joka lähetettiin osoitteeseen {hint}}}.',\n"
     "  'account.guardCode': 'Steam Guard -koodi',\n"
     "  'account.credentialsPrivacy': 'Steam-käyttäjänimesi muistetaan. Salasanat ja Guard-koodit lähetetään vain tätä kirjautumista varten eikä BlaskScore koskaan tallenna niitä.',\n"
     "  'account.steamUsername': 'Steam-käyttäjänimi',\n"
     "  'account.steamPassword': 'Steam-salasana',\n"
     "  'account.connectSteam': 'Yhdistä Steam',\n"
     "  'account.claimAdmin': 'Valmentajaliitosten hallinta',\n"
     "  'account.claimAdminHelp': 'Poista virheellinen liitos, jotta oikea käyttäjä voi liittää valmentajan.',\n"
     "  'account.removeClaim': 'Poista liitos',\n"
     "  'account.authFailed': 'Tunnistautuminen epäonnistui',\n"),
    ("frontend/src/i18n/messages.js",
     "  'disclaimer.checkTerms': 'Zapoznaj się również z Warunkami i Polityką prywatności.',\n",
     "  'disclaimer.checkTerms': 'Zapoznaj się również z Warunkami i Polityką prywatności.',\n"
     "  'status.Registration': 'Rejestracja',\n"
     "  'status.InProgress': 'W toku',\n"
     "  'status.Finished': 'Zakończone',\n"
     "  'status.Unknown': 'Nieznane',\n"
     "  'format.RoundRobin': 'Każdy z każdym',\n"
     "  'format.Wissen': 'System szwajcarski',\n"
     "  'format.Knockout': 'Pucharowy',\n"
     "  'format.Ladder': 'Drabinka rankingowa',\n"
     "  'format.Arena': 'Arena',\n"
     "  'account.heading': 'Konto BlaskScore',\n"
     "  'account.steamOptional': 'Steam jest opcjonalny. Przypisania trenerów określają, które historyczne drużyny i zawodnicy są twoi.',\n"
     "  'account.myCoaches': 'Moi trenerzy',\n"
     "  'account.noCoaches': 'Nie przypisano jeszcze trenerów.',\n"
     "  'account.claimedFromSteam': 'przypisano ze Steam',\n"
     "  'account.manuallyClaimed': 'przypisano ręcznie',\n"
     "  'account.releaseClaim': 'Zwolnij przypisanie',\n"
     "  'account.bbVersion': 'Wersja Blood Bowl',\n"
     "  'account.selectCoachNames': 'Wybierz swoje nazwy trenerów. Wewnętrzne identyfikatory są celowo ukryte.',\n"
     "  'account.teamCount': '{count, plural, one {# drużyna} few {# drużyny} other {# drużyn}}',\n"
     "  'account.claimSelected': 'Przypisz wybranych trenerów',\n"
     "  'account.connectedAs': 'Połączono jako {name}',\n"
     "  'account.autoClaim': 'Trener BB3 z tej sesji Steam jest przypisywany automatycznie, jeśli nie jest już zajęty.',\n"
     "  'account.myTeams': 'Moje drużyny',\n"
     "  'account.loadingTeams': 'Ładowanie drużyn…',\n"
     "  'account.noTeams': 'Nie znaleziono drużyn BB3.',\n"
     "  'account.raceTv': 'ID rasy: {race} · TV: {tv}',\n"
     "  'account.disconnect': 'Rozłącz sesję',\n"
     "  'account.approveSteam': 'Zatwierdź logowanie w aplikacji Steam, a następnie potwierdź poniżej.',\n"
     "  'account.enterGuard': 'Wpisz kod Steam Guard{hint, select, none {} other { wysłany do {hint}}}.',\n"
     "  'account.guardCode': 'Kod Steam Guard',\n"
     "  'account.credentialsPrivacy': 'Twoja nazwa użytkownika Steam jest zapamiętywana. Hasła i kody Guard są wysyłane wyłącznie dla tego logowania i nigdy nie są przechowywane przez BlaskScore.',\n"
     "  'account.steamUsername': 'Nazwa użytkownika Steam',\n"
     "  'account.steamPassword': 'Hasło Steam',\n"
     "  'account.connectSteam': 'Połącz Steam',\n"
     "  'account.claimAdmin': 'Administracja przypisaniami trenerów',\n"
     "  'account.claimAdminHelp': 'Usuń błędne przypisanie, aby właściwy użytkownik mógł przypisać tego trenera.',\n"
     "  'account.removeClaim': 'Usuń przypisanie',\n"
     "  'account.authFailed': 'Uwierzytelnianie nie powiodło się',\n"),
    ("frontend/src/components/competition/CompetitionProgress.jsx",
     "  const progressText = `${prettyPrint(status)}${currentRoundText} ${totalRoundsText}`;\n",
     "  const localizedStatus = intl.formatMessage({ id: `status.${status}`, defaultMessage: prettyPrint(status) });\n"
     "  const progressText = `${localizedStatus}${currentRoundText} ${totalRoundsText}`;\n"),
    ("frontend/src/pages/CompetitionPage.jsx",
     "            <InfoItem key=\"Format\" label={intl.formatMessage({ id: 'competition.format' })} info={prettyPrint(competition?.format)} />\n",
     "            <InfoItem key=\"Format\" label={intl.formatMessage({ id: 'competition.format' })} info={intl.formatMessage({ id: `format.${competition?.format}`, defaultMessage: prettyPrint(competition?.format) })} />\n"),
    ("frontend/src/pages/AccountPage.jsx",
     "import { useMyTeams } from '../context/MyTeamsContext';\n",
     "import { useMyTeams } from '../context/MyTeamsContext';\nimport { useIntl } from 'react-intl';\n"),
    ("frontend/src/pages/AccountPage.jsx",
     "export default function AccountPage() {\n  const { user, userPermissions, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();\n",
     "export default function AccountPage() {\n  const intl = useIntl();\n  const { user, userPermissions, getAccessTokenSilently, getAccessTokenWithPopup } = useAuth0WithUserPermissions();\n"),
    ("frontend/src/pages/AccountPage.jsx",
     "reason?.message || 'Authentication failed'",
     "reason?.message || intl.formatMessage({ id: 'account.authFailed' })"),
    ("frontend/src/pages/AccountPage.jsx",
     "<Box><Heading size=\"lg\">BlaskScore account</Heading><Text>{user?.name || user?.email}</Text>\n        <Text fontSize=\"sm\" color=\"gray.500\">Steam is optional. Coach claims determine which historical teams and players are yours.</Text></Box>",
     "<Box><Heading size=\"lg\">{intl.formatMessage({ id: 'account.heading' })}</Heading><Text>{user?.name || user?.email}</Text>\n        <Text fontSize=\"sm\" color=\"gray.500\">{intl.formatMessage({ id: 'account.steamOptional' })}</Text></Box>"),
    ("frontend/src/pages/AccountPage.jsx",
     "<Heading size=\"md\" mb={3}>My coaches</Heading>\n        {claims.length===0?<Text color=\"gray.500\" mb={4}>No coaches claimed yet.</Text>",
     "<Heading size=\"md\" mb={3}>{intl.formatMessage({ id: 'account.myCoaches' })}</Heading>\n        {claims.length===0?<Text color=\"gray.500\" mb={4}>{intl.formatMessage({ id: 'account.noCoaches' })}</Text>"),
    ("frontend/src/pages/AccountPage.jsx",
     "{claim.game} · {claim.source==='STEAM_LOGIN'?'claimed from Steam':'manually claimed'}</Text><Button mt={2} size=\"sm\" variant=\"outline\"",
     "{claim.game} · {intl.formatMessage({ id: claim.source==='STEAM_LOGIN' ? 'account.claimedFromSteam' : 'account.manuallyClaimed' })}</Text><Button mt={2} size=\"sm\" variant=\"outline\""),
    ("frontend/src/pages/AccountPage.jsx", ">Release claim</Button>", ">{intl.formatMessage({ id: 'account.releaseClaim' })}</Button>"),
    ("frontend/src/pages/AccountPage.jsx", "<FormLabel>Blood Bowl version</FormLabel>", "<FormLabel>{intl.formatMessage({ id: 'account.bbVersion' })}</FormLabel>"),
    ("frontend/src/pages/AccountPage.jsx",
     "<Text fontSize=\"sm\" color=\"gray.500\" mb={3}>Select your coach names. Internal coach IDs are deliberately hidden.</Text>",
     "<Text fontSize=\"sm\" color=\"gray.500\" mb={3}>{intl.formatMessage({ id: 'account.selectCoachNames' })}</Text>"),
    ("frontend/src/pages/AccountPage.jsx",
     "{candidate.teamCount} team{candidate.teamCount===1?'':'s'}",
     "{intl.formatMessage({ id: 'account.teamCount' }, { count: candidate.teamCount })}"),
    ("frontend/src/pages/AccountPage.jsx", ">Claim selected coaches</Button>", ">{intl.formatMessage({ id: 'account.claimSelected' })}</Button>"),
    ("frontend/src/pages/AccountPage.jsx",
     "<Text>Connected as <strong>{connection.steamUsername}</strong></Text>\n          <Text fontSize=\"sm\" color=\"gray.500\">The BB3 coach exposed by this Steam session is claimed automatically if unclaimed.</Text>\n          <Heading size=\"sm\" pt={2}>My teams</Heading>",
     "<Text>{intl.formatMessage({ id: 'account.connectedAs' }, { name: connection.steamUsername })}</Text>\n          <Text fontSize=\"sm\" color=\"gray.500\">{intl.formatMessage({ id: 'account.autoClaim' })}</Text>\n          <Heading size=\"sm\" pt={2}>{intl.formatMessage({ id: 'account.myTeams' })}</Heading>"),
    ("frontend/src/pages/AccountPage.jsx",
     "{teamsLoading ? <Text>Loading teams…</Text> : teams.length === 0 ? <Text>No BB3 teams found.</Text>",
     "{teamsLoading ? <Text>{intl.formatMessage({ id: 'account.loadingTeams' })}</Text> : teams.length === 0 ? <Text>{intl.formatMessage({ id: 'account.noTeams' })}</Text>"),
    ("frontend/src/pages/AccountPage.jsx",
     "<Text fontSize=\"sm\">Race ID: {team.raceId ?? 'unknown'} · TV: {team.teamValue ?? 'unknown'}</Text>",
     "<Text fontSize=\"sm\">{intl.formatMessage({ id: 'account.raceTv' }, { race: team.raceId ?? intl.formatMessage({ id: 'common.unknown' }), tv: team.teamValue ?? intl.formatMessage({ id: 'common.unknown' }) })}</Text>"),
    ("frontend/src/pages/AccountPage.jsx", ">Disconnect session</Button>", ">{intl.formatMessage({ id: 'account.disconnect' })}</Button>"),
    ("frontend/src/pages/AccountPage.jsx",
     "<Text>{challenge.method === 'device_confirmation' ? 'Approve the sign-in in the Steam app, then confirm below.' : `Enter the Steam Guard code${challenge.emailHint ? ` sent to ${challenge.emailHint}` : ''}.`}</Text>",
     "<Text>{challenge.method === 'device_confirmation' ? intl.formatMessage({ id: 'account.approveSteam' }) : intl.formatMessage({ id: 'account.enterGuard' }, { hint: challenge.emailHint || 'none' })}</Text>"),
    ("frontend/src/pages/AccountPage.jsx", "<FormLabel>Steam Guard code</FormLabel>", "<FormLabel>{intl.formatMessage({ id: 'account.guardCode' })}</FormLabel>"),
    ("frontend/src/pages/AccountPage.jsx", ">Continue</Button>", ">{intl.formatMessage({ id: 'common.continue' })}</Button>"),
    ("frontend/src/pages/AccountPage.jsx", ">Cancel</Button>", ">{intl.formatMessage({ id: 'common.cancel' })}</Button>"),
    ("frontend/src/pages/AccountPage.jsx",
     "<Text fontSize=\"sm\">Your Steam username is remembered. Passwords and Guard codes are sent only for this login and are never stored by BlaskScore.</Text>",
     "<Text fontSize=\"sm\">{intl.formatMessage({ id: 'account.credentialsPrivacy' })}</Text>"),
    ("frontend/src/pages/AccountPage.jsx", "<FormLabel>Steam username</FormLabel>", "<FormLabel>{intl.formatMessage({ id: 'account.steamUsername' })}</FormLabel>"),
    ("frontend/src/pages/AccountPage.jsx", "<FormLabel>Steam password</FormLabel>", "<FormLabel>{intl.formatMessage({ id: 'account.steamPassword' })}</FormLabel>"),
    ("frontend/src/pages/AccountPage.jsx", ">Connect Steam</Button>", ">{intl.formatMessage({ id: 'account.connectSteam' })}</Button>"),
    ("frontend/src/pages/AccountPage.jsx",
     "<Heading size=\"md\" mb={3}>Coach claims administration</Heading><Text fontSize=\"sm\" color=\"gray.500\" mb={3}>Remove a bad claim; the correct user can then claim that coach.</Text>",
     "<Heading size=\"md\" mb={3}>{intl.formatMessage({ id: 'account.claimAdmin' })}</Heading><Text fontSize=\"sm\" color=\"gray.500\" mb={3}>{intl.formatMessage({ id: 'account.claimAdminHelp' })}</Text>"),
    ("frontend/src/pages/AccountPage.jsx", ">Remove claim</Button>", ">{intl.formatMessage({ id: 'account.removeClaim' })}</Button>"),
]


ADMIN_OPERATIONS = [
    ("frontend/src/i18n/messages.js",
     "  'account.authFailed': 'Authentication failed',\n",
     "  'account.authFailed': 'Authentication failed',\n"
     "  'adminUsers.heading': 'Users and permissions',\n"
     "  'adminUsers.noEmail': 'No email',\n"
     "  'adminUsers.unknownProvider': 'unknown provider',\n"
     "  'adminUsers.siteAdmin': 'Site admin',\n"
     "  'adminUsers.allSystemsAdmin': 'Admin for all LeagueSystems',\n"
     "  'adminUsers.mayRegister': 'May register/import leagues',\n"
     "  'adminUsers.systemAdmin': 'LeagueSystem-specific admin',\n"
     "  'adminUsers.save': 'Save permissions',\n"
     "  'rejected.heading': 'Rejected match articles',\n"
     "  'rejected.help': 'Hidden from the editor view. Site administrators can permanently delete them from the database.',\n"
     "  'rejected.refresh': 'Refresh',\n"
     "  'rejected.badge': 'REJECTED',\n"
     "  'rejected.byMatch': '{author} · match {matchId}',\n"
     "  'rejected.delete': 'Delete permanently',\n"
     "  'rejected.confirmDelete': 'Delete \"{title}\" permanently from the database?',\n"
     "  'rejected.empty': 'No rejected match articles.',\n"),
    ("frontend/src/i18n/messages.js",
     "  'account.authFailed': 'Autentiseringen misslyckades',\n",
     "  'account.authFailed': 'Autentiseringen misslyckades',\n"
     "  'adminUsers.heading': 'Användare och behörigheter',\n"
     "  'adminUsers.noEmail': 'Ingen e-postadress',\n"
     "  'adminUsers.unknownProvider': 'okänd leverantör',\n"
     "  'adminUsers.siteAdmin': 'Siteadmin',\n"
     "  'adminUsers.allSystemsAdmin': 'Admin för alla LeagueSystems',\n"
     "  'adminUsers.mayRegister': 'Får registrera/importera ligor',\n"
     "  'adminUsers.systemAdmin': 'LeagueSystem-specifik admin',\n"
     "  'adminUsers.save': 'Spara behörigheter',\n"
     "  'rejected.heading': 'Refuserade matchartiklar',\n"
     "  'rejected.help': 'Dolda från redaktörsvyn. Siteadmin kan radera dem permanent ur databasen.',\n"
     "  'rejected.refresh': 'Uppdatera',\n"
     "  'rejected.badge': 'REFUSERAD',\n"
     "  'rejected.byMatch': '{author} · match {matchId}',\n"
     "  'rejected.delete': 'Radera permanent',\n"
     "  'rejected.confirmDelete': 'Radera \"{title}\" permanent från databasen?',\n"
     "  'rejected.empty': 'Inga refuserade matchartiklar.',\n"),
    ("frontend/src/i18n/messages.js",
     "  'account.authFailed': 'Error de autenticación',\n",
     "  'account.authFailed': 'Error de autenticación',\n"
     "  'adminUsers.heading': 'Usuarios y permisos',\n"
     "  'adminUsers.noEmail': 'Sin correo electrónico',\n"
     "  'adminUsers.unknownProvider': 'proveedor desconocido',\n"
     "  'adminUsers.siteAdmin': 'Administrador del sitio',\n"
     "  'adminUsers.allSystemsAdmin': 'Administrador de todos los LeagueSystems',\n"
     "  'adminUsers.mayRegister': 'Puede registrar/importar ligas',\n"
     "  'adminUsers.systemAdmin': 'Administrador de LeagueSystems específicos',\n"
     "  'adminUsers.save': 'Guardar permisos',\n"
     "  'rejected.heading': 'Artículos de partido rechazados',\n"
     "  'rejected.help': 'Ocultos de la vista editorial. Los administradores del sitio pueden eliminarlos permanentemente de la base de datos.',\n"
     "  'rejected.refresh': 'Actualizar',\n"
     "  'rejected.badge': 'RECHAZADO',\n"
     "  'rejected.byMatch': '{author} · partido {matchId}',\n"
     "  'rejected.delete': 'Eliminar permanentemente',\n"
     "  'rejected.confirmDelete': '¿Eliminar \"{title}\" permanentemente de la base de datos?',\n"
     "  'rejected.empty': 'No hay artículos de partido rechazados.',\n"),
    ("frontend/src/i18n/messages.js",
     "  'account.authFailed': 'Tunnistautuminen epäonnistui',\n",
     "  'account.authFailed': 'Tunnistautuminen epäonnistui',\n"
     "  'adminUsers.heading': 'Käyttäjät ja oikeudet',\n"
     "  'adminUsers.noEmail': 'Ei sähköpostia',\n"
     "  'adminUsers.unknownProvider': 'tuntematon palveluntarjoaja',\n"
     "  'adminUsers.siteAdmin': 'Sivuston ylläpitäjä',\n"
     "  'adminUsers.allSystemsAdmin': 'Kaikkien LeagueSystems-järjestelmien ylläpitäjä',\n"
     "  'adminUsers.mayRegister': 'Voi rekisteröidä/tuoda liigoja',\n"
     "  'adminUsers.systemAdmin': 'LeagueSystem-kohtainen ylläpito',\n"
     "  'adminUsers.save': 'Tallenna oikeudet',\n"
     "  'rejected.heading': 'Hylätyt otteluartikkelit',\n"
     "  'rejected.help': 'Piilotettu toimitusnäkymästä. Sivuston ylläpitäjä voi poistaa ne pysyvästi tietokannasta.',\n"
     "  'rejected.refresh': 'Päivitä',\n"
     "  'rejected.badge': 'HYLÄTTY',\n"
     "  'rejected.byMatch': '{author} · ottelu {matchId}',\n"
     "  'rejected.delete': 'Poista pysyvästi',\n"
     "  'rejected.confirmDelete': 'Poistetaanko \"{title}\" pysyvästi tietokannasta?',\n"
     "  'rejected.empty': 'Ei hylättyjä otteluartikkeleita.',\n"),
    ("frontend/src/i18n/messages.js",
     "  'account.authFailed': 'Uwierzytelnianie nie powiodło się',\n",
     "  'account.authFailed': 'Uwierzytelnianie nie powiodło się',\n"
     "  'adminUsers.heading': 'Użytkownicy i uprawnienia',\n"
     "  'adminUsers.noEmail': 'Brak adresu e-mail',\n"
     "  'adminUsers.unknownProvider': 'nieznany dostawca',\n"
     "  'adminUsers.siteAdmin': 'Administrator serwisu',\n"
     "  'adminUsers.allSystemsAdmin': 'Administrator wszystkich LeagueSystems',\n"
     "  'adminUsers.mayRegister': 'Może rejestrować/importować ligi',\n"
     "  'adminUsers.systemAdmin': 'Administrator wybranych LeagueSystems',\n"
     "  'adminUsers.save': 'Zapisz uprawnienia',\n"
     "  'rejected.heading': 'Odrzucone artykuły meczowe',\n"
     "  'rejected.help': 'Ukryte w widoku redakcyjnym. Administrator serwisu może je trwale usunąć z bazy danych.',\n"
     "  'rejected.refresh': 'Odśwież',\n"
     "  'rejected.badge': 'ODRZUCONY',\n"
     "  'rejected.byMatch': '{author} · mecz {matchId}',\n"
     "  'rejected.delete': 'Usuń trwale',\n"
     "  'rejected.confirmDelete': 'Usunąć \"{title}\" trwale z bazy danych?',\n"
     "  'rejected.empty': 'Brak odrzuconych artykułów meczowych.',\n"),
    ("frontend/src/components/admin/SiteUserAdmin.jsx",
     "import WarpScoresApiService from '../../WarpScoresApiService';\n",
     "import WarpScoresApiService from '../../WarpScoresApiService';\nimport { useIntl } from 'react-intl';\n"),
    ("frontend/src/components/admin/SiteUserAdmin.jsx",
     "export default function SiteUserAdmin({ auth }) {\n",
     "export default function SiteUserAdmin({ auth }) {\n  const intl = useIntl();\n"),
    ("frontend/src/components/admin/SiteUserAdmin.jsx", ">Users and permissions</Heading>", ">{intl.formatMessage({ id: 'adminUsers.heading' })}</Heading>"),
    ("frontend/src/components/admin/SiteUserAdmin.jsx",
     "{draft.email || 'No email'} · {draft.provider || 'unknown provider'}",
     "{draft.email || intl.formatMessage({ id: 'adminUsers.noEmail' })} · {draft.provider || intl.formatMessage({ id: 'adminUsers.unknownProvider' })}"),
    ("frontend/src/components/admin/SiteUserAdmin.jsx", ">Site admin</Checkbox>", ">{intl.formatMessage({ id: 'adminUsers.siteAdmin' })}</Checkbox>"),
    ("frontend/src/components/admin/SiteUserAdmin.jsx", ">Admin for all LeagueSystems</Checkbox>", ">{intl.formatMessage({ id: 'adminUsers.allSystemsAdmin' })}</Checkbox>"),
    ("frontend/src/components/admin/SiteUserAdmin.jsx", ">May register/import leagues</Checkbox>", ">{intl.formatMessage({ id: 'adminUsers.mayRegister' })}</Checkbox>"),
    ("frontend/src/components/admin/SiteUserAdmin.jsx", ">LeagueSystem-specific admin</Text>", ">{intl.formatMessage({ id: 'adminUsers.systemAdmin' })}</Text>"),
    ("frontend/src/components/admin/SiteUserAdmin.jsx", ">Save permissions</Button>", ">{intl.formatMessage({ id: 'adminUsers.save' })}</Button>"),
    ("frontend/src/components/admin/RejectedMatchArticlesAdmin.jsx",
     "import EditorialCommunityApi from '../../EditorialCommunityApi';\n",
     "import EditorialCommunityApi from '../../EditorialCommunityApi';\nimport { useIntl } from 'react-intl';\n"),
    ("frontend/src/components/admin/RejectedMatchArticlesAdmin.jsx",
     "export default function RejectedMatchArticlesAdmin({ getAccessTokenSilently, onError }) {\n",
     "export default function RejectedMatchArticlesAdmin({ getAccessTokenSilently, onError }) {\n  const intl = useIntl();\n"),
    ("frontend/src/components/admin/RejectedMatchArticlesAdmin.jsx",
     "if (!window.confirm(`Radera \"${article.title}\" permanent från databasen?`)) return;",
     "if (!window.confirm(intl.formatMessage({ id: 'rejected.confirmDelete' }, { title: article.title }))) return;"),
    ("frontend/src/components/admin/RejectedMatchArticlesAdmin.jsx", ">Refuserade matchartiklar</Heading>", ">{intl.formatMessage({ id: 'rejected.heading' })}</Heading>"),
    ("frontend/src/components/admin/RejectedMatchArticlesAdmin.jsx",
     "Dolda från redaktörsvyn. Siteadmin kan radera dem permanent ur databasen.",
     "{intl.formatMessage({ id: 'rejected.help' })}"),
    ("frontend/src/components/admin/RejectedMatchArticlesAdmin.jsx", ">Uppdatera</Button>", ">{intl.formatMessage({ id: 'rejected.refresh' })}</Button>"),
    ("frontend/src/components/admin/RejectedMatchArticlesAdmin.jsx", ">REJECTED</Badge>", ">{intl.formatMessage({ id: 'rejected.badge' })}</Badge>"),
    ("frontend/src/components/admin/RejectedMatchArticlesAdmin.jsx",
     "{article.authorDisplayName || article.authorSubject} · match {article.matchId}",
     "{intl.formatMessage({ id: 'rejected.byMatch' }, { author: article.authorDisplayName || article.authorSubject, matchId: article.matchId })}"),
    ("frontend/src/components/admin/RejectedMatchArticlesAdmin.jsx", "Radera permanent\n", "{intl.formatMessage({ id: 'rejected.delete' })}\n"),
    ("frontend/src/components/admin/RejectedMatchArticlesAdmin.jsx",
     "{!loading && articles.length === 0 && <Text color=\"gray.500\">Inga refuserade matchartiklar.</Text>}",
     "{!loading && articles.length === 0 && <Text color=\"gray.500\">{intl.formatMessage({ id: 'rejected.empty' })}</Text>}"),
]

FINAL_OPERATIONS = [('frontend/src/i18n/messages.js',
  "  'rejected.empty': 'No rejected match articles.',\n",
  "  'rejected.empty': 'No rejected match articles.',\n"
  "  'about.heading': 'About',\n"
  "  'about.fallback': 'This is a Spike-like facade for BB3 data provided by Cyanide’s BB3 API.',\n"
  "  'staff.heading': 'BlaskScore staff',\n"
  "  'staff.description': 'The AI reporters covering Blood Bowl for BlaskScore.',\n"
  "  'staff.loading': 'Loading staff…',\n"
  "  'reporter.untitled': 'Untitled report',\n"
  "  'reporter.staff': 'BlaskScore staff',\n"
  "  'reporter.aiReporter': 'AI reporter',\n"
  "  'reporter.inactive': 'Inactive',\n"
  "  'reporter.settingsFor': 'Settings for {name}',\n"
  "  'reporter.settings': 'Reporter settings',\n"
  "  'reporter.about': 'About {name}',\n"
  "  'reporter.latestReports': 'Latest reports',\n"
  "  'reporter.noReports': 'No published reports yet.',\n"
  "  'aiAdmin.heading': 'AI reporters',\n"
  "  'aiAdmin.help': 'Runtime overrides for all AI reporters.',\n"
  "  'aiAdmin.defaultArticleLanguage': 'Default article language',\n"
  "  'aiAdmin.defaultLanguageHelp': 'Used when a reporter has no profile or runtime language override.',\n"
  "  'aiAdmin.reporter': 'Reporter',\n"
  "  'aiAdmin.enabled': 'Enabled',\n"
  "  'aiAdmin.reports': 'Reports',\n"
  "  'aiAdmin.interactions': 'Interactions',\n"
  "  'aiAdmin.ratings': 'Ratings',\n"
  "  'aiAdmin.language': 'Language',\n"
  "  'aiAdmin.writingWeight': 'Writing weight',\n"
  "  'aiAdmin.inherit': 'Inherit ({language})',\n"
  "  'aiAdmin.effective': 'Effective: {language}',\n"
  "  'aiAdmin.reset': 'Reset',\n"
  "  'replay.loading': 'Loading replay service…',\n"
  "  'replay.heading': 'BB3 replay sweeper',\n"
  "  'replay.ticketExpired': 'The Steam ticket has expired and must be renewed.',\n"
  "  'replay.notAuthenticated': 'No replay service Steam account has been authenticated.',\n"
  "  'replay.enabled': 'Enabled',\n"
  "  'replay.steamUsername': 'Steam username',\n"
  "  'replay.cron': 'Cron',\n"
  "  'replay.timeZone': 'Time zone',\n"
  "  'replay.batchSize': 'Batch size',\n"
  "  'replay.save': 'Save',\n"
  "  'replay.sync': 'Sync replays',\n"
  "  'replay.scan': 'Scan matches (Cyanide API, pybb3 fallback)',\n"
  "  'replay.defaultSchedule': 'Default: 05:00 Europe/Stockholm. Last run: {lastRun} · downloaded: {downloaded}',\n"
  "  'replay.never': 'never',\n"
  "  'replay.approveSteam': 'Approve in the Steam app, then continue.',\n"
  "  'replay.enterGuard': 'Enter the Steam Guard code.',\n"
  "  'replay.continue': 'Continue',\n"
  "  'replay.steamPassword': 'Steam password',\n"
  "  'replay.authenticate': 'Authenticate replay account',\n"
  "  'replay.artifacts': 'Replay artifacts and analysis',\n"
  "  'replay.importFiles': 'Import local .bbr files',\n"
  "  'replay.import': 'Import {count}',\n"
  "  'replay.imported': 'IMPORTED',\n"
  "  'replay.skipped': 'SKIPPED',\n"
  "  'replay.windowHelp': 'Only recent attempts inside the configured replay availability window are shown. Failed recent downloads are at "
  "risk of being lost when Steam removes them.',\n"
  "  'replay.match': 'Match',\n"
  "  'replay.played': 'Played',\n"
  "  'replay.replay': 'Replay',\n"
  "  'replay.analysis': 'Analysis',\n"
  "  'replay.sizes': 'Original / compact',\n"
  "  'replay.unknownMatch': 'Unknown match',\n"
  "  'replay.saved': 'SAVED',\n"
  "  'replay.missingLocal': 'MISSING LOCALLY',\n"
  "  'replay.atRisk': 'AT RISK',\n"
  "  'replay.pending': 'PENDING',\n"
  "  'replay.notAvailable': 'NOT AVAILABLE',\n"
  "  'replay.inspect': 'Inspect',\n"
  "  'replay.analyzeAgain': 'Analyze again',\n"
  "  'replay.adminLog': 'Administrator log',\n"
  "  'replay.noRuns': 'No replay runs logged yet.',\n"
  "  'replay.parsed': 'Parsed replay · {match}',\n"
  "  'replay.parserWarning': 'Parser v1 is structurally lossless but its statistical interpretation is experimental. Unknown enums are "
  "shown raw.',\n"
  "  'replay.copyJson': 'Copy full JSON',\n"
  "  'replay.downloadJson': 'Download inspect JSON',\n"
  "  'replay.previewHelp': 'Preview is limited to 100,000 characters; Copy and Download contain the complete parsed replay.',\n"
  "  'replay.minutesAgo': '{count} min ago',\n"
  "  'replay.hoursAgo': '{count} h ago',\n"
  "  'replay.daysAgo': '{count} days ago',\n"),
 ('frontend/src/i18n/messages.js',
  "  'rejected.empty': 'Inga refuserade matchartiklar.',\n",
  "  'rejected.empty': 'Inga refuserade matchartiklar.',\n"
  "  'about.heading': 'Om',\n"
  "  'about.fallback': 'Det här är ett Spike-liknande gränssnitt för BB3-data från Cyanides BB3-API.',\n"
  "  'staff.heading': 'BlaskScores redaktion',\n"
  "  'staff.description': 'AI-reportrarna som bevakar Blood Bowl för BlaskScore.',\n"
  "  'staff.loading': 'Laddar redaktionen…',\n"
  "  'reporter.untitled': 'Namnlös rapport',\n"
  "  'reporter.staff': 'BlaskScores redaktion',\n"
  "  'reporter.aiReporter': 'AI-reporter',\n"
  "  'reporter.inactive': 'Inaktiv',\n"
  "  'reporter.settingsFor': 'Inställningar för {name}',\n"
  "  'reporter.settings': 'Reporterinställningar',\n"
  "  'reporter.about': 'Om {name}',\n"
  "  'reporter.latestReports': 'Senaste rapporter',\n"
  "  'reporter.noReports': 'Inga publicerade rapporter ännu.',\n"
  "  'aiAdmin.heading': 'AI-reportrar',\n"
  "  'aiAdmin.help': 'Körtidsinställningar för alla AI-reportrar.',\n"
  "  'aiAdmin.defaultArticleLanguage': 'Standardspråk för artiklar',\n"
  "  'aiAdmin.defaultLanguageHelp': 'Används när reportern saknar språkval i profil eller körtidsinställning.',\n"
  "  'aiAdmin.reporter': 'Reporter',\n"
  "  'aiAdmin.enabled': 'Aktiv',\n"
  "  'aiAdmin.reports': 'Rapporter',\n"
  "  'aiAdmin.interactions': 'Interaktioner',\n"
  "  'aiAdmin.ratings': 'Betyg',\n"
  "  'aiAdmin.language': 'Språk',\n"
  "  'aiAdmin.writingWeight': 'Skrivvikt',\n"
  "  'aiAdmin.inherit': 'Ärv ({language})',\n"
  "  'aiAdmin.effective': 'Aktuellt: {language}',\n"
  "  'aiAdmin.reset': 'Återställ',\n"
  "  'replay.loading': 'Laddar replaytjänsten…',\n"
  "  'replay.heading': 'BB3 replay-sweeper',\n"
  "  'replay.ticketExpired': 'Steam-biljetten har gått ut och måste förnyas.',\n"
  "  'replay.notAuthenticated': 'Inget Steam-konto har autentiserats för replaytjänsten.',\n"
  "  'replay.enabled': 'Aktiverad',\n"
  "  'replay.steamUsername': 'Steam-användarnamn',\n"
  "  'replay.cron': 'Cron',\n"
  "  'replay.timeZone': 'Tidszon',\n"
  "  'replay.batchSize': 'Batchstorlek',\n"
  "  'replay.save': 'Spara',\n"
  "  'replay.sync': 'Synka replays',\n"
  "  'replay.scan': 'Skanna matcher (Cyanide API, pybb3 fallback)',\n"
  "  'replay.defaultSchedule': 'Standard: 05:00 Europe/Stockholm. Senaste körning: {lastRun} · hämtade: {downloaded}',\n"
  "  'replay.never': 'aldrig',\n"
  "  'replay.approveSteam': 'Godkänn i Steam-appen och fortsätt sedan.',\n"
  "  'replay.enterGuard': 'Ange Steam Guard-koden.',\n"
  "  'replay.continue': 'Fortsätt',\n"
  "  'replay.steamPassword': 'Steam-lösenord',\n"
  "  'replay.authenticate': 'Autentisera replaykontot',\n"
  "  'replay.artifacts': 'Replayfiler och analys',\n"
  "  'replay.importFiles': 'Importera lokala .bbr-filer',\n"
  "  'replay.import': 'Importera {count}',\n"
  "  'replay.imported': 'IMPORTERAD',\n"
  "  'replay.skipped': 'HOPPAD ÖVER',\n"
  "  'replay.windowHelp': 'Endast nya försök inom det konfigurerade replayfönstret visas. Misslyckade hämtningar riskerar att gå förlorade "
  "när Steam tar bort dem.',\n"
  "  'replay.match': 'Match',\n"
  "  'replay.played': 'Spelad',\n"
  "  'replay.replay': 'Replay',\n"
  "  'replay.analysis': 'Analys',\n"
  "  'replay.sizes': 'Original / kompakt',\n"
  "  'replay.unknownMatch': 'Okänd match',\n"
  "  'replay.saved': 'SPARAD',\n"
  "  'replay.missingLocal': 'SAKNAS LOKALT',\n"
  "  'replay.atRisk': 'RISKERAR ATT FÖRLORAS',\n"
  "  'replay.pending': 'VÄNTAR',\n"
  "  'replay.notAvailable': 'INTE TILLGÄNGLIG',\n"
  "  'replay.inspect': 'Inspektera',\n"
  "  'replay.analyzeAgain': 'Analysera igen',\n"
  "  'replay.adminLog': 'Administratörslogg',\n"
  "  'replay.noRuns': 'Inga replaykörningar loggade ännu.',\n"
  "  'replay.parsed': 'Tolkad replay · {match}',\n"
  "  'replay.parserWarning': 'Parser v1 bevarar strukturen utan förlust men den statistiska tolkningen är experimentell. Okända "
  "enum-värden visas råa.',\n"
  "  'replay.copyJson': 'Kopiera full JSON',\n"
  "  'replay.downloadJson': 'Ladda ned inspect-JSON',\n"
  "  'replay.previewHelp': 'Förhandsvisningen är begränsad till 100 000 tecken; Kopiera och Ladda ned innehåller hela den tolkade "
  "replayen.',\n"
  "  'replay.minutesAgo': 'för {count} min sedan',\n"
  "  'replay.hoursAgo': 'för {count} h sedan',\n"
  "  'replay.daysAgo': 'för {count} dagar sedan',\n"),
 ('frontend/src/i18n/messages.js',
  "  'rejected.empty': 'No hay artículos de partido rechazados.',\n",
  "  'rejected.empty': 'No hay artículos de partido rechazados.',\n"
  "  'about.heading': 'Acerca de',\n"
  "  'about.fallback': 'Esta es una interfaz al estilo Spike para datos de BB3 proporcionados por la API de BB3 de Cyanide.',\n"
  "  'staff.heading': 'Redacción de BlaskScore',\n"
  "  'staff.description': 'Los reporteros de IA que cubren Blood Bowl para BlaskScore.',\n"
  "  'staff.loading': 'Cargando redacción…',\n"
  "  'reporter.untitled': 'Informe sin título',\n"
  "  'reporter.staff': 'Redacción de BlaskScore',\n"
  "  'reporter.aiReporter': 'Reportero de IA',\n"
  "  'reporter.inactive': 'Inactivo',\n"
  "  'reporter.settingsFor': 'Configuración de {name}',\n"
  "  'reporter.settings': 'Configuración del reportero',\n"
  "  'reporter.about': 'Acerca de {name}',\n"
  "  'reporter.latestReports': 'Últimos informes',\n"
  "  'reporter.noReports': 'Aún no hay informes publicados.',\n"
  "  'aiAdmin.heading': 'Reporteros de IA',\n"
  "  'aiAdmin.help': 'Anulaciones en tiempo de ejecución para todos los reporteros de IA.',\n"
  "  'aiAdmin.defaultArticleLanguage': 'Idioma predeterminado de los artículos',\n"
  "  'aiAdmin.defaultLanguageHelp': 'Se usa cuando un reportero no tiene idioma definido en su perfil o en tiempo de ejecución.',\n"
  "  'aiAdmin.reporter': 'Reportero',\n"
  "  'aiAdmin.enabled': 'Activo',\n"
  "  'aiAdmin.reports': 'Informes',\n"
  "  'aiAdmin.interactions': 'Interacciones',\n"
  "  'aiAdmin.ratings': 'Valoraciones',\n"
  "  'aiAdmin.language': 'Idioma',\n"
  "  'aiAdmin.writingWeight': 'Peso de escritura',\n"
  "  'aiAdmin.inherit': 'Heredar ({language})',\n"
  "  'aiAdmin.effective': 'Efectivo: {language}',\n"
  "  'aiAdmin.reset': 'Restablecer',\n"
  "  'replay.loading': 'Cargando servicio de repeticiones…',\n"
  "  'replay.heading': 'Barrido de repeticiones BB3',\n"
  "  'replay.ticketExpired': 'El ticket de Steam ha caducado y debe renovarse.',\n"
  "  'replay.notAuthenticated': 'No se ha autenticado ninguna cuenta de Steam para el servicio de repeticiones.',\n"
  "  'replay.enabled': 'Activado',\n"
  "  'replay.steamUsername': 'Usuario de Steam',\n"
  "  'replay.cron': 'Cron',\n"
  "  'replay.timeZone': 'Zona horaria',\n"
  "  'replay.batchSize': 'Tamaño del lote',\n"
  "  'replay.save': 'Guardar',\n"
  "  'replay.sync': 'Sincronizar repeticiones',\n"
  "  'replay.scan': 'Buscar partidos (API de Cyanide, fallback pybb3)',\n"
  "  'replay.defaultSchedule': 'Predeterminado: 05:00 Europe/Stockholm. Última ejecución: {lastRun} · descargadas: {downloaded}',\n"
  "  'replay.never': 'nunca',\n"
  "  'replay.approveSteam': 'Aprueba en la aplicación de Steam y continúa.',\n"
  "  'replay.enterGuard': 'Introduce el código de Steam Guard.',\n"
  "  'replay.continue': 'Continuar',\n"
  "  'replay.steamPassword': 'Contraseña de Steam',\n"
  "  'replay.authenticate': 'Autenticar cuenta de repeticiones',\n"
  "  'replay.artifacts': 'Repeticiones y análisis',\n"
  "  'replay.importFiles': 'Importar archivos .bbr locales',\n"
  "  'replay.import': 'Importar {count}',\n"
  "  'replay.imported': 'IMPORTADO',\n"
  "  'replay.skipped': 'OMITIDO',\n"
  "  'replay.windowHelp': 'Solo se muestran intentos recientes dentro de la ventana de disponibilidad configurada. Las descargas fallidas "
  "pueden perderse cuando Steam las elimine.',\n"
  "  'replay.match': 'Partido',\n"
  "  'replay.played': 'Jugado',\n"
  "  'replay.replay': 'Repetición',\n"
  "  'replay.analysis': 'Análisis',\n"
  "  'replay.sizes': 'Original / compacto',\n"
  "  'replay.unknownMatch': 'Partido desconocido',\n"
  "  'replay.saved': 'GUARDADO',\n"
  "  'replay.missingLocal': 'FALTA LOCALMENTE',\n"
  "  'replay.atRisk': 'EN RIESGO',\n"
  "  'replay.pending': 'PENDIENTE',\n"
  "  'replay.notAvailable': 'NO DISPONIBLE',\n"
  "  'replay.inspect': 'Inspeccionar',\n"
  "  'replay.analyzeAgain': 'Analizar de nuevo',\n"
  "  'replay.adminLog': 'Registro de administración',\n"
  "  'replay.noRuns': 'Aún no hay ejecuciones registradas.',\n"
  "  'replay.parsed': 'Repetición analizada · {match}',\n"
  "  'replay.parserWarning': 'El parser v1 conserva la estructura sin pérdidas, pero su interpretación estadística es experimental. Los "
  "enums desconocidos se muestran sin modificar.',\n"
  "  'replay.copyJson': 'Copiar JSON completo',\n"
  "  'replay.downloadJson': 'Descargar JSON de inspección',\n"
  "  'replay.previewHelp': 'La vista previa está limitada a 100 000 caracteres; Copiar y Descargar incluyen la repetición analizada "
  "completa.',\n"
  "  'replay.minutesAgo': 'hace {count} min',\n"
  "  'replay.hoursAgo': 'hace {count} h',\n"
  "  'replay.daysAgo': 'hace {count} días',\n"),
 ('frontend/src/i18n/messages.js',
  "  'rejected.empty': 'Ei hylättyjä otteluartikkeleita.',\n",
  "  'rejected.empty': 'Ei hylättyjä otteluartikkeleita.',\n"
  "  'about.heading': 'Tietoja',\n"
  "  'about.fallback': 'Tämä on Spike-tyylinen käyttöliittymä Cyaniden BB3-API:n tarjoamalle BB3-datalle.',\n"
  "  'staff.heading': 'BlaskScoren toimitus',\n"
  "  'staff.description': 'BlaskScoren Blood Bowlia seuraavat tekoälytoimittajat.',\n"
  "  'staff.loading': 'Ladataan toimitusta…',\n"
  "  'reporter.untitled': 'Nimetön raportti',\n"
  "  'reporter.staff': 'BlaskScoren toimitus',\n"
  "  'reporter.aiReporter': 'Tekoälytoimittaja',\n"
  "  'reporter.inactive': 'Ei aktiivinen',\n"
  "  'reporter.settingsFor': 'Käyttäjän {name} asetukset',\n"
  "  'reporter.settings': 'Toimittajan asetukset',\n"
  "  'reporter.about': 'Tietoja: {name}',\n"
  "  'reporter.latestReports': 'Uusimmat raportit',\n"
  "  'reporter.noReports': 'Julkaistuja raportteja ei vielä ole.',\n"
  "  'aiAdmin.heading': 'Tekoälytoimittajat',\n"
  "  'aiAdmin.help': 'Kaikkien tekoälytoimittajien ajonaikaiset ohitukset.',\n"
  "  'aiAdmin.defaultArticleLanguage': 'Artikkelien oletuskieli',\n"
  "  'aiAdmin.defaultLanguageHelp': 'Käytetään, kun toimittajalla ei ole profiilin tai ajonaikaista kieliohitusta.',\n"
  "  'aiAdmin.reporter': 'Toimittaja',\n"
  "  'aiAdmin.enabled': 'Käytössä',\n"
  "  'aiAdmin.reports': 'Raportit',\n"
  "  'aiAdmin.interactions': 'Vuorovaikutus',\n"
  "  'aiAdmin.ratings': 'Arviot',\n"
  "  'aiAdmin.language': 'Kieli',\n"
  "  'aiAdmin.writingWeight': 'Kirjoituspaino',\n"
  "  'aiAdmin.inherit': 'Peri ({language})',\n"
  "  'aiAdmin.effective': 'Käytössä: {language}',\n"
  "  'aiAdmin.reset': 'Palauta',\n"
  "  'replay.loading': 'Ladataan replay-palvelua…',\n"
  "  'replay.heading': 'BB3 replay -kerääjä',\n"
  "  'replay.ticketExpired': 'Steam-lippu on vanhentunut ja se on uusittava.',\n"
  "  'replay.notAuthenticated': 'Replay-palvelulle ei ole autentikoitu Steam-tiliä.',\n"
  "  'replay.enabled': 'Käytössä',\n"
  "  'replay.steamUsername': 'Steam-käyttäjänimi',\n"
  "  'replay.cron': 'Cron',\n"
  "  'replay.timeZone': 'Aikavyöhyke',\n"
  "  'replay.batchSize': 'Eräkoko',\n"
  "  'replay.save': 'Tallenna',\n"
  "  'replay.sync': 'Synkronoi replayt',\n"
  "  'replay.scan': 'Etsi ottelut (Cyanide API, pybb3-vararatkaisu)',\n"
  "  'replay.defaultSchedule': 'Oletus: 05:00 Europe/Stockholm. Viimeisin ajo: {lastRun} · ladattu: {downloaded}',\n"
  "  'replay.never': 'ei koskaan',\n"
  "  'replay.approveSteam': 'Hyväksy Steam-sovelluksessa ja jatka sitten.',\n"
  "  'replay.enterGuard': 'Syötä Steam Guard -koodi.',\n"
  "  'replay.continue': 'Jatka',\n"
  "  'replay.steamPassword': 'Steam-salasana',\n"
  "  'replay.authenticate': 'Autentikoi replay-tili',\n"
  "  'replay.artifacts': 'Replayt ja analyysi',\n"
  "  'replay.importFiles': 'Tuo paikallisia .bbr-tiedostoja',\n"
  "  'replay.import': 'Tuo {count}',\n"
  "  'replay.imported': 'TUOTU',\n"
  "  'replay.skipped': 'OHITETTU',\n"
  "  'replay.windowHelp': 'Vain määritetyn replay-saatavuusikkunan viimeisimmät yritykset näytetään. Epäonnistuneet lataukset voivat "
  "kadota, kun Steam poistaa ne.',\n"
  "  'replay.match': 'Ottelu',\n"
  "  'replay.played': 'Pelattu',\n"
  "  'replay.replay': 'Replay',\n"
  "  'replay.analysis': 'Analyysi',\n"
  "  'replay.sizes': 'Alkuperäinen / kompakti',\n"
  "  'replay.unknownMatch': 'Tuntematon ottelu',\n"
  "  'replay.saved': 'TALLENNETTU',\n"
  "  'replay.missingLocal': 'PUUTTUU PAIKALLISESTI',\n"
  "  'replay.atRisk': 'VAARASSA',\n"
  "  'replay.pending': 'ODOTTAA',\n"
  "  'replay.notAvailable': 'EI SAATAVILLA',\n"
  "  'replay.inspect': 'Tarkasta',\n"
  "  'replay.analyzeAgain': 'Analysoi uudelleen',\n"
  "  'replay.adminLog': 'Ylläpitoloki',\n"
  "  'replay.noRuns': 'Replay-ajoja ei ole vielä lokissa.',\n"
  "  'replay.parsed': 'Jäsennetty replay · {match}',\n"
  "  'replay.parserWarning': 'Parser v1 säilyttää rakenteen häviöttömästi, mutta tilastollinen tulkinta on kokeellinen. Tuntemattomat "
  "enum-arvot näytetään sellaisinaan.',\n"
  "  'replay.copyJson': 'Kopioi koko JSON',\n"
  "  'replay.downloadJson': 'Lataa tarkastus-JSON',\n"
  "  'replay.previewHelp': 'Esikatselu on rajattu 100 000 merkkiin; Kopioi ja Lataa sisältävät koko jäsennetyn replayn.',\n"
  "  'replay.minutesAgo': '{count} min sitten',\n"
  "  'replay.hoursAgo': '{count} h sitten',\n"
  "  'replay.daysAgo': '{count} päivää sitten',\n"),
 ('frontend/src/i18n/messages.js',
  "  'rejected.empty': 'Brak odrzuconych artykułów meczowych.',\n",
  "  'rejected.empty': 'Brak odrzuconych artykułów meczowych.',\n"
  "  'about.heading': 'O serwisie',\n"
  "  'about.fallback': 'To interfejs w stylu Spike dla danych BB3 udostępnianych przez API BB3 firmy Cyanide.',\n"
  "  'staff.heading': 'Redakcja BlaskScore',\n"
  "  'staff.description': 'Reporterzy AI relacjonujący Blood Bowl dla BlaskScore.',\n"
  "  'staff.loading': 'Ładowanie redakcji…',\n"
  "  'reporter.untitled': 'Raport bez tytułu',\n"
  "  'reporter.staff': 'Redakcja BlaskScore',\n"
  "  'reporter.aiReporter': 'Reporter AI',\n"
  "  'reporter.inactive': 'Nieaktywny',\n"
  "  'reporter.settingsFor': 'Ustawienia dla {name}',\n"
  "  'reporter.settings': 'Ustawienia reportera',\n"
  "  'reporter.about': 'O {name}',\n"
  "  'reporter.latestReports': 'Najnowsze raporty',\n"
  "  'reporter.noReports': 'Brak opublikowanych raportów.',\n"
  "  'aiAdmin.heading': 'Reporterzy AI',\n"
  "  'aiAdmin.help': 'Nadpisania ustawień wykonawczych dla wszystkich reporterów AI.',\n"
  "  'aiAdmin.defaultArticleLanguage': 'Domyślny język artykułów',\n"
  "  'aiAdmin.defaultLanguageHelp': 'Używany, gdy reporter nie ma języka ustawionego w profilu ani w ustawieniach wykonawczych.',\n"
  "  'aiAdmin.reporter': 'Reporter',\n"
  "  'aiAdmin.enabled': 'Aktywny',\n"
  "  'aiAdmin.reports': 'Raporty',\n"
  "  'aiAdmin.interactions': 'Interakcje',\n"
  "  'aiAdmin.ratings': 'Oceny',\n"
  "  'aiAdmin.language': 'Język',\n"
  "  'aiAdmin.writingWeight': 'Waga pisania',\n"
  "  'aiAdmin.inherit': 'Dziedzicz ({language})',\n"
  "  'aiAdmin.effective': 'Efektywny: {language}',\n"
  "  'aiAdmin.reset': 'Resetuj',\n"
  "  'replay.loading': 'Ładowanie usługi replay…',\n"
  "  'replay.heading': 'Zbieranie replayów BB3',\n"
  "  'replay.ticketExpired': 'Bilet Steam wygasł i musi zostać odnowiony.',\n"
  "  'replay.notAuthenticated': 'Nie uwierzytelniono konta Steam dla usługi replay.',\n"
  "  'replay.enabled': 'Włączone',\n"
  "  'replay.steamUsername': 'Nazwa użytkownika Steam',\n"
  "  'replay.cron': 'Cron',\n"
  "  'replay.timeZone': 'Strefa czasowa',\n"
  "  'replay.batchSize': 'Rozmiar partii',\n"
  "  'replay.save': 'Zapisz',\n"
  "  'replay.sync': 'Synchronizuj replaye',\n"
  "  'replay.scan': 'Skanuj mecze (API Cyanide, fallback pybb3)',\n"
  "  'replay.defaultSchedule': 'Domyślnie: 05:00 Europe/Stockholm. Ostatnie uruchomienie: {lastRun} · pobrano: {downloaded}',\n"
  "  'replay.never': 'nigdy',\n"
  "  'replay.approveSteam': 'Zatwierdź w aplikacji Steam, a następnie kontynuuj.',\n"
  "  'replay.enterGuard': 'Wpisz kod Steam Guard.',\n"
  "  'replay.continue': 'Kontynuuj',\n"
  "  'replay.steamPassword': 'Hasło Steam',\n"
  "  'replay.authenticate': 'Uwierzytelnij konto replay',\n"
  "  'replay.artifacts': 'Replay i analiza',\n"
  "  'replay.importFiles': 'Importuj lokalne pliki .bbr',\n"
  "  'replay.import': 'Importuj {count}',\n"
  "  'replay.imported': 'ZAIMPORTOWANO',\n"
  "  'replay.skipped': 'POMINIĘTO',\n"
  "  'replay.windowHelp': 'Pokazywane są tylko ostatnie próby z ustawionego okna dostępności replayów. Nieudane pobrania mogą zostać "
  "utracone, gdy Steam je usunie.',\n"
  "  'replay.match': 'Mecz',\n"
  "  'replay.played': 'Rozegrano',\n"
  "  'replay.replay': 'Replay',\n"
  "  'replay.analysis': 'Analiza',\n"
  "  'replay.sizes': 'Oryginał / kompakt',\n"
  "  'replay.unknownMatch': 'Nieznany mecz',\n"
  "  'replay.saved': 'ZAPISANO',\n"
  "  'replay.missingLocal': 'BRAK LOKALNIE',\n"
  "  'replay.atRisk': 'ZAGROŻONE',\n"
  "  'replay.pending': 'OCZEKUJE',\n"
  "  'replay.notAvailable': 'NIEDOSTĘPNE',\n"
  "  'replay.inspect': 'Sprawdź',\n"
  "  'replay.analyzeAgain': 'Analizuj ponownie',\n"
  "  'replay.adminLog': 'Dziennik administratora',\n"
  "  'replay.noRuns': 'Brak zarejestrowanych uruchomień replay.',\n"
  "  'replay.parsed': 'Przetworzony replay · {match}',\n"
  "  'replay.parserWarning': 'Parser v1 bezstratnie zachowuje strukturę, ale jego interpretacja statystyczna jest eksperymentalna. "
  "Nieznane wartości enum są wyświetlane bez zmian.',\n"
  "  'replay.copyJson': 'Kopiuj pełny JSON',\n"
  "  'replay.downloadJson': 'Pobierz JSON inspekcji',\n"
  "  'replay.previewHelp': 'Podgląd jest ograniczony do 100 000 znaków; Kopiuj i Pobierz zawierają cały przetworzony replay.',\n"
  "  'replay.minutesAgo': '{count} min temu',\n"
  "  'replay.hoursAgo': '{count} godz. temu',\n"
  "  'replay.daysAgo': '{count} dni temu',\n"),
 ('frontend/src/pages/AboutPage.jsx',
  "import markDownTheme from '../theme/components/Markdown';\n",
  "import markDownTheme from '../theme/components/Markdown';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/AboutPage.jsx', 'function AboutPage() {\n', 'function AboutPage() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/AboutPage.jsx', '        heading="About"\n', "        heading={intl.formatMessage({ id: 'about.heading' })}\n"),
 ('frontend/src/pages/AboutPage.jsx',
  '            This is a Spike-like (good old Spike made by poncho for BB2 <Icon as={FaRegHeart} />) facade to BB3 data\n'
  '            provided by Cyanide&apos;s BB3-API.\n',
  "            {intl.formatMessage({ id: 'about.fallback' })} <Icon as={FaRegHeart} />\n"),
 ('frontend/src/pages/StaffPage.jsx',
  "import AiReporterApi from '../AiReporterApi';\n",
  "import AiReporterApi from '../AiReporterApi';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/StaffPage.jsx', 'function StaffPage() {\n', 'function StaffPage() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/StaffPage.jsx',
  '<Heading mt={6}>BlaskScore staff</Heading>',
  "<Heading mt={6}>{intl.formatMessage({ id: 'staff.heading' })}</Heading>"),
 ('frontend/src/pages/StaffPage.jsx',
  '        The AI reporters covering Blood Bowl for BlaskScore.\n',
  "        {intl.formatMessage({ id: 'staff.description' })}\n"),
 ('frontend/src/pages/StaffPage.jsx',
  '      {!reporters && !error && <Text mt={8}>Loading staff…</Text>',
  "      {!reporters && !error && <Text mt={8}>{intl.formatMessage({ id: 'staff.loading' })}</Text>"),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  "import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';\n",
  "import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  'function AdminAiReportersPage() {\n',
  'function AdminAiReportersPage() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  '<Heading mt={6}>AI Reporters</Heading>',
  "<Heading mt={6}>{intl.formatMessage({ id: 'aiAdmin.heading' })}</Heading>"),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  '<Text mt={2} color="gray.400">Runtime overrides for all AI reporters.</Text>',
  '<Text mt={2} color="gray.400">{intl.formatMessage({ id: \'aiAdmin.help\' })}</Text>'),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  '<Text mb={1} fontSize="sm" fontWeight="700">Default article language</Text>',
  '<Text mb={1} fontSize="sm" fontWeight="700">{intl.formatMessage({ id: \'aiAdmin.defaultArticleLanguage\' })}</Text>'),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  '            Used when a reporter has no profile or runtime language override.\n',
  "            {intl.formatMessage({ id: 'aiAdmin.defaultLanguageHelp' })}\n"),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  '<Th minW="260px">Reporter</Th>',
  '<Th minW="260px">{intl.formatMessage({ id: \'aiAdmin.reporter\' })}</Th>'),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  '<Th textAlign="center">Enabled</Th>',
  '<Th textAlign="center">{intl.formatMessage({ id: \'aiAdmin.enabled\' })}</Th>'),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  '<Th textAlign="center">Reports</Th>',
  '<Th textAlign="center">{intl.formatMessage({ id: \'aiAdmin.reports\' })}</Th>'),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  '<Th textAlign="center">Interactions</Th>',
  '<Th textAlign="center">{intl.formatMessage({ id: \'aiAdmin.interactions\' })}</Th>'),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  '<Th textAlign="center">Ratings</Th>',
  '<Th textAlign="center">{intl.formatMessage({ id: \'aiAdmin.ratings\' })}</Th>'),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  '<Th minW="175px">Language</Th>',
  '<Th minW="175px">{intl.formatMessage({ id: \'aiAdmin.language\' })}</Th>'),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  '<Th minW="150px">Writing weight</Th>',
  '<Th minW="150px">{intl.formatMessage({ id: \'aiAdmin.writingWeight\' })}</Th>'),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  "                        Inherit ({reporter.profileLanguage || settings?.defaultLanguage || 'sv'})\n",
  "                        {intl.formatMessage({ id: 'aiAdmin.inherit' }, { language: reporter.profileLanguage || "
  "settings?.defaultLanguage || 'sv' })}\n"),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  '                      Effective: {reporter.primaryLanguage}\n',
  "                      {intl.formatMessage({ id: 'aiAdmin.effective' }, { language: reporter.primaryLanguage })}\n"),
 ('frontend/src/pages/AdminAiReportersPage.jsx',
  '                      Reset\n',
  "                      {intl.formatMessage({ id: 'aiAdmin.reset' })}\n"),
 ('frontend/src/pages/ReporterProfilePage.jsx',
  "import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';\n",
  "import useAuth0WithUserPermissions from '../hooks/useAuth0WithUserPermissions';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/ReporterProfilePage.jsx',
  'function ReportCard({ report }) {\n',
  'function ReportCard({ report }) {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/ReporterProfilePage.jsx',
  "          {report.headline || 'Untitled report'}\n",
  "          {report.headline || intl.formatMessage({ id: 'reporter.untitled' })}\n"),
 ('frontend/src/pages/ReporterProfilePage.jsx',
  'function ReporterProfilePage() {\n',
  'function ReporterProfilePage() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/ReporterProfilePage.jsx',
  '                        BlaskScore staff\n',
  "                        {intl.formatMessage({ id: 'reporter.staff' })}\n"),
 ('frontend/src/pages/ReporterProfilePage.jsx',
  '<Badge colorScheme="purple">AI Reporter</Badge>',
  '<Badge colorScheme="purple">{intl.formatMessage({ id: \'reporter.aiReporter\' })}</Badge>'),
 ('frontend/src/pages/ReporterProfilePage.jsx',
  '{!reporter.active && <Badge colorScheme="gray">Inactive</Badge>}',
  '{!reporter.active && <Badge colorScheme="gray">{intl.formatMessage({ id: \'reporter.inactive\' })}</Badge>}'),
 ('frontend/src/pages/ReporterProfilePage.jsx',
  '                            aria-label={`Settings for ${reporter.alias}`}\n                            title="Reporter settings"\n',
  "                            aria-label={intl.formatMessage({ id: 'reporter.settingsFor' }, { name: reporter.alias })}\n"
  "                            title={intl.formatMessage({ id: 'reporter.settings' })}\n"),
 ('frontend/src/pages/ReporterProfilePage.jsx',
  '                            Reporter settings\n',
  "                            {intl.formatMessage({ id: 'reporter.settings' })}\n"),
 ('frontend/src/pages/ReporterProfilePage.jsx',
  '                  About {reporter.alias}\n',
  "                  {intl.formatMessage({ id: 'reporter.about' }, { name: reporter.alias })}\n"),
 ('frontend/src/pages/ReporterProfilePage.jsx',
  '<HStack justify="space-between" mb={4}>\n                    <Heading size="md">Latest reports</Heading>',
  '<HStack justify="space-between" mb={4}>\n                    <Heading size="md">{intl.formatMessage({ id: \'reporter.latestReports\' })}</Heading>'),
 ('frontend/src/pages/ReporterProfilePage.jsx',
  '<Box mt={8} pt={6} borderTopWidth="1px">\n                <Heading size="md">Latest reports</Heading>',
  '<Box mt={8} pt={6} borderTopWidth="1px">\n                <Heading size="md">{intl.formatMessage({ id: \'reporter.latestReports\' })}</Heading>'),
 ('frontend/src/pages/ReporterProfilePage.jsx',
  '                  No published reports yet.\n',
  "                  {intl.formatMessage({ id: 'reporter.noReports' })}\n"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  "import WarpScoresApiService from '../../WarpScoresApiService';\n",
  "import WarpScoresApiService from '../../WarpScoresApiService';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  "const relativeAge=value=>{if(!value)return '—';const date=new "
  'Date(value),seconds=Math.max(0,Math.floor((Date.now()-date.getTime())/1000));if(Number.isNaN(seconds))return '
  'value;if(seconds<3600)return `${Math.max(1,Math.floor(seconds/60))} min ago`;if(seconds<86400)return `${Math.floor(seconds/3600)} h '
  'ago`;return `${Math.floor(seconds/86400)} days ago`;};',
  "const relativeAge=(value,intl)=>{if(!value)return '—';const date=new "
  'Date(value),seconds=Math.max(0,Math.floor((Date.now()-date.getTime())/1000));if(Number.isNaN(seconds))return '
  'value;if(seconds<3600)return '
  "intl.formatMessage({id:'replay.minutesAgo'},{count:Math.max(1,Math.floor(seconds/60))});if(seconds<86400)return "
  "intl.formatMessage({id:'replay.hoursAgo'},{count:Math.floor(seconds/3600)});return "
  "intl.formatMessage({id:'replay.daysAgo'},{count:Math.floor(seconds/86400)});};"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  'export default function ReplaySweeperAdmin({auth}){\n',
  'export default function ReplaySweeperAdmin({auth}){\n  const intl=useIntl();\n'),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '<Text>Loading replay service…</Text>',
  "<Text>{intl.formatMessage({id:'replay.loading'})}</Text>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '<Heading size="md" mb={3}>BB3 replay sweeper</Heading>',
  '<Heading size="md" mb={3}>{intl.formatMessage({id:\'replay.heading\'})}</Heading>'),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  "{status.credentialConfigured?'The Steam ticket has expired and must be renewed.':'No replay service Steam account has been "
  "authenticated.'}",
  "{status.credentialConfigured?intl.formatMessage({id:'replay.ticketExpired'}):intl.formatMessage({id:'replay.notAuthenticated'})}"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx', '>Enabled</Checkbox>', ">{intl.formatMessage({id:'replay.enabled'})}</Checkbox>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '>Steam username</FormLabel>',
  ">{intl.formatMessage({id:'replay.steamUsername'})}</FormLabel>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx', '>Cron</FormLabel>', ">{intl.formatMessage({id:'replay.cron'})}</FormLabel>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '>Time zone</FormLabel>',
  ">{intl.formatMessage({id:'replay.timeZone'})}</FormLabel>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '>Batch size</FormLabel>',
  ">{intl.formatMessage({id:'replay.batchSize'})}</FormLabel>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx', '>Save</Button>', ">{intl.formatMessage({id:'replay.save'})}</Button>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '<Tooltip label="Sync replays"><IconButton aria-label="Sync replays"',
  "<Tooltip label={intl.formatMessage({id:'replay.sync'})}><IconButton aria-label={intl.formatMessage({id:'replay.sync'})}"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '<Tooltip label="Scan matches (Cyanide API, pybb3 fallback)"><IconButton aria-label="Scan matches"',
  "<Tooltip label={intl.formatMessage({id:'replay.scan'})}><IconButton aria-label={intl.formatMessage({id:'replay.scan'})}"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '<Text color="gray.500" fontSize="sm" mt={2}>Default: 05:00 Europe/Stockholm. Last run: {status.lastCompletedAt||\'never\'} · '
  'downloaded: {status.lastDownloaded||0}</Text>',
  '<Text color="gray.500" fontSize="sm" '
  "mt={2}>{intl.formatMessage({id:'replay.defaultSchedule'},{lastRun:status.lastCompletedAt||intl.formatMessage({id:'replay.never'}),downloaded:status.lastDownloaded||0})}</Text>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  "{challenge.method==='device_confirmation'?'Approve in the Steam app, then continue.':'Enter the Steam Guard code.'}",
  "{challenge.method==='device_confirmation'?intl.formatMessage({id:'replay.approveSteam'}):intl.formatMessage({id:'replay.enterGuard'})}"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx', '>Continue</Button>', ">{intl.formatMessage({id:'replay.continue'})}</Button>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '>Steam password</FormLabel>',
  ">{intl.formatMessage({id:'replay.steamPassword'})}</FormLabel>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '>Authenticate replay account</Button>',
  ">{intl.formatMessage({id:'replay.authenticate'})}</Button>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '>Replay artifacts and analysis</Heading>',
  ">{intl.formatMessage({id:'replay.artifacts'})}</Heading>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '>Import local .bbr files</FormLabel>',
  ">{intl.formatMessage({id:'replay.importFiles'})}</FormLabel>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  ">Import {files.length||''}</Button>",
  ">{intl.formatMessage({id:'replay.import'},{count:files.length||''})}</Button>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  "{result.imported?'IMPORTED':'SKIPPED'}",
  "{result.imported?intl.formatMessage({id:'replay.imported'}):intl.formatMessage({id:'replay.skipped'})}"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '<Text fontSize="sm" color="gray.500" mb={2}>Only recent attempts inside the configured replay availability window are shown. Failed '
  'recent downloads are at risk of being lost when Steam removes them.</Text>',
  '<Text fontSize="sm" color="gray.500" mb={2}>{intl.formatMessage({id:\'replay.windowHelp\'})}</Text>'),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '<Thead><Tr><Th>Match</Th><Th>Played</Th><Th>Replay</Th><Th>Analysis</Th><Th>Original / compact</Th><Th/></Tr></Thead>',
  "<Thead><Tr><Th>{intl.formatMessage({id:'replay.match'})}</Th><Th>{intl.formatMessage({id:'replay.played'})}</Th><Th>{intl.formatMessage({id:'replay.replay'})}</Th><Th>{intl.formatMessage({id:'replay.analysis'})}</Th><Th>{intl.formatMessage({id:'replay.sizes'})}</Th><Th/></Tr></Thead>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  "replay.teams?.join(' – ')||'Unknown match'",
  "replay.teams?.join(' – ')||intl.formatMessage({id:'replay.unknownMatch'})"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx', 'relativeAge(replay.playedAt)', 'relativeAge(replay.playedAt,intl)'),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  "{downloaded?(replay.originalAvailable?'SAVED':'MISSING LOCALLY'):'AT RISK'}",
  "{downloaded?(replay.originalAvailable?intl.formatMessage({id:'replay.saved'}):intl.formatMessage({id:'replay.missingLocal'})):intl.formatMessage({id:'replay.atRisk'})}"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  "{downloaded?(replay.analysisStatus||'PENDING'):'NOT AVAILABLE'}",
  "{downloaded?(replay.analysisStatus||intl.formatMessage({id:'replay.pending'})):intl.formatMessage({id:'replay.notAvailable'})}"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx', '>Inspect</Button>', ">{intl.formatMessage({id:'replay.inspect'})}</Button>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '>Analyze again</Button>',
  ">{intl.formatMessage({id:'replay.analyzeAgain'})}</Button>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '>Administrator log</Heading>',
  ">{intl.formatMessage({id:'replay.adminLog'})}</Heading>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '>No replay runs logged yet.</Text>',
  ">{intl.formatMessage({id:'replay.noRuns'})}</Text>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  "<ModalHeader>Parsed replay · {inspect?.replay?.teams?.join(' – ')||inspect?.replay?.matchId}</ModalHeader>",
  "<ModalHeader>{intl.formatMessage({id:'replay.parsed'},{match:inspect?.replay?.teams?.join(' – "
  "')||inspect?.replay?.matchId})}</ModalHeader>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '<Alert status="warning" mb={3}><AlertIcon/>Parser v1 is structurally lossless but its statistical interpretation is experimental. '
  'Unknown enums are shown raw.</Alert>',
  '<Alert status="warning" mb={3}><AlertIcon/>{intl.formatMessage({id:\'replay.parserWarning\'})}</Alert>'),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '>Copy full JSON</Button>',
  ">{intl.formatMessage({id:'replay.copyJson'})}</Button>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '>Download inspect JSON</Button>',
  ">{intl.formatMessage({id:'replay.downloadJson'})}</Button>"),
 ('frontend/src/components/admin/ReplaySweeperAdmin.jsx',
  '>Preview is limited to 100,000 characters; Copy and Download contain the complete parsed replay.</Text>',
  ">{intl.formatMessage({id:'replay.previewHelp'})}</Text>")]

POLISH_OPERATIONS = [('frontend/src/i18n/messages.js',
  "  'replay.daysAgo': '{count} days ago',\n",
  "  'replay.daysAgo': '{count} days ago',\n"
  "  'disclaimer.basedOnPrefix': 'This work is based heavily on',\n"
  "  'disclaimer.basedOnSuffix': 'by Naytsyrhc.',\n"
  "  'disclaimer.maintainedPrefix': 'Page maintained by',\n"
  "  'disclaimer.pleaseCheck': 'Please also check',\n"
  "  'disclaimer.terms': 'Terms',\n"
  "  'disclaimer.and': 'and',\n"
  "  'disclaimer.privacy': 'Privacy Policy',\n"),
 ('frontend/src/i18n/messages.js',
  "  'replay.daysAgo': 'för {count} dagar sedan',\n",
  "  'replay.daysAgo': 'för {count} dagar sedan',\n"
  "  'disclaimer.basedOnPrefix': 'Det här arbetet bygger i hög grad på',\n"
  "  'disclaimer.basedOnSuffix': 'av Naytsyrhc.',\n"
  "  'disclaimer.maintainedPrefix': 'Sidan underhålls av',\n"
  "  'disclaimer.pleaseCheck': 'Läs även',\n"
  "  'disclaimer.terms': 'Villkor',\n"
  "  'disclaimer.and': 'och',\n"
  "  'disclaimer.privacy': 'Integritetspolicy',\n"),
 ('frontend/src/i18n/messages.js',
  "  'replay.daysAgo': 'hace {count} días',\n",
  "  'replay.daysAgo': 'hace {count} días',\n"
  "  'disclaimer.basedOnPrefix': 'Este trabajo se basa en gran medida en',\n"
  "  'disclaimer.basedOnSuffix': 'de Naytsyrhc.',\n"
  "  'disclaimer.maintainedPrefix': 'Página mantenida por',\n"
  "  'disclaimer.pleaseCheck': 'Consulta también',\n"
  "  'disclaimer.terms': 'Términos',\n"
  "  'disclaimer.and': 'y',\n"
  "  'disclaimer.privacy': 'Política de privacidad',\n"),
 ('frontend/src/i18n/messages.js',
  "  'replay.daysAgo': '{count} päivää sitten',\n",
  "  'replay.daysAgo': '{count} päivää sitten',\n"
  "  'disclaimer.basedOnPrefix': 'Tämä työ perustuu vahvasti palveluun',\n"
  "  'disclaimer.basedOnSuffix': 'tekijältä Naytsyrhc.',\n"
  "  'disclaimer.maintainedPrefix': 'Sivua ylläpitää',\n"
  "  'disclaimer.pleaseCheck': 'Tutustu myös',\n"
  "  'disclaimer.terms': 'käyttöehtoihin',\n"
  "  'disclaimer.and': 'ja',\n"
  "  'disclaimer.privacy': 'tietosuojakäytäntöön',\n"),
 ('frontend/src/i18n/messages.js',
  "  'replay.daysAgo': '{count} dni temu',\n",
  "  'replay.daysAgo': '{count} dni temu',\n"
  "  'disclaimer.basedOnPrefix': 'Ta praca w dużej mierze opiera się na',\n"
  "  'disclaimer.basedOnSuffix': 'autorstwa Naytsyrhc.',\n"
  "  'disclaimer.maintainedPrefix': 'Stronę utrzymuje',\n"
  "  'disclaimer.pleaseCheck': 'Zapoznaj się również z',\n"
  "  'disclaimer.terms': 'Warunkami',\n"
  "  'disclaimer.and': 'i',\n"
  "  'disclaimer.privacy': 'Polityką prywatności',\n"),
 ('frontend/src/components/misc/Disclaimer.jsx',
  '        <Text fontSize={textSize}>\n'
  "          {intl.formatMessage({ id: 'disclaimer.basedOn' })}\n"
  '        </Text>\n'
  '        <Text fontSize={textSize}>\n'
  '          <Link href="mailto:dennis.granasen@gmail.com" isExternal>\n'
  "            {intl.formatMessage({ id: 'disclaimer.maintainedBy' }, { name: 'd-rock' })}\n"
  '          </Link>\n'
  '        </Text>\n'
  '        <Text fontSize={textSize}>\n'
  '          <Link as={RouteLink} to="/terms.md">\n'
  "            {intl.formatMessage({ id: 'disclaimer.checkTerms' })}\n"
  "          </Link>{' '}\n"
  '          <Link as={RouteLink} to="/privacy.md">\n'
  '            Privacy\n'
  '          </Link>\n'
  '        </Text>\n',
  '        <Text fontSize={textSize}>\n'
  "          {intl.formatMessage({ id: 'disclaimer.basedOnPrefix' })}{' '}\n"
  '          <Link href="https://warp-scores.net" isExternal>Warp-Scores</Link>{\' \'}\n'
  "          {intl.formatMessage({ id: 'disclaimer.basedOnSuffix' })}\n"
  '        </Text>\n'
  '        <Text fontSize={textSize}>\n'
  "          {intl.formatMessage({ id: 'disclaimer.maintainedPrefix' })}{' '}\n"
  '          <Link href="mailto:dennis.granasen@gmail.com" isExternal>d-rock</Link>.\n'
  '        </Text>\n'
  '        <Text fontSize={textSize}>\n'
  "          {intl.formatMessage({ id: 'disclaimer.pleaseCheck' })}{' '}\n"
  '          <Link as={RouteLink} to="/terms.md">{intl.formatMessage({ id: \'disclaimer.terms\' })}</Link>{\' \'}\n'
  "          {intl.formatMessage({ id: 'disclaimer.and' })}{' '}\n"
  '          <Link as={RouteLink} to="/privacy.md">{intl.formatMessage({ id: \'disclaimer.privacy\' })}</Link>.\n'
  '        </Text>\n')]

PUBLIC_LEGACY_OPERATIONS = [('frontend/src/i18n/messages.js',
  "  'disclaimer.privacy': 'Privacy Policy',\n",
  "  'disclaimer.privacy': 'Privacy Policy',\n"
  "  'error.error': 'Error',\n"
  "  'error.warning': 'Warning',\n"
  "  'error.info': 'Information',\n"
  "  'arena.details': 'Arena details',\n"
  "  'arena.coaches': 'Coaches',\n"
  "  'arena.activeRuns': 'Active runs',\n"
  "  'arena.completedRuns': 'Completed runs',\n"
  "  'arena.failedRuns': 'Failed runs',\n"
  "  'arena.completedRunsCount': 'Completed runs ({count})',\n"
  "  'arena.activeRunsCount': 'Active runs ({count})',\n"
  "  'arena.failedRunsCount': 'Failed runs ({count})',\n"
  "  'arena.coachDetails': 'Arena coach details',\n"
  "  'arena.competition': 'Competition: {name}',\n"
  "  'arena.playedRaces': 'Played races',\n"
  "  'arena.playedMatches': 'Played matches',\n"
  "  'arena.completed': 'Completed',\n"
  "  'arena.failed': 'Failed',\n"
  "  'arena.active': 'Active',\n"
  "  'arena.teamsRaces': '{teams} teams, {races} races',\n"
  "  'arena.winRateOverall': 'Win rate (overall)',\n"
  "  'arena.winRates': 'Win rates',\n"
  "  'arena.completedTeams': 'Completed teams ({count})',\n"
  "  'arena.activeTeams': 'Active teams ({count})',\n"
  "  'arena.failedTeams': 'Failed teams ({count})',\n"
  "  'arena.notAvailable': 'Not yet available…',\n"
  "  'arena.none': 'None…',\n"
  "  'arena.firstGame': 'First game',\n"
  "  'arena.lastGame': 'Last game',\n"
  "  'arena.gamesPlayed': 'Games played',\n"
  "  'arena.runs': 'Runs',\n"
  "  'arena.progress': 'Progress',\n"
  "  'circuit.fallback': 'Circuit',\n"
  "  'circuit.details': 'Circuit details',\n"
  "  'circuit.loading': 'Loading…',\n"
  "  'circuit.legDetails': 'Circuit leg details',\n"
  "  'circuit.legNotFound': 'No circuit leg found with ID {id}.',\n"),
 ('frontend/src/i18n/messages.js',
  "  'disclaimer.privacy': 'Integritetspolicy',\n",
  "  'disclaimer.privacy': 'Integritetspolicy',\n"
  "  'error.error': 'Fel',\n"
  "  'error.warning': 'Varning',\n"
  "  'error.info': 'Information',\n"
  "  'arena.details': 'Arenadetaljer',\n"
  "  'arena.coaches': 'Coacher',\n"
  "  'arena.activeRuns': 'Aktiva runs',\n"
  "  'arena.completedRuns': 'Avslutade runs',\n"
  "  'arena.failedRuns': 'Misslyckade runs',\n"
  "  'arena.completedRunsCount': 'Avslutade runs ({count})',\n"
  "  'arena.activeRunsCount': 'Aktiva runs ({count})',\n"
  "  'arena.failedRunsCount': 'Misslyckade runs ({count})',\n"
  "  'arena.coachDetails': 'Arena-coachdetaljer',\n"
  "  'arena.competition': 'Tävling: {name}',\n"
  "  'arena.playedRaces': 'Spelade raser',\n"
  "  'arena.playedMatches': 'Spelade matcher',\n"
  "  'arena.completed': 'Avslutade',\n"
  "  'arena.failed': 'Misslyckade',\n"
  "  'arena.active': 'Aktiva',\n"
  "  'arena.teamsRaces': '{teams} lag, {races} raser',\n"
  "  'arena.winRateOverall': 'Vinstprocent (totalt)',\n"
  "  'arena.winRates': 'Vinstprocent',\n"
  "  'arena.completedTeams': 'Avslutade lag ({count})',\n"
  "  'arena.activeTeams': 'Aktiva lag ({count})',\n"
  "  'arena.failedTeams': 'Misslyckade lag ({count})',\n"
  "  'arena.notAvailable': 'Inte tillgängligt ännu…',\n"
  "  'arena.none': 'Inga…',\n"
  "  'arena.firstGame': 'Första match',\n"
  "  'arena.lastGame': 'Senaste match',\n"
  "  'arena.gamesPlayed': 'Spelade matcher',\n"
  "  'arena.runs': 'Runs',\n"
  "  'arena.progress': 'Förlopp',\n"
  "  'circuit.fallback': 'Circuit',\n"
  "  'circuit.details': 'Circuit-detaljer',\n"
  "  'circuit.loading': 'Laddar…',\n"
  "  'circuit.legDetails': 'CircuitLeg-detaljer',\n"
  "  'circuit.legNotFound': 'Ingen Circuit Leg hittades med ID {id}.',\n"),
 ('frontend/src/i18n/messages.js',
  "  'disclaimer.privacy': 'Política de privacidad',\n",
  "  'disclaimer.privacy': 'Política de privacidad',\n"
  "  'error.error': 'Error',\n"
  "  'error.warning': 'Advertencia',\n"
  "  'error.info': 'Información',\n"
  "  'arena.details': 'Detalles de arena',\n"
  "  'arena.coaches': 'Entrenadores',\n"
  "  'arena.activeRuns': 'Rachas activas',\n"
  "  'arena.completedRuns': 'Rachas completadas',\n"
  "  'arena.failedRuns': 'Rachas fallidas',\n"
  "  'arena.completedRunsCount': 'Rachas completadas ({count})',\n"
  "  'arena.activeRunsCount': 'Rachas activas ({count})',\n"
  "  'arena.failedRunsCount': 'Rachas fallidas ({count})',\n"
  "  'arena.coachDetails': 'Detalles del entrenador de arena',\n"
  "  'arena.competition': 'Competición: {name}',\n"
  "  'arena.playedRaces': 'Razas jugadas',\n"
  "  'arena.playedMatches': 'Partidos jugados',\n"
  "  'arena.completed': 'Completadas',\n"
  "  'arena.failed': 'Fallidas',\n"
  "  'arena.active': 'Activas',\n"
  "  'arena.teamsRaces': '{teams} equipos, {races} razas',\n"
  "  'arena.winRateOverall': 'Porcentaje de victorias (total)',\n"
  "  'arena.winRates': 'Porcentajes de victoria',\n"
  "  'arena.completedTeams': 'Equipos completados ({count})',\n"
  "  'arena.activeTeams': 'Equipos activos ({count})',\n"
  "  'arena.failedTeams': 'Equipos fallidos ({count})',\n"
  "  'arena.notAvailable': 'Aún no disponible…',\n"
  "  'arena.none': 'Ninguno…',\n"
  "  'arena.firstGame': 'Primer partido',\n"
  "  'arena.lastGame': 'Último partido',\n"
  "  'arena.gamesPlayed': 'Partidos jugados',\n"
  "  'arena.runs': 'Rachas',\n"
  "  'arena.progress': 'Progreso',\n"
  "  'circuit.fallback': 'Circuito',\n"
  "  'circuit.details': 'Detalles del circuito',\n"
  "  'circuit.loading': 'Cargando…',\n"
  "  'circuit.legDetails': 'Detalles de la etapa del circuito',\n"
  "  'circuit.legNotFound': 'No se encontró una etapa del circuito con ID {id}.',\n"),
 ('frontend/src/i18n/messages.js',
  "  'disclaimer.privacy': 'tietosuojakäytäntöön',\n",
  "  'disclaimer.privacy': 'tietosuojakäytäntöön',\n"
  "  'error.error': 'Virhe',\n"
  "  'error.warning': 'Varoitus',\n"
  "  'error.info': 'Tieto',\n"
  "  'arena.details': 'Areenan tiedot',\n"
  "  'arena.coaches': 'Valmentajat',\n"
  "  'arena.activeRuns': 'Aktiiviset jaksot',\n"
  "  'arena.completedRuns': 'Päättyneet jaksot',\n"
  "  'arena.failedRuns': 'Epäonnistuneet jaksot',\n"
  "  'arena.completedRunsCount': 'Päättyneet jaksot ({count})',\n"
  "  'arena.activeRunsCount': 'Aktiiviset jaksot ({count})',\n"
  "  'arena.failedRunsCount': 'Epäonnistuneet jaksot ({count})',\n"
  "  'arena.coachDetails': 'Areenavalmentajan tiedot',\n"
  "  'arena.competition': 'Kilpailu: {name}',\n"
  "  'arena.playedRaces': 'Pelatut rodut',\n"
  "  'arena.playedMatches': 'Pelatut ottelut',\n"
  "  'arena.completed': 'Päättyneet',\n"
  "  'arena.failed': 'Epäonnistuneet',\n"
  "  'arena.active': 'Aktiiviset',\n"
  "  'arena.teamsRaces': '{teams} joukkuetta, {races} rotua',\n"
  "  'arena.winRateOverall': 'Voittoprosentti (yhteensä)',\n"
  "  'arena.winRates': 'Voittoprosentit',\n"
  "  'arena.completedTeams': 'Päättyneet joukkueet ({count})',\n"
  "  'arena.activeTeams': 'Aktiiviset joukkueet ({count})',\n"
  "  'arena.failedTeams': 'Epäonnistuneet joukkueet ({count})',\n"
  "  'arena.notAvailable': 'Ei vielä saatavilla…',\n"
  "  'arena.none': 'Ei yhtään…',\n"
  "  'arena.firstGame': 'Ensimmäinen ottelu',\n"
  "  'arena.lastGame': 'Viimeisin ottelu',\n"
  "  'arena.gamesPlayed': 'Pelatut ottelut',\n"
  "  'arena.runs': 'Jaksot',\n"
  "  'arena.progress': 'Eteneminen',\n"
  "  'circuit.fallback': 'Circuit',\n"
  "  'circuit.details': 'Circuitin tiedot',\n"
  "  'circuit.loading': 'Ladataan…',\n"
  "  'circuit.legDetails': 'Circuit-osuuden tiedot',\n"
  "  'circuit.legNotFound': 'Circuit-osuutta tunnuksella {id} ei löytynyt.',\n"),
 ('frontend/src/i18n/messages.js',
  "  'disclaimer.privacy': 'Polityką prywatności',\n",
  "  'disclaimer.privacy': 'Polityką prywatności',\n"
  "  'error.error': 'Błąd',\n"
  "  'error.warning': 'Ostrzeżenie',\n"
  "  'error.info': 'Informacja',\n"
  "  'arena.details': 'Szczegóły areny',\n"
  "  'arena.coaches': 'Trenerzy',\n"
  "  'arena.activeRuns': 'Aktywne serie',\n"
  "  'arena.completedRuns': 'Ukończone serie',\n"
  "  'arena.failedRuns': 'Nieudane serie',\n"
  "  'arena.completedRunsCount': 'Ukończone serie ({count})',\n"
  "  'arena.activeRunsCount': 'Aktywne serie ({count})',\n"
  "  'arena.failedRunsCount': 'Nieudane serie ({count})',\n"
  "  'arena.coachDetails': 'Szczegóły trenera areny',\n"
  "  'arena.competition': 'Rozgrywki: {name}',\n"
  "  'arena.playedRaces': 'Rozegrane rasy',\n"
  "  'arena.playedMatches': 'Rozegrane mecze',\n"
  "  'arena.completed': 'Ukończone',\n"
  "  'arena.failed': 'Nieudane',\n"
  "  'arena.active': 'Aktywne',\n"
  "  'arena.teamsRaces': '{teams} drużyn, {races} ras',\n"
  "  'arena.winRateOverall': 'Procent zwycięstw (łącznie)',\n"
  "  'arena.winRates': 'Procent zwycięstw',\n"
  "  'arena.completedTeams': 'Ukończone drużyny ({count})',\n"
  "  'arena.activeTeams': 'Aktywne drużyny ({count})',\n"
  "  'arena.failedTeams': 'Nieudane drużyny ({count})',\n"
  "  'arena.notAvailable': 'Jeszcze niedostępne…',\n"
  "  'arena.none': 'Brak…',\n"
  "  'arena.firstGame': 'Pierwszy mecz',\n"
  "  'arena.lastGame': 'Ostatni mecz',\n"
  "  'arena.gamesPlayed': 'Rozegrane mecze',\n"
  "  'arena.runs': 'Serie',\n"
  "  'arena.progress': 'Postęp',\n"
  "  'circuit.fallback': 'Circuit',\n"
  "  'circuit.details': 'Szczegóły circuitu',\n"
  "  'circuit.loading': 'Ładowanie…',\n"
  "  'circuit.legDetails': 'Szczegóły etapu circuitu',\n"
  "  'circuit.legNotFound': 'Nie znaleziono etapu circuitu o ID {id}.',\n"),
 ('frontend/src/components/common/LoadingOrErrorWrapper.jsx',
  "import prettyPrint from '../../util/prettyPrint';\n",
  "import prettyPrint from '../../util/prettyPrint';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/common/LoadingOrErrorWrapper.jsx',
  'function LoadingOrErrorWrapper({ loading, error, children }) {\n',
  'function LoadingOrErrorWrapper({ loading, error, children }) {\n  const intl = useIntl();\n'),
 ('frontend/src/components/common/LoadingOrErrorWrapper.jsx',
  '<Text fontWeight="bold">{`${prettyPrint(error.type)}:`}</Text>',
  '<Text fontWeight="bold">{`${intl.formatMessage({ id: `error.${error.type}`, defaultMessage: prettyPrint(error.type) })}:`}</Text>'),
 ('frontend/src/pages/ArenaPage.jsx',
  "import ArenaRunAccordionItem from '../components/arena/ArenaRunAccordionItem';\n",
  "import ArenaRunAccordionItem from '../components/arena/ArenaRunAccordionItem';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/ArenaPage.jsx', 'function ArenaPage() {\n', 'function ArenaPage() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/ArenaPage.jsx',
  'subHeading={<RouteLink to={`/${competition?.leagueId}`}>League: {competition?.leagueName}</RouteLink>}',
  "subHeading={<RouteLink to={`/${competition?.leagueId}`}>{intl.formatMessage({ id: 'competition.league' }, { name: "
  'competition?.leagueName })}</RouteLink>}'),
 ('frontend/src/pages/ArenaPage.jsx', 'detailsHeading="Arena details"', "detailsHeading={intl.formatMessage({ id: 'arena.details' })}"),
 ('frontend/src/pages/ArenaPage.jsx',
  '<InfoItem key="Coaches" label="Coaches" info={arenaInfo?.coaches} />',
  '<InfoItem key="Coaches" label={intl.formatMessage({ id: \'arena.coaches\' })} info={arenaInfo?.coaches} />'),
 ('frontend/src/pages/ArenaPage.jsx',
  '<InfoItem key="Teams" label="Teams" info={arenaInfo?.teams} />',
  '<InfoItem key="Teams" label={intl.formatMessage({ id: \'common.teams\' })} info={arenaInfo?.teams} />'),
 ('frontend/src/pages/ArenaPage.jsx',
  '<InfoItem key="Active" label="Active runs" info={arenaInfo?.activeRuns} />',
  '<InfoItem key="Active" label={intl.formatMessage({ id: \'arena.activeRuns\' })} info={arenaInfo?.activeRuns} />'),
 ('frontend/src/pages/ArenaPage.jsx',
  '<InfoItem key="Completed" label="Completed runs" info={arenaInfo?.completedRuns} />',
  '<InfoItem key="Completed" label={intl.formatMessage({ id: \'arena.completedRuns\' })} info={arenaInfo?.completedRuns} />'),
 ('frontend/src/pages/ArenaPage.jsx',
  '<InfoItem key="Failed" label="Failed runs" info={arenaInfo?.failedRuns} />',
  '<InfoItem key="Failed" label={intl.formatMessage({ id: \'arena.failedRuns\' })} info={arenaInfo?.failedRuns} />'),
 ('frontend/src/pages/ArenaPage.jsx',
  'label={`Completed runs (${arenaInfo?.completedRuns})`}',
  "label={intl.formatMessage({ id: 'arena.completedRunsCount' }, { count: arenaInfo?.completedRuns })}"),
 ('frontend/src/pages/ArenaPage.jsx',
  'label={`Active runs (${arenaInfo?.activeRuns})`}',
  "label={intl.formatMessage({ id: 'arena.activeRunsCount' }, { count: arenaInfo?.activeRuns })}"),
 ('frontend/src/pages/ArenaPage.jsx',
  'label={`Failed runs (${arenaInfo?.failedRuns})`}',
  "label={intl.formatMessage({ id: 'arena.failedRunsCount' }, { count: arenaInfo?.failedRuns })}"),
 ('frontend/src/pages/ArenaCoachPage.jsx',
  "import { identityUtils } from '../util/identityUtil';\n",
  "import { identityUtils } from '../util/identityUtil';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/ArenaCoachPage.jsx', 'function ArenaCoachPage() {\n', 'function ArenaCoachPage() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/ArenaCoachPage.jsx',
  "heading={`Coach: ${coachName ? `${coachName}` : ''}`}",
  "heading={intl.formatMessage({ id: 'team.coach' }, { name: coachName || '' })}"),
 ('frontend/src/pages/ArenaCoachPage.jsx',
  'subHeading={<RouteLink to={`/competition/${competitionId}`}>Competition: {competition?.name}</RouteLink>}',
  "subHeading={<RouteLink to={`/competition/${competitionId}`}>{intl.formatMessage({ id: 'arena.competition' }, { name: competition?.name "
  '})}</RouteLink>}'),
 ('frontend/src/pages/ArenaCoachPage.jsx',
  'detailsHeading="Arena Coach Details"',
  "detailsHeading={intl.formatMessage({ id: 'arena.coachDetails' })}"),
 ('frontend/src/pages/ArenaCoachPage.jsx',
  '<InfoItem key="playedRaces" label="Played races" info={getDistinctRaces(arenaCoachTeams)} />',
  '<InfoItem key="playedRaces" label={intl.formatMessage({ id: \'arena.playedRaces\' })} info={getDistinctRaces(arenaCoachTeams)} />'),
 ('frontend/src/pages/ArenaCoachPage.jsx',
  '<InfoItem key="playedMatches" label="Played matches" info={getPlayedMatchesCount(arenaCoachTeams)} />',
  '<InfoItem key="playedMatches" label={intl.formatMessage({ id: \'arena.playedMatches\' })} info={getPlayedMatchesCount(arenaCoachTeams)} '
  '/>'),
 ('frontend/src/pages/ArenaCoachPage.jsx',
  'label="Completed"\n                info={`${completedTeamsCount} Teams, ${completedRacesCount} Races`}',
  "label={intl.formatMessage({ id: 'arena.completed' })}\n"
  "                info={intl.formatMessage({ id: 'arena.teamsRaces' }, { teams: completedTeamsCount, races: completedRacesCount })}"),
 ('frontend/src/pages/ArenaCoachPage.jsx',
  '<InfoItem key="failedRuns" label="Failed" info={`${failedTeamsCount} Teams, ${failedRacesCount} Races`} />',
  '<InfoItem key="failedRuns" label={intl.formatMessage({ id: \'arena.failed\' })} info={intl.formatMessage({ id: \'arena.teamsRaces\' }, '
  '{ teams: failedTeamsCount, races: failedRacesCount })} />'),
 ('frontend/src/pages/ArenaCoachPage.jsx',
  '<InfoItem key="activeRuns" label="Active" info={`${activeTeamsCount} Teams, ${activeRacesCount} Races`} />',
  '<InfoItem key="activeRuns" label={intl.formatMessage({ id: \'arena.active\' })} info={intl.formatMessage({ id: \'arena.teamsRaces\' }, '
  '{ teams: activeTeamsCount, races: activeRacesCount })} />'),
 ('frontend/src/pages/ArenaCoachPage.jsx', 'label="Win rate (overall)"', "label={intl.formatMessage({ id: 'arena.winRateOverall' })}"),
 ('frontend/src/pages/ArenaCoachPage.jsx',
  '<Heading size="md">Win Rates</Heading>',
  '<Heading size="md">{intl.formatMessage({ id: \'arena.winRates\' })}</Heading>'),
 ('frontend/src/pages/ArenaCoachPage.jsx',
  'label={`Completed teams (${completedTeamsCount})`}',
  "label={intl.formatMessage({ id: 'arena.completedTeams' }, { count: completedTeamsCount })}"),
 ('frontend/src/pages/ArenaCoachPage.jsx',
  'label={`Active teams (${activeTeamsCount})`}',
  "label={intl.formatMessage({ id: 'arena.activeTeams' }, { count: activeTeamsCount })}"),
 ('frontend/src/pages/ArenaCoachPage.jsx',
  'label={`Failed teams (${failedTeamsCount})`}',
  "label={intl.formatMessage({ id: 'arena.failedTeams' }, { count: failedTeamsCount })}"),
 ('frontend/src/components/arena/ArenaRunAccordionItem.jsx',
  "import prettyPrint from '../../util/prettyPrint';\n",
  "import prettyPrint from '../../util/prettyPrint';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/arena/ArenaRunAccordionItem.jsx',
  'function ArenaRunAccordionItem({ competitionId, label, loading, error, arenaTeams, coachOrRace }) {\n',
  'function ArenaRunAccordionItem({ competitionId, label, loading, error, arenaTeams, coachOrRace }) {\n'
  '  const intl = useIntl();\n'
  "  const coachOrRaceLabel = intl.formatMessage({ id: coachOrRace === 'Race' ? 'team.race' : 'common.coach' });\n"),
 ('frontend/src/components/arena/ArenaRunAccordionItem.jsx',
  '<Box>Not yet available...</Box>',
  "<Box>{intl.formatMessage({ id: 'arena.notAvailable' })}</Box>"),
 ('frontend/src/components/arena/ArenaRunAccordionItem.jsx',
  '{arenaTeams && arenaTeams.length === 0 && <Box>None...</Box>}',
  "{arenaTeams && arenaTeams.length === 0 && <Box>{intl.formatMessage({ id: 'arena.none' })}</Box>}"),
 ('frontend/src/components/arena/ArenaRunAccordionItem.jsx',
  '<Thead>\n'
  '                  <Tr>\n'
  '                    <Th>Team</Th>\n'
  '                    <Th>{coachOrRace}</Th>\n'
  '                    <Th>First game</Th>\n'
  '                    <Th>Last game</Th>\n'
  '                    <Th>Games played</Th>\n'
  '                    <Th>Runs</Th>\n'
  '                    <Th>Progress</Th>\n'
  '                  </Tr>\n'
  '                </Thead>',
  '<Thead>\n'
  '                  <Tr>\n'
  "                    <Th>{intl.formatMessage({ id: 'common.team' })}</Th>\n"
  '                    <Th>{coachOrRaceLabel}</Th>\n'
  "                    <Th>{intl.formatMessage({ id: 'arena.firstGame' })}</Th>\n"
  "                    <Th>{intl.formatMessage({ id: 'arena.lastGame' })}</Th>\n"
  "                    <Th>{intl.formatMessage({ id: 'arena.gamesPlayed' })}</Th>\n"
  "                    <Th>{intl.formatMessage({ id: 'arena.runs' })}</Th>\n"
  "                    <Th>{intl.formatMessage({ id: 'arena.progress' })}</Th>\n"
  '                  </Tr>\n'
  '                </Thead>'),
 ('frontend/src/components/arena/ArenaRunAccordionItem.jsx',
  '<Tfoot>\n'
  '                  <Tr>\n'
  '                    <Th>Team</Th>\n'
  '                    <Th>{coachOrRace}</Th>\n'
  '                    <Th>First game</Th>\n'
  '                    <Th>Last game</Th>\n'
  '                    <Th>Games played</Th>\n'
  '                    <Th>Runs</Th>\n'
  '                    <Th>Progress</Th>\n'
  '                  </Tr>\n'
  '                </Tfoot>',
  '<Tfoot>\n'
  '                  <Tr>\n'
  "                    <Th>{intl.formatMessage({ id: 'common.team' })}</Th>\n"
  '                    <Th>{coachOrRaceLabel}</Th>\n'
  "                    <Th>{intl.formatMessage({ id: 'arena.firstGame' })}</Th>\n"
  "                    <Th>{intl.formatMessage({ id: 'arena.lastGame' })}</Th>\n"
  "                    <Th>{intl.formatMessage({ id: 'arena.gamesPlayed' })}</Th>\n"
  "                    <Th>{intl.formatMessage({ id: 'arena.runs' })}</Th>\n"
  "                    <Th>{intl.formatMessage({ id: 'arena.progress' })}</Th>\n"
  '                  </Tr>\n'
  '                </Tfoot>'),
 ('frontend/src/pages/CircuitPage.jsx',
  "import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';\n",
  "import LoadingOrErrorWrapper from '../components/common/LoadingOrErrorWrapper';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/CircuitPage.jsx', 'function CircuitPage() {\n', 'function CircuitPage() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/CircuitPage.jsx',
  '<HeaderCard heading={circuit ? circuit.circuitName : \'Circuit\'} detailsHeading="Circuit details" />',
  "<HeaderCard heading={circuit ? circuit.circuitName : intl.formatMessage({ id: 'circuit.fallback' })} "
  "detailsHeading={intl.formatMessage({ id: 'circuit.details' })} />"),
 ('frontend/src/pages/CircuitPage.jsx',
  '<Heading size="md">{circuit ? circuit.circuitName : \'Loading...\'}</Heading>',
  '<Heading size="md">{circuit ? circuit.circuitName : intl.formatMessage({ id: \'circuit.loading\' })}</Heading>'),
 ('frontend/src/pages/CircuitLegPage.jsx',
  "import useFetchTeams from '../hooks/useFetchTeamsForCircuitLeg';\n",
  "import useFetchTeams from '../hooks/useFetchTeamsForCircuitLeg';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/CircuitLegPage.jsx', 'function CircuitLegPage() {\n', 'function CircuitLegPage() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/CircuitLegPage.jsx',
  '<HeaderCard heading={circuitLeg.name} detailsHeading="CircuitLeg details">',
  "<HeaderCard heading={circuitLeg.name} detailsHeading={intl.formatMessage({ id: 'circuit.legDetails' })}>"),
 ('frontend/src/pages/CircuitLegPage.jsx',
  ': (error ? null : <Box>No Circuit Leg found with ID {legId}.</Box>)',
  ": (error ? null : <Box>{intl.formatMessage({ id: 'circuit.legNotFound' }, { id: legId })}</Box>)"),
 ('frontend/src/pages/CircuitLegEntityPage.jsx',
  "import useFetchRanks from '../hooks/useFetchRanksForCircuitLegEntity';\n",
  "import useFetchRanks from '../hooks/useFetchRanksForCircuitLegEntity';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/pages/CircuitLegEntityPage.jsx',
  'function CircuitLegEntityPage() {\n',
  'function CircuitLegEntityPage() {\n  const intl = useIntl();\n'),
 ('frontend/src/pages/CircuitLegEntityPage.jsx',
  '<HeaderCard heading={circuit.name} detailsHeading="CircuitLeg details">',
  "<HeaderCard heading={circuit.name} detailsHeading={intl.formatMessage({ id: 'circuit.legDetails' })}>"),
 ('frontend/src/pages/CircuitLegEntityPage.jsx',
  '<InfoItem key="Created" label="Created" info={formatter.formatAsDate(competition.dateCreated, \'-\')} />',
  '<InfoItem key="Created" label={intl.formatMessage({ id: \'competition.created\' })} '
  "info={formatter.formatAsDate(competition.dateCreated, '-')} />"),
 ('frontend/src/pages/CircuitLegEntityPage.jsx',
  '<InfoItem key="Format" label="Format" info={prettyPrint(competition.format)} />',
  '<InfoItem key="Format" label={intl.formatMessage({ id: \'competition.format\' })} info={intl.formatMessage({ id: '
  '`format.${competition.format}`, defaultMessage: prettyPrint(competition.format) })} />'),
 ('frontend/src/pages/CircuitLegEntityPage.jsx', 'label="Progress"', "label={intl.formatMessage({ id: 'competition.progress' })}"),
 ('frontend/src/pages/CircuitLegEntityPage.jsx',
  '<InfoItem key="Teams" label="Teams" info={formatter.formatAsNumber(ranks.length)} />',
  '<InfoItem key="Teams" label={intl.formatMessage({ id: \'common.teams\' })} info={formatter.formatAsNumber(ranks.length)} />'),
 ('frontend/src/pages/CircuitLegEntityPage.jsx',
  'label="Time settings"\n'
  '                info={`Turn: ${formatter.formatAsNumber((competition?.turnDuration ?? 0) / 60)}m`}\n'
  '                additionalInfo={`Bonus: ${formatter.formatAsNumber((competition?.timeBonusDuration ?? 0) / 60)}m`}',
  "label={intl.formatMessage({ id: 'competition.timeSettings' })}\n"
  "                info={intl.formatMessage({ id: 'competition.turnMinutes' }, { minutes: "
  'formatter.formatAsNumber((competition?.turnDuration ?? 0) / 60) })}\n'
  "                additionalInfo={intl.formatMessage({ id: 'competition.bonusMinutes' }, { minutes: "
  'formatter.formatAsNumber((competition?.timeBonusDuration ?? 0) / 60) })}'),
 ('frontend/src/pages/CircuitLegEntityPage.jsx',
  ': (error ? null : <Box>No Circuit Leg found with ID {legId}.</Box>)',
  ": (error ? null : <Box>{intl.formatMessage({ id: 'circuit.legNotFound' }, { id: legId })}</Box>)")]

TABLE_OPERATIONS = [('frontend/src/i18n/messages.js',
  "  'circuit.legNotFound': 'No circuit leg found with ID {id}.',\n",
  "  'circuit.legNotFound': 'No circuit leg found with ID {id}.',\n"
  "  'competition.statusLabel': 'Status',\n"
  "  'competition.currentRound': 'Current round',\n"
  "  'competition.roundMatchesLeft': 'Round matches left',\n"
  "  'competition.totalMatchesLeft': 'Total matches left',\n"),
 ('frontend/src/i18n/messages.js',
  "  'circuit.legNotFound': 'Ingen Circuit Leg hittades med ID {id}.',\n",
  "  'circuit.legNotFound': 'Ingen Circuit Leg hittades med ID {id}.',\n"
  "  'competition.statusLabel': 'Status',\n"
  "  'competition.currentRound': 'Aktuell omgång',\n"
  "  'competition.roundMatchesLeft': 'Matcher kvar i omgången',\n"
  "  'competition.totalMatchesLeft': 'Matcher kvar totalt',\n"),
 ('frontend/src/i18n/messages.js',
  "  'circuit.legNotFound': 'No se encontró una etapa del circuito con ID {id}.',\n",
  "  'circuit.legNotFound': 'No se encontró una etapa del circuito con ID {id}.',\n"
  "  'competition.statusLabel': 'Estado',\n"
  "  'competition.currentRound': 'Ronda actual',\n"
  "  'competition.roundMatchesLeft': 'Partidos restantes de la ronda',\n"
  "  'competition.totalMatchesLeft': 'Partidos restantes en total',\n"),
 ('frontend/src/i18n/messages.js',
  "  'circuit.legNotFound': 'Circuit-osuutta tunnuksella {id} ei löytynyt.',\n",
  "  'circuit.legNotFound': 'Circuit-osuutta tunnuksella {id} ei löytynyt.',\n"
  "  'competition.statusLabel': 'Tila',\n"
  "  'competition.currentRound': 'Nykyinen kierros',\n"
  "  'competition.roundMatchesLeft': 'Kierroksen otteluita jäljellä',\n"
  "  'competition.totalMatchesLeft': 'Otteluita jäljellä yhteensä',\n"),
 ('frontend/src/i18n/messages.js',
  "  'circuit.legNotFound': 'Nie znaleziono etapu circuitu o ID {id}.',\n",
  "  'circuit.legNotFound': 'Nie znaleziono etapu circuitu o ID {id}.',\n"
  "  'competition.statusLabel': 'Status',\n"
  "  'competition.currentRound': 'Aktualna runda',\n"
  "  'competition.roundMatchesLeft': 'Mecze pozostałe w rundzie',\n"
  "  'competition.totalMatchesLeft': 'Łącznie pozostałe mecze',\n"),
 ('frontend/src/components/common/Standings.jsx',
  "import WarpScoresApiService from '../../WarpScoresApiService';\n",
  "import WarpScoresApiService from '../../WarpScoresApiService';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/common/Standings.jsx',
  'function TableColumns() {\n  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);\n',
  'function TableColumns() {\n  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);\n  const intl = useIntl();\n'),
 ('frontend/src/components/common/Standings.jsx',
  "<Center>{isSmallScreen ? 'R' : 'Rank'}</Center>",
  "<Center>{isSmallScreen ? 'R' : intl.formatMessage({ id: 'common.rank' })}</Center>"),
 ('frontend/src/components/common/Standings.jsx', '<Th>Team/Coach</Th>', "<Th>{intl.formatMessage({ id: 'statistics.teamCoach' })}</Th>"),
 ('frontend/src/components/common/Standings.jsx', '<Th>Team-Name</Th>', "<Th>{intl.formatMessage({ id: 'statistics.teamName' })}</Th>"),
 ('frontend/src/components/common/Standings.jsx', '<Th>Coach-Name</Th>', "<Th>{intl.formatMessage({ id: 'statistics.coachName' })}</Th>"),
 ('frontend/src/components/common/Standings.jsx', '<Th>Race</Th>', "<Th>{intl.formatMessage({ id: 'team.race' })}</Th>"),
 ('frontend/src/components/common/Standings.jsx',
  "<Center>{isSmallScreen ? 'Sc.' : 'Score'}</Center>",
  "<Center>{isSmallScreen ? 'Sc.' : intl.formatMessage({ id: 'common.score' })}</Center>"),
 ('frontend/src/components/common/Standings.jsx',
  "<Center>{isSmallScreen ? 'GP' : 'Games'}</Center>",
  "<Center>{isSmallScreen ? 'GP' : intl.formatMessage({ id: 'common.games' })}</Center>"),
 ('frontend/src/components/competition/Competitions.jsx',
  "import config from '../../config';\n",
  "import config from '../../config';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/competition/Competitions.jsx',
  'function TableColumns() {\n  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);\n',
  'function TableColumns() {\n  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);\n  const intl = useIntl();\n'),
 ('frontend/src/components/competition/Competitions.jsx',
  '<Th>Competition</Th>',
  "<Th>{intl.formatMessage({ id: 'common.competition' })}</Th>"),
 ('frontend/src/components/competition/Competitions.jsx',
  "<Th isNumeric>{isSmallScreen ? 'T' : 'Teams'}</Th>",
  "<Th isNumeric>{isSmallScreen ? 'T' : intl.formatMessage({ id: 'common.teams' })}</Th>"),
 ('frontend/src/components/competition/Competitions.jsx',
  "<Th>{isSmallScreen ? 'F' : 'Format'}</Th>",
  "<Th>{isSmallScreen ? 'F' : intl.formatMessage({ id: 'competition.format' })}</Th>"),
 ('frontend/src/components/competition/Competitions.jsx',
  '{!isSmallScreen && <Th>Status</Th>}',
  "{!isSmallScreen && <Th>{intl.formatMessage({ id: 'competition.statusLabel' })}</Th>}"),
 ('frontend/src/components/competition/Competitions.jsx',
  "<Th>{isSmallScreen ? 'CR' : 'Current Round'}</Th>",
  "<Th>{isSmallScreen ? 'CR' : intl.formatMessage({ id: 'competition.currentRound' })}</Th>"),
 ('frontend/src/components/competition/Competitions.jsx',
  "<Th>{isSmallScreen ? 'RML' : 'Round matches left'}</Th>",
  "<Th>{isSmallScreen ? 'RML' : intl.formatMessage({ id: 'competition.roundMatchesLeft' })}</Th>"),
 ('frontend/src/components/competition/Competitions.jsx',
  "<Th>{isSmallScreen ? 'TML' : 'Total matches left'}</Th>",
  "<Th>{isSmallScreen ? 'TML' : intl.formatMessage({ id: 'competition.totalMatchesLeft' })}</Th>"),
 ('frontend/src/components/competition/Competitions.jsx',
  'function Competitions({ competitions, league }) {\n',
  'function Competitions({ competitions, league }) {\n  const intl = useIntl();\n'),
 ('frontend/src/components/competition/Competitions.jsx',
  'header="Competitions In Progress"',
  "header={intl.formatMessage({ id: 'league.activeCompetitions' })}"),
 ('frontend/src/components/competition/Competitions.jsx',
  'header="Competitions In Registration"',
  "header={intl.formatMessage({ id: 'league.registrationCompetitions' })}"),
 ('frontend/src/components/competition/Competitions.jsx',
  'header="Finished Competitions"',
  "header={intl.formatMessage({ id: 'league.finishedCompetitions' })}"),
 ('frontend/src/components/competition/Competitions.jsx',
  'header="Competitions with Unknown Status"',
  "header={intl.formatMessage({ id: 'league.unknownCompetitions' })}")]

LEAGUE_SYSTEM_OPERATIONS = [('frontend/src/i18n/messages.js',
  "  'competition.totalMatchesLeft': 'Total matches left',\n",
  "  'competition.totalMatchesLeft': 'Total matches left',\n"
  "  'leagueSystems.group': 'Group',\n"
  "  'leagueSystems.noTable': 'No table results yet',\n"
  "  'leagueSystems.playIn': 'Play-in',\n"
  "  'leagueSystems.quarterfinals': 'Quarterfinals',\n"
  "  'leagueSystems.semifinals': 'Semifinals',\n"
  "  'leagueSystems.final': 'Final',\n"
  "  'leagueSystems.finals': 'Finals',\n"
  "  'leagueSystems.bronze': 'Bronze match',\n"
  "  'leagueSystems.round': 'Round {round}',\n"
  "  'leagueSystems.match': 'Match {number}',\n"
  "  'leagueSystems.noDate': 'No date',\n"
  "  'leagueSystems.casualties': 'Casualties {home}–{away}',\n"
  "  'leagueSystems.conceded': 'Conceded/WO',\n"
  "  'leagueSystems.replay': 'Replay',\n"
  "  'leagueSystems.noMatches': 'No matches yet',\n"
  "  'leagueSystems.matchesPerRound': 'Matches by round',\n"
  "  'leagueSystems.matchMissingId': 'The match has no internal BlaskScore ID.',\n"
  "  'leagueSystems.statsNotFound': 'Match statistics were not found.',\n"
  "  'leagueSystems.statsFetchFailed': 'Match statistics could not be loaded.',\n"
  "  'leagueSystems.matchStatistics': 'Match statistics',\n"
  "  'leagueSystems.loadingStats': 'Loading match statistics…',\n"
  "  'leagueSystems.systemFallback': 'League system',\n"
  "  'leagueSystems.primary': 'Primary',\n"
  "  'leagueSystems.selectSystem': 'Select league system',\n"
  "  'leagueSystems.season': 'Season {number}',\n"
  "  'leagueSystems.selectSeason': 'Select season',\n"
  "  'leagueSystems.empty': 'No group stage or playoff matches yet',\n"),
 ('frontend/src/i18n/messages.js',
  "  'competition.totalMatchesLeft': 'Matcher kvar totalt',\n",
  "  'competition.totalMatchesLeft': 'Matcher kvar totalt',\n"
  "  'leagueSystems.group': 'Grupp',\n"
  "  'leagueSystems.noTable': 'Inga tabellresultat ännu',\n"
  "  'leagueSystems.playIn': 'Play-in',\n"
  "  'leagueSystems.quarterfinals': 'Kvartsfinaler',\n"
  "  'leagueSystems.semifinals': 'Semifinaler',\n"
  "  'leagueSystems.final': 'Final',\n"
  "  'leagueSystems.finals': 'Finaler',\n"
  "  'leagueSystems.bronze': 'Bronsmatch',\n"
  "  'leagueSystems.round': 'Omgång {round}',\n"
  "  'leagueSystems.match': 'Match {number}',\n"
  "  'leagueSystems.noDate': 'Inget datum',\n"
  "  'leagueSystems.casualties': 'Casualties {home}–{away}',\n"
  "  'leagueSystems.conceded': 'Conceded/WO',\n"
  "  'leagueSystems.replay': 'Replay',\n"
  "  'leagueSystems.noMatches': 'Inga matcher ännu',\n"
  "  'leagueSystems.matchesPerRound': 'Matcher per omgång',\n"
  "  'leagueSystems.matchMissingId': 'Matchen saknar ett internt BlaskScore-ID.',\n"
  "  'leagueSystems.statsNotFound': 'Matchstatistiken hittades inte.',\n"
  "  'leagueSystems.statsFetchFailed': 'Matchstatistiken kunde inte hämtas.',\n"
  "  'leagueSystems.matchStatistics': 'Matchstatistik',\n"
  "  'leagueSystems.loadingStats': 'Hämtar matchstatistik…',\n"
  "  'leagueSystems.systemFallback': 'Ligasystem',\n"
  "  'leagueSystems.primary': 'Primärt',\n"
  "  'leagueSystems.selectSystem': 'Välj ligasystem',\n"
  "  'leagueSystems.season': 'Säsong {number}',\n"
  "  'leagueSystems.selectSeason': 'Välj säsong',\n"
  "  'leagueSystems.empty': 'Inga gruppspels- eller slutspelsmatcher ännu',\n"),
 ('frontend/src/i18n/messages.js',
  "  'competition.totalMatchesLeft': 'Partidos restantes en total',\n",
  "  'competition.totalMatchesLeft': 'Partidos restantes en total',\n"
  "  'leagueSystems.group': 'Grupo',\n"
  "  'leagueSystems.noTable': 'Aún no hay resultados de clasificación',\n"
  "  'leagueSystems.playIn': 'Play-in',\n"
  "  'leagueSystems.quarterfinals': 'Cuartos de final',\n"
  "  'leagueSystems.semifinals': 'Semifinales',\n"
  "  'leagueSystems.final': 'Final',\n"
  "  'leagueSystems.finals': 'Finales',\n"
  "  'leagueSystems.bronze': 'Partido por el bronce',\n"
  "  'leagueSystems.round': 'Ronda {round}',\n"
  "  'leagueSystems.match': 'Partido {number}',\n"
  "  'leagueSystems.noDate': 'Sin fecha',\n"
  "  'leagueSystems.casualties': 'Bajas {home}–{away}',\n"
  "  'leagueSystems.conceded': 'Concesión/WO',\n"
  "  'leagueSystems.replay': 'Repetición',\n"
  "  'leagueSystems.noMatches': 'Aún no hay partidos',\n"
  "  'leagueSystems.matchesPerRound': 'Partidos por ronda',\n"
  "  'leagueSystems.matchMissingId': 'El partido no tiene un ID interno de BlaskScore.',\n"
  "  'leagueSystems.statsNotFound': 'No se encontraron las estadísticas del partido.',\n"
  "  'leagueSystems.statsFetchFailed': 'No se pudieron cargar las estadísticas del partido.',\n"
  "  'leagueSystems.matchStatistics': 'Estadísticas del partido',\n"
  "  'leagueSystems.loadingStats': 'Cargando estadísticas del partido…',\n"
  "  'leagueSystems.systemFallback': 'Sistema de liga',\n"
  "  'leagueSystems.primary': 'Principal',\n"
  "  'leagueSystems.selectSystem': 'Seleccionar sistema de liga',\n"
  "  'leagueSystems.season': 'Temporada {number}',\n"
  "  'leagueSystems.selectSeason': 'Seleccionar temporada',\n"
  "  'leagueSystems.empty': 'Aún no hay partidos de fase de grupos o eliminatorias',\n"),
 ('frontend/src/i18n/messages.js',
  "  'competition.totalMatchesLeft': 'Otteluita jäljellä yhteensä',\n",
  "  'competition.totalMatchesLeft': 'Otteluita jäljellä yhteensä',\n"
  "  'leagueSystems.group': 'Lohko',\n"
  "  'leagueSystems.noTable': 'Sarjataulukkotuloksia ei vielä ole',\n"
  "  'leagueSystems.playIn': 'Play-in',\n"
  "  'leagueSystems.quarterfinals': 'Puolivälierät',\n"
  "  'leagueSystems.semifinals': 'Välierät',\n"
  "  'leagueSystems.final': 'Finaali',\n"
  "  'leagueSystems.finals': 'Finaalit',\n"
  "  'leagueSystems.bronze': 'Pronssiottelu',\n"
  "  'leagueSystems.round': 'Kierros {round}',\n"
  "  'leagueSystems.match': 'Ottelu {number}',\n"
  "  'leagueSystems.noDate': 'Ei päivämäärää',\n"
  "  'leagueSystems.casualties': 'Casualties {home}–{away}',\n"
  "  'leagueSystems.conceded': 'Luovutus/WO',\n"
  "  'leagueSystems.replay': 'Replay',\n"
  "  'leagueSystems.noMatches': 'Otteluita ei vielä ole',\n"
  "  'leagueSystems.matchesPerRound': 'Ottelut kierroksittain',\n"
  "  'leagueSystems.matchMissingId': 'Ottelulta puuttuu BlaskScoren sisäinen tunnus.',\n"
  "  'leagueSystems.statsNotFound': 'Ottelutilastoja ei löytynyt.',\n"
  "  'leagueSystems.statsFetchFailed': 'Ottelutilastoja ei voitu ladata.',\n"
  "  'leagueSystems.matchStatistics': 'Ottelutilastot',\n"
  "  'leagueSystems.loadingStats': 'Ladataan ottelutilastoja…',\n"
  "  'leagueSystems.systemFallback': 'Liigajärjestelmä',\n"
  "  'leagueSystems.primary': 'Ensisijainen',\n"
  "  'leagueSystems.selectSystem': 'Valitse liigajärjestelmä',\n"
  "  'leagueSystems.season': 'Kausi {number}',\n"
  "  'leagueSystems.selectSeason': 'Valitse kausi',\n"
  "  'leagueSystems.empty': 'Lohko- tai pudotuspelivaiheen otteluita ei vielä ole',\n"),
 ('frontend/src/i18n/messages.js',
  "  'competition.totalMatchesLeft': 'Łącznie pozostałe mecze',\n",
  "  'competition.totalMatchesLeft': 'Łącznie pozostałe mecze',\n"
  "  'leagueSystems.group': 'Grupa',\n"
  "  'leagueSystems.noTable': 'Brak wyników tabeli',\n"
  "  'leagueSystems.playIn': 'Play-in',\n"
  "  'leagueSystems.quarterfinals': 'Ćwierćfinały',\n"
  "  'leagueSystems.semifinals': 'Półfinały',\n"
  "  'leagueSystems.final': 'Finał',\n"
  "  'leagueSystems.finals': 'Finały',\n"
  "  'leagueSystems.bronze': 'Mecz o brąz',\n"
  "  'leagueSystems.round': 'Runda {round}',\n"
  "  'leagueSystems.match': 'Mecz {number}',\n"
  "  'leagueSystems.noDate': 'Brak daty',\n"
  "  'leagueSystems.casualties': 'Casualties {home}–{away}',\n"
  "  'leagueSystems.conceded': 'Poddanie/WO',\n"
  "  'leagueSystems.replay': 'Replay',\n"
  "  'leagueSystems.noMatches': 'Brak meczów',\n"
  "  'leagueSystems.matchesPerRound': 'Mecze według rund',\n"
  "  'leagueSystems.matchMissingId': 'Mecz nie ma wewnętrznego ID BlaskScore.',\n"
  "  'leagueSystems.statsNotFound': 'Nie znaleziono statystyk meczu.',\n"
  "  'leagueSystems.statsFetchFailed': 'Nie udało się pobrać statystyk meczu.',\n"
  "  'leagueSystems.matchStatistics': 'Statystyki meczu',\n"
  "  'leagueSystems.loadingStats': 'Ładowanie statystyk meczu…',\n"
  "  'leagueSystems.systemFallback': 'System ligowy',\n"
  "  'leagueSystems.primary': 'Główny',\n"
  "  'leagueSystems.selectSystem': 'Wybierz system ligowy',\n"
  "  'leagueSystems.season': 'Sezon {number}',\n"
  "  'leagueSystems.selectSeason': 'Wybierz sezon',\n"
  "  'leagueSystems.empty': 'Brak meczów fazy grupowej lub pucharowej',\n"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  "import { resolveRace } from '../../util/raceUtil';\n",
  "import { resolveRace } from '../../util/raceUtil';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  'function GroupTable({ stage }) {\n  const standings = standingsFor(stage);\n',
  'function GroupTable({ stage }) {\n  const intl = useIntl();\n  const standings = standingsFor(stage);\n'),
 ('frontend/src/components/league/LeagueSystems.jsx',
  "{stage.name || 'Group'}",
  "{stage.name || intl.formatMessage({ id: 'leagueSystems.group' })}"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  '>No table results yet</Text>',
  ">{intl.formatMessage({ id: 'leagueSystems.noTable' })}</Text>"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  'function PlayoffBracket({ phase, onMatchClick }) {\n',
  'function PlayoffBracket({ phase, onMatchClick }) {\n'
  '  const intl = useIntl();\n'
  '  const localizedRoundName = (name) => ({\n'
  "    'Play-in': intl.formatMessage({ id: 'leagueSystems.playIn' }),\n"
  "    Quarterfinals: intl.formatMessage({ id: 'leagueSystems.quarterfinals' }),\n"
  "    Semifinals: intl.formatMessage({ id: 'leagueSystems.semifinals' }),\n"
  "    Final: intl.formatMessage({ id: 'leagueSystems.final' }),\n"
  "    Finals: intl.formatMessage({ id: 'leagueSystems.finals' }),\n"
  '  }[name] || name);\n'),
 ('frontend/src/components/league/LeagueSystems.jsx',
  'tournamentRoundText: currentRound?.name,',
  'tournamentRoundText: localizedRoundName(currentRound?.name),'),
 ('frontend/src/components/league/LeagueSystems.jsx',
  'label: `Match ${index + 1}`,',
  "label: intl.formatMessage({ id: 'leagueSystems.match' }, { number: index + 1 }),"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  'tournamentRoundText: sourceRound.name,',
  'tournamentRoundText: localizedRoundName(sourceRound.name),'),
 ('frontend/src/components/league/LeagueSystems.jsx',
  "const roundLabels = ['Play-in', 'Quarterfinals', 'Semifinals', 'Final'];",
  "const roundLabels = [intl.formatMessage({ id: 'leagueSystems.playIn' }), intl.formatMessage({ id: 'leagueSystems.quarterfinals' }), "
  "intl.formatMessage({ id: 'leagueSystems.semifinals' }), intl.formatMessage({ id: 'leagueSystems.final' })];"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  '|| `Round ${roundNumber}`',
  "|| intl.formatMessage({ id: 'leagueSystems.round' }, { round: roundNumber })"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  '>Bronze match</Heading>',
  ">{intl.formatMessage({ id: 'leagueSystems.bronze' })}</Heading>"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  'function RichMatchCard({ match, onMatchClick }) {\n',
  'function RichMatchCard({ match, onMatchClick }) {\n  const intl = useIntl();\n'),
 ('frontend/src/components/league/LeagueSystems.jsx',
  "{match.finishedAt || match.startedAt || 'No date'}",
  "{match.finishedAt || match.startedAt || intl.formatMessage({ id: 'leagueSystems.noDate' })}"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  "{match.teams?.length === 2 ? ` · Casualties ${match.teams[0].casualties ?? '-'}–${match.teams[1].casualties ?? '-'}` : ''}",
  "{match.teams?.length === 2 ? ` · ${intl.formatMessage({ id: 'leagueSystems.casualties' }, { home: match.teams[0].casualties ?? '-', "
  "away: match.teams[1].casualties ?? '-' })}` : ''}"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  "{match.conceded ? ' · Conceded/WO' : ''}",
  "{match.conceded ? ` · ${intl.formatMessage({ id: 'leagueSystems.conceded' })}` : ''}"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  "{match.replayAvailable ? ' · 🎞 Replay' : ''}",
  "{match.replayAvailable ? ` · 🎞 ${intl.formatMessage({ id: 'leagueSystems.replay' })}` : ''}"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  'function StageRoundMatches({ stage, onMatchClick }) {\n',
  'function StageRoundMatches({ stage, onMatchClick }) {\n  const intl = useIntl();\n'),
 ('frontend/src/components/league/LeagueSystems.jsx',
  'if (!rounds.length) return <Text color="gray.500">No matches yet</Text>;',
  'if (!rounds.length) return <Text color="gray.500">{intl.formatMessage({ id: \'leagueSystems.noMatches\' })}</Text>;'),
 ('frontend/src/components/league/LeagueSystems.jsx',
  '<Heading size="sm" mb={2}>Matcher per omgång</Heading>',
  '<Heading size="sm" mb={2}>{intl.formatMessage({ id: \'leagueSystems.matchesPerRound\' })}</Heading>'),
 ('frontend/src/components/league/LeagueSystems.jsx',
  '>Omgång {round}</Tab>',
  ">{intl.formatMessage({ id: 'leagueSystems.round' }, { round })}</Tab>"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  'function MatchDetails({ summary, isOpen, onClose }) {\n',
  'function MatchDetails({ summary, isOpen, onClose }) {\n  const intl = useIntl();\n'),
 ('frontend/src/components/league/LeagueSystems.jsx',
  "setMatch(null); setLoading(false); setError('Matchen saknar ett internt BlaskScore-ID.');",
  "setMatch(null); setLoading(false); setError(intl.formatMessage({ id: 'leagueSystems.matchMissingId' }));"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  ".then((result) => result ? setMatch(result) : setError('Matchstatistiken hittades inte.'))",
  ".then((result) => result ? setMatch(result) : setError(intl.formatMessage({ id: 'leagueSystems.statsNotFound' })))"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  ".catch(() => setError('Matchstatistiken kunde inte hämtas.'))",
  ".catch(() => setError(intl.formatMessage({ id: 'leagueSystems.statsFetchFailed' })))"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  '>Matchstatistik</ModalHeader>',
  ">{intl.formatMessage({ id: 'leagueSystems.matchStatistics' })}</ModalHeader>"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  '>Hämtar matchstatistik…</Text>',
  ">{intl.formatMessage({ id: 'leagueSystems.loadingStats' })}</Text>"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  'function LeagueSystems({ summaries, leagueSystem, onSelectSystem, onSelectSeason }) {\n',
  'function LeagueSystems({ summaries, leagueSystem, onSelectSystem, onSelectSeason }) {\n  const intl = useIntl();\n'),
 ('frontend/src/components/league/LeagueSystems.jsx',
  "{leagueSystem?.name || 'League system'}",
  "{leagueSystem?.name || intl.formatMessage({ id: 'leagueSystems.systemFallback' })}"),
 ('frontend/src/components/league/LeagueSystems.jsx', '>Primary</Badge>', ">{intl.formatMessage({ id: 'leagueSystems.primary' })}</Badge>"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  'aria-label="Select league system"',
  "aria-label={intl.formatMessage({ id: 'leagueSystems.selectSystem' })}"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  '{selectedSeason.name || `Season ${selectedSeason.number}`}',
  "{selectedSeason.name || intl.formatMessage({ id: 'leagueSystems.season' }, { number: selectedSeason.number })}"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  'aria-label="Select season"',
  "aria-label={intl.formatMessage({ id: 'leagueSystems.selectSeason' })}"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  '{season.name || `Season ${season.number}`}',
  "{season.name || intl.formatMessage({ id: 'leagueSystems.season' }, { number: season.number })}"),
 ('frontend/src/components/league/LeagueSystems.jsx',
  '>No group stage or playoff matches yet</Text>',
  ">{intl.formatMessage({ id: 'leagueSystems.empty' })}</Text>")]

RANK_OPERATIONS = [('frontend/src/components/competition/Competition.jsx',
  "import ImageUrls from '../../imageUrls';\n",
  "import ImageUrls from '../../imageUrls';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/competition/Competition.jsx',
  'function Competition({ competition, league }) {\n',
  'function Competition({ competition, league }) {\n  const intl = useIntl();\n'),
 ('frontend/src/components/competition/Competition.jsx',
  '<Td>{isSmallScreen ? abbreviators.makeInitials(competition.format) : prettyPrint(competition.format)}</Td>',
  '<Td>{isSmallScreen ? abbreviators.makeInitials(competition.format) : intl.formatMessage({ id: `format.${competition.format}`, '
  'defaultMessage: prettyPrint(competition.format) })}</Td>'),
 ('frontend/src/components/league/Ranks.jsx',
  "import WarpScoresApiService from '../../WarpScoresApiService';\n",
  "import WarpScoresApiService from '../../WarpScoresApiService';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/league/Ranks.jsx',
  'function TableColumns() {\n  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);\n',
  'function TableColumns() {\n  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);\n  const intl = useIntl();\n'),
 ('frontend/src/components/league/Ranks.jsx',
  "<Center>{isSmallScreen ? 'R' : 'Rank'}</Center>",
  "<Center>{isSmallScreen ? 'R' : intl.formatMessage({ id: 'common.rank' })}</Center>"),
 ('frontend/src/components/league/Ranks.jsx', '<Th>Team/Coach</Th>', "<Th>{intl.formatMessage({ id: 'statistics.teamCoach' })}</Th>"),
 ('frontend/src/components/league/Ranks.jsx', '<Th>Team-Name</Th>', "<Th>{intl.formatMessage({ id: 'statistics.teamName' })}</Th>"),
 ('frontend/src/components/league/Ranks.jsx', '<Th>Coach-Name</Th>', "<Th>{intl.formatMessage({ id: 'statistics.coachName' })}</Th>"),
 ('frontend/src/components/league/Ranks.jsx', '<Th>Race</Th>', "<Th>{intl.formatMessage({ id: 'team.race' })}</Th>"),
 ('frontend/src/components/league/Ranks.jsx',
  "<Center>{isSmallScreen ? 'Sc.' : 'Score'}</Center>",
  "<Center>{isSmallScreen ? 'Sc.' : intl.formatMessage({ id: 'common.score' })}</Center>"),
 ('frontend/src/components/league/Ranks.jsx',
  "<Center>{isSmallScreen ? 'GP' : 'Games'}</Center>",
  "<Center>{isSmallScreen ? 'GP' : intl.formatMessage({ id: 'common.games' })}</Center>"),
 ('frontend/src/components/circuit/Ranks.jsx',
  "import WarpScoresApiService from '../../WarpScoresApiService';\n",
  "import WarpScoresApiService from '../../WarpScoresApiService';\nimport { useIntl } from 'react-intl';\n"),
 ('frontend/src/components/circuit/Ranks.jsx',
  'function TableColumns() {\n  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);\n',
  'function TableColumns() {\n  const isSmallScreen = useBreakpointValue(smallScreenBreakpointValues);\n  const intl = useIntl();\n'),
 ('frontend/src/components/circuit/Ranks.jsx',
  "<Center>{isSmallScreen ? 'R' : 'Rank'}</Center>",
  "<Center>{isSmallScreen ? 'R' : intl.formatMessage({ id: 'common.rank' })}</Center>"),
 ('frontend/src/components/circuit/Ranks.jsx', '<Th>Team/Coach</Th>', "<Th>{intl.formatMessage({ id: 'statistics.teamCoach' })}</Th>"),
 ('frontend/src/components/circuit/Ranks.jsx', '<Th>Team-Name</Th>', "<Th>{intl.formatMessage({ id: 'statistics.teamName' })}</Th>"),
 ('frontend/src/components/circuit/Ranks.jsx', '<Th>Coach-Name</Th>', "<Th>{intl.formatMessage({ id: 'statistics.coachName' })}</Th>"),
 ('frontend/src/components/circuit/Ranks.jsx', '<Th>Race</Th>', "<Th>{intl.formatMessage({ id: 'team.race' })}</Th>"),
 ('frontend/src/components/circuit/Ranks.jsx',
  "<Center>{isSmallScreen ? 'Sc.' : 'Score'}</Center>",
  "<Center>{isSmallScreen ? 'Sc.' : intl.formatMessage({ id: 'common.score' })}</Center>"),
 ('frontend/src/components/circuit/Ranks.jsx',
  "<Center>{isSmallScreen ? 'GP' : 'Games'}</Center>",
  "<Center>{isSmallScreen ? 'GP' : intl.formatMessage({ id: 'common.games' })}</Center>")]

def main():
    root = Path.cwd()
    git_head = root / ".git" / "HEAD"
    if not git_head.exists():
        raise SystemExit("Run this script from the cyanidebowl repository root.")
    head = git_head.read_text(encoding="utf-8").strip()
    if head.startswith("ref: refs/heads/"):
        branch = head.rsplit("/", 1)[-1]
        if branch != EXPECTED_BRANCH:
            raise SystemExit(f"Expected branch {EXPECTED_BRANCH!r}, found {branch!r}. Switch to dev first.")

    by_file = {}
    all_operations = OPERATIONS + EXTRA_OPERATIONS + ADMIN_OPERATIONS + FINAL_OPERATIONS + POLISH_OPERATIONS + PUBLIC_LEGACY_OPERATIONS + TABLE_OPERATIONS + LEAGUE_SYSTEM_OPERATIONS + RANK_OPERATIONS
    for path, old, new in all_operations:
        by_file.setdefault(path, []).append((old, new))

    staged = {}
    for relpath, replacements in by_file.items():
        path = root / relpath
        if not path.exists():
            raise SystemExit(f"Missing expected file: {relpath}")
        text = path.read_text(encoding="utf-8-sig")
        for index, (old, new) in enumerate(replacements, start=1):
            count = text.count(old)
            if count != 1:
                raise SystemExit(
                    f"{relpath} replacement {index} expected exactly one match, found {count}. "
                    "The checkout has drifted; no files were written."
                )
            text = text.replace(old, new, 1)
        staged[path] = text

    for path, text in staged.items():
        path.write_text(text, encoding="utf-8")

    print(f"Applied {len(all_operations)} localization edits across {len(staged)} files.")
    print("Review with: git diff --check && git diff")
    print("Then run the frontend build/tests before committing.")

if __name__ == "__main__":
    main()
