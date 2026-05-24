#!/bin/bash

# This script runs the Liquibase migrations in the db directory.
# It uses the environment variables specified in the .env file to authenticate to the database.

# Liquibase resolves liquibase.properties and changelog paths relative to pwd.
cd "$(dirname "$0")"

username=$DB_USER
password=$DB_PASSWORD
db_name=$DB_NAME

liquibase updateSql --url=jdbc:postgresql://localhost:5432/"$db_name" --username="$username" --password="$password" || exit 1

liquibase migrate --url=jdbc:postgresql://localhost:5432/"$db_name" --username="$username" --password="$password" || exit 1