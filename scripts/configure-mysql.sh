#!/bin/bash

MYSQL_SERVER=/opt/mysql/server-5.5/support-files/mysql.server

# Start MySQL service
${MYSQL_SERVER} start

# Run the secure installation script non-interactively
mysql_secure_installation <<EOF

n
y
y
y
y
EOF

# Create SAAF database and user
mysql -u root -e "CREATE DATABASE saaftest;"
mysql -u root -e "CREATE USER 'saafuser'@'localhost' IDENTIFIED BY 'saafpass';"
mysql -u root -e "GRANT ALL PRIVILEGES ON saaftest.* TO 'saafuser'@'localhost';"
mysql -u root -e "FLUSH PRIVILEGES;"

# Stop MySQL service
${MYSQL_SERVER} stop
