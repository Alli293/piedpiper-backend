# Coding Conventions — CarbonHub Backend

Single source of truth for how code is written in this repo. Applies to the whole team and to any AI assistant (Claude, Codex, ChatGPT, …) generating code here.

**For AI agents:** read this before writing or modifying code. Every rule reflects a pattern already established in the repo. If you need to deviate, say so explicitly and explain why — don't do it silently.

> **This doc is in English for brevity. The code is not.** Domain field names, user-facing messages, test names, commit messages, and PRs are all **Spanish** — see §2 and §7. Do not "translate" the codebase because this document is in English.

These conventions came out of two audits of `develop` (Sprint 1) that found 18 inconsistencies. Almost none were bugs — they were deviations from patterns the repo already used elsewhere. The point is to stop that from recurring.

---

## 1. Package structure

Everything hangs off `com.piedpiper.carbonhub`, organized **by domain**, not by technical layer:

```
<domain>/
  controller/          REST controllers
  service/             Business logic
  repository/          Spring Data repositories
  mappers/             MapStruct mappers
  validation/          Custom validators (only if the domain needs them)
  config/              Domain-specific config (only if applicable)
  models/
    dtos/              Request and response DTOs
    entities/          JPA entities
    enums/             Domain enums
```

Current domains: `auth`, `emision`, `empresa`, `ima`, `invitacion`, `limite`, `notification`, `user`.

Cross-cutting packages:
- `common/` — shared utilities (`Autenticaciones`, `Catalogos`, `ApiErrorDTO`)
- `exceptions/` — `ApiException` and `GlobalExceptionHandler`

**Rule:** new functionality goes in the domain it belongs to. Do not create layer-first packages at the root (`services/`, `controllers/`). Genuinely cross-cutting code goes in `common/`.

---

## 2. Naming

### Classes
| Type | Pattern | Example |
|---|---|---|
| Controller | `<Domain>Controller` | `EmisionController` |
| Service | `<Domain><Action>Service` | `EmisionEnvioService` |
| Repository | `<Entity>Repository` | `LimiteEmisionesRepository` |
| Mapper | `<Entity>Mapper` | `EmisionVueloMapper` |
| Request DTO | `<Action><Domain>RequestDTO` | `RegistrarEnvioRequestDTO` |
| Response DTO | `<Entity>ResponseDTO` | `EmisionVueloResponseDTO` |

DTOs in the same family share a prefix. `VueloResponseDTO` was renamed to `EmisionVueloResponseDTO` precisely because it broke the pattern of its siblings (`EmisionEnvioResponseDTO`, `EmisionFlotaResponseDTO`, `EmisionElectricidadResponseDTO`).

### Fields
**Spanish for domain vocabulary.** This is the repo's convention: `nombreEmpresa`, `cedulaJuridica`, `sectorIndustrial`, `justificacion`, `fechaEmision`, `actualizadoEn`, `aceptaTerminos`.

**English only for universal technical terms**, where translating would be worse: `id`, `token`, `email`, `slug`, `url`, `carbonKg`.

> **Known debt:** the `emision` DTOs mix languages (`distanceValue`, `weightUnit`, `passengers`, `transportMethod`). Identified, pending a team decision. **Do not copy that pattern into new code** — use Spanish for new fields.

### Database
- Tables: **plural, Spanish**, always with an explicit `@Table(name = "...")` — `usuarios`, `empresas`, `invitaciones`, `emisiones`, `limites_emisiones`.
- Columns: `snake_case`, with an explicit `@Column(name = "...")` whenever the name isn't trivially identical to the field.

Never rely on Hibernate's inferred name: `Empresa` had no `@Table` and resolved to `empresa` (singular), silently breaking the convention.

---

## 3. Controllers

```java
@RestController
@RequestMapping("/api/emisiones")
@PreAuthorize("hasAnyRole('ADMINISTRADOR_EMPRESA', 'USUARIO_GENERAL')")
public class EmisionController {

    private final EmisionConsultaService emisionConsultaService;

    public EmisionController(EmisionConsultaService emisionConsultaService) {
        this.emisionConsultaService = emisionConsultaService;
    }

    @GetMapping
    public ResponseEntity<List<EmisionResponseDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(emisionConsultaService.listar(Autenticaciones.usuarioId(authentication)));
    }
}
```

**Rules:**

1. **Constructor injection.** Never field `@Autowired`. (There is not a single `@Autowired` in the repo — keep it that way.)
2. **Resolve the authenticated user via an injected `Authentication` method parameter**, converted with `Autenticaciones.usuarioId(authentication)`. Never read `SecurityContextHolder` manually from a controller.
3. **`@PreAuthorize` at class level** when every method shares the same expression. Method level only when a specific endpoint differs.
4. **Always declare `@PreAuthorize` explicitly** on protected endpoints, even though `SecurityConfig` already requires authentication. Role authorization must be readable in the controller itself.
5. **Return `ResponseEntity<T>`**, not a bare DTO.
6. **Validate the body with `@Valid @RequestBody`.**
7. **No business logic and no `try/catch` in controllers.** Delegate to the service; `GlobalExceptionHandler` handles errors.

### Status codes
- `201 CREATED` when creating; `200 OK` when updating.
- If one endpoint creates **or** updates (upsert), the service reports which happened via a flag on the DTO and the controller decides:
  ```java
  HttpStatus status = response.isRecienCreada() ? HttpStatus.CREATED : HttpStatus.OK;
  return ResponseEntity.status(status).body(response);
  ```
- `204 NO_CONTENT` on deletes.

---

## 4. Services

1. **Constructor injection**, same as controllers.
2. **`@Transactional` on every writing method**, `@Transactional(readOnly = true)` on read-only ones. Use `org.springframework.transaction.annotation.Transactional`.
3. **Throw `ApiException`** — never raw exceptions or `ResponseStatusException`.
4. **Map entity → DTO with MapStruct**, not by hand.
5. **External side effects (email, HTTP calls) run after commit**, not inside the transaction:
   ```java
   TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
       @Override public void afterCommit() { envioCorreoService.enviar(...); }
   });
   ```
   See `InvitacionService.enviarTrasCommit(...)` as the reference.
6. **Shared preconditions belong in a collaborator**, not duplicated per domain. E.g. the "user has a configured empresa" check uses `ApiException.empresaNoConfigurada()` uniformly.
7. **Don't add defensive guards for cases an earlier layer already guarantees.** E.g. `ClimatiqClient.validarRespuesta()` guarantees `co2e()` is never null; re-checking downstream is dead code.

---

## 5. JPA entities

```java
@Entity
@Table(name = "invitaciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "fecha_emision", nullable = false)
    private Instant fechaEmision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInvitacion estado;
}
```

**Rules:**

1. **Lombok, not hand-written boilerplate.** `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`. In inheritance hierarchies, `@SuperBuilder` + `@NoArgsConstructor` (see `Emision` and its subclasses).
2. **Never `@Data` on entities.** It generates `equals`/`hashCode` over every field, which breaks with JPA (lazy collections, generated ids). Entities rely on `Object` identity, which is correct.
3. **`Instant` for timestamps, never `LocalDateTime`.** `LocalDateTime` loses the timezone.
4. **Always an explicit `@Table(name = "...")`** — except `@DiscriminatorValue` subclasses under `SINGLE_TABLE` inheritance, which inherit the parent's table.
5. **`@Enumerated(EnumType.STRING)`**, never ordinal.
6. **`@ManyToOne(fetch = FetchType.LAZY)`** by default.
7. **Accessors stay symmetric.** If Lombok generates getters and setters for every field, don't hand-write partial accessors.

---

## 6. DTOs

- **Simple DTO:** `@Data @NoArgsConstructor @AllArgsConstructor`. This is the dominant pattern (28 of 31 DTOs).
- **DTO in an inheritance hierarchy:** `@Getter @Setter @SuperBuilder @NoArgsConstructor` (see the `Emision*ResponseDTO` family).
- **`@Data` *is* fine on DTOs** (unlike entities): they're flat transport objects with no JPA identity.
- **Don't put `@JsonIgnoreProperties(ignoreUnknown = true)` on response DTOs.** It only affects deserialization; on an outbound-only DTO it's a no-op.

---

## 7. Validation

1. **Bean Validation on the DTO**, with messages **in Spanish, written for the end user** (they go straight to the UI):
   ```java
   @NotBlank(message = "Ingrese un título para este registro.")
   @Size(max = 150, message = "El título no puede contener más de 150 caracteres")
   private String titulo;
   ```
2. **Custom validators** live in `<domain>/validation/`, as an annotation + `ConstraintValidator` pair (see `@AnioLimiteValido` / `AnioLimiteValidoValidator`).
3. **Validate in the service only when Bean Validation can't express it**, and document why on the DTO. Only current case: `PreferenciasUsuarioRequestDTO` takes `String` instead of enums on purpose, to return `422` rather than the `400` Jackson would produce on a deserialization failure.

---

## 8. Error handling

1. **All error → HTTP translation lives in `GlobalExceptionHandler`.** A controller never builds an error response by hand.
2. **Prefer `ApiException` factory methods** (`ApiException.empresaNoConfigurada()`, `recursoNoEncontrado(...)`, `accesoDenegado(...)`) over `new ApiException(status, "message")`. They centralize message + status and stop the same error being worded differently in two places.
   > *Existing exception:* `LoginService` constructs `ApiException` directly. Known debt — don't take it as the example.
3. **New error? Add a factory method** to `ApiException` instead of instantiating inline.
4. **Error messages are for end users: Spanish, no internal detail.** Never leak stack traces, class names, or API keys in a response.
5. **Log before returning 5xx.** The generic handler logs the full exception; if you catch an error in a service to rewrap it, log the original with context:
   ```java
   log.error("Error inesperado al guardar las preferencias del usuario {}", usuarioId, e);
   throw ApiException.errorInterno("No se pudieron guardar tus preferencias. Intenta nuevamente.");
   ```

---

## 9. Mappers

- MapStruct with `@Mapper(componentModel = "spring")`.
- One mapper per entity, in `<domain>/mappers/`.
- Fields the service computes or sets separately are marked `@Mapping(target = "...", ignore = true)`:
  ```java
  @Mapper(componentModel = "spring")
  public interface LimiteEmisionesMapper {
      @Mapping(target = "mensaje", ignore = true)
      @Mapping(target = "recienCreada", ignore = true)
      LimiteEmisionesResponseDTO toDto(LimiteEmisiones limite);
  }
  ```
- If the build emits *unmapped property* warnings, fix them — don't ignore them.

---

## 10. Tests

1. **`@MockitoBean`, never `@MockBean`** (deprecated in Spring Boot 3.4+).
2. **Controller tests:** `@WebMvcTest` + `@AutoConfigureMockMvc(addFilters = false)`.
3. **For `@PreAuthorize` to actually be evaluated in a slice test**, import a config with `@EnableMethodSecurity`:
   ```java
   @Import(EmisionControllerTest.MethodSecurityTestConfig.class)
   class EmisionControllerTest {
       @TestConfiguration
       @EnableMethodSecurity
       static class MethodSecurityTestConfig { }
   }
   ```
4. **`@WithMockUser` and `.principal(...)` do different jobs, and you usually need both:**
   - `@WithMockUser` populates `SecurityContextHolder` → used by `@PreAuthorize`.
   - `.principal(...)` populates `request.getUserPrincipal()` → this is where the method's `Authentication` parameter comes from.

   With `addFilters = false` there's no filter to sync them, so a test hitting an endpoint with an injected `Authentication` **needs both**. Omitting `.principal(...)` yields a misleading `403`.
5. **Descriptive test names, in Spanish**, stating scenario and outcome: `postValidoRetornaCreatedCuandoCreaLimite`, `rolNoAutorizadoDevuelve403`.
6. **Don't test unreachable code.** If a guard can't be reached in production, the guard is the problem, not the missing test.

---

## 11. Before opening a PR

- [ ] `./mvnw test` green (with `JAVA_HOME` pointing at JDK 21).
- [ ] No new compiler warnings (unused imports, deprecated APIs).
- [ ] No dead code: unreachable guards, no-op annotations, empty config classes, unused imports.
- [ ] PR title and body **in Spanish**, following `.github/PULL_REQUEST_TEMPLATE.md`.
- [ ] Touched an entity? Consider the schema impact: the project runs `ddl-auto=update` **with no migration tool**. Hibernate does not rename tables or columns — a rename creates a new structure and orphans the old data.

---

## 12. Known technical debt

Not examples to follow. Documented so nobody half-fixes or replicates them:

| Item | Status |
|---|---|
| English field names in `emision` DTOs | Pending team decision |
| `LoginService` instantiates `ApiException` directly instead of using factory methods | Known debt |
| Spring AI dependency declared but unused | Intentional — will be used soon |
| `spring.jpa.open-in-view` unset (defaults to `true`, known anti-pattern) | Pending |
| `ddl-auto=update` with no Flyway/Liquibase | Pending; risky for production |
