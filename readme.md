# CarbonHub — Proyecto Backend

API REST para gestión de huella de carbono con autenticación JWT, roles y funcionalidades asistidas por IA (Spring AI + Google Gemini), construida con Spring Boot 3.5.x y PostgreSQL.

---

## Requisitos previos

- **Java 21 (JDK temurin-21 Eclipse Temurin 21.0.11)**
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
docker compose -f docker/docker-compose.yml up -d
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
CLIMATIQ_API_KEY
GMAIL_USERNAME
GMAIL_APP_PASSWORD
```

#### Ejemplo de variables de entorno

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_URL` | URL de conexión JDBC a PostgreSQL | `jdbc:postgresql://localhost:5432/carbonhub` |
| `DB_USER` | Usuario de la base de datos | `carbonhub` |
| `DB_PASSWORD` | Password del usuario de la base de datos | `carbonhub` |
| `JWT_SECRET` | Clave secreta para firmar los JWT (Base64, mínimo 256 bits) | `your_jwt_secret_here` |
| `JWT_EXPIRATION` | Tiempo de expiración del token en milisegundos (se renueva en cada petición autenticada, ver `X-Refresh-Token`) | `1800000` (30 minutos) |
| `GEMINI_API_KEY` | API Key de Google Gemini (Spring AI) | `your_gemini_api_key_here` |
| `CLIMATIQ_API_KEY` | API Key de Climatiq (climatiq.io) | `your_climatiq_api_key_here` |
| `GMAIL_USERNAME` | Correo de la cuenta de Gmail dedicada del proyecto (SMTP) | `tu_correo@gmail.com` |
| `GMAIL_APP_PASSWORD` | Contraseña de aplicación de esa cuenta de Gmail (no la contraseña normal) | `tu_app_password_de_gmail` |

> ⚠️ **Importante:** `JWT_SECRET`, `GEMINI_API_KEY`, `CLIMATIQ_API_KEY` y `GMAIL_APP_PASSWORD` son credenciales sensibles. El valor real de cada una debe compartirse por un canal privado del equipo.

#### Envío de correos: `app.email.provider`

No es una variable obligatoria — tiene un valor por defecto y **no necesitás configurarla** para correr el proyecto localmente.

- Por defecto (`app.email.provider=stub`, o sin configurar nada) los correos de verificación **no se envían de verdad**: se loguea el enlace en la consola. Así nadie del equipo se bloquea por no tener credenciales de Gmail.
- Para probar el envío real, configurá `APP_EMAIL_PROVIDER=gmail` (además de `GMAIL_USERNAME`/`GMAIL_APP_PASSWORD` reales) — esto activa `EmailVerificacionServiceImpl` en vez del stub.

#### Configuración en IntelliJ

Como la mayoría del equipo usa IntelliJ, hay dos formas de configurar estas variables:

**Opción A: Desde Run → Edit Configurations**

1. Ir a **Run → Edit Configurations…**
2. Seleccionar la configuración de la clase principal (`Application`).
3. En el campo **Environment variables**, hacer clic en el ícono al final del campo para abrir el editor.
4. Agregar cada variable en formato `NOMBRE=valor`, una por línea. Ejemplo:

   ```
   DB_URL=jdbc:postgresql://localhost:5432/carbonhub
   DB_USER=carbonhub
   DB_PASSWORD=carbonhub
   JWT_EXPIRATION=1800000
   JWT_SECRET=<valor real, pedirlo al equipo>
   GEMINI_API_KEY=<valor real, pedirlo al equipo>
   GMAIL_USERNAME=tu_correo@gmail.com
   GMAIL_APP_PASSWORD=<valor real, pedirlo al equipo>
   ```

5. Aplicar y correr normalmente.

**Opción B: Usando un archivo `.env` con el plugin EnvFile**

1. Instalar el plugin **EnvFile** desde **File → Settings → Plugins**, buscarlo e instalarlo (requiere reiniciar IntelliJ).
2. Crear un archivo `.env` en la raíz del proyecto (mismo nivel que `pom.xml`), usando el siguiente formato como base y completando los valores reales:

   ```
   DB_URL=jdbc:postgresql://localhost:5432/carbonhub
   DB_USER=carbonhub
   DB_PASSWORD=carbonhub
   JWT_EXPIRATION=1800000
   JWT_SECRET=<valor real, pedirlo al equipo>
   GEMINI_API_KEY=<valor real, pedirlo al equipo>
   GMAIL_USERNAME=tu_correo@gmail.com
   GMAIL_APP_PASSWORD=<valor real, pedirlo al equipo>
   ```

3. Ir a **Run → Edit Configurations…** y seleccionar la configuración de la app.
4. Marcar el checkbox **Enable EnvFile**.
5. Hacer clic en **+** y seleccionar el archivo `.env` creado.
6. Aplicar y correr. IntelliJ carga las variables automáticamente desde ese archivo en cada ejecución.

> ⚠️ El archivo `.env` real **nunca se sube al repositorio**.

---

### 4. Ejecutar el proyecto

Desde IDE:

- Ejecutar la clase principal:

    ```
    Application.java
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
spring.ai.google.genai.chat.options.model=gemini-3.1-flash-lite
spring.ai.google.genai.chat.options.temperature=0.2
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
- `JWT_SECRET` y `GEMINI_API_KEY` son credenciales sensibles: nunca deben publicarse en el README, ni en commits. Compartirlas por un canal privado del equipo.