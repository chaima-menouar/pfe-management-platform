# Security policy

## Supported version

Security fixes are applied to the latest version on the default branch.

## Reporting a vulnerability

Please report vulnerabilities privately to the repository maintainer instead of opening a public issue. Do not include real student, teacher, database, or authentication data in a report.

## Deployment notes

- Never commit `.env` or real credentials.
- Set a unique `APP_ADMIN_PASSWORD` with at least 12 characters before the first start.
- Use TLS and a reverse proxy for any public deployment.
- Replace `spring.jpa.hibernate.ddl-auto=update` with managed database migrations before production use.
- Excel imports are limited to 5 MB per file, but uploaded academic data must still be handled according to the institution's privacy rules.
