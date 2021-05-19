FROM tomcat:9-jdk11-slim

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"

RUN rm -rf /usr/local/tomcat/webapps/*

COPY target/regvalidate.war /usr/local/tomcat/webapps/
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]
