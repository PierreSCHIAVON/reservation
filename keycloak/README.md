# Configuration Keycloak

Ce dossier contient la configuration Keycloak utilisée par le projet.

La configuration est importée automatiquement au démarrage du conteneur grâce à
l’option `--import-realm`.

## Contenu inclus
- Realm : reservation
- Rôles du realm : USER, OWNER, ADMIN
- Client public : reservation-frontend (Angular)

## Contenu non inclus
- Utilisateurs
- Mots de passe
- Secrets

Les utilisateurs doivent être créés manuellement dans les environnements locaux
ou via des scripts qui ne sont pas versionnés dans le dépôt.

Ce dossier peut être versionné en toute sécurité dans un dépôt public.
