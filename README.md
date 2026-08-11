# AEMET Weather App

Aplicación full stack para consultar la previsión meteorológica de mañana de cualquier municipio español, usando los datos de AEMET OpenData. Permite buscar el municipio mediante autocomplete, ver la temperatura media en Celsius o Fahrenheit y la probabilidad de precipitación por franjas horarias.

## Tecnologías

**Backend:**
- Java 21
- Spring Boot 4.1
- Spring MVC / RestClient
- Spring Cache
- Maven

**Frontend:**
- Angular 22
- Angular Material
- RxJS
- TypeScript

**Infraestructura:**
- Docker
- Docker Compose
- Nginx

## Arquitectura

```
Navegador
   ↓
Angular
   ↓ /api
Spring Boot
   ↓
AEMET OpenData
```

El frontend nunca llama a AEMET directamente: todas las peticiones pasan por el backend, que es quien conoce la API key y el contrato real de AEMET.

En Docker, Angular se sirve a través de Nginx, y Nginx hace de proxy de `/api` hacia el backend. En desarrollo local, ese mismo papel lo hace el proxy del dev server de Angular.

## Funcionalidad

- Búsqueda de municipios con autocomplete.
- La previsión se consulta únicamente después de seleccionar un municipio del autocomplete.
- Previsión del día siguiente: temperatura media y probabilidad de precipitación.
- Cambio entre Celsius y Fahrenheit.
- Precipitación desglosada en las franjas 00-06, 06-12, 12-18 y 18-24.
- Actualización automática al cambiar el municipio o la unidad, sin botón "Buscar".
- El último municipio seleccionado se recuerda entre sesiones (localStorage).

## Algunas decisiones técnicas

### Integración con AEMET

AEMET OpenData responde en dos pasos: la primera petición devuelve un JSON con una URL temporal en el campo `datos`, y es esa segunda URL la que contiene los datos reales. El backend hace ambas peticiones de forma transparente para el frontend.

Durante las pruebas se detectó que esa segunda URL a veces sirve el JSON con `Content-Type: text/plain` y un charset concreto en vez de `application/json`. El cliente HTTP lee el cuerpo como texto y lo parsea posteriormente, evitando depender de que la respuesta se sirva como application/json.

### Caché de municipios

El endpoint de listado de municipios de AEMET devuelve el catálogo completo y no ofrece filtrado parcial por nombre, por lo que el backend realiza esa búsqueda localmente. En la primera implementación se consultaba AEMET en cada búsqueda del autocomplete y, durante las pruebas reales, esto provocó un HTTP 429 por exceso de peticiones.

El catálogo se guarda en memoria durante la vida de la instancia del backend, de modo que las búsquedas posteriores reutilizan esa copia local en vez de volver a pedirla a AEMET. Esto hace el autocomplete más rápido y evita alcanzar el límite de peticiones del proveedor. Al reiniciar la aplicación la caché se pierde, y la primera búsqueda vuelve a cargar el catálogo desde AEMET.

### Predicción de mañana

No se asume que la predicción de "mañana" esté en una posición fija de la respuesta de AEMET: se recorre la lista de días devuelta y se busca la que coincide con la fecha de mañana, calculada con un `Clock` configurado en `Europe/Madrid`.

### Frontend reactivo

El campo de búsqueda usa `debounceTime` para no lanzar una petición por cada tecla, y `switchMap` para descartar la respuesta de una búsqueda anterior si el usuario ya ha escrito algo nuevo. La previsión se vuelve a solicitar automáticamente al cambiar el municipio seleccionado o la unidad de temperatura.

### Último municipio

Al seleccionar un municipio se guarda en `localStorage` únicamente `{ code, name }`. Al arrancar la aplicación, si hay un valor guardado y tiene forma válida, se restaura y se pide su previsión automáticamente.

## Ejecutar con Docker

Es la forma recomendada de levantar el proyecto completo. Hace falta una API key de AEMET (gratuita, se solicita en su web).

1. Copia el archivo de ejemplo y añade tu API key:

   Windows:
   ```
   copy .env.example .env
   ```

   Linux/macOS:
   ```
   cp .env.example .env
   ```

   Edita `.env` y pon:

   ```
   AEMET_API_KEY=tu-api-key
   ```

2. Desde la raíz del proyecto:

   ```
   docker compose up --build
   ```

3. Abre [http://localhost:4200](http://localhost:4200)

El backend también queda accesible directamente en `http://localhost:8080` si quieres probarlo por separado.

## Ejecutar en local

Requisitos: Java 21, Node.js compatible con Angular 22, npm y una API key de AEMET.

**Backend:**

```
cd backend
$env:AEMET_API_KEY="tu-api-key"
.\mvnw.cmd spring-boot:run
```

**Frontend:**

```
cd frontend
npm ci
npm start
```

Abre [http://localhost:4200](http://localhost:4200)

En desarrollo, Angular usa un proxy (`proxy.conf.json`) que redirige `/api` a `localhost:8080`, así que no hace falta configurar CORS en el backend.

## Tests

**Backend:**

```
cd backend
.\mvnw.cmd test
```

**Frontend:**

```
cd frontend
npm test
```

Hay tests para la integración con AEMET (con mocks, sin llamar a la API real), los servicios de municipios y previsión, los controllers, la caché y el manejo de errores en el backend, y para el cliente HTTP y el comportamiento del componente principal en el frontend.

## Endpoints

**Municipios**

```
GET /api/municipalities?name=madrid
```

```json
[
  { "code": "28079", "name": "Madrid" }
]
```

**Previsión**

```
GET /api/forecast/{municipalityCode}?temperatureUnit=G_CEL
```

`temperatureUnit` acepta `G_CEL` o `G_FAH` (por defecto `G_CEL`).

```json
{
  "mediaTemperatura": 25.5,
  "unidadTemperatura": "G_CEL",
  "probPrecipitacion": [
    { "probabilidad": 0, "periodo": "00-06" },
    { "probabilidad": 0, "periodo": "06-12" },
    { "probabilidad": 5, "periodo": "12-18" },
    { "probabilidad": 0, "periodo": "18-24" }
  ]
}
```

**Errores**

Las respuestas de error usan `ProblemDetail`:

- `400` — petición inválida (parámetro ausente, código de municipio con formato incorrecto, unidad de temperatura no reconocida...).
- `404` — el municipio no existe o no hay previsión disponible para mañana.
- `502` — no se ha podido obtener la información de AEMET.

## Estructura del proyecto

```
aemet-weather-app/
├── backend/
├── frontend/
├── compose.yaml
└── .env.example
```
