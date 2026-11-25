// Génère DriveEasy.postman_collection.json (format Postman v2.1).
// Usage : node postman/generate-collection.mjs
import { writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const OUT = join(dirname(fileURLToPath(import.meta.url)), 'DriveEasy.postman_collection.json');

/** Construit une requête Postman avec ses tests. */
function req(name, method, url, { body, tests = [], prerequest = [], headers = [] } = {}) {
  const item = {
    name,
    request: {
      method,
      header: [...headers, ...(body ? [{ key: 'Content-Type', value: 'application/json' }] : [])],
      url: url,
    },
    event: [],
  };
  if (body) item.request.body = { mode: 'raw', raw: JSON.stringify(body, null, 2) };
  if (prerequest.length) item.event.push({ listen: 'prerequest', script: { type: 'text/javascript', exec: prerequest } });
  if (tests.length) item.event.push({ listen: 'test', script: { type: 'text/javascript', exec: tests } });
  return item;
}

const status = (code) => `pm.test("Statut ${code}", () => pm.response.to.have.status(${code}));`;
const json = 'const r = pm.response.json();';
const errMsg = (fragment) => [json, `pm.test("Message : ${fragment}", () => pm.expect(r.message).to.include("${fragment}"));`];
const save = (varName, path = 'r.id') => `pm.collectionVariables.set("${varName}", ${path});`;

const G = '{{gateway}}/api';

const vehicule = (overrides = {}) => ({
  immatriculation: '{{immat}}', marque: 'Dacia', modele: 'Sandero', annee: 2024, couleur: 'Gris',
  categorie: 'ECONOMIQUE', carburant: 'ESSENCE', boite: 'MANUELLE', nbPlaces: 5,
  kilometrage: 1000, prixJournalier: 100, imageUrl: 'https://placehold.co/600x400?text=Dacia+Sandero',
  ...overrides,
});
const client = (overrides = {}) => ({
  cin: '{{cin}}', nom: 'Postman', prenom: 'Test', dateNaissance: '1995-05-20', email: '{{email}}',
  telephone: '+216 20 000 000', adresse: 'Tunis', numeroPermis: '{{permis}}', dateExpirationPermis: '2035-01-01',
  ...overrides,
});
const reservation = (clientId, vehiculeId, debut = '{{dateDebut}}', fin = '{{dateFin}}') =>
  ({ clientId, vehiculeId, dateDebut: debut, dateFin: fin });

const collection = {
  info: {
    name: 'DriveEasy API',
    description:
      "Tests de bout en bout de DriveEasy via l'API Gateway (port 8080).\n\n" +
      "Prérequis : Eureka, Config Server, les 3 microservices et la gateway démarrés.\n" +
      "La collection crée son propre client et son propre véhicule (valeurs aléatoires) : elle peut être relancée.\n" +
      "À exécuter dans l'ordre (Run collection).",
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
  },
  variable: [
    { key: 'gateway', value: 'http://localhost:8080' },
    { key: 'eureka', value: 'http://localhost:8761' },
    { key: 'config', value: 'http://localhost:8888' },
  ],
  event: [{
    listen: 'prerequest',
    script: {
      type: 'text/javascript',
      exec: [
        '// Dates relatives au jour d\'exécution (location de 3 jours commençant aujourd\'hui)',
        'const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;',
        'const plus = (n) => { const d = new Date(); d.setDate(d.getDate() + n); return iso(d); };',
        'pm.collectionVariables.set("dateDebut", plus(0));',
        'pm.collectionVariables.set("dateFin", plus(3));',
        'pm.collectionVariables.set("dateChevauchement", plus(2));',
        'pm.collectionVariables.set("dateFinChevauchement", plus(5));',
        'pm.collectionVariables.set("dateRetourRetard", plus(5));',
        'pm.collectionVariables.set("dateDebut2", plus(10));',
        'pm.collectionVariables.set("dateFin2", plus(12));',
      ],
    },
  }],
  item: [
    {
      name: '0. Infrastructure',
      item: [
        req('Eureka : 4 services enregistrés', 'GET', '{{eureka}}/eureka/apps', {
          headers: [{ key: 'Accept', value: 'application/json' }],
          tests: [status(200),
            'const noms = pm.response.json().applications.application.map(a => a.name);',
            '["API-GATEWAY", "VEHICULE-SERVICE", "CLIENT-SERVICE", "LOCATION-SERVICE"].forEach(n =>',
            '  pm.test(n + " enregistré", () => pm.expect(noms).to.include(n)));'],
        }),
        req('Config Server : configuration de location-service', 'GET', '{{config}}/location-service/default', {
          tests: [status(200), json,
            'const props = Object.assign({}, ...r.propertySources.map(s => s.source));',
            'pm.test("Port externalisé", () => pm.expect(props["server.port"]).to.eql(8083));',
            'pm.test("Remise VIP externalisée", () => pm.expect(props["driveeasy.location.remise-vip"]).to.eql(0.1));',
            'pm.test("Zipkin à 1.0", () => pm.expect(props["management.tracing.sampling.probability"]).to.eql(1.0));'],
        }),
        req('Gateway : health', 'GET', '{{gateway}}/actuator/health', {
          tests: [status(200), 'pm.test("UP", () => pm.expect(pm.response.json().status).to.eql("UP"));'],
        }),
        req('Gateway : CORS pour Angular (preflight)', 'OPTIONS', `${G}/vehicules`, {
          headers: [{ key: 'Origin', value: 'http://localhost:4200' }, { key: 'Access-Control-Request-Method', value: 'GET' }],
          tests: [status(200),
            'pm.test("Origine autorisée", () => pm.expect(pm.response.headers.get("Access-Control-Allow-Origin")).to.eql("http://localhost:4200"));'],
        }),
      ],
    },
    {
      name: '1. Véhicules',
      item: [
        req('Lister avec filtres', 'GET', `${G}/vehicules?carburant=DIESEL&prixMax=200`, {
          tests: [status(200), json,
            'pm.test("Filtre carburant et prix", () => r.forEach(v => {',
            '  pm.expect(v.carburant).to.eql("DIESEL"); pm.expect(v.prixJournalier).to.be.at.most(200); }));'],
        }),
        req('Créer un véhicule', 'POST', `${G}/vehicules`, {
          prerequest: ['pm.collectionVariables.set("immat", `${_.random(100, 999)} TU ${_.random(1000, 9999)}`);'],
          body: vehicule(),
          tests: [status(201), json, save('vehiculeId'),
            'pm.test("Statut DISPONIBLE par défaut", () => pm.expect(r.statut).to.eql("DISPONIBLE"));',
            'pm.test("En-tête Location", () => pm.expect(pm.response.headers.get("Location")).to.include("/vehicules/" + r.id));'],
        }),
        req('Immatriculation en double → 409', 'POST', `${G}/vehicules`, {
          body: vehicule(), tests: [status(409), ...errMsg('existe déjà')],
        }),
        req('Prix journalier négatif → 400', 'POST', `${G}/vehicules`, {
          body: vehicule({ immatriculation: 'XX', prixJournalier: -10 }),
          tests: [status(400), json, 'pm.test("Champ en erreur", () => pm.expect(r.details).to.have.property("prixJournalier"));'],
        }),
        req('Année future → 400', 'POST', `${G}/vehicules`, {
          body: vehicule({ immatriculation: 'YY', annee: 2099 }), tests: [status(400), ...errMsg('année')],
        }),
        req('Détail', 'GET', `${G}/vehicules/{{vehiculeId}}`, {
          tests: [status(200), json, 'pm.test("Bon véhicule", () => pm.expect(r.id).to.eql(Number(pm.collectionVariables.get("vehiculeId"))));'],
        }),
        req('Véhicule inexistant → 404', 'GET', `${G}/vehicules/999999`, { tests: [status(404)] }),
        req('Modifier', 'PUT', `${G}/vehicules/{{vehiculeId}}`, {
          body: vehicule({ couleur: 'Bleu' }),
          tests: [status(200), json, 'pm.test("Couleur modifiée", () => pm.expect(r.couleur).to.eql("Bleu"));'],
        }),
        req('Passer au statut LOUE via PUT → 409', 'PUT', `${G}/vehicules/{{vehiculeId}}`, {
          body: vehicule({ statut: 'LOUE' }), tests: [status(409)],
        }),
        req('Disponibles sur la période', 'GET', `${G}/vehicules/disponibles?dateDebut={{dateDebut}}&dateFin={{dateFin}}`, {
          tests: [status(200), json,
            'pm.test("Contient le nouveau véhicule", () => pm.expect(r.map(v => v.id)).to.include(Number(pm.collectionVariables.get("vehiculeId"))));',
            'pm.test("Aucun véhicule en maintenance", () => r.forEach(v => pm.expect(v.statut).to.eql("DISPONIBLE")));'],
        }),
      ],
    },
    {
      name: '2. Clients',
      item: [
        req('Créer un client', 'POST', `${G}/clients`, {
          prerequest: [
            'const n = _.random(10000000, 99999999);',
            'pm.collectionVariables.set("cin", String(n));',
            'pm.collectionVariables.set("email", `postman.${n}@example.tn`);',
            'pm.collectionVariables.set("permis", `P-${n}`);'],
          body: client(),
          tests: [status(201), json, save('clientId'),
            'pm.test("Catégorie STANDARD par défaut", () => pm.expect(r.categorieClient).to.eql("STANDARD"));',
            'pm.test("Permis non expiré", () => pm.expect(r.permisExpire).to.be.false);'],
        }),
        req('CIN en double → 409', 'POST', `${G}/clients`, {
          body: client({ email: 'autre@example.tn', numeroPermis: 'AUTRE' }), tests: [status(409), ...errMsg('CIN')],
        }),
        req('Email invalide → 400', 'POST', `${G}/clients`, {
          body: client({ cin: '00000001', email: 'pas-un-email', numeroPermis: 'X1' }),
          tests: [status(400), json, 'pm.test("Champ email en erreur", () => pm.expect(r.details).to.have.property("email"));'],
        }),
        req('Rechercher par CIN', 'GET', `${G}/clients?cin={{cin}}`, {
          tests: [status(200), json, 'pm.test("Un résultat", () => pm.expect(r).to.have.lengthOf(1));'],
        }),
        req('Éligibilité : client valide', 'GET', `${G}/clients/{{clientId}}/eligibilite`, {
          tests: [status(200), 'pm.test("Éligible", () => pm.expect(pm.response.json().eligible).to.be.true);'],
        }),
        req('Éligibilité : client de 19 ans (démo id 4)', 'GET', `${G}/clients/4/eligibilite`, {
          tests: [status(200), json, 'pm.test("Non éligible", () => pm.expect(r.eligible).to.be.false);',
            'pm.test("Motif âge", () => pm.expect(r.motifs.join()).to.include("Âge"));'],
        }),
        req('Éligibilité : permis expiré (démo id 5)', 'GET', `${G}/clients/5/eligibilite`, {
          tests: [status(200), json, 'pm.test("Permis invalide", () => pm.expect(r.permisValide).to.be.false);'],
        }),
        req('Client inexistant → 404', 'GET', `${G}/clients/999999`, { tests: [status(404)] }),
      ],
    },
    {
      name: '3. Locations — cycle complet',
      item: [
        req('Simulation du prix', 'POST', `${G}/locations/simulation`, {
          body: reservation('{{clientId}}', '{{vehiculeId}}'),
          tests: [status(200), json,
            'pm.test("3 jours × 100 = 300 (STANDARD, coef 1.0)", () => pm.expect(r.montantTotal).to.eql(300));',
            'pm.test("Client éligible et véhicule disponible", () => { pm.expect(r.clientEligible).to.be.true; pm.expect(r.vehiculeDisponible).to.be.true; });'],
        }),
        req('Simulation VIP + SUV (démo : client 3, véhicule 5)', 'POST', `${G}/locations/simulation`, {
          body: reservation(3, 5),
          tests: [status(200), json,
            'pm.test("Remise VIP 10 %", () => pm.expect(r.tauxRemise).to.eql(0.1));',
            'pm.test("Coefficient SUV 1.2", () => pm.expect(r.coefficientCategorie).to.eql(1.2));',
            'pm.test("Total = brut − remise", () => pm.expect(r.montantTotal).to.be.closeTo(r.montantBrut * 0.9, 0.01));'],
        }),
        req('Client non éligible → 400', 'POST', `${G}/locations`, {
          body: reservation(4, '{{vehiculeId}}'), tests: [status(400), ...errMsg('non éligible')],
        }),
        req('Véhicule en maintenance → 409', 'POST', `${G}/locations`, {
          body: reservation('{{clientId}}', 7), tests: [status(409), ...errMsg('maintenance')],
        }),
        req('Créer la réservation', 'POST', `${G}/locations`, {
          body: reservation('{{clientId}}', '{{vehiculeId}}'),
          tests: [status(201), json, save('locationId'),
            'pm.test("Statut RESERVEE", () => pm.expect(r.statut).to.eql("RESERVEE"));',
            'pm.test("Montant 300", () => pm.expect(r.montantTotal).to.eql(300));'],
        }),
        req('Réservation qui chevauche → 409', 'POST', `${G}/locations`, {
          body: reservation('{{clientId}}', '{{vehiculeId}}', '{{dateChevauchement}}', '{{dateFinChevauchement}}'),
          tests: [status(409), ...errMsg('déjà réservé')],
        }),
        req('Supprimer le client (location active) → 409', 'DELETE', `${G}/clients/{{clientId}}`, {
          tests: [status(409), ...errMsg('location')],
        }),
        req('Supprimer le véhicule (réservé) → 409', 'DELETE', `${G}/vehicules/{{vehiculeId}}`, { tests: [status(409)] }),
        req('Départ', 'PATCH', `${G}/locations/{{locationId}}/depart`, {
          tests: [status(200), json,
            'pm.test("EN_COURS", () => pm.expect(r.statut).to.eql("EN_COURS"));',
            'pm.test("kmDepart = kilométrage du véhicule", () => pm.expect(r.kmDepart).to.eql(1000));'],
        }),
        req('Véhicule passé à LOUE', 'GET', `${G}/vehicules/{{vehiculeId}}`, {
          tests: [status(200), 'pm.test("LOUE", () => pm.expect(pm.response.json().statut).to.eql("LOUE"));'],
        }),
        req('Départ en double → 409', 'PATCH', `${G}/locations/{{locationId}}/depart`, { tests: [status(409)] }),
        req('Retour avec km < départ → 400', 'PATCH', `${G}/locations/{{locationId}}/retour`, {
          body: { kmRetour: 500, dateRetour: '{{dateRetourRetard}}' }, tests: [status(400)],
        }),
        req('Retour avec 2 jours de retard', 'PATCH', `${G}/locations/{{locationId}}/retour`, {
          body: { kmRetour: 1450, dateRetour: '{{dateRetourRetard}}' },
          tests: [status(200), json,
            'pm.test("TERMINEE", () => pm.expect(r.statut).to.eql("TERMINEE"));',
            'pm.test("Pénalité = 2 × 1,5 × 100 = 300", () => pm.expect(r.penalite).to.eql(300));',
            'pm.test("Total = 300 + 300", () => pm.expect(r.montantTotal).to.eql(600));'],
        }),
        req('Véhicule de nouveau DISPONIBLE, km mis à jour', 'GET', `${G}/vehicules/{{vehiculeId}}`, {
          tests: [status(200), json,
            'pm.test("DISPONIBLE", () => pm.expect(r.statut).to.eql("DISPONIBLE"));',
            'pm.test("Kilométrage 1450", () => pm.expect(r.kilometrage).to.eql(1450));'],
        }),
        req('Annuler une location terminée → 409', 'PATCH', `${G}/locations/{{locationId}}/annulation`, { tests: [status(409)] }),
        req('Nouvelle réservation (à annuler)', 'POST', `${G}/locations`, {
          body: reservation('{{clientId}}', '{{vehiculeId}}', '{{dateDebut2}}', '{{dateFin2}}'),
          tests: [status(201), json, save('locationId2')],
        }),
        req('Annulation', 'PATCH', `${G}/locations/{{locationId2}}/annulation`, {
          tests: [status(200), 'pm.test("ANNULEE", () => pm.expect(pm.response.json().statut).to.eql("ANNULEE"));'],
        }),
        req('Période libérée après annulation', 'GET',
          `${G}/vehicules/{{vehiculeId}}/disponibilite?dateDebut={{dateDebut2}}&dateFin={{dateFin2}}`, {
            tests: [status(200), 'pm.test("Disponible", () => pm.expect(pm.response.json().disponible).to.be.true);'],
          }),
      ],
    },
    {
      name: '4. Consultation et statistiques',
      item: [
        req('Liste paginée filtrée', 'GET', `${G}/locations?clientId={{clientId}}&page=0&size=1`, {
          tests: [status(200), json,
            'pm.test("2 locations au total", () => pm.expect(r.totalElements).to.eql(2));',
            'pm.test("1 par page", () => { pm.expect(r.content).to.have.lengthOf(1); pm.expect(r.totalPages).to.eql(2); });'],
        }),
        req('Filtre par statut', 'GET', `${G}/locations?statut=ANNULEE&clientId={{clientId}}`, {
          tests: [status(200), 'pm.test("Seulement ANNULEE", () => pm.response.json().content.forEach(l => pm.expect(l.statut).to.eql("ANNULEE")));'],
        }),
        req('Détail enrichi (client + véhicule)', 'GET', `${G}/locations/{{locationId}}`, {
          tests: [status(200), json,
            'pm.test("Client agrégé", () => pm.expect(r.client.id).to.eql(Number(pm.collectionVariables.get("clientId"))));',
            'pm.test("Véhicule agrégé", () => pm.expect(r.vehicule.id).to.eql(Number(pm.collectionVariables.get("vehiculeId"))));',
            'pm.test("Aucun avertissement", () => pm.expect(r.avertissements).to.be.empty);'],
        }),
        req('Statistiques', 'GET', `${G}/locations/statistiques`, {
          tests: [status(200), json,
            'pm.test("Chiffre d\'affaires > 0", () => pm.expect(r.chiffreAffaires).to.be.above(0));',
            'pm.test("Taux d\'occupation calculé", () => pm.expect(r.tauxOccupation).to.be.a("number"));',
            'pm.test("Top véhicules (max 5)", () => pm.expect(r.topVehicules.length).to.be.within(1, 5));'],
        }),
        req('Pagination invalide → 400', 'GET', `${G}/locations?size=500`, { tests: [status(400)] }),
      ],
    },
    {
      name: '5. Fidélité et nettoyage',
      item: [
        req('Changer la catégorie (VIP)', 'PATCH', `${G}/clients/{{clientId}}/categorie`, {
          body: { categorieClient: 'VIP' },
          tests: [status(200), 'pm.test("VIP", () => pm.expect(pm.response.json().categorieClient).to.eql("VIP"));'],
        }),
        req('Supprimer le client (plus de location active)', 'DELETE', `${G}/clients/{{clientId}}`, { tests: [status(204)] }),
        req('Supprimer le véhicule', 'DELETE', `${G}/vehicules/{{vehiculeId}}`, { tests: [status(204)] }),
        req('Véhicule supprimé → 404', 'GET', `${G}/vehicules/{{vehiculeId}}`, { tests: [status(404)] }),
      ],
    },
  ],
};

writeFileSync(OUT, JSON.stringify(collection, null, 2) + '\n');
console.log('Collection écrite :', OUT);
