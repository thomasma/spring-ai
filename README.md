# Spring AI Demo Application

A comprehensive Spring Boot application demonstrating integration with OpenAI's GPT, DALL-E, and Embeddings APIs using the Spring AI framework. Includes implementations of basic chat, image generation, prompt templates, and Retrieval-Augmented Generation (RAG).

**Blog Post:** [Integrating Spring AI Framework in Your Java Application](https://blogs.justenougharchitecture.com/integrating-spring-ai-framework-in-your-java-application/)

## Table of Contents

- [Prerequisites](#prerequisites)
- [Setup](#setup)
- [Running the Application](#running-the-application)
- [Features](#features)
- [API Endpoints](#api-endpoints)
- [RAG (Retrieval-Augmented Generation)](#rag-retrieval-augmented-generation)
- [Testing the API](#testing-the-api)
- [Project Structure](#project-structure)
- [Configuration](#configuration)

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- OpenAI API key ([Get one here](https://platform.openai.com/api-keys))

## Setup

1. **Set your OpenAI API key as an environment variable:**

   ```bash
   export OPENAI_API_KEY=your-api-key-here
   ```

2. **Configure the application (optional):**

   The application uses the following defaults in [application.properties](src/main/resources/application.properties):
   ```properties
   # Chat Configuration
   spring.ai.openai.model=gpt-4o
   spring.ai.openai.api-key=${OPENAI_API_KEY}
   spring.ai.openai.temperature=0.7

   # Embedding Configuration (for RAG)
   spring.ai.openai.embedding.options.model=text-embedding-3-small
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

The application will start on `http://localhost:8080`

## Features

### 1. Chat Completion
Process custom prompts with configurable GPT models and temperature settings.

### 2. Image Generation
Generate images using DALL-E 3 with customizable prompts.

### 3. Horoscope Predictions
Demonstrate prompt templates with variable substitution using zodiac signs and date ranges.

### 4. RAG (Retrieval-Augmented Generation)
Query PDF documents using semantic search and context-grounded AI responses.

## API Endpoints

### Chat & Image Generation

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/chat` | POST | Send a chat prompt to GPT-4 |
| `/api/image/gen` | GET | Generate an image with DALL-E 3 |
| `/api/horoscope` | POST | Get AI-generated horoscope predictions |

### RAG Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/rag/status` | GET | Check document store status |
| `/api/rag/ingest` | POST | Ingest PDF documents into vector store |
| `/api/rag/chat` | POST | Ask questions about ingested documents |
| `/api/rag/search` | GET | Search for similar document chunks |
| `/api/rag/clear` | POST | Clear all documents from store |

## RAG (Retrieval-Augmented Generation)

This application includes a complete RAG implementation that allows you to query PDF documents using AI.

### Quick Start with RAG

1. **Check RAG status:**
   ```bash
   curl http://localhost:8080/api/rag/status
   ```

2. **Ingest a PDF document:**
   ```bash
   curl -X POST http://localhost:8080/api/rag/ingest \
     -H "Content-Type: application/json" \
     -d '{"filePath": "/path/to/your/document.pdf"}'
   ```

3. **Ask questions about the document:**
   ```bash
   curl -X POST http://localhost:8080/api/rag/chat \
     -H "Content-Type: application/json" \
     -d '{"question": "What are the key findings?", "topK": 3}'
   ```

### How RAG Works

```
PDF Document → Text Extraction → Chunking → Embedding → Vector Store
                                                             ↓
User Question → Embedding → Similarity Search → Context → LLM → Answer
```

1. **Document Ingestion**: PDF is extracted and split into chunks (1000 chars with 200 char overlap)
2. **Embedding**: Each chunk is converted to a vector using OpenAI's text-embedding-3-small
3. **Storage**: Vectors stored in an in-memory vector database
4. **Query**: User questions are embedded and matched against stored chunks
5. **Response**: Most relevant chunks are used as context for GPT-4 to generate accurate answers

For detailed RAG documentation, see [RAG-README.md](RAG-README.md)

## Testing the API

### Using Postman

Import the provided [Postman collection](Spring-AI-Demo.postman_collection.json) to test all endpoints.

### Using cURL

**Basic Chat:**
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Explain Spring Framework in simple terms"}'
```

**Generate Image:**
```bash
curl "http://localhost:8080/api/image/gen?message=A+serene+mountain+landscape+at+sunset"
```

**Get Horoscope:**
```bash
curl -X POST http://localhost:8080/api/horoscope \
  -H "Content-Type: application/json" \
  -d '{"zodiacSign": "Leo", "days": 7}'
```

**RAG Chat (after ingesting a document):**
```bash
curl -X POST http://localhost:8080/api/rag/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the main topics covered?", "topK": 3}'
```

## Configuration

### Model Configuration

Edit [application.properties](src/main/resources/application.properties) to change models:

```properties
# Chat Model
spring.ai.openai.model=gpt-4o
# Options: gpt-4o, gpt-4o-mini, gpt-4-turbo, gpt-3.5-turbo

# Embedding Model (for RAG)
spring.ai.openai.embedding.options.model=text-embedding-3-small
# Options: text-embedding-3-small, text-embedding-3-large, text-embedding-ada-002

# Temperature (0.0 = deterministic, 2.0 = creative)
spring.ai.openai.temperature=0.7
```

### RAG Configuration

Configure chunk size and overlap in [RagService.java](src/main/java/com/tryout/ai/RagService.java):

```java
private static final int CHUNK_SIZE = 1000;      // Characters per chunk
private static final int CHUNK_OVERLAP = 200;    // Overlap for context
```

Adjust the chunk limit for demo purposes (line 92):
```java
int maxChunks = Math.min(chunks.size(), 50);  // Change 50 to process more chunks
```

## Key Components

### ChatService
- Basic chat completion with GPT models
- Prompt template demonstration (horoscope)
- Image generation with DALL-E 3
- Input validation and security checks

### RagService
- PDF text extraction using Apache PDFBox
- Intelligent text chunking with overlap
- OpenAI embedding generation
- In-memory vector store with cosine similarity
- Context-grounded question answering

## Security Features

- Input validation for all endpoints
- Prompt injection protection
- Temperature and parameter bounds checking

## Benefits of RAG

1. **Up-to-date Information**: Query your latest documents without retraining models
2. **Source Attribution**: Know which documents informed the answer
3. **Cost-Effective**: Cheaper than fine-tuning for domain-specific knowledge
4. **Flexible**: Add/remove documents without model changes
5. **Accurate**: Responses grounded in your specific content

## Production Considerations

For production deployments, consider:

1. **Vector Database**: Replace in-memory store with PostgreSQL + pgvector, Pinecone, or Qdrant
2. **Async Processing**: Use async ingestion for large documents
3. **Caching**: Cache embeddings and frequently asked questions
4. **Rate Limiting**: Implement rate limits for API endpoints
5. **Authentication**: Add security for document ingestion endpoints
6. **Monitoring**: Add metrics and logging for vector search performance

## Troubleshooting

### OutOfMemoryError with Large PDFs

Increase heap size when running:
```bash
MAVEN_OPTS="-Xmx4g" mvn spring-boot:run
```

### Port Already in Use

Change the port in application.properties and/or kill any orphan processes:
```properties
server.port=8081
```

### OpenAI API Rate Limits

The embedding API has rate limits. For large documents, the service automatically limits to 50 chunks. Adjust this in RagService.java if needed.