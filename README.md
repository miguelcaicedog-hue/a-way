# A-WAY

Plataforma vacía para la administración de campeonatos. `a-way-web` es el front en React. `a-way-core` es la API Java 11 empaquetada como WAR y servida por Tomcat 9. PostgreSQL guarda los datos. Las tres piezas se levantan con Docker Compose.

## Arranque

Desde la raíz del repositorio:

```bash
docker compose up --build
```

- Front: http://localhost
- API: http://localhost:8080/api/health

La pantalla del front muestra el mismo estado de la API y de la base.

## Variables

| Variable | Uso | Valor local |
| --- | --- | --- |
| `POSTGRES_DB` | Nombre de la base | `away` |
| `POSTGRES_USER` | Usuario de PostgreSQL | `away` |
| `POSTGRES_PASSWORD` | Clave de PostgreSQL | `away` |
| `SPRING_DATASOURCE_URL` | JDBC de la API | `jdbc:postgresql://postgres:5432/away` |
| `SPRING_DATASOURCE_USERNAME` | Usuario JDBC | `away` |
| `SPRING_DATASOURCE_PASSWORD` | Clave JDBC | `away` |
| `APP_CORS_ALLOWED_ORIGINS` | Origen permitido para `/api` | `http://localhost` |
| `VITE_API_BASE_URL` | Prefijo de la API en el front | vacío: las llamadas van a `/api` |

Estas claves son solo para el entorno local. No uses estos valores en un servidor expuesto.
