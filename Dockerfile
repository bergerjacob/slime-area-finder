# Use an image that has BOTH Maven and Java 17
FROM maven:3.9-eclipse-temurin-17 as builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Create a smaller, final image with only the Java Runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the compiled JAR file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Tell Render what port the application is running on
EXPOSE 8080

# The command to run the application
CMD ["java", "-jar", "app.jar"]
