# htmx-movies

> Quarkus+HTMX prototype

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

```bash
# Run in dev mode (hot reload enabled)
./mvnw compile quarkus:dev

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=TestClassName

# Run a single test method
./mvnw test -Dtest=TestClassName#methodName

# Package application
./mvnw package

# Build native executable (requires GraalVM)
./mvnw package -Dnative

# Build native using container (no GraalVM needed)
./mvnw package -Dnative -Dquarkus.native.container-build=true
```
