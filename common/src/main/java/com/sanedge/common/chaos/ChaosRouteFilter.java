package com.sanedge.common.chaos;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class ChaosRouteFilter {

    @Inject
    ChaosManager chaosManager;

    public void init(@Observes Router router) {
        router.route().order(-100).handler(new ChaosHttpMiddleware(chaosManager));
    }
}
