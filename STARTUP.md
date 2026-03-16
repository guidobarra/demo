# Startup

## Requisitos

- Java 25
- Maven 3.9+
- Docker y Docker Compose

## 1. Base de datos

Levantar MariaDB con Docker Compose:

```bash
docker compose up -d
```

Esto crea el contenedor `vertx-mariadb` con la base de datos inicializada (`docker/init.sql`).

Verificar que esté healthy:

```bash
docker compose ps
```

## 2. Ejecución local

### Compilar

```bash
mvn clean compile
```

### Ejecutar

```bash
mvn clean compile exec:java
```

La app arranca en `http://localhost:9292` (perfil `dev` por defecto).

### Compilar fat JAR

```bash
mvn clean package -DskipTests
```

### Ejecutar fat JAR

```bash
java -jar target/vertx-1.0.0-SNAPSHOT-fat.jar
```

## 3. Ejecución con Docker Compose

Levanta la app y MariaDB juntos:

```bash
docker compose up -d --build
```

Reconstruir solo la app (sin cache):

```bash
docker compose build --no-cache vertx-app
```

Reconstruir y levantar:

```bash
docker compose up -d --build vertx-app
```

Ver logs:

```bash
docker compose logs -f vertx-app
```

Bajar todo:

```bash
docker compose down
```

Bajar todo y borrar volúmenes (resetea la DB):

```bash
docker compose down -v
```

## 4. Ejecución con Docker (standalone)

### Build de la imagen

```bash
docker build -t vertx-app .
```

### Run

```bash
docker run -p 9292:9292 vertx-app
```

### Con variables de entorno

```bash
docker run -p 9292:9292 \
  -e VERTX_PROFILE=prod \
  -e VERTX_DATABASE_HOST=host.docker.internal \
  -e VERTX_DATABASE_PASSWORD=secret \
  -e JAVA_XMS=128m \
  -e JAVA_XMX=1g \
  -e TZ=UTC \
  vertx-app
```

## 5. Perfiles de configuración

El perfil se selecciona con la variable de entorno `VERTX_PROFILE`:

| Valor | Archivo | Uso |
|-------|---------|-----|
| `dev` (default) | `config/application-dev.yaml` | Desarrollo local |
| `prod` | `config/application-prod.yaml` | Producción |

```bash
# Local
VERTX_PROFILE=prod java -jar target/vertx-1.0.0-SNAPSHOT-fat.jar

# Docker
docker run -e VERTX_PROFILE=prod vertx-app
```

La configuración se carga en este orden (el último gana):

1. `config/application.yaml` — valores base
2. `config/application-{profile}.yaml` — valores del perfil
3. System properties (`-D`)
4. Variables de entorno con prefijo `VERTX_`

## 6. Variables de entorno

Cualquier variable con prefijo `VERTX_` sobrescribe la configuración YAML.

La convención es:

- `_` (underscore) separa niveles de anidación
- `-` (guion) se mantiene tal cual

| Variable de entorno | Clave en YAML |
|---------------------|---------------|
| `VERTX_SERVER_PORT` | `server.port` |
| `VERTX_DATABASE_HOST` | `database.host` |
| `VERTX_DATABASE_PASSWORD` | `database.password` |
| `VERTX_DATABASE_POOL_MAX-SIZE` | `database.pool.max-size` |

## 7. Parámetros de JVM (Docker)

| Variable | Default | Descripción |
|----------|---------|-------------|
| `JAVA_XMS` | `256m` | Heap inicial (`-Xms`) |
| `JAVA_XMX` | `512m` | Heap máximo (`-Xmx`) |
| `TZ` | `UTC` | Timezone de la JVM |

Flags fijos incluidos en el `Dockerfile`:

| Flag | Descripción |
|------|-------------|
| `-XX:+UseZGC` | Z Garbage Collector, pausas ultra-bajas |
| `-XX:+UseStringDeduplication` | Deduplica strings en el heap |
| `-XX:+ExitOnOutOfMemoryError` | Mata la JVM ante OOM (para que el orquestador reinicie) |
| `-Dfile.encoding=UTF-8` | Encoding por defecto |
| `-Djava.security.egd=file:/dev/./urandom` | Evita bloqueos de entropía en contenedores |

## 8. Endpoints

| Método | URL | Descripción |
|--------|-----|-------------|
| `GET` | `/health-check/` | Health-check |
| `GET` | `/api/users/` | Listar usuarios |
| `GET` | `/api/users/:id` | Obtener usuario por ID |
| `POST` | `/api/users/` | Crear usuario |
| `PUT` | `/api/users/:id` | Actualizar usuario |
| `DELETE` | `/api/users/:id` | Eliminar usuario |

### Ejemplos

Health-check:

```bash
curl http://localhost:9292/health-check/
```

Listar usuarios:

```bash
curl http://localhost:9292/api/users/
```

Obtener usuario por ID:

```bash
curl http://localhost:9292/api/users/1
```

Crear usuario:

```bash
curl -X POST http://localhost:9292/api/users/ \
  -H "Content-Type: application/json" \
  -d '{"name": "Juan", "email": "juan@mail.com"}'
```

Actualizar usuario:

```bash
curl -X PUT http://localhost:9292/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Juan Perez", "email": "juan.perez@mail.com"}'
```

Eliminar usuario:

```bash
curl -X DELETE http://localhost:9292/api/users/1
```
