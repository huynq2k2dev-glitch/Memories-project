# Backend module structure

Backend uses feature-first modules directly under:

`src/main/java/com/memories/platform/<module-name>/`

Each feature module keeps its own implementation in the following packages when those responsibilities exist:

- `controller`: REST controllers only.
- `dto`: request, response, command, and result data transferred across the module interface.
- `service`: business workflows, transactions, and feature-specific adapters.
- `repository`: persistence interfaces for the module's entities.
- `entity`: JPA entities, value objects, and persistence enums.
- `constants`: module-specific constants in classes such as `AuthConstants`.
- `exception`: module-specific failures translated by the common web error handler.

Cross-cutting packages stay outside feature modules:

- `config`: application-wide Spring configuration.
- `utils`: stateless utilities that are reusable by more than one feature module.
- `common`: shared web and infrastructure behavior.

Dependency direction:

- Controllers depend on DTOs and services.
- Services own business behavior and use repositories/entities internally.
- Repositories depend on entities, not controllers or DTOs.
- One feature module must not use another module's repository or entity directly; use the other module's service/DTO interface.
- Keep module interfaces small. Do not add empty packages, pass-through classes, or generic utilities before a real responsibility exists.

Use lowercase Java package names. `Constants` refers to a class suffix, not an uppercase package name.
