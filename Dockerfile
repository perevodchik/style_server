FROM openjdk:14-alpine
COPY build/libs/style_server-*-all.jar style_server.jar
EXPOSE 8080
CMD ["java", "-Dcom.sun.management.jmxremote", "-Xmx128m", "-jar", "style_server.jar"]