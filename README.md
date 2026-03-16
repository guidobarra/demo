# Vert.x 5 - Reactive Toolkit para la JVM

> Proyecto basado en **Java 25** y **Eclipse Vert.x 5.0.8**

---

## Documentacion

- [STARTUP.md](STARTUP.md) — Como compilar, ejecutar y desplegar la aplicacion

## Tabla de contenidos

1. [Introduccion a Vert.x](#1-introduccion-a-vertx)
2. [Que problematica resuelve y en que se enfoca](#2-que-problematica-resuelve-y-en-que-se-enfoca)
3. [Como funciona Vert.x por dentro](#3-como-funciona-vertx-por-dentro)
4. [Como manejar I/O bloqueante sin bloquear el Event Loop](#4-como-manejar-io-bloqueante-sin-bloquear-el-event-loop)
   - [4.10 Pero... COMO funciona? Que pasa durante esos 10 segundos?](#410-pero-como-funciona-que-pasa-durante-esos-10-segundos)
5. [Por que elegir Vert.x frente a otros frameworks](#5-por-que-elegir-vertx-frente-a-otros-frameworks)
6. [Ventajas y desventajas](#6-ventajas-y-desventajas)

---

## 1. Introduccion a Vert.x

### Que es Vert.x

Eclipse Vert.x es un **toolkit reactivo, no bloqueante y orientado a eventos** para la Java Virtual Machine (JVM). No es un framework en el sentido tradicional: es un conjunto de herramientas (toolkit) que te da libertad total sobre como estructurar tu aplicacion. No impone una arquitectura especifica, no requiere un contenedor de aplicaciones y puede embeberse dentro de cualquier aplicacion Java existente.

Vert.x fue creado por Tim Fox en 2012, inspirado en Node.js pero con la ambicion de superar sus limitaciones (single-threaded) aprovechando las capacidades de la JVM. Hoy es un proyecto de la **Eclipse Foundation** con una comunidad activa y respaldado por **Red Hat**.

### Vert.x 5: La evolucion

Vert.x 5 (lanzado en 2024) representa una evolucion mayor respecto a Vert.x 4:

- **Modelo exclusivamente basado en Futures**: se elimina el modelo de callbacks. Todas las APIs retornan `Future<T>` en lugar de recibir callbacks como parametro.
- **`VerticleBase` reemplaza a `AbstractVerticle`** como clase base recomendada. Los metodos `start()` y `stop()` ahora retornan `Future<?>`.
- **Java 11+ como minimo** (este proyecto usa Java 25).
- **Soporte JPMS** (Java Platform Module System) para arquitecturas modulares.
- **gRPC mejorado**: soporte para gRPC Web, Transcoding y formato JSON wire sin necesidad de proxies intermedios.
- **Soporte para Virtual Threads** (Java 21+) mediante `ThreadingModel.VIRTUAL_THREAD`.

### Ejemplo basico con Vert.x 5

```java
public class MainVerticle extends VerticleBase {

  @Override
  public Future<?> start() {
    return vertx.createHttpServer()
      .requestHandler(req -> req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x!"))
      .listen(8888)
      .onSuccess(http -> System.out.println("HTTP server started on port 8888"));
  }
}
```

Observa que `start()` retorna un `Future<?>`. No hay callbacks, no hay `Promise<Void>`. El servidor HTTP se inicia de forma asincrona y el verticle se considera "started" cuando el future se completa exitosamente.

---

## 2. Que problematica resuelve y en que se enfoca

### El problema del modelo thread-per-request

El modelo clasico de servidores Java (Servlets, Spring MVC tradicional) asigna **un hilo del sistema operativo por cada request entrante**. Esto funciona bien con pocas conexiones concurrentes, pero tiene limites severos:

| Problema | Descripcion |
|---|---|
| **Consumo de memoria** | Cada hilo consume ~1MB de stack. 10.000 conexiones = ~10GB solo en stacks de hilos. |
| **Context switching** | El SO pierde tiempo valioso alternando entre miles de hilos bloqueados. |
| **Escalabilidad vertical limitada** | Mas hilos no significa mas rendimiento; hay un punto de retorno decreciente. |
| **Hilos ociosos** | Mientras esperan I/O (base de datos, red, disco), los hilos no hacen nada util pero siguen ocupando recursos. |

### Que resuelve Vert.x

Vert.x resuelve estos problemas con un modelo **event-driven y non-blocking I/O**:

- **Pocos hilos, mucha concurrencia**: un solo event loop puede manejar miles de conexiones simultaneas porque nunca se bloquea esperando I/O.
- **Uso eficiente de recursos**: al no bloquear hilos, la misma cantidad de memoria y CPU maneja ordenes de magnitud mas de conexiones.
- **Escalabilidad horizontal y vertical**: multiples event loops aprovechan todos los cores del CPU. El event bus permite escalar a multiples nodos.
- **Alta densidad de servicios**: ideal para microservicios donde se necesitan muchas instancias con bajo consumo de recursos.

### En que se enfoca Vert.x

1. **Rendimiento extremo**: esta consistentemente entre los frameworks mas rapidos en benchmarks como TechEmpower.
2. **Programacion reactiva**: todo es asincrono, no bloqueante, basado en eventos.
3. **Poliglotismo**: APIs idiomaticas para Java, Kotlin, Groovy, JavaScript, Ruby, Scala.
4. **Modularidad**: el core pesa ~650KB. Agregas solo los modulos que necesitas.
5. **Distribucion transparente**: el event bus extiende la comunicacion de forma transparente a multiples nodos.
6. **Libertad arquitectonica**: no impone estructura, patrones, ni contenedores.

---

## 3. Como funciona Vert.x por dentro

### 3.1 Arquitectura general

Vert.x se construye sobre cuatro pilares fundamentales:

```
+---------------------------------------------------------------+
|                       Tu Aplicacion                           |
|  +----------+  +----------+  +----------+  +----------+      |
|  | Verticle |  | Verticle |  | Verticle |  | Verticle |      |
|  |    A     |  |    B     |  |    C     |  |    D     |      |
|  +----+-----+  +----+-----+  +----+-----+  +----+-----+      |
|       |             |             |             |              |
|  +----v-------------v-------------v-------------v----------+  |
|  |                    EVENT BUS                             |  |
|  |         (sistema nervioso de Vert.x)                     |  |
|  +----^-------------^-------------^-------------^----------+  |
|       |             |             |             |              |
|  +----+-----+  +----+-----+  +----+-----+  +----+-----+      |
|  |Event Loop|  |Event Loop|  |Event Loop|  |Event Loop|      |
|  | Thread 1 |  | Thread 2 |  | Thread 3 |  | Thread 4 |      |
|  +----+-----+  +----+-----+  +----+-----+  +----+-----+      |
|       |             |             |             |              |
|  +----v-------------v-------------v-------------v----------+  |
|  |                      NETTY                               |  |
|  |            (motor de I/O no bloqueante)                  |  |
|  +----------------------------------------------------------+  |
+---------------------------------------------------------------+
```

### 3.2 Netty: el motor de I/O

En la base de Vert.x esta **Netty**, un framework de networking asincrono de alto rendimiento. Netty provee:

- **NIO (Non-blocking I/O)**: permite manejar multiples conexiones de red en un solo hilo sin bloquear.
- **Buffers eficientes**: manejo de memoria optimizado con pools de buffers que minimizan la recoleccion de basura.
- **Codecs de protocolos**: HTTP/1.1, HTTP/2, WebSocket, TCP, UDP, DNS y mas.
- **Transporte nativo**: soporte para epoll (Linux) y kqueue (macOS) para rendimiento optimo del SO.

Vert.x abstrae completamente Netty. Nunca interactuas con Netty directamente a menos que estes escribiendo extensiones avanzadas. Sin embargo, es gracias a Netty que Vert.x logra su rendimiento excepcional en operaciones de red.

### 3.3 El Event Loop y el patron Multi-Reactor

#### Reactor Pattern clasico

El patron Reactor (usado por Node.js) tiene **un unico hilo (event loop)** que recibe eventos del SO y los despacha a los handlers correspondientes. Es eficiente porque el hilo nunca se bloquea, pero tiene una limitacion critica: **solo puede usar un core del CPU**.

#### Multi-Reactor Pattern (la innovacion de Vert.x)

Vert.x evoluciona el patron Reactor con lo que llama **Multi-Reactor**:

- En lugar de un unico event loop, Vert.x mantiene **N event loops** (por defecto `cores * 2`).
- Cada event loop corre en su propio hilo.
- En una maquina de 4 cores, tendras por defecto **8 event loop threads**.
- Cada event loop puede manejar miles de conexiones concurrentes.
- **Un solo proceso Vert.x escala sobre todos los cores**, a diferencia de Node.js que necesita multiples procesos.

```
CPU Core 0           CPU Core 1           CPU Core 2           CPU Core 3
+-----------+        +-----------+        +-----------+        +-----------+
|Event Loop |        |Event Loop |        |Event Loop |        |Event Loop |
|  Thread 0 |        |  Thread 2 |        |  Thread 4 |        |  Thread 6 |
+-----------+        +-----------+        +-----------+        +-----------+
|Event Loop |        |Event Loop |        |Event Loop |        |Event Loop |
|  Thread 1 |        |  Thread 3 |        |  Thread 5 |        |  Thread 7 |
+-----------+        +-----------+        +-----------+        +-----------+
```

#### Como funciona el Event Loop paso a paso

1. **Vert.x arranca** y crea N event loop threads (hilos del event loop).
2. Cuando se **deploya un verticle**, Vert.x le asigna uno de los event loops disponibles y crea un **Context** asociado.
3. Cuando llega un evento (request HTTP, dato en un socket, timer, mensaje del event bus), Netty lo detecta y lo pasa al event loop correspondiente.
4. El event loop ejecuta el **handler** registrado para ese evento.
5. El handler procesa el evento **sin bloquear** (no hace Thread.sleep, no espera I/O sincrono).
6. El handler termina y el event loop pasa al siguiente evento.
7. Si el handler necesita hacer I/O (leer BD, llamar a un servicio), lo hace de forma **asincrona** y registra un callback/future para cuando el resultado este disponible.

#### Garantia de thread-safety

Vert.x garantiza algo crucial: **un handler particular nunca se ejecuta concurrentemente en multiples hilos**. Todos los eventos para un verticle se despachan siempre en el mismo event loop thread. Esto significa:

- No necesitas `synchronized`.
- No necesitas `volatile`.
- No hay race conditions.
- No hay deadlocks.
- Tu codigo es efectivamente single-threaded dentro de cada verticle.

### 3.4 La Regla de Oro: No bloquear el Event Loop

Esta es la regla mas importante de Vert.x:

> **Nunca bloquees el event loop.**

Si bloqueas un event loop, todos los handlers asignados a ese event loop se detienen. Si bloqueas todos los event loops, tu aplicacion se congela completamente.

**Ejemplos de operaciones bloqueantes que NO debes hacer en el event loop:**

- `Thread.sleep()`
- Esperar un lock o mutex
- Bloques `synchronized`
- Operaciones de base de datos sincronas (JDBC clasico)
- Calculos intensivos de larga duracion
- I/O de archivos sincrono
- Loops infinitos o muy largos

**Vert.x te ayuda a detectar violaciones**: si un event loop no retorna en un tiempo configurable, Vert.x imprime un warning con el stack trace:

```
Thread vertx-eventloop-thread-3 has been blocked for 20458 ms
```

**Que hacer si necesitas ejecutar codigo bloqueante:**

```java
vertx.executeBlocking(() -> {
    // codigo bloqueante aqui (JDBC, calculo pesado, etc.)
    return someBlockingResult();
}).onSuccess(result -> {
    // resultado disponible en el event loop
});
```

`executeBlocking` mueve la tarea a un **worker thread pool** (por defecto 20 hilos) y cuando termina, entrega el resultado de vuelta al event loop del contexto original.

### 3.5 Verticles: la unidad de despliegue

Los verticles son la unidad fundamental de organizacion y despliegue en Vert.x. Estan inspirados en el **Actor Model**:

- Cada verticle es como un actor independiente.
- Los verticles se comunican entre si mediante mensajes (event bus).
- Cada verticle tiene su propio estado aislado.
- Vert.x garantiza que un verticle solo se ejecuta en un hilo a la vez.

#### Tipos de Verticles

| Tipo | Hilo de ejecucion | Uso |
|---|---|---|
| **Standard Verticle** | Event loop thread | Operaciones no bloqueantes, HTTP handlers, logica de negocio reactiva |
| **Worker Verticle** | Worker thread pool | Operaciones bloqueantes como JDBC, calculos pesados |
| **Virtual Thread Verticle** | Virtual thread (Java 21+) | Codigo que parece sincrono pero usa virtual threads por debajo |

#### Standard Verticles

Son el tipo mas comun. Se asignan a un event loop y toda su ejecucion ocurre en ese hilo:

```java
public class ApiVerticle extends VerticleBase {

  @Override
  public Future<?> start() {
    return vertx.createHttpServer()
      .requestHandler(this::handleRequest)
      .listen(8080);
  }

  private void handleRequest(HttpServerRequest req) {
    req.response()
      .putHeader("content-type", "application/json")
      .end("{\"status\": \"ok\"}");
  }
}
```

#### Worker Verticles

Para codigo bloqueante inevitable:

```java
DeploymentOptions options = new DeploymentOptions()
    .setThreadingModel(ThreadingModel.WORKER);
vertx.deployVerticle(new MyBlockingVerticle(), options);
```

Un worker verticle nunca se ejecuta concurrentemente por mas de un hilo, pero puede ejecutarse en diferentes hilos del worker pool en diferentes momentos.

#### Virtual Thread Verticles

Disponibles con Java 21+. Permiten escribir codigo que parece sincrono usando virtual threads:

```java
DeploymentOptions options = new DeploymentOptions()
    .setThreadingModel(ThreadingModel.VIRTUAL_THREAD);
vertx.deployVerticle(new MyVerticle(), options);
```

#### Despliegue de multiples instancias

Puedes escalar un verticle horizontalmente dentro de la misma JVM desplegando multiples instancias. Cada instancia se asigna a un event loop diferente:

```java
DeploymentOptions options = new DeploymentOptions().setInstances(16);
vertx.deployVerticle(() -> new MyVerticle(), options);
```

En una maquina de 8 cores con 16 event loops, las 16 instancias del verticle se distribuyen entre los event loops, aprovechando todo el hardware disponible.

#### VerticleBase vs AbstractVerticle (Vert.x 5)

En Vert.x 5, `VerticleBase` es la clase base recomendada:

| Caracteristica | `AbstractVerticle` (Vert.x 4) | `VerticleBase` (Vert.x 5) |
|---|---|---|
| `start()` retorna | `void` (usa `Promise<Void>`) | `Future<?>` |
| `stop()` retorna | `void` (usa `Promise<Void>`) | `Future<?>` |
| Modelo | Callbacks + Futures | Solo Futures |
| Acceso a `vertx` | Campo protegido | Campo protegido |
| Acceso a `context` | Via metodo | Campo protegido directo |
| Estado | No deprecado, pero no recomendado | Recomendado para codigo nuevo |

### 3.6 El Event Bus: el sistema nervioso

El event bus es el mecanismo de comunicacion central de Vert.x. Es un sistema de mensajeria distribuida peer-to-peer.

#### Caracteristicas fundamentales

- **Una unica instancia** por proceso Vert.x (accesible via `vertx.eventBus()`).
- **Basado en direcciones (addresses)**: los mensajes se envian a direcciones (strings simples).
- **Polivalente**: funciona dentro de un proceso, entre multiples nodos en un cluster, e incluso con browsers via bridges.
- **Best-effort delivery**: Vert.x hace su mejor esfuerzo, pero los mensajes pueden perderse en caso de falla.
- **Tipos de mensajes**: primitivos, Strings, Buffers, JSON (la convencion mas comun), u objetos arbitrarios con codecs personalizados.

#### Patrones de comunicacion

**1. Publish/Subscribe (uno a muchos)**

Un mensaje se entrega a **todos** los handlers registrados en una direccion:

```java
// Publicador
vertx.eventBus().publish("news.sports", "Gol de Argentina!");

// Suscriptor 1
vertx.eventBus().consumer("news.sports", msg -> {
    System.out.println("Suscriptor 1: " + msg.body());
});

// Suscriptor 2
vertx.eventBus().consumer("news.sports", msg -> {
    System.out.println("Suscriptor 2: " + msg.body());
});
// Ambos suscriptores reciben el mensaje
```

**2. Point-to-Point (uno a uno)**

El mensaje se entrega a **un solo** handler (round-robin si hay multiples registrados):

```java
// Emisor
vertx.eventBus().send("orders.process", orderJson);

// Receptor (uno de los registrados recibe el mensaje)
vertx.eventBus().consumer("orders.process", msg -> {
    processOrder(msg.body());
});
```

**3. Request-Response (peticion-respuesta)**

Simula comunicacion sincrona. El emisor espera una respuesta:

```java
// Emisor (request)
vertx.eventBus()
    .request("services.time", "")
    .onSuccess(reply -> {
        System.out.println("La hora es: " + reply.body());
    });

// Receptor (responde)
vertx.eventBus().consumer("services.time", msg -> {
    msg.reply(LocalDateTime.now().toString());
});
```

#### Event Bus en modo cluster

En modo cluster, el event bus se extiende de forma transparente a multiples nodos:

```
Nodo 1 (JVM)                    Nodo 2 (JVM)
+------------------+            +------------------+
| Verticle A       |            | Verticle C       |
| Verticle B       |            | Verticle D       |
|                  |            |                  |
| Event Bus Local  |<---------->| Event Bus Local  |
+------------------+  TCP/IP   +------------------+
         |                              |
         +--------- Cluster Manager ----+
              (Hazelcast / Infinispan
               / Apache Ignite)
```

El **cluster manager** (Hazelcast, Infinispan o Apache Ignite) maneja:
- Descubrimiento de nodos.
- Replicacion de informacion de suscripciones.
- Deteccion de nodos caidos.

Vert.x establece conexiones directas entre nodos para el envio de mensajes, usando un protocolo interno optimizado. El cluster manager solo gestiona la metadata de suscripciones, no el transporte de mensajes en si.

### 3.7 El Context: control de ejecucion

El `Context` es un concepto interno fundamental. Cada verticle tiene un contexto asociado que controla en que hilo se ejecuta su codigo:

```java
Context context = vertx.getOrCreateContext();

if (context.isEventLoopContext()) {
    // Se ejecuta en un event loop thread
} else if (context.isWorkerContext()) {
    // Se ejecuta en un worker thread
}
```

**Propagacion de contexto**: cuando ejecutas una operacion asincrona desde un contexto, Vert.x garantiza que el callback se ejecuta en el **mismo contexto**. Esto es lo que permite la thread-safety sin sincronizacion explicita.

```java
// Todo esto se ejecuta en el mismo event loop thread
vertx.fileSystem().readFile("data.json")
    .compose(buffer -> {
        // Mismo thread
        return vertx.fileSystem().writeFile("copy.json", buffer);
    })
    .onSuccess(v -> {
        // Mismo thread
        System.out.println("Copiado!");
    });
```

### 3.8 Future Composition: el modelo de programacion de Vert.x 5

Vert.x 5 abandona los callbacks en favor de **composicion de futures**:

#### Encadenamiento secuencial con `compose`

```java
Future<Void> pipeline = vertx.fileSystem()
    .createFile("/tmp/data.txt")
    .compose(v -> vertx.fileSystem().writeFile("/tmp/data.txt", Buffer.buffer("hello")))
    .compose(v -> vertx.fileSystem().move("/tmp/data.txt", "/tmp/final.txt"));
```

#### Ejecucion paralela con `Future.all`

```java
Future<HttpServer> http = httpServer.listen(8080);
Future<NetServer> tcp = netServer.listen(9090);

Future.all(http, tcp).onSuccess(v -> {
    System.out.println("Ambos servidores arrancados");
});
```

#### Composicion avanzada

```java
Future.any(future1, future2);   // Exito si al menos uno tiene exito
Future.join(future1, future2);  // Espera a que todos terminen (exito o fallo)
```

### 3.9 Pool de hilos

Vert.x mantiene dos pools de hilos separados:

| Pool | Hilos por defecto | Proposito |
|---|---|---|
| **Event Loop Pool** | `cores * 2` | Operaciones no bloqueantes, I/O asincrono, handlers |
| **Worker Pool** | 20 | Operaciones bloqueantes via `executeBlocking()` o worker verticles |

Puedes personalizar ambos pools:

```java
Vertx vertx = Vertx.vertx(new VertxOptions()
    .setEventLoopPoolSize(16)
    .setWorkerPoolSize(40));
```

Tambien puedes crear worker pools dedicados para verticles especificos:

```java
vertx.deployVerticle(new HeavyProcessingVerticle(),
    new DeploymentOptions().setWorkerPoolName("heavy-pool"));
```

### 3.10 Ecosistema de modulos

El ecosistema de Vert.x es extenso y modular:

| Categoria | Modulos |
|---|---|
| **Core** | vertx-core, vertx-launcher-application |
| **Web** | vertx-web, vertx-web-client, vertx-web-validation, vertx-web-openapi, GraphQL |
| **Bases de datos** | PostgreSQL, MySQL, Oracle, DB2, MSSQL, MongoDB, Redis, Cassandra, JDBC |
| **Mensajeria** | Kafka, AMQP, RabbitMQ, MQTT |
| **Autenticacion** | OAuth2, JWT, WebAuthn, LDAP, OTP, ABAC |
| **Clustering** | Hazelcast, Infinispan, Apache Ignite |
| **Microservicios** | Circuit Breaker, Config, Service Resolver |
| **Monitoring** | Micrometer, Dropwizard, OpenTelemetry, Zipkin, Health Check |
| **gRPC** | gRPC Server/Client, gRPC Web, Transcoding |
| **Reactive** | RxJava 2/3, Mutiny, Reactive Streams |
| **DevOps** | Shell |

---

## 4. Como manejar I/O bloqueante sin bloquear el Event Loop

Esta es la pregunta mas importante que surge al aprender Vert.x: si no puedo bloquear el event loop, como hago operaciones que inherentemente tardan (leer archivos, llamadas HTTP, queries a base de datos que tardan 500ms o segundos)?

La respuesta corta: **las librerias propias de Vert.x ya lo resuelven internamente**. No tienes que hacer nada especial. El problema aparece solo cuando usas librerias externas bloqueantes.

### 4.1 Las librerias de Vert.x son non-blocking por diseno

Cada libreria del ecosistema Vert.x esta construida desde cero sobre **Netty** y el modelo asincrono. Nunca bloquean el event loop, sin importar cuanto tarde la operacion. Asi es como lo logran internamente:

#### Llamadas HTTP: `vertx-web-client`

El Web Client de Vert.x **no crea un hilo por cada request HTTP**. Internamente funciona asi:

1. Tu codigo llama a `webClient.get(...)`. Esto **no** lanza un hilo nuevo.
2. Netty registra el socket en un **selector NIO** del sistema operativo.
3. El event loop continua procesando otros eventos (no se queda esperando).
4. Cuando la respuesta HTTP llega (100ms, 2s, 10s despues), el SO notifica al selector.
5. Netty recibe la notificacion y despacha el evento al event loop.
6. El event loop ejecuta tu callback/future con la respuesta.

**En ningun momento se bloqueo un hilo.** La espera la hace el sistema operativo, no un hilo de Java.

```java
WebClient client = WebClient.create(vertx);

// Esto NO bloquea. El event loop queda libre inmediatamente.
client.get(443, "api.example.com", "/users")
    .ssl(true)
    .send()
    .onSuccess(response -> {
        // Se ejecuta cuando llega la respuesta (puede ser 2 segundos despues)
        // Pero el event loop estuvo libre todo ese tiempo
        System.out.println("Status: " + response.statusCode());
        JsonObject body = response.bodyAsJsonObject();
    })
    .onFailure(err -> {
        System.err.println("Error: " + err.getMessage());
    });

// Esta linea se ejecuta INMEDIATAMENTE, no espera la respuesta HTTP
System.out.println("Request enviado, el event loop sigue libre");
```

Ademas, el Web Client maneja internamente:
- **Connection pooling**: reutiliza conexiones TCP (configurable con `PoolOptions`).
- **HTTP/2 multiplexing**: multiples requests sobre una sola conexion TCP.
- **Idle timeout**: cierra conexiones inactivas automaticamente.

#### Base de datos: clientes reactivos (`vertx-pg-client`, `vertx-mysql-client`, etc.)

Los clientes reactivos de Vert.x **no usan JDBC**. Implementan el protocolo de la base de datos directamente sobre Netty:

1. Tu codigo llama a `pool.query("SELECT ...").execute()`. No bloquea.
2. Vert.x serializa la query en el protocolo nativo de PostgreSQL/MySQL.
3. Netty envia los bytes por el socket de forma non-blocking.
4. El event loop queda libre.
5. Cuando la DB responde (500ms despues, por ejemplo), Netty recibe los bytes.
6. Vert.x deserializa la respuesta y ejecuta tu future.

```java
// Pool de conexiones reactivo (NO es JDBC)
Pool pool = PgBuilder.pool()
    .connectingTo("postgresql://localhost:5432/mydb")
    .using(vertx)
    .build();

// Esto NO bloquea el event loop, aunque la query tarde 2 segundos
pool.query("SELECT * FROM users WHERE active = true")
    .execute()
    .onSuccess(rows -> {
        for (Row row : rows) {
            System.out.println(row.getString("name"));
        }
    })
    .onFailure(err -> {
        System.err.println("Query fallo: " + err.getMessage());
    });
```

**Diferencia clave con JDBC tradicional:**

```
JDBC tradicional (BLOQUEANTE):
  Hilo Java ──> envia query ──> ESPERA BLOQUEADO 500ms ──> recibe resultado
  (el hilo no hizo nada util durante 500ms)

Vert.x Reactive Client (NON-BLOCKING):
  Event loop ──> envia query ──> SIGUE PROCESANDO OTROS EVENTOS
                                     ... 500ms despues ...
                 SO notifica ──> event loop ejecuta el callback con el resultado
  (el event loop proceso cientos de otros eventos durante esos 500ms)
```

#### Sistema de archivos: `vertx.fileSystem()`

Vert.x provee una API de filesystem completamente asincrona:

```java
// Leer archivo - NO bloquea el event loop
vertx.fileSystem().readFile("config.json")
    .onSuccess(buffer -> {
        JsonObject config = buffer.toJsonObject();
        System.out.println("Config cargada: " + config);
    });

// Escribir archivo - NO bloquea
vertx.fileSystem().writeFile("output.txt", Buffer.buffer("datos"))
    .onSuccess(v -> System.out.println("Archivo escrito"));

// Copiar archivo - NO bloquea
vertx.fileSystem().copy("source.txt", "dest.txt")
    .onSuccess(v -> System.out.println("Copiado"));
```

Internamente, Vert.x usa `AsynchronousFileChannel` de Java NIO.2 para operaciones de lectura/escritura, delegando al SO la operacion de I/O sin bloquear hilos.

> **Nota**: Vert.x tambien ofrece versiones sincronas (e.g., `readFileBlocking`), pero estas estan pensadas **solo para inicializacion** y nunca deben usarse dentro de handlers.

#### Resumen: librerias Vert.x que son non-blocking automaticamente

| Operacion | Libreria Vert.x | Bloquea el event loop? | Como funciona internamente |
|---|---|---|---|
| HTTP requests | `vertx-web-client` | **No** | NIO sockets via Netty, connection pooling |
| PostgreSQL | `vertx-pg-client` | **No** | Protocolo nativo PG sobre Netty |
| MySQL | `vertx-mysql-client` | **No** | Protocolo nativo MySQL sobre Netty |
| MongoDB | `vertx-mongo-client` | **No** | Driver reactivo sobre Netty |
| Redis | `vertx-redis-client` | **No** | Protocolo RESP sobre Netty |
| Kafka | `vertx-kafka-client` | **No** | Wrapper asincrono del Kafka consumer |
| Archivos | `vertx.fileSystem()` | **No** | AsynchronousFileChannel (NIO.2) |
| DNS | `vertx.createDnsClient()` | **No** | NIO via Netty |
| TCP/UDP | `vertx.createNetClient()` | **No** | NIO via Netty |
| WebSocket | `vertx-web` | **No** | NIO via Netty |
| AMQP | `vertx-amqp-client` | **No** | Protocolo AMQP sobre Netty |
| MQTT | `vertx-mqtt` | **No** | Protocolo MQTT sobre Netty |
| gRPC | `vertx-grpc` | **No** | HTTP/2 sobre Netty |

**Si usas exclusivamente librerias Vert.x, nunca vas a bloquear el event loop, sin importar cuanto tarde la operacion.**

### 4.2 El problema: librerias de terceros bloqueantes

El problema aparece cuando necesitas usar librerias que **no** son del ecosistema Vert.x y que internamente hacen I/O bloqueante:

- **JDBC clasico** (java.sql.Connection, HikariCP, etc.)
- **Apache HttpClient** (bloqueante)
- **java.io.File / java.io.InputStream** (I/O bloqueante)
- **OkHttp** en modo sincrono
- **AWS SDK v1** (bloqueante)
- **Cualquier SDK** que haga `Thread.sleep()` o espere respuesta de red de forma sincrona
- **Calculos CPU-intensivos** (hashing pesado, procesamiento de imagenes, ML inference)

Vert.x te da **tres estrategias** para manejar estos casos:

### 4.3 Estrategia 1: `executeBlocking()` — la mas simple

Mueve una tarea puntual al **worker thread pool** y devuelve el resultado al event loop:

```java
public class ApiVerticle extends VerticleBase {

  @Override
  public Future<?> start() {
    return vertx.createHttpServer()
        .requestHandler(this::handleRequest)
        .listen(8080);
  }

  private void handleRequest(HttpServerRequest req) {
    // La tarea bloqueante se ejecuta en un worker thread
    vertx.executeBlocking(() -> {
            // ESTO CORRE EN UN WORKER THREAD, NO EN EL EVENT LOOP
            Connection conn = DriverManager.getConnection("jdbc:postgresql://...");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users"); // Bloquea 500ms? No importa
            List<String> users = new ArrayList<>();
            while (rs.next()) {
                users.add(rs.getString("name"));
            }
            conn.close();
            return users;
        })
        .onSuccess(users -> {
            // ESTO VUELVE AL EVENT LOOP
            req.response()
                .putHeader("content-type", "application/json")
                .end(new JsonArray(users).encode());
        })
        .onFailure(err -> {
            req.response().setStatusCode(500).end(err.getMessage());
        });
  }
}
```

**Como funciona internamente:**

```
Event Loop Thread          Worker Thread Pool (20 hilos por defecto)
      |                              |
      |--- executeBlocking() ------->|
      |    (encola la tarea)         |--- ejecuta codigo bloqueante
      |                              |    (JDBC, I/O, calculo pesado)
      |  (sigue procesando           |    ... 500ms ...
      |   otros eventos)             |
      |                              |--- termina
      |<--- resultado (Future) ------|
      |                              |
      |--- ejecuta onSuccess() en el event loop
```

**Cuando usarlo**: operaciones bloqueantes puntuales y esporadicas. No abuses de este patron porque el worker pool tiene un limite (20 hilos por defecto). Si los 20 estan ocupados, las tareas se encolan.

### 4.4 Estrategia 2: Worker Verticles — para servicios bloqueantes dedicados

Si tienes un componente entero que hace I/O bloqueante (un servicio que usa JDBC pesadamente), es mejor encapsularlo en un **worker verticle**:

```java
// Verticle que hace operaciones bloqueantes
public class DatabaseVerticle extends VerticleBase {

  private Connection jdbcConnection;

  @Override
  public Future<?> start() {
    // Toda esta logica corre en un WORKER THREAD
    jdbcConnection = DriverManager.getConnection("jdbc:postgresql://...");

    // Escucha mensajes del event bus
    vertx.eventBus().consumer("db.query.users", this::handleQuery);
    return super.start();
  }

  private void handleQuery(Message<JsonObject> msg) {
    // Esto se ejecuta en worker thread, puede bloquear sin problemas
    try {
      ResultSet rs = jdbcConnection.createStatement()
          .executeQuery("SELECT * FROM users");
      JsonArray results = new JsonArray();
      while (rs.next()) {
        results.add(new JsonObject().put("name", rs.getString("name")));
      }
      msg.reply(results); // Responde via event bus
    } catch (SQLException e) {
      msg.fail(500, e.getMessage());
    }
  }
}

// Verticle de API (standard, event loop)
public class ApiVerticle extends VerticleBase {

  @Override
  public Future<?> start() {
    return vertx.createHttpServer()
        .requestHandler(req -> {
            // Pide datos al worker verticle via event bus (non-blocking)
            vertx.eventBus()
                .request("db.query.users", new JsonObject())
                .onSuccess(reply -> {
                    req.response()
                        .putHeader("content-type", "application/json")
                        .end(reply.body().toString());
                });
        })
        .listen(8080);
  }
}

// Despliegue
vertx.deployVerticle(new ApiVerticle()); // Event loop
vertx.deployVerticle(new DatabaseVerticle(),
    new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)); // Worker
```

**Ventajas sobre `executeBlocking()`**:
- Desacople limpio via event bus.
- El worker verticle tiene su propio ciclo de vida (`start`/`stop`).
- Puedes escalar instancias del worker verticle independientemente.
- Separacion clara de responsabilidades.

### 4.5 Estrategia 3: Virtual Thread Verticles (Java 21+) — la mas moderna

Con Java 21+, puedes usar **virtual threads** que combinan lo mejor de ambos mundos: codigo que parece sincrono pero no bloquea platform threads:

```java
public class ModernDbVerticle extends VerticleBase {

  private Pool pgPool;

  @Override
  public Future<?> start() {
    pgPool = PgBuilder.pool()
        .connectingTo("postgresql://localhost/mydb")
        .using(vertx)
        .build();

    return vertx.createHttpServer()
        .requestHandler(this::handleRequest)
        .listen(8080);
  }

  private void handleRequest(HttpServerRequest req) {
    // Con virtual threads, puedes escribir codigo que "parece" sincrono
    // pero internamente no bloquea platform threads
    pgPool.query("SELECT * FROM users")
        .execute()
        .onSuccess(rows -> {
            JsonArray arr = new JsonArray();
            for (Row row : rows) {
                arr.add(row.getString("name"));
            }
            req.response()
                .putHeader("content-type", "application/json")
                .end(arr.encode());
        });
  }
}

// Despliegue con virtual threads
vertx.deployVerticle(() -> new ModernDbVerticle(),
    new DeploymentOptions().setThreadingModel(ThreadingModel.VIRTUAL_THREAD));
```

### 4.6 Caso especial: `vertx-jdbc-client` (el wrapper)

Vert.x incluye un modulo `vertx-jdbc-client` que envuelve JDBC clasico para darle una API asincrona. Pero **atencion**: internamente sigue usando JDBC bloqueante. Lo que hace es mover las operaciones JDBC al worker pool automaticamente.

```java
JDBCPool pool = JDBCPool.pool(vertx, new JsonObject()
    .put("url", "jdbc:postgresql://localhost/mydb")
    .put("user", "admin")
    .put("password", "secret"));

// API asincrona, pero internamente usa JDBC bloqueante en worker threads
pool.query("SELECT * FROM users")
    .execute()
    .onSuccess(rows -> { /* ... */ });
```

**Diferencia critica:**

| Aspecto | `vertx-pg-client` (reactivo) | `vertx-jdbc-client` (wrapper) |
|---|---|---|
| Protocolo | Nativo PostgreSQL sobre Netty | JDBC sobre driver bloqueante |
| Bloquea hilos? | **Nunca** | Si, worker threads internamente |
| Escalabilidad | Limitada solo por file descriptors | Limitada por el worker pool (20 hilos) |
| Rendimiento | Superior | Menor (overhead de context switching) |
| Cuando usarlo | **Siempre que sea posible** | Solo si no existe cliente reactivo para tu DB |

### 4.7 Diagrama de decision

```
¿Necesitas hacer I/O o una operacion que tarda?
│
├── ¿Existe libreria Vert.x para eso?
│   ├── SI → Usala directamente. No bloquea. No necesitas hacer nada especial.
│   │        (vertx-web-client, vertx-pg-client, vertx-mongo-client,
│   │         vertx.fileSystem(), vertx-redis-client, vertx-kafka-client...)
│   │
│   └── NO → ¿Es una operacion puntual o un servicio completo?
│       │
│       ├── Puntual → executeBlocking()
│       │              (simple, inline, para operaciones esporadicas)
│       │
│       ├── Servicio completo → Worker Verticle
│       │              (desacoplado via event bus, escalable, ciclo de vida propio)
│       │
│       └── Java 21+ disponible? → Virtual Thread Verticle
│                  (codigo "sincrono" sin bloquear platform threads)
```

### 4.8 Error comun: usar librerias bloqueantes sin darse cuenta

```java
// MAL - Bloquea el event loop
public class BadVerticle extends VerticleBase {
  @Override
  public Future<?> start() {
    return vertx.createHttpServer()
        .requestHandler(req -> {
            // java.net.HttpURLConnection es BLOQUEANTE
            URL url = new URL("https://api.example.com/data");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStream is = conn.getInputStream(); // BLOQUEA EL EVENT LOOP
            String response = new String(is.readAllBytes());
            req.response().end(response);
        })
        .listen(8080);
  }
}

// BIEN - Usa el Web Client de Vert.x (non-blocking)
public class GoodVerticle extends VerticleBase {
  private WebClient client;

  @Override
  public Future<?> start() {
    client = WebClient.create(vertx);
    return vertx.createHttpServer()
        .requestHandler(req -> {
            client.getAbs("https://api.example.com/data")
                .send()
                .onSuccess(resp -> req.response().end(resp.bodyAsString()))
                .onFailure(err -> req.response().setStatusCode(502).end());
        })
        .listen(8080);
  }
}
```

### 4.9 Regla practica

> **Si la operacion retorna un `Future<T>` de Vert.x, no bloquea el event loop. Punto.**
>
> Si la operacion retorna un valor directo (String, int, ResultSet, InputStream...) y hace I/O, es bloqueante y necesitas envolverla.

### 4.10 Pero... COMO funciona? Que pasa durante esos 10 segundos?

Esta es la pregunta clave: si una query a la base de datos tarda 10 segundos, **que hace exactamente el event loop durante ese tiempo?** No es que "detecte" que algo esta bloqueado y cambie de tarea. Es un mecanismo fundamentalmente diferente: **el hilo nunca se bloquea en primer lugar.**

#### El modelo bloqueante (como funciona JDBC)

En el modelo tradicional, cuando haces una query JDBC, pasa esto:

```
Tu hilo Java
    │
    ├── 1. Serializa la query SQL en bytes
    ├── 2. Llama a socket.write() para enviar al servidor PostgreSQL
    ├── 3. Llama a socket.read() para leer la respuesta
    │      │
    │      └── EL HILO SE DUERME AQUI (el kernel lo saca del CPU)
    │          El SO pone el hilo en estado "BLOCKED/WAITING"
    │          No consume CPU, pero consume ~1MB de stack en RAM
    │          ... 10 segundos ...
    │          El kernel recibe datos en el socket
    │          El kernel despierta el hilo
    │          El SO lo pone de vuelta en la cola de ejecucion
    │      │
    ├── 4. Deserializa los bytes en un ResultSet
    └── 5. Retorna el resultado
```

El hilo **existe** durante esos 10 segundos, ocupando memoria, pero sin hacer nada. Si tienes 1000 requests concurrentes con queries de 10s, necesitas 1000 hilos bloqueados = ~1GB de RAM solo en stacks.

#### El modelo non-blocking (como funciona Vert.x)

En Vert.x con el cliente reactivo, el mecanismo es completamente diferente. **Ningun hilo se bloquea nunca.** Funciona asi:

```
Event Loop Thread (1 solo hilo)
    │
    │  REQUEST 1: query a PostgreSQL
    ├── 1. Serializa la query SQL en bytes
    ├── 2. Llama a socket.write() en modo NON-BLOCKING
    │      (Netty escribe los bytes al buffer del kernel, retorna inmediatamente)
    ├── 3. Registra "cuando haya datos en este socket, ejecuta este callback"
    │      (registra el file descriptor en el SELECTOR con el callback)
    ├── 4. RETORNA INMEDIATAMENTE ← no espera nada
    │
    │  REQUEST 2: llega otro HTTP request (0.001ms despues)
    ├── 5. Procesa el request, genera respuesta, la envia
    │
    │  REQUEST 3: llega otro HTTP request
    ├── 6. Procesa este request tambien
    │
    │  ... procesa cientos de requests mas durante 10 segundos ...
    │
    │  EVENTO: el SO notifica que el socket de PostgreSQL tiene datos
    ├── 7. El selector detecta que el file descriptor esta "readable"
    ├── 8. Lee los bytes del socket (non-blocking, los datos YA estan ahi)
    ├── 9. Deserializa la respuesta
    ├── 10. Ejecuta el callback/future del REQUEST 1 con el resultado
    │
    │  ... sigue procesando mas eventos ...
```

**El hilo NUNCA se detuvo.** No hubo 10 segundos de espera. El hilo proceso cientos de otros eventos mientras PostgreSQL procesaba la query.

#### El Selector: la pieza clave

El secreto esta en el **Selector** (Java NIO), que a su vez usa **epoll** (Linux) o **kqueue** (macOS) del sistema operativo. Asi funciona en detalle:

**Paso 1: Registro**

Cuando Vert.x (via Netty) abre un socket TCP hacia PostgreSQL, lo configura como **non-blocking** y lo registra en un Selector:

```
selector.register(socketPostgres, OP_READ, callbackParaQuery)
selector.register(socketHTTP_1,   OP_READ, callbackParaRequest1)
selector.register(socketHTTP_2,   OP_READ, callbackParaRequest2)
... cientos de sockets registrados ...
```

**Paso 2: El loop principal**

El event loop de Netty ejecuta un ciclo infinito que se ve conceptualmente asi:

```
while (true) {
    // Pregunta al SO: "¿Alguno de estos sockets tiene datos listos?"
    // Esta llamada NO consume CPU. El kernel duerme el hilo
    // SOLO hasta que al menos UN socket tenga actividad.
    List<Event> readyEvents = selector.select();

    // Solo se despierta cuando hay algo que hacer
    for (Event event : readyEvents) {
        // Ejecuta el handler correspondiente
        event.handler.handle(event.data);
    }

    // Ejecuta tareas programadas (timers, runOnContext, etc.)
    runPendingTasks();
}
```

**Paso 3: Notificacion del kernel**

Cuando PostgreSQL termina la query (10s despues) y envia la respuesta:

1. Los bytes llegan a la tarjeta de red del servidor.
2. El **kernel del SO** los copia al buffer del socket.
3. El kernel marca el file descriptor como "readable".
4. `epoll`/`kqueue` notifica al Selector: "el socket de PostgreSQL tiene datos".
5. `selector.select()` retorna con ese evento.
6. El event loop lee los datos (ya estan en el buffer, la lectura es instantanea).
7. Ejecuta el callback/future registrado.

**Nadie estuvo esperando.** El kernel maneja la espera a nivel de hardware/interrupciones de red. Ningun hilo de Java estuvo involucrado durante los 10 segundos.

#### Analogia: el restaurante

Piensa en dos modelos de restaurante:

**Modelo bloqueante (thread-per-request):**
- Cada cliente tiene un mesero dedicado.
- El mesero lleva el pedido a la cocina y **se queda parado ahi esperando** hasta que el plato esta listo.
- Para 100 clientes, necesitas 100 meseros (la mayoria parados sin hacer nada).

**Modelo non-blocking (event loop):**
- Hay 1 mesero para todo el salon.
- Toma el pedido de la mesa 1, lo lleva a cocina, **vuelve inmediatamente** al salon.
- Toma el pedido de la mesa 2, lo lleva a cocina, vuelve.
- Cuando la cocina tiene un plato listo, **toca una campana** (= el selector recibe un evento).
- El mesero recoge el plato y lo lleva a la mesa correcta.
- 1 mesero puede atender 100 mesas porque nunca se queda parado esperando.

La "campana" es `epoll`/`kqueue`. La "cocina" es la base de datos/el servicio remoto. El "mesero" es el event loop thread.

#### Diagrama temporal completo: 3 requests, 1 event loop

```
Tiempo    Event Loop Thread              PostgreSQL         Servicio HTTP externo
──────    ──────────────────             ──────────         ─────────────────────
 0ms      Recibe HTTP Request A
 0.1ms    Envia query SQL (non-blocking) ──> Recibe query
 0.2ms    Registra callback en selector      Procesando...
 0.3ms    ¡LIBRE! Pasa al siguiente evento
                                             Procesando...
 5ms      Recibe HTTP Request B              Procesando...
 5.1ms    Envia HTTP call (non-blocking)                    ──> Recibe request
 5.2ms    Registra callback en selector      Procesando...      Procesando...
 5.3ms    ¡LIBRE!                            Procesando...      Procesando...
                                             Procesando...      Procesando...
 50ms     Recibe HTTP Request C              Procesando...      Procesando...
 50.1ms   Responde directamente (sin I/O)    Procesando...      Procesando...
 50.2ms   ¡LIBRE!                            Procesando...      Procesando...
                                             Procesando...      Procesando...
 200ms    Selector: "servicio HTTP respondio"                <── Envia response
 200.1ms  Lee respuesta (instantanea, datos ya en buffer)
 200.2ms  Ejecuta callback Request B → envia HTTP response al cliente B
 200.3ms  ¡LIBRE!
                                             Procesando...
                                             Procesando...
                                             ... 10 segundos total ...
                                             Procesando...
10000ms   Selector: "PostgreSQL respondio" <── Envia resultado
10000.1ms Lee resultado (instantaneo)
10000.2ms Ejecuta callback Request A → envia HTTP response al cliente A
10000.3ms ¡LIBRE!
```

**Un solo hilo manejo 3 requests, incluyendo uno con 10 segundos de latencia, sin bloquearse nunca.** Durante esos 10 segundos, el hilo process cientos o miles de otros requests.

#### ¿Y `selector.select()` no bloquea?

Si, `selector.select()` **puede** bloquear, pero de una forma inteligente:

- Bloquea **solo si no hay absolutamente ningun evento pendiente**.
- En cuanto **cualquier** socket tiene actividad, se desbloquea inmediatamente.
- Si hay tareas programadas (timers), usa `select(timeout)` para despertarse a tiempo.
- En un servidor con trafico, **casi nunca bloquea** porque constantemente hay eventos.

Esto es muy diferente a bloquear en un `socket.read()`:
- `socket.read()` bloquea esperando datos de **un solo** socket.
- `selector.select()` espera eventos de **todos** los sockets a la vez, y se despierta con el primero que tenga actividad.

#### Resumen: por que funciona

| Aspecto | Modelo bloqueante | Modelo non-blocking (Vert.x) |
|---|---|---|
| **Quien espera** | Un hilo de Java por cada operacion | El kernel del SO (via epoll/kqueue) |
| **Costo de esperar** | ~1MB RAM por hilo + context switching | Casi cero (un file descriptor en el kernel) |
| **1000 queries de 10s** | 1000 hilos bloqueados = ~1GB RAM | 1 hilo libre, 1000 file descriptors ~= KB |
| **¿Hilo se duerme?** | Si, el SO lo saca del CPU | No, sigue procesando otros eventos |
| **¿Detecta bloqueo?** | No aplica, el bloqueo es el diseno | No hay nada que detectar, nunca bloquea |
| **¿Cambia de tarea?** | El SO hace context switch (caro) | El event loop pasa al siguiente evento (barato) |

**No es deteccion de bloqueo. No es cambio de contexto. Es que el hilo literalmente nunca se detuvo.** La operacion de I/O se delega completamente al sistema operativo a nivel de kernel, y el hilo solo interviene cuando los datos ya estan disponibles en memoria.

---

## 5. Por que elegir Vert.x frente a otros frameworks

### Comparativa detallada

#### Vert.x vs Spring Boot

| Aspecto | Vert.x | Spring Boot |
|---|---|---|
| **Naturaleza** | Toolkit reactivo | Framework full-stack |
| **Modelo I/O** | Non-blocking por defecto | Blocking por defecto (WebFlux para reactivo) |
| **Startup** | Milisegundos | ~1-2 segundos (JVM) |
| **Memoria** | Muy baja (~10-30MB) | Alta (~150-300MB) |
| **Throughput** | Extremo (top en benchmarks) | Bueno, pero menor en escenarios reactivos |
| **Ecosistema** | Modular, focalizado | Masivo, el mas grande del mundo Java |
| **Curva de aprendizaje** | Media-Alta (requiere entender async) | Baja (modelo tradicional) |
| **Comunidad** | Pequena pero activa | Enorme |
| **Inyeccion de dependencias** | No incluida (traer la propia) | Integrada (Spring IoC) |
| **Ideal para** | Alto throughput, baja latencia, microservicios ligeros | Aplicaciones empresariales, CRUD, equipos grandes |

**Cuando Spring Boot es mejor**: Si tu equipo ya conoce Spring, si necesitas el vasto ecosistema de starters, si tu aplicacion es CRUD convencional, si priorizas productividad del desarrollador sobre rendimiento extremo.

**Cuando Vert.x es mejor**: Si necesitas manejar miles/millones de conexiones simultaneas, si cada milisegundo de latencia importa, si necesitas un footprint de memoria minimo, si construyes sistemas event-driven.

#### Vert.x vs Quarkus

| Aspecto | Vert.x | Quarkus |
|---|---|---|
| **Nivel de abstraccion** | Bajo (toolkit) | Alto (framework sobre Vert.x) |
| **Motor interno** | Es el motor | **Usa Vert.x internamente** |
| **Compilacion nativa** | No directamente | GraalVM native image (~50ms startup) |
| **MicroProfile** | No | Si, con Jakarta EE |
| **Dev mode** | Manual | Live coding integrado |
| **Inyeccion de dependencias** | No incluida | ArC (CDI) |
| **Kubernetes** | Manual | Integrado con extensiones |

**Dato clave**: Quarkus **usa Vert.x como su motor reactivo interno**. Si usas Quarkus, ya estas usando Vert.x por debajo. Elegir Vert.x directamente te da control total sobre el motor sin las capas de abstraccion adicionales.

#### Vert.x vs Helidon

| Aspecto | Vert.x | Helidon |
|---|---|---|
| **Enfoque** | Event-driven reactivo | Virtual threads (Helidon 4 Nima) |
| **Virtual threads** | Soporte opcional | Core del framework |
| **Rendimiento** | Excelente en event-driven | Excelente con virtual threads |
| **Ecosistema** | Amplio y maduro | Mas reducido |
| **Respaldo** | Eclipse Foundation / Red Hat | Oracle |

#### Vert.x vs Micronaut

| Aspecto | Vert.x | Micronaut |
|---|---|---|
| **Enfoque** | Toolkit reactivo | Framework con DI en compilacion |
| **Startup** | Extremadamente rapido | Muy rapido (~656ms JVM) |
| **DI** | No incluida | Compile-time DI |
| **GraalVM** | No directamente | Excelente soporte nativo |
| **AOT** | No | Si, procesamiento en compilacion |

### Cuando elegir Vert.x

Vert.x es la opcion correcta cuando:

- Necesitas el **maximo rendimiento** posible en la JVM.
- Tu caso de uso es **I/O intensivo**: APIs de alta concurrencia, WebSockets, streaming, IoT, gateways.
- Quieres **control total** sobre la arquitectura sin que un framework te imponga decisiones.
- Necesitas un **footprint minimo** de memoria y CPU.
- Construyes **sistemas distribuidos** donde el event bus te da comunicacion transparente.
- Tu equipo entiende programacion **asincrona y reactiva**.
- Quieres usar **multiples lenguajes** en la JVM (polyglot).
- Estas construyendo **infraestructura de plataforma** (proxies, brokers, gateways) donde la eficiencia es critica.

---

## 6. Ventajas y desventajas

### Ventajas

| Ventaja | Detalle |
|---|---|
| **Rendimiento excepcional** | Consistentemente entre los frameworks mas rapidos de la JVM. Un solo event loop puede manejar decenas de miles de requests por segundo. |
| **Bajo consumo de recursos** | Core de ~650KB. Aplicaciones en produccion con 10-30MB de heap. Ideal para contenedores y entornos cloud con costos por recurso. |
| **Escalabilidad natural** | Multi-reactor escala sobre todos los cores. Event bus distribuido escala sobre multiples nodos. Despliegue de multiples instancias de verticles con una linea. |
| **Thread-safety sin sincronizacion** | El modelo de contexto garantiza que tu codigo en un verticle siempre se ejecuta en el mismo hilo. Eliminas race conditions, deadlocks y la necesidad de `synchronized`/`volatile`. |
| **Modelo de Futures limpio (v5)** | La composicion de futures con `compose`, `map`, `recover`, `all`, `any`, `join` permite escribir pipelines asincronos legibles y mantenibles. |
| **Libertad total** | No impone estructura, DI container, ni ORM. Usas lo que quieras. Puedes embeber Vert.x en cualquier app existente. |
| **Poliglota** | APIs idiomaticas en Java, Kotlin, Groovy, JavaScript, Ruby, Scala. Mezcla lenguajes segun conveniencia. |
| **Ecosistema completo** | Clientes reactivos para PostgreSQL, MySQL, MongoDB, Redis, Kafka, AMQP, MQTT, gRPC, GraphQL, OAuth2, JWT, OpenTelemetry y mas. |
| **Event bus poderoso** | Comunicacion pub/sub, point-to-point y request/reply. Se extiende transparentemente a clusters. Bridges para browsers (SockJS). |
| **Arranque instantaneo** | Sin escaneo de classpath, sin reflection pesada, sin autoconfiguracion. El servidor arranca en milisegundos. |
| **Virtual threads ready** | Soporte para `ThreadingModel.VIRTUAL_THREAD` en Java 21+, combinando el modelo reactivo con la simplicidad del codigo sincrono. |
| **Alta disponibilidad** | Soporte integrado para HA con failover automatico de verticles en clusters. |

### Desventajas

| Desventaja | Detalle |
|---|---|
| **Curva de aprendizaje** | El modelo asincrono y no bloqueante requiere un cambio de mentalidad significativo para desarrolladores acostumbrados a codigo sincrono/imperativo. |
| **Debugging mas complejo** | Las stack traces en codigo asincrono son menos intuitivas. Seguir el flujo de ejecucion a traves de futures encadenados puede ser dificil. |
| **Disciplina del event loop** | El desarrollador **debe** asegurarse de no bloquear el event loop. Un error accidental (una libreria bloqueante, un `Thread.sleep`) puede degradar toda la aplicacion. |
| **Comunidad mas pequena** | Comparado con Spring Boot, la comunidad es significativamente menor. Menos tutoriales, menos respuestas en StackOverflow, menos developers con experiencia en el mercado. |
| **Librerias de terceros bloqueantes** | La mayoria de librerias Java estan disenadas con I/O bloqueante. Usarlas en Vert.x requiere envolverlas en `executeBlocking()`, lo que complica el codigo. |
| **Sin DI integrada** | No incluye inyeccion de dependencias. Debes traer tu propio framework (Guice, Dagger, CDI) o gestionar dependencias manualmente. |
| **Sin ORM integrado** | No hay equivalente a Spring Data/JPA. Trabajas con SQL directo mediante los clientes reactivos, lo cual es mas eficiente pero menos productivo para CRUD. |
| **Event bus sin persistencia** | Los mensajes del event bus no se persisten a disco. Si un nodo cae antes de procesar un mensaje, se pierde. Para mensajeria garantizada, necesitas un broker externo (Kafka, RabbitMQ). |
| **Testing mas complejo** | Testear codigo asincrono requiere herramientas especializadas (`vertx-junit5`) y patrones diferentes a los tests unitarios tradicionales. |
| **No es "opinionated"** | La libertad total puede ser una desventaja para equipos que prefieren convenciones claras y estructura predefinida. Cada equipo puede terminar con una arquitectura diferente. |

### Resumen rapido

```
Vert.x es ideal cuando:
  + Rendimiento y eficiencia son prioridad
  + Alto volumen de conexiones concurrentes
  + Sistemas event-driven y reactivos
  + Microservicios ligeros
  + Control total sobre la arquitectura

Vert.x NO es ideal cuando:
  - El equipo no tiene experiencia con programacion asincrona
  - La aplicacion es CRUD convencional
  - Se necesita el ecosistema mas grande posible
  - Se prefiere productividad sobre rendimiento
  - Se necesitan convenciones estrictas y estructura predefinida
```

---

## Referencias

- [Documentacion oficial Vert.x 5.0.8](https://vertx.io/docs/5.0.8)
- [Vert.x Core Manual](https://vertx.io/docs/vertx-core/java/)
- [What's new in Vert.x 5](https://vertx.io/blog/whats-new-in-vert-x-5)
- [Guia de migracion Vert.x 4 a 5](https://vertx.io/docs/guides/vertx-5-migration-guide)
- [Advanced Vert.x Guide](https://vertx.io/docs/guides/advanced-vertx-guide)
- [Vert.x 5 Wiki - GitHub](https://github.com/eclipse-vertx/vert.x/wiki/Vert.x-5)
- [VerticleBase API Docs](https://vertx.io/docs/apidocs/io/vertx/core/VerticleBase.html)
- [App Generator - start.vertx.io](https://start.vertx.io/)
