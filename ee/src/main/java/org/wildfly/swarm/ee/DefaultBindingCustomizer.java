package org.wildfly.swarm.ee;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.wildfly.swarm.config.MessagingActiveMQ;
import org.wildfly.swarm.config.ee.DefaultBindingsService;
import org.wildfly.swarm.spi.api.Customizer;
import org.wildfly.swarm.spi.api.Pre;

/**
 * @author Bob McWhirter
 */
@Pre
@ApplicationScoped
public class DefaultBindingCustomizer implements Customizer {

    @Inject
    @Any
    private Instance<MessagingActiveMQ> messaging;

    @Inject @Any
    private EEFraction fraction;

    @Override
    public void customize() {
        if (!this.messaging.isUnsatisfied()) {
            if (this.fraction.subresources().defaultBindingsService() == null) {
                this.fraction.defaultBindingsService(new DefaultBindingsService());
            }
            if (this.fraction.subresources().defaultBindingsService().jmsConnectionFactory() == null) {
                this.fraction.subresources().defaultBindingsService()
                        .jmsConnectionFactory("java:jboss/DefaultJMSConnectionFactory");
            }
        }
    }
}