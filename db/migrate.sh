#!/bin/bash

# This script runs the Liquibase migrations in the db directory.
# It uses the environment variables specified in the .env file to authenticate to the database.

username=$(find ~ -name "button.env" -exec grep -m 1 "DB_USER" {} \; | cut -d '=' -f 2)
password=$(find ~ -name "button.env" -exec grep -m 1 "DB_PASSWORD" {} \; | cut -d '=' -f 2)
db_name=$(find ~ -name "button.env" -exec grep -m 1 "DB_NAME" {} \; | cut -d '=' -f 2)

liquibase updateSql --url=jdbc:postgresql://localhost:5432/"$db_name" --username="$username" --password="$password" || exit 1

liquibase migrate --url=jdbc:postgresql://localhost:5432/"$db_name" --username="$username" --password="$password" || exit 1