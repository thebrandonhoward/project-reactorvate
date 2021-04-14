package com.thebrandonhoward.demo.projectreactorvate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
@EnableReactiveMongoRepositories
public class ProjectReactorvateApplication {

    public static void main(String... args) {
        SpringApplication.run(ProjectReactorvateApplication.class, args);
    }

}

 @Document
 @Data
 @AllArgsConstructor
 @NoArgsConstructor
 class Person {
    @MongoId()
    private String id;
    private String name;
}

interface ReactivePersonRepository extends ReactiveCrudRepository<Person,String> {
    public Flux<Person> findPersonByName(String name);
}

@Component
@RequiredArgsConstructor
@Log4j2
class SampleDataInitializer {

    private final ReactivePersonRepository reactivePersonRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void ready() {

        Flux<Person> personFlux
            = Flux.just("GOD", "JESUS", "Charles", "Virgina", "Fred", "Marilyn", "Brandon", "Daisia", "Pamela", "Briden")
                  .map(name -> new Person(null, name))
                  .flatMap(person -> this.reactivePersonRepository.save(person));

        this.reactivePersonRepository
                .deleteAll()
                .thenMany(personFlux)
                .thenMany(this.reactivePersonRepository.findAll())
                .subscribe(log::info);
    }

    // ---------------------- WEBSERVICE -----------------------

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class GreetingRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class GreetingResponse {
        private String message;
    }

    @Service
    class GreetingService {
        public Mono<GreetingResponse> perform(GreetingRequest greetingRequest) {
            return Mono.just(perform(greetingRequest.getName()));
        }

        public GreetingResponse perform(String name) {
            return new GreetingResponse("Hello " + name);
        }
    }

//    @RestController
//    @RequiredArgsConstructor
//    class GreetingRestController {
//        private final GreetingService greetingService;
//
//        @GetMapping("/{name}")
//        public Mono<GreetingResponse> get(@PathVariable("name") String name) {
//            return greetingService.perform(new GreetingRequest(name));
//        }
//    }

    @Bean
    RouterFunction<ServerResponse> routes(GreetingService greetingService) {
        return route()
                .GET("/{name}", serverRequest ->
                    ServerResponse.ok().body(greetingService.perform(new GreetingRequest(serverRequest.pathVariable("name"))), GreetingResponse.class))
                .build();
    }
}