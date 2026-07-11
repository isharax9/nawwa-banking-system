FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml pom.xml
COPY core/pom.xml core/pom.xml
COPY banking-services/pom.xml banking-services/pom.xml
COPY transaction-services/pom.xml transaction-services/pom.xml
COPY timer-services/pom.xml timer-services/pom.xml
COPY security-module/pom.xml security-module/pom.xml
COPY notification-services/pom.xml notification-services/pom.xml
COPY web/pom.xml web/pom.xml
COPY ear/pom.xml ear/pom.xml
RUN mvn --batch-mode --no-transfer-progress -DskipTests dependency:go-offline

COPY core/src core/src
COPY banking-services/src banking-services/src
COPY transaction-services/src transaction-services/src
COPY timer-services/src timer-services/src
COPY security-module/src security-module/src
COPY web/src web/src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package \
    && cp /root/.m2/repository/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar \
        /workspace/mysql-connector-j.jar

FROM omnifish/glassfish:7.0.5

USER root
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends default-mysql-client \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build --chown=glassfish:glassfish \
    /workspace/mysql-connector-j.jar \
    /opt/glassfish7/glassfish/domains/domain1/lib/mysql-connector-j.jar

RUN asadmin start-domain \
    && asadmin --user "$AS_USER" --passwordfile "$AS_PASSWORD_FILE" create-jdbc-connection-pool \
        --datasourceclassname=com.mysql.cj.jdbc.MysqlDataSource \
        --restype=javax.sql.DataSource \
        --property='serverName=mysql:portNumber=3306:databaseName=macna_banking:user=banking_user:password=banking_password:useSSL=false:allowPublicKeyRetrieval=true:serverTimezone=UTC' \
        MacnaBankingPool \
    && asadmin --user "$AS_USER" --passwordfile "$AS_PASSWORD_FILE" create-jdbc-resource \
        --connectionpoolid=MacnaBankingPool \
        macna_jdbc_v2 \
    && asadmin stop-domain

COPY --from=build --chown=glassfish:glassfish \
    /workspace/ear/target/banking-system-ear.ear \
    /opt/macna/banking-system-ear.ear

COPY --chown=glassfish:glassfish database/schema.sql /opt/macna/schema.sql
COPY --chown=glassfish:glassfish docker/heroku-entrypoint.sh /opt/macna/heroku-entrypoint.sh

RUN chmod 755 /opt/macna/heroku-entrypoint.sh \
    && chmod -R a+rwX /opt/glassfish7/glassfish/domains/domain1

USER glassfish
CMD ["/opt/macna/heroku-entrypoint.sh"]
