# Lab 10 TDSN - Twitter-like seguro con Auth0

Aplicación tipo Twitter (stream público de posts cortos) construida **hasta este punto** como:

- **Backend monolítico** en Spring Boot
- **Frontend SPA** en React + Vite
- **Autenticación/autorización** con Auth0 usando JWT

El objetivo académico es evolucionar luego a microservicios/serverless en AWS, pero este repositorio actualmente cubre la fase monolítica funcional y segura.

## Funcionalidades implementadas

- Login / logout en frontend con Auth0
- Crear posts de hasta 140 caracteres (endpoint protegido)
- Ver stream público global (endpoint público)
- Consultar perfil del usuario autenticado (`/api/me`, protegido)
- Documentación OpenAPI/Swagger disponible en backend

## Estructura del proyecto

- `backend/`: API REST Spring Boot (monolito)
- `frontend/`: SPA React que consume la API
- `.gitignore`: ignora secretos y artefactos de build

## Backend (Spring Boot)

### Stack

- Java 17
- Spring Boot 3.3.x
- Spring Web, Validation, Data JPA
- Spring Security OAuth2 Resource Server
- H2 (memoria, para desarrollo)
- Springdoc OpenAPI (Swagger UI)

### Entidades principales

- `AppUser`: usuario sincronizado desde claims del JWT (`sub`, `email`, `name`, `picture`)
- `Post`: contenido del post (máx. 140), autor y fecha

### Endpoints

- `GET /api/posts` -> Público, devuelve stream global
- `GET /api/stream` -> Público, alias del stream global
- `POST /api/posts` -> Protegido, requiere JWT con scope `write:posts`
- `GET /api/me` -> Protegido, requiere JWT con scope `read:profile`

### Documentación API

- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Seguridad con Auth0

### Flujo actual

- El frontend solicita access token a Auth0.
- El backend valida el JWT como Resource Server.
- Se verifica issuer (`AUTH0_DOMAIN`) y audience (`AUTH0_AUDIENCE`).
- Las rutas protegidas usan scopes (`SCOPE_write:posts`, `SCOPE_read:profile`).

### Configuración recomendada en Auth0

1. Crear una **API** con Identifier = valor de `AUTH0_AUDIENCE`.
2. Definir scopes:
   - `read:posts`
   - `write:posts`
   - `read:profile`
3. Crear una **Single Page Application** (SPA).
4. Configurar Allowed Callback URLs, Logout URLs y Web Origins.

## Variables de entorno

> Importante: `.env` está ignorado por git para no subir secretos.

### Backend (`backend/.env`)

- `AUTH0_DOMAIN`
- `AUTH0_AUDIENCE`
- `AUTH0_CLIENT_ID` (referencia de configuración)
- `CORS_ALLOWED_ORIGINS`

Existe plantilla: `backend/.env.example`.

### Frontend (`frontend/.env`)

- `VITE_AUTH0_DOMAIN`
- `VITE_AUTH0_CLIENT_ID`
- `VITE_AUTH0_AUDIENCE`
- `VITE_API_BASE_URL`

Existe plantilla: `frontend/.env.example`.

## Cómo ejecutar en local

## 1) Backend

Desde `backend/`:

```bash
mvn spring-boot:run
```

API en:

- `http://localhost:8080`

Swagger en:

- `http://localhost:8080/swagger-ui.html`

## 2) Frontend

Desde `frontend/`:

```bash
npm install
npm run dev
```

Frontend en:

- `http://localhost:5173`

## Pruebas

En `backend/`:

```bash
mvn test
```

Actualmente hay pruebas de integración para:

- Endpoints públicos sin token
- Endpoints protegidos con/sin scopes correctos

## Despliegue esperado del frontend en S3 (estado de avance)

La app ya está preparada para build estático con Vite:

```bash
npm run build
```

Esto genera `frontend/dist/`, carpeta que se puede publicar en S3 Static Website Hosting.

Pendiente operativo (fuera de código): crear/configurar bucket S3, política pública, y ajustar URLs finales en Auth0 + CORS backend.

## Estado del proyecto (hasta este punto)

- Monolito Spring Boot funcional
- SPA React integrada con Auth0
- API protegida con JWT y scopes
- Swagger disponible
- Base para continuar con la fase de microservicios/serverless en AWS
