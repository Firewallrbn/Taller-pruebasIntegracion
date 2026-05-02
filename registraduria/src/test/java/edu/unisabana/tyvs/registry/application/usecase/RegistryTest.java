package edu.unisabana.tyvs.registry.application.usecase;

import edu.unisabana.tyvs.registry.application.port.out.RegistryRepositoryPort;
import edu.unisabana.tyvs.registry.domain.model.Gender;
import edu.unisabana.tyvs.registry.domain.model.Person;
import edu.unisabana.tyvs.registry.domain.model.RegisterResult;
import edu.unisabana.tyvs.registry.infrastructure.persistence.RegistryRecord;
import edu.unisabana.tyvs.registry.infrastructure.persistence.RegistryRepository;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Pruebas de integración para el caso de uso {@link Registry}, aplicando el formato AAA:
 * <ul>
 *   <li><b>Arrange</b>: preparación de datos y objetos a probar.</li>
 *   <li><b>Act</b>: ejecución del método bajo prueba.</li>
 *   <li><b>Assert</b>: verificación de los resultados esperados.</li>
 * </ul>
 */
public class RegistryTest {

    private RegistryRepositoryPort repo;
    private Registry registry;

    /**
     * Arrange común a todos los tests:
     * <ul>
     *   <li>Instancia un repositorio H2 en memoria.</li>
     *   <li>Inicializa el esquema (tabla) y limpia datos previos.</li>
     *   <li>Construye el caso de uso inyectando el repositorio.</li>
     * </ul>
     */
    @Before
    public void setup() throws Exception {
        String jdbc = "jdbc:h2:mem:regdb;DB_CLOSE_DELAY=-1";
        repo = new RegistryRepository(jdbc);

        repo.initSchema();   // Arrange: crear tabla
        repo.deleteAll();    // Arrange: limpiar datos previos

        registry = new Registry(repo); // Arrange: inyectar dependencia
    }

    /**
     * Caso de prueba:
     * <p>Una persona válida debe ser registrada exitosamente.</p>
     */
    @Test
    public void shouldRegisterValidPerson() throws Exception {
        // Arrange
        Person p1 = new Person("Ana", 100, 30, Gender.FEMALE, true);

        // Act
        RegisterResult result = registry.registerVoter(p1);

        // Assert
        assertEquals(RegisterResult.VALID, result);
        assertTrue(repo.existsById(100));
    }

    /**
     * Caso de prueba:
     * <p>Al intentar registrar dos personas con el mismo ID:</p>
     * <ul>
     *   <li>La primera se guarda como válida.</li>
     *   <li>La segunda es rechazada como duplicada.</li>
     * </ul>
     */
    @Test
    public void shouldPersistValidVoterAndRejectDuplicates() throws Exception {
        // Arrange
        Person p1 = new Person("Ana", 100, 30, Gender.FEMALE, true);
        Person p2 = new Person("AnaDos", 100, 40, Gender.FEMALE, true);

        // Act (primer registro)
        RegisterResult result1 = registry.registerVoter(p1);

        // Assert primer registro
        assertEquals(RegisterResult.VALID, result1);
        assertTrue(repo.existsById(100));

        // Act (segundo registro con mismo ID)
        RegisterResult result2 = registry.registerVoter(p2);

        // Assert segundo registro
        assertEquals(RegisterResult.DUPLICATED, result2);
    }

    @Test
    public void shouldReturnUnderageWhenPersonIsLessThan18() throws Exception {
        Person p = new Person("Juan", 200, 16, Gender.MALE, true);

        RegisterResult result = registry.registerVoter(p);

        assertEquals(RegisterResult.UNDERAGE, result);
        assertFalse(repo.existsById(200));
    }

    @Test
    public void shouldReturnDeadWhenPersonIsNotAlive() throws Exception {
        Person p = new Person("Maria", 300, 40, Gender.FEMALE, false);

        RegisterResult result = registry.registerVoter(p);

        assertEquals(RegisterResult.DEAD, result);
        assertFalse(repo.existsById(300));
    }

    @Test
    public void shouldReturnInvalidWhenIdIsZeroOrNegative() throws Exception {
        Person pZero = new Person("Pedro", 0, 30, Gender.MALE, true);
        Person pNeg = new Person("Luis", -1, 30, Gender.MALE, true);

        assertEquals(RegisterResult.INVALID, registry.registerVoter(pZero));
        assertEquals(RegisterResult.INVALID, registry.registerVoter(pNeg));
        assertFalse(repo.existsById(0));
        assertFalse(repo.existsById(-1));
    }

    @Test
    public void shouldReturnInvalidWhenPersonIsNull() throws Exception {
        RegisterResult result = registry.registerVoter(null);

        assertEquals(RegisterResult.INVALID, result);
    }

    @Test
    public void shouldFindSavedPersonById() throws Exception {
        Person p = new Person("Laura", 500, 28, Gender.FEMALE, true);
        registry.registerVoter(p);

        Optional<RegistryRecord> found = repo.findById(500);

        assertTrue(found.isPresent());
        assertEquals(500, found.get().getId());
        assertEquals("Laura", found.get().getName());
        assertEquals(28, found.get().getAge());
        assertTrue(found.get().isAlive());
    }

    @Test
    public void shouldReturnEmptyWhenPersonNotFound() throws Exception {
        Optional<RegistryRecord> found = repo.findById(999);

        assertFalse(found.isPresent());
    }

    @Test
    public void shouldUseFullConstructorForRepository() throws Exception {
        String jdbc = "jdbc:h2:mem:regdb2;DB_CLOSE_DELAY=-1";
        RegistryRepository fullRepo = new RegistryRepository(jdbc, "sa", "");
        fullRepo.initSchema();
        fullRepo.deleteAll();
        Registry reg = new Registry(fullRepo);

        RegisterResult result = reg.registerVoter(new Person("Test", 1, 30, Gender.MALE, true));

        assertEquals(RegisterResult.VALID, result);
        assertTrue(fullRepo.existsById(1));
    }
}
