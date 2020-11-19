package com.example.demo.Services;

import com.example.demo.Person;
import com.example.demo.Repositories.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
public class PersonService {
    @Autowired
    private PersonRepository repository;

    public Flux<Person> listAll() {
        return repository.findAll();
    }

    /*PRIMERA VERSION
    public Mono<Void> insert(Mono<Person> personMono) {
        return personMono
                .doOnNext(person -> repository.save(person))
                .then();
    }*/

    //SEGUNDA VERSION
    public Mono<Void> insert(Mono<Person> personMono) {
        return personMono
                .filter(validateBeforeInsert.apply(repository))
                .doOnNext(person -> repository.save(person))
                .then();
    }

    private final Function<PersonRepository, Predicate<Person>> validateBeforeInsert = repo -> {
        return person -> {
            var data = repo.findByName(person.getName()).block();
            return Objects.isNull(data);
        };
    };

}
