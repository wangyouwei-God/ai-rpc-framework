# Contributing to AI-RPC Framework

Thank you for your interest in contributing to AI-RPC Framework. This document provides guidelines for contributing to the project.

## Code of Conduct

This project adheres to the [Contributor Covenant Code of Conduct](https://www.contributor-covenant.org/version/2/1/code_of_conduct/). By participating, you are expected to uphold this code.

## How to Contribute

### Reporting Issues

Before creating an issue, please:

1. Search existing issues to avoid duplicates
2. Use the issue templates when available
3. Provide clear reproduction steps
4. Include environment details (Java version, OS, etc.)

### Pull Requests

1. Fork the repository
2. Create a feature branch from `main`
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

### Branch Naming

- `feature/xxx` - New features
- `fix/xxx` - Bug fixes
- `docs/xxx` - Documentation changes
- `refactor/xxx` - Code refactoring
- `test/xxx` - Test additions or fixes

### Commit Messages

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <subject>

<body>

<footer>
```

Types:
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation
- `style` - Code style (formatting, semicolons, etc.)
- `refactor` - Code refactoring
- `test` - Adding or updating tests
- `chore` - Maintenance tasks

Example:
```
feat(loadbalancer): add multi-dimensional health scoring

- Added CPU and memory metrics to health calculation
- Implemented configurable weight factors
- Added unit tests for new scoring algorithm

Closes #123
```

## Development Setup

### Prerequisites

- JDK 11 or higher
- Maven 3.6 or higher
- Python 3.9 or higher
- Docker (for running Nacos and Prometheus)

### Building

```bash
# Clone the repository
git clone https://github.com/wangyouwei-God/ai-rpc-framework.git
cd ai-rpc-framework

# Build
mvn clean install

# Run tests
mvn test
```

### Code Style

- Follow standard Java code conventions
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Add Javadoc for public APIs

### Testing

- Write unit tests for all new code
- Maintain test coverage above 70%
- Use meaningful test method names

## Project Structure

```
ai-rpc-framework/
├── rpc-api/                    # Core API definitions
├── rpc-core/                   # Core implementation
│   ├── client/                 # RPC client
│   ├── server/                 # RPC server
│   ├── codec/                  # Serialization
│   ├── loadbalance/            # Load balancing
│   └── registry/               # Service registry
├── rpc-registry/               # Registry implementations
├── example-provider/           # Example provider
├── example-consumer/           # Example consumer
├── ai-forecasting-service/     # Python AI service
└── docs/                       # Documentation
```

## Review Process

1. All submissions require code review
2. Maintainers will review within 3 business days
3. Address review comments promptly
4. Squash commits before merging

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
