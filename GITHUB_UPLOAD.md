# Publication sur GitHub

## Méthode recommandée avec Git

1. Créer un dépôt GitHub vide, sans ajouter automatiquement de README, licence ou `.gitignore`.
2. Extraire l'archive et ouvrir un terminal dans le dossier `gestion-pfe-final`.
3. Exécuter les commandes suivantes en remplaçant l'URL :

   ```bash
   git init
   git add .
   git update-index --chmod=+x mvnw
   git commit -m "Initial portfolio release"
   git branch -M main
   git remote add origin https://github.com/VOTRE-UTILISATEUR/VOTRE-DEPOT.git
   git push -u origin main
   ```

## Vérification avant publication

- Ne jamais ajouter un fichier `.env`.
- Vérifier que le nom du collègue est ajouté dans `CONTRIBUTORS.md` si vous souhaitez afficher son identité complète.
- Attendre que le workflow **CI** soit vert dans l'onglet **Actions**.
- Ajouter dans la description GitHub : `Spring Boot application for PFE supervision assignment and defense scheduling.`

## En cas d'erreur `remote origin already exists`

```bash
git remote set-url origin https://github.com/VOTRE-UTILISATEUR/VOTRE-DEPOT.git
git push -u origin main
```
