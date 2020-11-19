package com.example.demo.Controllers;

import com.example.demo.Person;
import com.example.demo.Repositories.PersonRepository;
import com.example.demo.Services.PersonService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = PersonController.class)

public class PersonControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @SpyBean
    private PersonService personService;

    @MockBean
    private PersonRepository repository;

    @Captor
    private ArgumentCaptor<Mono<Person>> argumentCaptor;

    /*
    PRIMERA VERSION
    @Test
    void post(){
        var request = Mono.just(new Person());
        webTestClient.post()
                .uri("/person")
                .body(request, Person.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
    }*/

    /*
    SEGUNDA VERSION
    @Test
    void post() {
        var request = Mono.just(new Person("Raul Alzate"));
        webTestClient.post()
                .uri("/person")
                .body(request, Person.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(personService).insert(argumentCaptor.capture());
        verify(repository).save(any());

        var person = argumentCaptor.getValue().block();

        Assertions.assertEquals("Raul Alzate", person.getName());
    }*/

    //TERCERA VERSION
    @ParameterizedTest
    @CsvSource({"Raul Alzate,0"})
    void post(String name, Integer times) {
        when(repository.findByName(name)).thenReturn(
                Mono.just(new Person())
        );

        var request = Mono.just(new Person(name));
        webTestClient.post()
                .uri("/person")
                .body(request, Person.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        verify(personService).insert(argumentCaptor.capture());
        verify(repository, times(times)).save(any());

        var person = argumentCaptor.getValue().block();

        Assertions.assertEquals(name, person.getName());

    }

    @Test
    void get(){
        webTestClient.get()
                .uri("/person/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Person.class)
                .consumeWith(personEntityExchangeResult -> {
                    var person = personEntityExchangeResult.getResponseBody();
                    assert person != null;
                });
    }

    @Test
    void update(){
        var request = Mono.just(new Person());
        webTestClient.put()
                .uri("/person")
                .body(request, Person.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
    }

    @Test
    void delete(){
        webTestClient.delete()
                .uri("/person/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
    }

    /*
    PRIMERA VERSION
    @Test
    void list(){
        webTestClient.get()
                .uri("/person")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].name").isEqualTo("Raul Alzate")
                .jsonPath("$[1].name").isEqualTo("Pedro");
    }*/

    //SEGUNDA VERSION
    @Test
    void list() {
        var list = Flux.just(
                new Person("Raul Alzate"),
                new Person("Pedro" )
        );

        when(repository.findAll()).thenReturn(list);

        webTestClient.get()
                .uri("/person")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].name").isEqualTo("Raul Alzate")
                .jsonPath("$[1].name").isEqualTo("Pedro");

        verify(personService).listAll();
        verify(repository).findAll();
    }
}