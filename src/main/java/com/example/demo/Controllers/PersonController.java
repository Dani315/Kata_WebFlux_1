package com.example.demo.Controllers;

import com.example.demo.Person;
import com.example.demo.Services.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping(value = "/person")
public class PersonController {
    @Autowired
    private PersonService personService;

    /*
    PRIMERA VERSION
    @PostMapping
    public Mono<Void> post(@RequestBody Mono<Person> personMono){
        return Mono.empty();
    }*/

    //EL DEBER SER
    @PostMapping
    public Mono<Void> post(@RequestBody Mono<Person> personMono) {
        return personService.insert(personMono);
    }

    @GetMapping("/{id}")
    public Mono<Person> getPerson(@PathVariable("id") String id){
        return Mono.just(new Person());
    }

    @PutMapping
    public Mono<Void> update(@RequestBody Mono<Person> personMono){
        return Mono.empty();
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable("id") String id){
        return Mono.empty();
    }

    /*
    PRIMERA VERSION
    @GetMapping
    public Flux<Person> list(){
        var persons = List.of(
                new Person("Raul Alzate"),
                new Person("Pedro")
        );
        return  Flux.fromStream(persons.stream());
    }*/

    //EL DEBER SER
    @GetMapping
    public Flux<Person> list() {
        return personService.listAll();
    }
}
