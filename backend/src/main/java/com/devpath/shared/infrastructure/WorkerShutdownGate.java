package com.devpath.shared.infrastructure;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class WorkerShutdownGate {
    private final AtomicBoolean acceptingClaims = new AtomicBoolean(true);

    public boolean acceptingClaims() {
        return acceptingClaims.get();
    }

    @EventListener
    public void stopAcceptingClaims(ContextClosedEvent ignored) {
        acceptingClaims.set(false);
    }
}
