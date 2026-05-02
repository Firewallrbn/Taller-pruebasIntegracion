package edu.unisabana.tyvs.registry.delivery.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import edu.unisabana.tyvs.registry.application.port.out.RegistryRepositoryPort;
import edu.unisabana.tyvs.registry.application.usecase.Registry;
import edu.unisabana.tyvs.registry.infrastructure.persistence.RegistryRepository;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RegistryControllerIT {

    @TestConfiguration
    static class TestBeans {
        @Bean
        public RegistryRepositoryPort registryRepositoryPort() throws Exception {
            String jdbc = "jdbc:h2:mem:regdb;DB_CLOSE_DELAY=-1";
            RegistryRepository repo = new RegistryRepository(jdbc);
            repo.initSchema();
            repo.deleteAll();
            return repo;
        }

        @Bean
        public Registry registry(RegistryRepositoryPort port) {
            return new Registry(port);
        }
    }

    @Autowired
    private TestRestTemplate rest;

    @Test
    public void shouldRegisterValidPerson() {
        String json = "{\"name\":\"Ana\",\"id\":100,\"age\":30,\"gender\":\"FEMALE\",\"alive\":true}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json, headers), String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("VALID", resp.getBody());
    }

    @Test
    public void shouldReturn409WhenDuplicatedPerson() {
        String json1 = "{\"name\":\"Ana\",\"id\":200,\"age\":30,\"gender\":\"FEMALE\",\"alive\":true}";
        String json2 = "{\"name\":\"Pedro\",\"id\":200,\"age\":25,\"gender\":\"MALE\",\"alive\":true}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> resp1 = rest.postForEntity("/register", new HttpEntity<>(json1, headers), String.class);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());
        assertEquals("VALID", resp1.getBody());

        ResponseEntity<String> resp2 = rest.postForEntity("/register", new HttpEntity<>(json2, headers), String.class);
        assertEquals(HttpStatus.CONFLICT, resp2.getStatusCode());
        assertEquals("DUPLICATED", resp2.getBody());
    }

    @Test
    public void shouldReturn422WhenPersonIsUnderage() {
        String json = "{\"name\":\"Juan\",\"id\":300,\"age\":16,\"gender\":\"MALE\",\"alive\":true}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json, headers), String.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals("UNDERAGE", resp.getBody());
    }

    @Test
    public void shouldReturn422WhenPersonIsDead() {
        String json = "{\"name\":\"Maria\",\"id\":400,\"age\":45,\"gender\":\"FEMALE\",\"alive\":false}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json, headers), String.class);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resp.getStatusCode());
        assertEquals("DEAD", resp.getBody());
    }

    @Test
    public void shouldReturn400WhenIdIsInvalid() {
        String json = "{\"name\":\"Luis\",\"id\":0,\"age\":30,\"gender\":\"MALE\",\"alive\":true}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json, headers), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(resp.getBody().contains("id must be greater than 0"));
    }

    @Test
    public void shouldReturn400WhenJsonMissingFields() {
        String json = "{\"name\":\"Ana\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json, headers), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    public void shouldReturn400WhenGenderIsInvalid() {
        String json = "{\"name\":\"Laura\",\"id\":500,\"age\":20,\"gender\":\"OTHER\",\"alive\":true}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.postForEntity("/register", new HttpEntity<>(json, headers), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("INVALID_GENDER", resp.getBody());
    }
}
