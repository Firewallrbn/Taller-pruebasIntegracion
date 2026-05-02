package edu.unisabana.tyvs.registry.delivery.rest;

import edu.unisabana.tyvs.registry.application.usecase.Registry;
import edu.unisabana.tyvs.registry.domain.model.Gender;
import edu.unisabana.tyvs.registry.domain.model.Person;
import edu.unisabana.tyvs.registry.domain.model.RegisterResult;
import edu.unisabana.tyvs.registry.domain.model.rq.PersonDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;

@RestController
@RequestMapping("/register")
public class RegistryController {

    private final Registry registry;

    public RegistryController(Registry registry) {
        this.registry = registry;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> register(@Valid @RequestBody PersonDTO dto) {
        Gender gender;
        try {
            gender = Gender.valueOf(dto.getGender());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("INVALID_GENDER");
        }

        Person p = new Person(dto.getName(), dto.getId(), dto.getAge(), gender, dto.isAlive());
        RegisterResult r = registry.registerVoter(p);
        return toResponse(r);
    }

    private ResponseEntity<String> toResponse(RegisterResult r) {
        switch (r) {
            case VALID:
                return ResponseEntity.ok(r.name());
            case DUPLICATED:
                return ResponseEntity.status(HttpStatus.CONFLICT).body(r.name());
            case UNDERAGE:
            case DEAD:
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(r.name());
            case INVALID:
            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(r.name());
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getReason());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("PERSISTENCE_ERROR");
    }
}
