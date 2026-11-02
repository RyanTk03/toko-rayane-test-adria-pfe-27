# Plateforme bancaire : microservices, assistant IA et Kafka

Plateforme de consultation de comptes et d'enregistrement de transactions, basée sur Spring Cloud. Un assistant conversationnel (Spring AI) exécute les opérations via des outils MCP exposés par les microservices. Chaque transaction publie un événement Kafka. L'ensemble se lance avec Docker Compose.

## Architecture

```
Client REST ──> api-gateway (8080) ──┬─> account-service (8081)       REST + MCP
                                     ├─> transaction-service (8082)   MCP (REST à venir)
                                     └─> assistant-service (8083)     Spring AI + client MCP
                                                    │
                                                    └─ MCP (SSE) ──> account-service, transaction-service

transaction-service ──Feign──> account-service            (lecture et mise à jour du solde)
transaction-service ──Kafka──> bank.transactions.created ──> transaction-consumer-service (8084)

config-service (8888) : fournit bank.transaction.max-amount
eureka-server (8761)  : découverte des services
```

| Module | Rôle | Port |
|---|---|---|
| `config-service` | Config server (mode native) | 8888 |
| `eureka-server` | Registre des services | 8761 |
| `api-gateway` | Point d'entrée, routes `lb://` via Eureka | 8080 |
| `account-service` | Comptes et soldes (H2) | 8081 |
| `transaction-service` | Transactions et règles métier (H2) | 8082 |
| `assistant-service` | Agent IA, client MCP | 8083 |
| `transaction-consumer-service` | Consumer Kafka, journalisation | 8084 (aucune API) |
| `kafka` | Broker Kafka (mode KRaft) | interne, 9092 |

Dans chaque service métier, les packages sont `domain/`, `repository/`, `service/`, `web/rest/`, `mcp/`. Les contrôleurs REST et les outils MCP appellent la même couche `service/`.

## Structure du dépôt

```
.
├── docker-compose.yml
├── .env.example
├── README.md
├── config-service/                  pom.xml, src/, Dockerfile, .dockerignore
├── eureka-server/                   idem
├── api-gateway/                     idem
├── account-service/                 idem
├── transaction-service/             idem
├── assistant-service/               idem
└── transaction-consumer-service/    idem
```

Le `Dockerfile` et le `.dockerignore` sont identiques dans les 7 modules. Le build est en deux étapes (Maven, puis JRE), donc aucun JDK ni Maven n'est requis sur la machine pour lancer avec Docker.

## Prérequis

- Docker avec Docker Compose v2 (`docker compose`)
- Une clé API Anthropic
- De la mémoire pour Docker : 7 JVM tournent en parallèle (le heap de chacune est limité à 512 Mo), prévoir 4 Go ou plus
- `spring-boot-starter-actuator` dans `account-service` et `transaction-service` : Compose s'en sert pour savoir quand ils sont prêts

## Démarrage avec Docker Compose

```bash
cp .env.example .env        # puis renseigner ANTHROPIC_API_KEY dans .env
docker compose up --build -d
docker compose ps           # attendre que les services soient "healthy" ou "running"
```

Le premier build télécharge les dépendances Maven : compter plusieurs minutes. Les démarrages suivants utilisent le cache.

Commandes utiles :

```bash
docker compose logs -f assistant-service transaction-service transaction-consumer-service
docker compose restart assistant-service
docker compose up --build -d transaction-service     # reconstruire un seul service
docker compose down                                  # tout arrêter (les données H2 sont perdues)
```

L'ordre de démarrage est géré par `depends_on` et des healthchecks :

1. `kafka`, `config-service`, `eureka-server`
2. `account-service`
3. `transaction-service` (attend config-service, Eureka et Kafka)
4. `assistant-service` (attend `account-service` et `transaction-service`, car son client MCP s'y connecte au démarrage)
5. `transaction-consumer-service` (attend Kafka) et `api-gateway` (attend Eureka)

Vérifications rapides :

- Config : `http://localhost:8888/transaction-service/default` doit afficher `bank.transaction.max-amount` à `1000`.
- Eureka : `http://localhost:8761` liste `ACCOUNT-SERVICE`, `TRANSACTION-SERVICE`, `ASSISTANT-SERVICE` et `API-GATEWAY`.
- Base H2 : `http://localhost:8081/h2-console`, URL JDBC `jdbc:h2:mem:accountdb`.

## Configuration

Les fichiers `application.properties` gardent des valeurs en `localhost`. Dans Docker, Compose les remplace par des variables d'environnement (aucune modification du code) :

| Variable (service) | Valeur dans Docker |
|---|---|
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` (gateway, account, transaction, assistant) | `http://eureka-server:8761/eureka/` |
| `EUREKA_INSTANCE_PREFERIPADDRESS` (idem) | `true` (enregistrement par adresse IP) |
| `SPRING_CONFIG_IMPORT` (transaction) | `configserver:http://config-service:8888` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` (transaction, consumer) | `kafka:9092` |
| `SPRING_AI_MCP_CLIENT_SSE_CONNECTIONS_ACCOUNT_URL` (assistant) | `http://account-service:8081` |
| `SPRING_AI_MCP_CLIENT_SSE_CONNECTIONS_TRANSACTION_URL` (assistant) | `http://transaction-service:8082` |
| `ANTHROPIC_API_KEY` (assistant) | lue depuis `.env` |
| `JAVA_VERSION` (build) | lue depuis `.env`, 17 par défaut |

Autres propriétés :

| Propriété | Où | Valeur |
|---|---|---|
| `bank.transaction.max-amount` | `config-service`, fichier `config/transaction-service.properties` | `1000` |
| `spring.ai.anthropic.chat.options.model` | `assistant-service` | `claude-sonnet-5-5` |

Le fournisseur de LLM peut être changé (OpenAI, Ollama) en remplaçant le starter Spring AI et les propriétés `spring.ai.*`. Le code ne dépend pas du fournisseur.

## Règles métier

Appliquées par `TransactionService.saveTransaction`, donc aussi pour les opérations de l'assistant :

1. Le montant est strictement positif et ne dépasse pas `bank.transaction.max-amount`.
2. Le compte existe et est `ACTIVE`.
3. La devise de la transaction est celle du compte.
4. Le solde est consulté : une transaction qui dépasse le solde est refusée.
5. La transaction est enregistrée, puis le solde du compte est mis à jour (débit).

Si la mise à jour du solde échoue, l'enregistrement local est annulé et aucun événement n'est publié.

## Données de test

`account-service` crée 5 comptes au démarrage (`CommandLineRunner`). Leurs UUID sont affichés dans les logs (`docker compose logs account-service`).

| Client | Devise | Solde | Statut |
|---|---|---|---|
| CUST-001 | MAD | 10 000 | ACTIVE |
| CUST-001 | EUR | 2 500 | ACTIVE |
| CUST-002 | MAD | 500 | ACTIVE |
| CUST-003 | USD | 8 000 | ACTIVE |
| CUST-004 | MAD | 1 200 | BLOCKED |

## API

### account-service (REST)

| Méthode | Route | Rôle |
|---|---|---|
| POST | `/api/accounts` | Créer un compte |
| GET | `/api/accounts` | Lister (`?customerReference=` pour filtrer) |
| GET | `/api/accounts/{id}` | Détail d'un compte |
| GET | `/api/accounts/{id}/balance` | Solde |
| PATCH | `/api/accounts/{id}/balance` | Appliquer un delta (`{"delta": -50}`) |
| PATCH | `/api/accounts/{id}/status` | Changer le statut (`?status=BLOCKED`) |

Erreurs (format `ProblemDetail`) : 404 compte introuvable, 409 compte bloqué, 422 solde insuffisant, 400 validation.

### assistant-service (REST)

`POST /api/assistant/chat`

```json
{"message": "Quels sont les comptes du client CUST-001 ?", "conversationId": "optionnel"}
```

La réponse contient `answer` et un `conversationId` à réutiliser pour garder le contexte (mémoire de 20 messages, en RAM).

### Outils MCP

| Service | Outils |
|---|---|
| `account-service` | `getAccount`, `getBalance`, `listAccountsByCustomer` |
| `transaction-service` | `createTransaction`, `listTransactionsByAccount`, `listTransactions`, `getTransaction` |

### Événement Kafka

Topic `bank.transactions.created`, clé = `accountId`, valeur JSON publiée après le commit de la transaction :

```json
{
  "eventId": "UUID",
  "eventType": "TransactionCreated",
  "transactionId": "UUID",
  "accountId": "UUID",
  "amount": 250.00,
  "currency": "EUR",
  "occurredAt": "2026-01-15T10:30:00Z"
}
```

## Tests avec un client REST

Tout passe par la gateway sur `localhost:8080`.

Comptes :

```bash
curl "http://localhost:8080/api/accounts?customerReference=CUST-001"

curl -X POST http://localhost:8080/api/accounts -H "Content-Type: application/json" \
  -d '{"customerReference":"CUST-005","currency":"MAD","initialBalance":1000}'
```

Assistant (`transaction-service` n'a pas encore de contrôleur REST, les transactions passent par l'assistant) :

```bash
curl -X POST http://localhost:8080/api/assistant/chat -H "Content-Type: application/json" \
  -d '{"message":"Débite 200 MAD du compte MAD de CUST-001, commerçant GROCERY, pays MA"}'
```

L'assistant résume l'opération et demande confirmation. Répondre ensuite avec le même `conversationId` :

```bash
curl -X POST http://localhost:8080/api/assistant/chat -H "Content-Type: application/json" \
  -d '{"message":"Oui, confirme","conversationId":"<id reçu>"}'
```

Cas de refus à vérifier (plafond à 1000) : montant de 1500, compte de CUST-004 (bloqué), devise USD sur un compte MAD, montant de 900 sur le compte de CUST-002 (solde 500).

Kafka : après une transaction réussie, les logs de `transaction-service` indiquent la publication et ceux du consumer affichent « Événement reçu ». Pour voir le message brut :

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:9092 --topic bank.transactions.created \
  --from-beginning --property print.key=true
```

Un refus (par exemple un montant de 1500) ne doit produire aucun événement.

## Dépannage

| Symptôme | Piste |
|---|---|
| `transaction-service` ne démarre pas | `config-service` n'est pas encore `healthy` : `docker compose logs config-service` |
| `assistant-service` s'arrête au démarrage | Clé `ANTHROPIC_API_KEY` vide dans `.env`, ou un serveur MCP non joignable |
| Erreur de build « release version 21 not supported » | Le JDK du projet n'est pas 17 : mettre `JAVA_VERSION=21` dans `.env` |
| Un service reste `unhealthy` | `docker compose logs <service>` ; vérifier l'actuator pour `account-service` et `transaction-service` |
| Port déjà utilisé | Changer la partie gauche du mapping dans `docker-compose.yml` (par exemple `"9080:8080"`) |

## État d'avancement

Réalisé : `account-service` (REST, MCP), `transaction-service` (règles métier, MCP, Feign, publication Kafka), gateway, Eureka, config-service, assistant IA, consumer Kafka, conteneurisation avec Docker Compose.

À faire :

- Contrôleur REST de `transaction-service` (`/api/transactions`)
- `web/graphql/` pour les deux services
- Virement entre deux comptes
- Keycloak (authentification et autorisations)
- GitHub Actions
- Interface Angular, connecteurs Telegram et Discord
- Détection de fraude (Python, ML)

## Point d'attention

- `balance` est un `double` (modèle de l'énoncé). `BigDecimal` serait plus sûr pour de l'argent.
- Pas de transaction distribuée : si Kafka est indisponible juste après le commit, l'événement est perdu (erreur journalisée). Le pattern outbox règlerait ce point.
- Les bases H2 sont en mémoire : les données sont recréées à chaque démarrage des conteneurs.
- Les serveurs MCP sont appelés en direct sur leurs ports, sans passer par Eureka ni la gateway, et sans authentification.
- La console H2 est ouverte à l'extérieur du conteneur (`WEB_ALLOW_OTHERS`) et les ports des services sont publiés : à réserver au développement.
