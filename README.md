# CareerPath AI

A recruitment platform with separate Candidate, HR, and Admin workspaces, built with React, Spring Boot, and MySQL.

The application source, setup instructions, configuration templates, and tests are in [nilspath-ai](nilspath-ai/README.md).

## Project layout

- `nilspath-ai/frontend`: React and Vite frontend.
- `nilspath-ai/backend`: Spring Boot REST API and regression tests.
- `nilspath-ai/docker-compose.yml`: local MySQL service.
- `nilspath-ai/.env.example`: configuration template; copy to `.env` and supply your own secrets.

Local passwords, database contents, dependencies, and generated files are excluded from Git. Each deployment needs its own database and environment variables; uploading this repository does not transfer local accounts or passwords.

GitHub stores the source code. Running the complete application online also requires a Java backend host and a MySQL database, plus a frontend host configured for SPA routes and the backend API URL. See the application README for current production limitations.

## Deploy on Render with TiDB

Use the repository's `render.yaml` as a Render Blueprint. It selects a free Docker web service. The Docker image builds React and bundles it into Spring Boot, so frontend and API share one HTTPS origin. Login URLs support direct visits and refreshes.

Provide `DB_URL` (a JDBC URL with `sslMode=VERIFY_IDENTITY`), `DB_USER`, `DB_PASSWORD`, and a new strong `ADMIN_PASSWORD` of at least 12 characters in Render's environment form. Do not commit these secrets. `ADMIN_USERNAME` defaults to `admin`. Local users and jobs are not copied to the cloud database.

For the first deployment to an empty schema, the Blueprint sets `DDL_AUTO=update`. After successful initialization, change it to `validate` in Render and in the Blueprint; use reviewed schema migrations for subsequent schema changes. The production profile defaults to validation if this override is absent.

The app listens on Render's `PORT`. `/health` is the public health check, and `/admin/login` is the separate Admin entry. Free web services can sleep when idle. The database remains in TiDB when the web service restarts.
