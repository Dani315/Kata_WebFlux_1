# KATA - SPRING WEB FLUX - CRUD #

## Purpose ##

Vamos a realizar un CRUD usando Web Flux paso a pasa, usando Spring Boot como runner para exponer un recurso, ademas vamos aplicar pruebas de componentes para verificar el recurso.

El resultado final será un Servicio Rest verificado por pruebas de componentes y totalmente reactivo.

## Issues List ##

### 1) Prueba de controlador - Primera capa ###

En esta actividad vamos a realizar nuestro primer Rest Controller reactivo, crear un objeto llamado **Person** con dos atributos cualquiera.

  


Vamos a crear un controlador donde nos permita exponer un recurso llamado **Person**. Copiar el siguiente trozo de codigo en el repositorio que se clono de la kata.

  


~~~~~~~~~~
@RestController
@RequestMapping(value = "/person")
public class PersonController {

    @PostMapping
    public Mono<Void> post(@RequestBody Mono<Person> personMono){
        return Mono.empty();
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

    @GetMapping
    public Flux<Person> list(){
        var persons = List.of(
                new Person("Raul Alzate"),
                new Person("Pedro")
        );
        return  Flux.fromStream(persons.stream());
    }
}
~~~~~~~~~~

  


### 2) Prueba unitaria del controlador ###

Realizar su respectiva prueba automatizada donde se verifique los comportamiento del controlador y validando así su funcionamiento.

  


Crear la prueba para la clase PersonController

~~~~~~~~~~
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = PersonController.class)
public class PersonControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void post(){
        var request = Mono.just(new Person());
        webTestClient.post()
                .uri("/person")
                .body(request, Person.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();
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

    @Test
    void list(){
        webTestClient.get()
                .uri("/person")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].name").isEqualTo("Raul Alzate")
                .jsonPath("$[1].name").isEqualTo("Pedro");
    }

}
~~~~~~~~~~

En cada uno de los métodos se aplicar la prueba desde el controlador.

### 3) Crear capa de datos y de servicio ###

En esta actividad vamos a crear la capa de servicio para que traiga datos desde un repositorio, el servicio en este caso debería tener la siguiente estructura:

~~~~~~~~~~
@Service
public class PersonService {
    @Autowired
    private PersonRepository repository;

    public Flux<Person> listAll() {
        return repository.findAll();
    }
}
~~~~~~~~~~

  


Ademas se debe crear el Repositorio que permita realizar la consultas a la base de datos:

~~~~~~~~~~
@Repository
public interface PersonRepository extends ReactiveMongoRepository<Person, String> {
}
~~~~~~~~~~

***Notar que se utiliza ReactiveMongoRepository (es el repositorio de mongo de forma reactiva, retorna valores Flux y Mono).***

### 4) Pruebas unitarias la capa de servicio junto con la del controlador ###

Ahora vamos a realizar la prueba del servicio que se creo, vamos a realizar las siguientes actualizaciones de la clase test que tenemos creada.

1. Vamos a injectar un spy del servicio para determinar que valores estan ingresando al servicio tal

~~~~~~~~~~
@SpyBean
 private PersonService personService;
~~~~~~~~~~

2. Vamos a mokear el repositorio para que sea repetible la prueba sin depender de la conexión a la base de datos

~~~~~~~~~~
@MockBean
 private PersonRepository repository;
~~~~~~~~~~

3. Ahora debemos tener un Captor para poder revisar los valores del Spy

~~~~~~~~~~
@Captor
 private ArgumentCaptor<Person> argumentCaptor;
~~~~~~~~~~

  


Ya teniendo los objetos preparados vamos a realizar la prueba del servicio:

  


~~~~~~~~~~
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
        //TODO: aplicar el captor aqui
    }
~~~~~~~~~~

  


### 5) Crear un método para registrar persona con pruebas ###

Ya tenemos el repositorio y el servicio, ahora lo que tenemos que aplicar es el mecanismo para poder registrar una persona, para ello debemos actualizar el Servicio con el siguiente método:

~~~~~~~~~~
public Mono<Void> insert(Mono<Person> personMono) {
        return personMono
                .doOnNext(person -> repository.save(person))
                .then();
    }
~~~~~~~~~~

Recordar que es necesario actualizar el controlador.

  


La siguiente pieza de codigo realiza la verificación de la prueba de este nuevo metodo del servicio.

  


~~~~~~~~~~
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

    }
~~~~~~~~~~

  


### 6) Verificar el guardado y probamos con parametrización ###

Usando la programación funcional vamos a aplicar la siguiente función:

~~~~~~~~~~
private final Function<PersonRepository, Predicate<Person>> validateBeforeInsert = repo -> person -> {
        var data = repo.findByName(person.getName()).block();
        return Objects.isNull(data);
    };
~~~~~~~~~~

para poder validar previamente el guardado del objeto.

~~~~~~~~~~
public Mono<Void> insert(Mono<Person> personMono) {
        return personMono
                .filter(validateBeforeInsert.apply(repository))
                .doOnNext(person -> repository.save(person))
                .then();
    }
~~~~~~~~~~

## Verificamos con una prueba el comportamiento del guardado: ##

~~~~~~~~~~
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
~~~~~~~~~~

  


En este caso vamos a usar las parametrizaciones del las pruebas unitarias para realizar las verificaciones correctas.