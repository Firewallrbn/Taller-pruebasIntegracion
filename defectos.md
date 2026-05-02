# Registro de Defectos

Este documento recopila los **defectos detectados durante las pruebas unitarias, de integracion y de sistema** del proyecto **Registraduria**. Cada defecto se documenta de manera estructurada para facilitar su analisis, trazabilidad y correccion.

---

## Formato 1: Lista detallada (narrativa)

### Defecto 01 — Falta de validacion de edad negativa *(Prueba unitaria)*

- **Capa afectada:** Dominio (`Registry.registerVoter`)
- **Caso de prueba:** Registro de persona con edad `-1`.
- **Entrada:** `Person(name="Juan", id=101, age=-1, gender=MALE, alive=true)`
- **Resultado esperado:** `INVALID`
- **Resultado obtenido inicial:** `VALID` (validacion de edad se aplicaba como `< 18`, no como `< 0`)
- **Causa probable:** La logica de negocio evaluaba `age < 18` pero no verificaba que la edad no fuera negativa. Sin embargo, el caso `age < 18` captura correctamente edades negativas, por lo que el resultado actual es `UNDERAGE`.
- **Tipo de prueba:** Unitaria (dominio puro)
- **Estado:** Cerrado — La validacion `age < 18` cubre tambien edades negativas, retornando `UNDERAGE`. Adicionalmente, se agrego `@Min(0)` en `PersonDTO` para validar en la capa de presentacion.
- **Prioridad:** Baja

---

### Defecto 02 — Registro de persona fallecida *(Prueba unitaria)*

- **Capa afectada:** Dominio (`Registry.registerVoter`)
- **Caso de prueba:** Persona con `alive=false`.
- **Entrada:** `Person(name="Ana", id=102, age=45, gender=FEMALE, alive=false)`
- **Resultado esperado:** `DEAD`
- **Resultado obtenido:** `DEAD` (la validacion `!p.isAlive()` existe y funciona correctamente)
- **Causa probable:** Inicialmente se reporto como `VALID` por un error simulado en la implementacion inicial del taller. La version actual del codigo maneja correctamente este caso.
- **Tipo de prueba:** Unitaria (regla de negocio)
- **Estado:** Cerrado — Validado con los tests `shouldReturnDeadWhenPersonIsNotAlive` (H2) y `shouldReturn422WhenPersonIsDead` (HTTP). El sistema retorna 422 Unprocessable Entity.
- **Prioridad:** Media

---

### Defecto 03 — No se detectan duplicados *(Prueba de integracion con H2)*

- **Capa afectada:** Infraestructura (`RegistryRepository`)
- **Caso de prueba:** Dos registros con el mismo `id`.
- **Entradas:**
  - Persona 1 → `Person(name="Carlos", id=200, age=30, gender=MALE, alive=true)`
  - Persona 2 → `Person(name="Carla", id=200, age=25, gender=FEMALE, alive=true)`
- **Resultado esperado:**
  - Persona 1 → `VALID`
  - Persona 2 → `DUPLICATED`
- **Resultado obtenido:** Correcto — el codigo actual verifica `repo.existsById(id)` antes de guardar.
- **Causa probable:** Error simulado en la version inicial del taller. La implementacion actual maneja correctamente la deteccion de duplicados.
- **Tipo de prueba:** Integracion (H2 + capa de aplicacion)
- **Estado:** Cerrado — Validado con `shouldPersistValidVoterAndRejectDuplicates` (H2), `shouldReturnDuplicatedWhenRepoSaysExists` (Mock) y `shouldReturn409WhenDuplicatedPerson` (HTTP).
- **Prioridad:** Alta

---

### Defecto 04 — Fallo en simulacion con mock *(Prueba de integracion con Mockito)*

- **Capa afectada:** Aplicacion (`RegistryWithMockTest`)
- **Caso de prueba:** Registro con `id` duplicado en un repositorio simulado.
- **Configuracion:** `when(repo.existsById(7)).thenReturn(true);`
- **Resultado esperado:** `DUPLICATED`
- **Resultado obtenido:** `DUPLICATED` (la configuracion del mock es correcta y el test pasa)
- **Causa probable:** El `NullPointerException` inicial se debia a no inicializar correctamente el repositorio mock en el `@Before`. En la version actual, `repo = mock(RegistryRepositoryPort.class)` se ejecuta correctamente.
- **Tipo de prueba:** Integracion (mock)
- **Estado:** Cerrado — Verificado con 3 tests mock: `shouldReturnDuplicatedWhenRepoSaysExists`, `shouldCallSaveWhenPersonDoesNotExist` y `shouldThrowIllegalStateExceptionWhenSaveFails`.
- **Prioridad:** Media

---

### Defecto 05 — Error HTTP 500 no manejado *(Prueba de sistema REST)*

- **Capa afectada:** Delivery (`RegistryController`)
- **Caso de prueba:** Envio de JSON con campo `gender` invalido.
- **Entrada:** `{ "name": "Laura", "id": 500, "age": 20, "gender": "OTHER", "alive": true }`
- **Resultado esperado:** `HTTP 400` (Bad Request)
- **Resultado obtenido inicial:** `HTTP 500` (Internal Server Error) — `IllegalArgumentException` de `Gender.valueOf("OTHER")` no era manejada.
- **Resultado actual:** `HTTP 400` (Bad Request) con body `INVALID_GENDER`
- **Causa:** Se agrego manejo de `IllegalArgumentException` en el controlador usando try-catch, retornando `ResponseEntity.status(BAD_REQUEST).body("INVALID_GENDER")`.
- **Tipo de prueba:** Sistema (HTTP)
- **Estado:** Cerrado — Validado con `shouldReturn400WhenGenderIsInvalid` en `RegistryControllerIT`.
- **Prioridad:** Alta

---

### Defecto 06 — Validacion de campos obligatorios ausente *(Prueba de sistema REST — nuevo)*

- **Capa afectada:** Delivery (`PersonDTO` + `RegistryController`)
- **Caso de prueba:** Envio de JSON incompleto (`{"name":"Ana"}`) sin `id`, `age`, `gender`.
- **Resultado esperado:** `HTTP 400` (Bad Request) con mensaje de validacion.
- **Resultado obtenido inicial:** Comportamiento no definido (Spring asignaba valores por defecto: id=0, age=0).
- **Resultado actual:** `HTTP 400` con mensajes de validacion descriptivos.
- **Causa:** Se agregaron anotaciones `@Valid`, `@NotBlank` y `@Min` en `PersonDTO`, y se agrego un `@ExceptionHandler` para `MethodArgumentNotValidException`.
- **Estado:** Cerrado — Validado con `shouldReturn400WhenJsonMissingFields` en `RegistryControllerIT`.
- **Prioridad:** Alta

---

## Formato 2: Tabla de defectos (bug tracking)

| ID | Caso de Prueba | Capa | Resultado Esperado | Resultado Obtenido | Tipo | Estado | Prioridad |
|----|----------------|------|--------------------|--------------------|------|--------|-----------|
| 01 | Edad negativa | Dominio | `INVALID` / `UNDERAGE` | `UNDERAGE` | Unitaria | Cerrado | Baja |
| 02 | Persona muerta | Dominio | `DEAD` | `DEAD` | Unitaria | Cerrado | Media |
| 03 | Duplicado por ID | Infraestructura | `DUPLICATED` | `DUPLICATED` | Integracion H2 | Cerrado | Alta |
| 04 | Mock mal configurado | Aplicacion | `DUPLICATED` | `DUPLICATED` | Integracion (mock) | Cerrado | Media |
| 05 | Error HTTP 500 → 400 | Delivery | `HTTP 400` | `HTTP 400` | Sistema (REST) | Cerrado | Alta |
| 06 | Validacion campos obligatorios | Delivery | `HTTP 400 + mensaje` | `HTTP 400 + mensaje` | Sistema (REST) | Cerrado | Alta |

---

## Convenciones de Estado

| Estado | Significado |
|--------|-------------|
| **Abierto** | El defecto fue detectado pero no corregido. |
| **En progreso** | El defecto se encuentra en analisis o correccion. |
| **Cerrado** | El defecto fue corregido y validado mediante pruebas. |

---

## Observaciones

- Todos los defectos detectados durante la fase de pruebas fueron corregidos y validados con nuevos casos de prueba.
- La inclusion de `@Valid` con anotaciones de Bean Validation previno futuros errores de datos invalidos al llegar a la capa de dominio.
- El manejo de excepciones con `@ExceptionHandler` en el controlador mejoro significativamente la experiencia de la API, evitando respuestas HTTP 500 genericas.
- La combinacion de pruebas H2 + Mockito + HTTP permitio detectar y corregir defectos en todas las capas de la arquitectura.
