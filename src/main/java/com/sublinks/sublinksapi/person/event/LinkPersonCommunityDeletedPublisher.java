package com.sublinks.sublinksapi.person.event;

import com.sublinks.sublinksapi.person.LinkPersonCommunity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class LinkPersonCommunityDeletedPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public LinkPersonCommunityDeletedPublisher(
            final ApplicationEventPublisher applicationEventPublisher
    ) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(final LinkPersonCommunity linkPersonCommunity) {

        LinkPersonCommunityDeletedEvent linkPersonCommunityDeletedEvent = new LinkPersonCommunityDeletedEvent(this, linkPersonCommunity);
        applicationEventPublisher.publishEvent(linkPersonCommunityDeletedEvent);
    }
}
