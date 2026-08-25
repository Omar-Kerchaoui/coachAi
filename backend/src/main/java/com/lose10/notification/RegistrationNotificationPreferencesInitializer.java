package com.lose10.notification;

import com.lose10.auth.event.UserRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RegistrationNotificationPreferencesInitializer {

    private final NotificationPreferencesRepository repository;

    public RegistrationNotificationPreferencesInitializer(NotificationPreferencesRepository repository) {
        this.repository = repository;
    }

    @EventListener
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        repository.save(new NotificationPreferences(event.userId()));
    }
}
