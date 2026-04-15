# Repository Guidelines

## Project Structure & Module Organization
This repository combines a Spring Boot backend, a Vue 3 admin frontend, a WeChat mini-program, and a landing page.

- `src/main/java/com/yizhaoqi/smartpai/`: backend code by layer (`controller`, `service`, `repository`, `model`, `entity`, `config`)
- `src/main/resources/`: Spring configuration files such as `application.yml`
- `src/test/java/`: JUnit-based backend tests
- `frontend/`: Vite + Vue 3 + TypeScript admin app
- `miniprogram/`: WeChat mini-program pages, assets, and config
- `homepage/`: separate landing page built with Vite/Tailwind
- `docs/` and `开发文档/`: operational docs, API notes, and planning material

## Build, Test, and Development Commands
- Backend run: `mvn spring-boot:run`
- Backend test: `mvn test`
- Backend package: `mvn clean package`
- Frontend dev: `cd frontend && pnpm install && pnpm dev`
- Frontend build: `cd frontend && pnpm build`
- Frontend checks: `cd frontend && pnpm typecheck && pnpm lint`
- Landing page dev: `cd homepage && pnpm install && pnpm dev`
- Landing page build: `cd homepage && pnpm build`

Use `start-dev-services.bat` on Windows if you need local support services started together.

## Coding Style & Naming Conventions
Use 2 spaces in Vue, JS, JSON, WXML, and CSS; use standard Java formatting with 4-space indentation. Keep backend packages layered and feature-oriented. Name Spring classes by role, for example `TravelController`, `ItineraryService`, `UserRepository`. Use `PascalCase` for Java/Vue components, `camelCase` for methods and variables, and kebab-case for mini-program page folders such as `my-itineraries`.

Frontend linting uses ESLint (`frontend`). Run it before committing. Prefer small, targeted changes and avoid unrelated reformatting.

## Encoding & File Safety
Preserve the existing file encoding when editing. Do not batch-convert source files between UTF-8, GBK, or other encodings, and do not “fix” mojibake unless the task explicitly requires it. In this repository, encoding drift has previously broken mini-program WXML/JS files. When touching Chinese-content files, prefer minimal edits and verify the file still opens correctly in the target toolchain after saving.

## Testing Guidelines
Backend tests use Spring Boot Test and JUnit under `src/test/java`. Name tests `*Test` and keep class names aligned with the unit under test, for example `UserServiceTest`. Run `mvn test` before merging backend work. For frontend changes, run `pnpm typecheck && pnpm lint` in `frontend`. Mini-program changes should be verified in WeChat DevTools with at least one real navigation flow.

## Commit & Pull Request Guidelines
Recent history favors short, imperative commit titles in Chinese, for example `添加旅游规划模块接口测试文档`. Keep commits focused and scoped to one logical change. PRs should include:

- what changed and why
- affected areas (`backend`, `frontend`, `miniprogram`, `homepage`)
- setup or migration notes if configs or SQL changed
- screenshots or screen recordings for UI and mini-program updates
- linked issue or task reference when available

## Security & Configuration Tips
Do not commit secrets, tokens, or machine-specific overrides. Keep local-only settings in `application-dev.yml`, `application-windows.yml`, or `project.private.config.json`. Review SQL and config files carefully before pushing.
