FROM maven:3.9.9-eclipse-temurin-23
WORKDIR /app
COPY . .
CMD ["mvn", "-q", "compile", "exec:java", "-Dexec.mainClass=org.example.App"]
