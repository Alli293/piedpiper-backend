# CarbonHub — Proyecto Backend

API REST para gestión de huella de carbono con autenticación JWT, roles y funcionalidades asistidas por IA (Spring AI + Google Gemini), construida con Spring Boot 3.5.x y PostgreSQL.

---

## Requisitos previos

- **Java 21 (JDK)**
- **Maven 3.9+** (o Maven integrado en IDE)
- **Docker + Docker Compose**
- **Postman** (opcional para pruebas de API)
- Cuenta/API Key de **Google Gemini (Spring AI)**

---

## Configuración del entorno

### 1. Clonar el repositorio

```
git clone https://github.com/Alli293/piedpiper-backend.git
```

---

### 2. Base de datos (PostgreSQL con Docker)

El proyecto incluye un `docker-compose.yml` para levantar PostgreSQL localmente.

Ejecutar:

```
docker compose-f docker/docker-compose.yml up-d
```

---

### 3. Configuración de variables de entorno

El proyecto **no incluye credenciales hardcodeadas**, por lo que debes configurar estas variables:

```
DB_URL
DB_USER
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION
GEMINI_API_KEY
```

---

### 4. Ejecutar el proyecto

Desde IDE:

- Ejecutar la clase principal:

    ```
    CarbonHubApplication.java
    ```

---

### 5. Acceso a la aplicación

La API corre en:

```
http://localhost:8080
```

---

## Estructura del proyecto

```
src/
 ├── main/
 │   ├── java/
 │   └── resources/
docker/
 └── docker-compose.yml
```

---

## Configuración importante

### Base de datos

Configuración en `application.properties`:

```
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
```

---

### Seguridad (JWT)

```
security.jwt.secret-key=${JWT_SECRET}
security.jwt.expiration-time=${JWT_EXPIRATION}
```

---

### Spring AI (Gemini)

```
spring.ai.google.genai.api-key=${GEMINI_API_KEY}
spring.ai.google.genai.chat.model=gemini-3.5-flash
```

---

## Dependencias principales

- Spring Boot Web
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL Driver
- Spring AI (Google Gemini)
- MapStruct
- Lombok

---

## Notas importantes

- No subir archivos `.env` al repositorio.
- El proyecto requiere variables de entorno para arrancar correctamente.
- PostgreSQL debe estar activo antes de ejecutar la aplicación.
- Spring AI requiere API Key válida de Google Gemini.
