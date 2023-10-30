package com.sublinks.sublinksapi.person.event;

import com.sublinks.sublinksapi.person.Person;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class PersonDeletedPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public PersonDeletedPublisher(final ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(Person person) {

        PersonDeletedEvent personDeletedEvent = new PersonDeletedEvent(this, person);
        applicationEventPublisher.publishEvent(personDeletedEvent);
    }
}
