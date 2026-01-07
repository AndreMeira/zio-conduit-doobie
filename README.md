# Conduit - RealWorld API Implementation

[![Scala](https://img.shields.io/badge/Scala-3-red.svg)](https://www.scala-lang.org/)
[![ZIO](https://img.shields.io/badge/ZIO-2-blue.svg)](https://zio.dev/)
[![Doobie](https://img.shields.io/badge/Doobie-1.0-green.svg)](https://tpolecat.github.io/doobie/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)

A production-ready implementation of the [RealWorld](https://realworld-docs.netlify.app/) specification - a blogging platform similar to Medium.com. This backend API demonstrates modern functional programming principles using Scala 3, ZIO, and Clean Architecture patterns.

## 🚀 What is Conduit?

Conduit is a social blogging platform that allows users to:
- **Create accounts** with secure authentication (JWT-based)
- **Write and publish articles** with rich metadata and tagging
- **Follow other authors** and curate personalized feeds
- **Engage with content** through comments and favorites
- **Discover content** through tags and author filtering

This implementation serves as a **reference architecture** for building production-ready Scala applications with:
- ✅ **Type-safe domain modeling** with compile-time guarantees
- ✅ **Functional error handling** with structured error types
- ✅ **Clean Architecture** with proper layer separation
- ✅ **Multiple deployment strategies** (in-memory, local, production)
- ✅ **Comprehensive observability** with OpenTelemetry
- ✅ **Database migrations** with Flyway
- ✅ **Container-ready** with Docker support

## 🏗️ Architecture Deep Dive

### Clean Architecture Implementation

This project follows **Clean Architecture** principles with clear separation of concerns across three main layers:

```
┌────────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                           │
│  ┌─────────────────┐  ┌─────────────────────┐  ┌─────────────┐ │
│  │   HTTP Routes   │  │   Middleware        │  │   Modules   │ │
│  │  - UserRoutes   │  │  - ErrorMiddleware  │  │  - HttpApp  │ │
│  │  - ArticleRoutes│  │  - MonitorMiddleware│  │  - Config   │ │
│  │  - CommentRoutes│  │  - CORS             │  │             │ │
│  └─────────────────┘  └─────────────────────┘  └─────────────┘ │
└────────────────────────────────────────────────────────────────┘
                               │                                  
┌────────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                              │
│  ┌──────────────────┐  ┌─────────────────┐  ┌─────────────┐    │
│  │   Entrypoints    │  │   Validation    │  │   Models    │    │
│  │  - UserEntrypoint│  │  - UserValidator│  │  - Entities │    │
│  │  - ArticleEntry. │  │  - ArticleValid.│  │  - Types    │    │
│  │  - CommentEntry. │  │  - CommentValid.│  │  - Errors   │    │
│  └──────────────────┘  └─────────────────┘  └─────────────┘    │
│  ┌──────────────────┐  ┌─────────────────┐  ┌─────────────┐    │
│  │   Repositories   │  │   Auth & Logic  │  │  Responses  │    │
│  │  - UserRepo      │  │  - Authenticator│  │  - UserResp │    │
│  │  - ArticleRepo   │  │  - Authorization│  │  - ArticleR.│    │
│  │  - CommentRepo   │  │  - Monitor      │  │  - CommentR.│    │
│  └──────────────────┘  └─────────────────┘  └─────────────┘    │
└────────────────────────────────────────────────────────────────┘
                               │                                  
┌────────────────────────────────────────────────────────────────┐
│                  INFRASTRUCTURE LAYER                          │
│  ┌──────────────────┐  ┌─────────────────┐  ┌──────────────┐   │
│  │   PostgreSQL     │  │   In-Memory     │  │ Observability│   │
│  │  - Doobie        │  │  - Concurrent   │  │  - OpenTel.  │   │
│  │  - Migrations    │  │  - STM/Ref      │  │  - Tracing   │   │
│  │  - Transactions  │  │  - Fast Testing │  │  - Metrics   │   │
│  └──────────────────┘  └─────────────────┘  └──────────────┘   │
└────────────────────────────────────────────────────────────────┘
```

### Key Architectural Decisions

#### 1. **Type-Safe Domain Modeling**
Uses **ZIO Prelude Subtypes** to create compile-time safe wrappers around primitive types:

```scala
// Type-safe identifiers prevent mixing different ID types
type UserId = UserId.Type
type ArticleId = ArticleId.Type

// Validated types ensure data integrity
type Email = Email.Type  // Must contain "@"
type ArticleSlug = ArticleSlug.Type  // URL-normalized
```

#### 2. **Functional Error Handling**
Structured error hierarchy with specific error types:

```scala
trait ApplicationError {
  def message: String
  def kind: String
}

// Specific error categories
trait NotFoundError extends ApplicationError
trait ValidationError extends ApplicationError
trait UnauthorisedError extends ApplicationError
```

#### 3. **Parse Don't Validate**
Following [Alexis King's principle](https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/), data is parsed once into validated domain types rather than repeatedly validated:

```scala
// Raw input → Validated domain type
def parse(request: RegistrationRequest): Result[Registration] = {
  // Returns validated (UserProfile.Data, Credentials.Clear) or errors
}
```

## 🛠️ Technology Stack

### Core Technologies

| Technology | Purpose | Why Chosen |
|------------|---------|------------|
| **Scala 3** | Programming Language | Modern syntax, improved type system, better performance |
| **ZIO 2** | Effect System | Type-safe concurrency, resource management, dependency injection |
| **ZIO HTTP** | Web Framework | High-performance, purely functional HTTP handling |
| **Doobie** | Database Access | Type-safe JDBC, functional database interactions |
| **ZIO Prelude** | Type Classes | Advanced type safety, validation, data structures |
| **PostgreSQL** | Database | ACID compliance, JSON support, mature ecosystem |
| **OpenTelemetry** | Observability | Industry-standard tracing and metrics |
| **Flyway** | Database Migrations | Version-controlled schema evolution |
| **JWT** | Authentication | Stateless, secure token-based auth |

### Supporting Tools

- **Docker & Docker Compose** - Containerization and local development
- **SBT** - Build tool with dependency management
- **ZIO Test** - Property-based and unit testing
- **Jaeger** - Distributed tracing visualization

## 🎯 Key Features

### Authentication & Authorization
- **JWT-based authentication** with secure password hashing (BCrypt)
- **Role-based access control** for articles and comments
- **Token refresh** and expiration handling
- **Cross-origin resource sharing (CORS)** support

### Content Management
- **CRUD operations** for articles with rich metadata
- **Tag-based categorization** for content discovery
- **URL-friendly slugs** with Unicode normalization

### Social Features
- **User following system** for personalized feeds
- **Article favoriting** with aggregate counts
- **Commenting system** with nested discussions
- **User profiles** with biographical information

### Developer Experience
- **Multiple deployment modes** for different environments

## 📁 Project Structure

```
src/main/scala/conduit/
├── application/              # Application Layer
│   ├── http/                # HTTP-specific concerns
│   │   ├── route/           # Route definitions
│   │   ├── middleware/      # HTTP middleware
│   │   ├── service/         # HTTP services
│   │   └── HttpApplication.scala
│   └── migration/           # Database migration runner
├── domain/                  # Domain Layer (Business Logic)
│   ├── model/              # Domain Models
│   │   ├── entity/         # Core business entities
│   │   ├── types/          # Type-safe wrappers
│   │   ├── error/          # Domain-specific errors
│   │   ├── request/        # Request models
│   │   └── response/       # Response models
│   ├── logic/              # Business Logic
│   │   ├── entrypoint/     # Use case orchestration
│   │   ├── validation/     # Input validation
│   │   ├── authentication/ # Auth logic
│   │   ├── authorization/  # Access control
│   │   └── persistence/    # Repository interfaces
│   └── service/            # Service implementations
└── infrastructure/          # Infrastructure Layer
    ├── postgres/           # PostgreSQL implementation
    ├── inmemory/           # In-memory implementation
    ├── opentelemetry/      # Observability setup
    └── configuration/      # Configuration management
```

## 🎭 Core Domain Model

### User Management

```scala
// Authentication state
enum User:
  case Anonymous
  case Authenticated(userId: UserId)

// Profile information
case class UserProfile(
  id: UserId,
  data: UserProfile.Data,
  metadata: UserProfile.Metadata
)

// Secure credential handling
enum Credentials:
  case Clear(email: Email, password: Password)
  case Hashed(email: Email, password: HashedPassword)
```

### Content Model

```scala
// Articles with rich metadata
case class Article(
  id: ArticleId,
  data: Article.Data,        // Title, body, description, author
  metadata: Article.Metadata // Created/updated timestamps
)

// Comments for discussions
case class Comment(
  id: CommentId,
  data: Comment.Data,        // Body, article, author
  metadata: Comment.Metadata
)

// Social relationships
case class Follower(by: UserId, author: AuthorId)
case class FavoriteArticle(by: UserId, article: ArticleId)
```

### Type Safety Examples

```scala
// Email validation with normalization
object Email extends Subtype[String] {
  def validated(email: String): Validation[Email.Error, Email] =
    if email.contains("@")
    then Validation.succeed(Email(email.toLowerCase))
    else Validation.fail(Error.InvalidEmail(email))
}

// Article slugs with Unicode normalization
object ArticleSlug extends Subtype[String] {
  def normalize(slug: String): String =
    Normalizer.normalize(slug.toLowerCase, Normalizer.Form.NFD)
      .replaceAll("\\p{M}", "")
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("(^-|-$)", "")
}
```

## 🚦 Running the Application

### Prerequisites

- **Java 11+** (JDK)
- **SBT 1.8+** (Scala Build Tool)
- **Docker & Docker Compose** (for database mode)

### Development Modes

#### 1. In-Memory Mode (Fastest)
Perfect for frontend development, testing, and demonstrations:

```bash
# Start the server with in-memory storage
sbt "runMain conduit.application.http.HttpApplication inmemory"

# API available at: http://localhost:8080
# Trace visualization: ./html/last-trace.html
```

**Features:**
- No database required
- Fast startup (~2 seconds)
- HTML-based trace visualization
- Perfect for rapid iteration

#### 2. Local Development Mode
Full-featured development with database persistence:

```bash
# Start PostgreSQL and Jaeger
docker-compose up -d

# Run with database (includes migrations)
sbt "runMain conduit.application.http.HttpApplication local"

# API available at: http://localhost:8080
# Jaeger UI: http://localhost:16686
```

**Features:**
- PostgreSQL persistence
- Automatic schema migrations
- OpenTelemetry tracing
- Jaeger trace visualization

#### 3. Production Mode
Optimized for deployment:

```bash
# Run migrations separately (recommended for production)
sbt "runMain conduit.application.http.HttpApplication migration"

# Start the application (expects migrated database)
sbt "runMain conduit.application.http.HttpApplication live"
```

### Docker Deployment

#### Build Container Image
```bash
./bin/dockerize.sh
```

#### Run in Container
```bash
# In-memory mode
./bin/run-in-docker.sh "inmemory"

# Local development mode (requires docker-compose up)
./bin/run-in-docker.sh "local"

# Production mode (requires pre-migrated database)
./bin/run-in-docker.sh "live"

# Migration only
./bin/run-in-docker.sh "migration"
```

## 🔌 API Endpoints

### Authentication
- `POST /api/users/login` - User authentication
- `POST /api/users` - User registration
- `GET /api/user` - Current user info
- `PUT /api/user` - Update user info

### Profiles
- `GET /api/profiles/:username` - Get user profile
- `POST /api/profiles/:username/follow` - Follow user
- `DELETE /api/profiles/:username/follow` - Unfollow user

### Articles
- `GET /api/articles` - List articles (with filtering)
- `GET /api/articles/feed` - Personal article feed
- `POST /api/articles` - Create article
- `GET /api/articles/:slug` - Get article by slug
- `PUT /api/articles/:slug` - Update article
- `DELETE /api/articles/:slug` - Delete article
- `POST /api/articles/:slug/favorite` - Favorite article
- `DELETE /api/articles/:slug/favorite` - Unfavorite article

### Comments
- `GET /api/articles/:slug/comments` - Get article comments
- `POST /api/articles/:slug/comments` - Add comment
- `DELETE /api/articles/:slug/comments/:id` - Delete comment

### Tags
- `GET /api/tags` - Get all tags

### Example Requests

#### Create Article
```bash
curl -X POST http://localhost:8080/api/articles \
  -H "Authorization: Token jwt.token.here" \
  -H "Content-Type: application/json" \
  -d '{
    "article": {
      "title": "How to build APIs with ZIO",
      "description": "A comprehensive guide",
      "body": "ZIO provides powerful abstractions...",
      "tagList": ["zio", "scala", "functional-programming"]
    }
  }'
```

#### Follow User
```bash
curl -X POST http://localhost:8080/api/profiles/john/follow \
  -H "Authorization: Token jwt.token.here"
```

## 🧪 Testing Strategy

### Test Structure
```bash
src/test/scala/conduit/
├── integration/        # Integration tests
├── unit/              # Unit tests
└── fixtures/          # Test data and utilities
```

### Running Tests

```bash
# Run all tests
sbt test

# Run specific test suite
sbt "testOnly *UserServiceSpec"

# Run tests with coverage
sbt coverage test coverageReport
```

### Test Technologies
- **ZIO Test** - Property-based and unit testing
- **TestContainers** - Integration testing with real databases
- **ScalaCheck** - Property-based testing
- **Mock libraries** - For external service mocking

## 📊 Monitoring & Observability

### OpenTelemetry Integration

The application includes comprehensive observability:

#### Tracing
- **HTTP requests** with timing and status codes
- **Database queries** with query plans and performance
- **Business operations** with custom spans
- **Error tracking** with stack traces and context

#### Metrics
- **Request throughput** and latency percentiles
- **Database connection pool** utilization
- **JVM metrics** (memory, GC, threads)
- **Custom business metrics** (user registrations, article creation)

#### Logging
- **Structured logging** with JSON output
- **Correlation IDs** across request boundaries
- **Error aggregation** with context preservation
- **Performance logging** for slow operations

### Visualization

#### Jaeger (Local Development)
Access distributed traces at `http://localhost:16686`

#### HTML Traces (In-Memory Mode)
Simple trace visualization in `./html/last-trace.html`

#### Production Monitoring
Compatible with:
- **Jaeger** or **Zipkin** for distributed tracing
- **Prometheus** for metrics collection
- **Grafana** for dashboards
- **ELK Stack** for log aggregation

## 🔧 Development Workflow

### Code Organization Principles

1. **Domain-Driven Design** - Business logic in the domain layer
2. **Dependency Inversion** - Infrastructure depends on domain abstractions
3. **Single Responsibility** - Each module has one reason to change
4. **Type Safety First** - Leverage the type system to prevent errors

### Adding New Features

#### 1. Define Domain Model
```scala
// Add to domain/model/entity/
case class NewEntity(
  id: NewEntityId,
  data: NewEntity.Data,
  metadata: NewEntity.Metadata
)
```

#### 2. Create Repository Interface
```scala
// Add to domain/logic/persistence/
trait NewEntityRepository[Tx] {
  def save(entity: NewEntity): Result[NewEntityId]
  def find(id: NewEntityId): Result[Option[NewEntity]]
}
```

#### 3. Implement Validation
```scala
// Add to domain/logic/validation/
trait NewEntityValidator[Tx] {
  def parse(request: CreateNewEntityRequest): Result[NewEntity.Data]
}
```

#### 4. Create Entrypoint
```scala
// Add to domain/logic/entrypoint/
trait NewEntityEntrypoint {
  def create(request: CreateNewEntityRequest): Result[NewEntityResponse]
}
```

#### 5. Add HTTP Routes
```scala
// Add to application/http/route/
object NewEntityRoutes {
  def routes: Routes[Any, ApplicationError] = ???
}
```

### Code Style Guidelines

- **Immutable by default** - Use `val` over `var`
- **Explicit types** for public APIs
- **Meaningful names** that reflect domain concepts
- **Small functions** with single responsibilities
- **Comprehensive error handling** with typed errors

## 🏗️ Advanced Topics

### Transaction Management

The application supports multiple transaction strategies:

```scala
// PostgreSQL with Doobie
class PostgresUserRepository extends UserRepository[PostgresTransaction] {
  def save(user: User): ZIO[PostgresTransaction, Error, UserId] = ???
}

// In-memory with STM
class InMemoryUserRepository extends UserRepository[MemoryTransaction] {
  def save(user: User): ZIO[MemoryTransaction, Error, UserId] = ???
}
```

### Configuration Management

Type-safe configuration with ZIO Config:

```scala
case class DatabaseConfig(
  host: String,
  port: Int,
  database: String,
  username: String,
  password: String
)

case class HttpConfig(
  host: String,
  port: Int
)
```

### Error Recovery Strategies

- **Retry policies** for transient failures
- **Circuit breakers** for external service calls
- **Graceful degradation** for non-critical features
- **Structured error responses** for client debugging

### Performance Optimizations

- **Connection pooling** with HikariCP
- **Query optimization** with indexed database access
- **Response caching** for expensive operations
- **Lazy loading** for optional associations

## 🤝 Contributing

### Getting Started

1. **Fork** the repository
2. **Clone** your fork locally
3. **Create a feature branch** from `main`
4. **Make changes** following the code style guidelines
5. **Add tests** for new functionality
6. **Run the test suite** to ensure nothing breaks
7. **Submit a pull request** with clear description

### Development Setup

```bash
# Clone the repository
git clone https://github.com/your-username/zio-conduit-doobie.git
cd zio-conduit-doobie

# Start development environment
docker-compose up -d

# Run tests to ensure everything works
sbt test

# Start development server
sbt "runMain conduit.application.http.HttpApplication local"
```

### Contribution Areas

- 🐛 **Bug fixes** - Issue resolution and edge case handling
- ✨ **New features** - RealWorld spec extensions
- 📚 **Documentation** - API docs, tutorials, examples
- 🧪 **Testing** - Improved test coverage and scenarios
- ⚡ **Performance** - Optimization and benchmarking
- 🔒 **Security** - Vulnerability assessment and fixes

### Code Review Process

1. All changes require **peer review**
2. **Automated tests** must pass
3. **Documentation** should be updated for API changes
4. **Breaking changes** need migration guides
5. **Performance impacts** should be benchmarked

## 📖 Additional Resources

### Learning Materials
- [ZIO Official Documentation](https://zio.dev/overview/)
- [Doobie Documentation](https://tpolecat.github.io/doobie/)
- [Clean Architecture in Scala](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [RealWorld Specification](https://realworld-docs.netlify.app/docs/specs/backend-specs/introduction)

### Related Projects
- [RealWorld Implementations](https://github.com/gothinkster/realworld) - Various language implementations
- [ZIO Examples](https://github.com/zio/zio-examples) - ZIO usage patterns
- [Scala 3 Migration Guide](https://scalacenter.github.io/scala-3-migration-guide/) - Scala 3 features

### Community
- [ZIO Discord](https://discord.gg/2ccFBr4) - ZIO community chat
- [Scala Users Forum](https://users.scala-lang.org/) - General Scala discussions
- [Typelevel Discord](https://discord.gg/XF3CXcMzqD) - Functional programming in Scala

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Built with ❤️ using Scala 3, ZIO, and functional programming principles.**

*This implementation demonstrates production-ready patterns for building type-safe, maintainable, and observable web applications in Scala.*