#!/bin/sh
set -eu

PORT="${PORT:-8080}"
ASADMIN="asadmin --user ${AS_USER:-admin} --passwordfile ${AS_PASSWORD_FILE:-/password.txt}"
DOMAIN_STARTED=false

stop_configuration_domain() {
    if [ "$DOMAIN_STARTED" = true ]; then
        asadmin stop-domain >/dev/null 2>&1 || true
    fi
}

trap stop_configuration_domain EXIT INT TERM

asadmin start-domain
DOMAIN_STARTED=true

$ASADMIN set "server-config.network-config.network-listeners.network-listener.http-listener-1.port=$PORT"

if [ -n "${JAWSDB_URL:-}" ]; then
    database_url="${JAWSDB_URL#*://}"
    credentials="${database_url%%@*}"
    host_and_path="${database_url#*@}"
    host_and_port="${host_and_path%%/*}"

    DB_USER="${credentials%%:*}"
    DB_PASSWORD="${credentials#*:}"
    DB_HOST="${host_and_port%%:*}"
    DB_PORT="${host_and_port#*:}"
    DB_NAME="${host_and_path#*/}"
    DB_NAME="${DB_NAME%%\?*}"

    $ASADMIN set "resources.jdbc-connection-pool.MacnaBankingPool.property.serverName=$DB_HOST"
    $ASADMIN set "resources.jdbc-connection-pool.MacnaBankingPool.property.portNumber=$DB_PORT"
    $ASADMIN set "resources.jdbc-connection-pool.MacnaBankingPool.property.databaseName=$DB_NAME"
    $ASADMIN set "resources.jdbc-connection-pool.MacnaBankingPool.property.user=$DB_USER"
    $ASADMIN set "resources.jdbc-connection-pool.MacnaBankingPool.property.password=$DB_PASSWORD"

    export MYSQL_PWD="$DB_PASSWORD"
    sed '1,/USE macna_banking;/d' /opt/macna/schema.sql \
        | mysql --protocol=TCP --ssl-mode=REQUIRED \
            --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" "$DB_NAME"
    unset MYSQL_PWD
fi

asadmin stop-domain
DOMAIN_STARTED=false
trap - EXIT INT TERM

exec asadmin start-domain --verbose
