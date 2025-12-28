## Overview

A Spring Boot application demonstrating integration with OpenAI's GPT and DALL-E models using Spring AI framework. Also refer to blog at https://blogs.justenougharchitecture.com/integrating-spring-ai-framework-in-your-java-application/

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- OpenAI API key

## Setup

1. **Set your OpenAI API key as an environment variable:**

   ```bash
   export OPENAI_API_KEY=your-api-key-here
   ```

2. **Configure the application (optional):**

   The application is pre-configured to use GPT-5.2 in [application.properties](src/main/resources/application.properties):
   ```properties
   spring.ai.openai.model=gpt-5.2
   spring.ai.openai.api-key=${OPENAI_API_KEY}
   ```

## Running the Application

### Using Maven

```bash
mvn spring-boot:run
```

### Using Maven Wrapper

```bash
./mvnw spring-boot:run
```

### Build and Run JAR

```bash
mvn clean package
java -jar target/spring-ai-0.0.1-SNAPSHOT.jar
```

## Features

The application provides the following AI capabilities through the `ChatService`:

- **Process custom prompts** with configurable GPT models and temperature settings
- **Generate horoscope predictions** based on zodiac signs
- **Generate images** using DALL-E 3

## Testing the API

Import the provided [Postman collection](Spring-AI-API.postman_collection.json) to test the available endpoints.

Or use the curl command line below to test one of the APIs
```bash
curl -X POST -H "Content-Type: application/json" http://localhost:8080/api/chat -d '{"prompt": "who are you?"}'
```