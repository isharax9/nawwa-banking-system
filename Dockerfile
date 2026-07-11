FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
RUN mvn -DskipTests package

FROM omnifish/glassfish:7.0.5

COPY --from=build --chown=glassfish:glassfish \
    /root/.m2/repository/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar \
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
    /opt/glassfish7/glassfish/domains/domain1/autodeploy/banking-system-ear.ear
