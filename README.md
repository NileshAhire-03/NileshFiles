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
