# Aurel — Minecraft Java 26.2 / Fabric

Menu client en Java 25, inspiré de la disposition de la référence : barre latérale,
panneaux sombres, accents cyan, interrupteurs et curseurs. Aucun module de gameplay
n'est ajouté.

Dans une partie, **Inser** ouvre ou ferme le menu. La touche est modifiable dans
**Options → Contrôles → Aurel**. **Échap** ferme aussi le menu. Le jeu continue
de tourner pendant son ouverture.

- **Apparence** : transparence, opacité, arrondis, ombre, assombrissement du jeu et bordures.
- **Couleurs** : deux teintes, saturation, luminosité, dégradé et orientation.
- **Animations** : activation, glissement, durée, réactivité et échelle.
- **Compte** : pseudo et modèle du joueur en cours, avec son skin réel.
- **Préréglages** : Océan, Améthyste, Jade et Crépuscule ; réinitialisation confirmée par un second clic.

Les modifications sont visibles immédiatement et sauvegardées à la fermeture,
ou avec **Enregistrer**, dans `config/aurel.properties`. Une écriture temporaire
puis un remplacement atomique protège la sauvegarde. En cas d'erreur, le menu
l'indique ; fermer une seconde fois permet de quitter sans rester bloqué.

## Installer le mod

Utiliser Minecraft **26.2**, **Java 25**, **Fabric Loader 0.19.5 ou supérieur** et
**Fabric API pour 26.2**. Placer le JAR compilé d'Aurel dans le dossier `mods`
du jeu. Ce mod fonctionne uniquement côté client ; le serveur n'a pas à l'installer.

## Compiler

Avec un JDK 25 :

Linux / macOS :

```sh
./gradlew build
```

Windows (PowerShell ou invite de commandes) :

```powershell
.\gradlew.bat build
```

Décompresser d'abord l'archive du projet dans un dossier, puis ouvrir un terminal
dans ce dossier. Le wrapper télécharge Gradle et les dépendances nécessaires ;
une connexion Internet est requise pour la première compilation. Les vérifications
du menu s'exécutent aussi sous Windows, sans nécessiter Bash.

Le mod installable est `build/libs/aurel-menu-1.0.0.jar`.
Le fichier se terminant par `-sources.jar` contient les sources et ne doit pas
être installé dans le jeu. Les versions de Minecraft, Fabric et Loom sont fixées
dans `gradle.properties`. Le wrapper vérifie le SHA-256 de Gradle.

Dans cet environnement cloud :

```sh
python3 scripts/setup-cloud.py
```

Ce script réutilise Java 25 sous `/workspace/.tools`, ou télécharge le JDK officiel
en vérifiant son SHA-256, puis compile. Le lanceur cloud transmet le proxy du
système à Java et utilise son magasin de certificats, sans désactiver TLS.

## Vérifier / développer

```sh
JAVA_HOME=/chemin/vers/jdk-25 bash scripts/check-core.sh
./gradlew runClient
```

Le lancement du client nécessite une session graphique OpenGL. Le test manuel
consiste à entrer dans un monde, appuyer sur Inser, déplacer un curseur, changer
de palette, vérifier le pseudo et le skin, puis fermer et rouvrir le menu pour
contrôler la sauvegarde. Le raccourci réaffecté doit aussi ouvrir et fermer le menu.

Le contrôle du cœur exécute six groupes de tests : sauvegarde, configuration
endommagée, interactions souris/clavier, navigation/préréglages, rendu des cinq
pages et transitions. `./gradlew build` inclut ce contrôle ; il peut aussi être
lancé seul avec `./gradlew coreCheck` (`.\gradlew.bat coreCheck` sous Windows).

`scripts/render-preview.sh` produit une image du même moteur d'interface via Java
AWT, avec une silhouette neutre. Ce rendu hors jeu ne valide pas le rendu Minecraft.

Chaque tâche cloud est déjà isolée : réutiliser `/workspace/mine`, sans créer de
worktree Git sauf demande explicite. Les caches et sorties de build sont ignorés.
