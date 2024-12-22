#!/bin/bash

# Étape 2: Créer le répertoire pour le projet dans le répertoire personnel de l'utilisateur
PROJECT_DIR="$HOME/myhttpserver"
if [ ! -d "$PROJECT_DIR" ]; then
    mkdir -p "$PROJECT_DIR"
fi

# Copier les fichiers du projet dans le répertoire personnel de l'utilisateur
echo "Copie des fichiers du projet dans le répertoire personnel de l'utilisateur..."
cp -r ./* "$PROJECT_DIR"

# Donner les permissions 775 de manière récursive au répertoire myhttpserver
echo "Donner les permissions 775 de manière récursive au répertoire myhttpserver..."
chmod -R 775 "$PROJECT_DIR"

# Étape 3: Créer le fichier de configuration
CONFIG_FILE="$PROJECT_DIR/conf/serverhttp.conf"

echo "Création du fichier de configuration par défaut..."
cat <<EOL > $CONFIG_FILE
port=1357
rootDirectory=$PROJECT_DIR/htdocs
php=true
EOL

# Étape 4: Créer le script de démarrage du serveur
SERVER_SCRIPT="$PROJECT_DIR/start-httpserver.sh"

echo "Création du script de démarrage du serveur..."
cat <<EOL > $SERVER_SCRIPT
#!/bin/bash
echo "Starting HTTP Server..." | tee -a $PROJECT_DIR/start-httpserver.log
cd $PROJECT_DIR
if ! java -cp $PROJECT_DIR/src main.Main 2>&1 | tee -a $PROJECT_DIR/start-httpserver.log; then
  echo "Failed to start HTTP Server" | tee -a $PROJECT_DIR/start-httpserver.log
  exit 1
fi
echo "HTTP Server started." | tee -a $PROJECT_DIR/start-httpserver.log
EOL

# Rendre le script exécutable
chmod +x $SERVER_SCRIPT

# Étape 5: Créer un service systemd pour démarrer le serveur au démarrage du système
SERVICE_FILE="/etc/systemd/system/httpserver.service"
echo "Création du service systemd..."

sudo bash -c "cat > $SERVICE_FILE" <<EOL
[Unit]
Description=HTTP Server Service
After=network.target

[Service]
WorkingDirectory=$PROJECT_DIR
ExecStart=$SERVER_SCRIPT
User=$(whoami)
Restart=always

[Install]
WantedBy=multi-user.target
EOL

# Recharger les daemons systemd et activer le service
sudo systemctl daemon-reload
sudo systemctl enable httpserver.service

echo "Installation terminée. Vous pouvez démarrer le serveur avec 'sudo systemctl start httpserver'."