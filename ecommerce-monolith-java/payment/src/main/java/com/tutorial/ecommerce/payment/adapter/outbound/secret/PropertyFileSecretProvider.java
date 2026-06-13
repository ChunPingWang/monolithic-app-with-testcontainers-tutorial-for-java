package com.tutorial.ecommerce.payment.adapter.outbound.secret;

import com.tutorial.ecommerce.sharedkernel.port.SecretProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** 替代 Adapter — Vault 不可用時的降級;從 Spring Environment 讀取 ecommerce.secrets.{path}.{key}。 */
@Component
@ConditionalOnProperty(name = "ecommerce.adapter.secret", havingValue = "property-file")
public class PropertyFileSecretProvider implements SecretProvider {

    private final Environment env;

    public PropertyFileSecretProvider(Environment env) {
        this.env = env;
    }

    @Override
    public Optional<String> getString(String path, String key) {
        return Optional.ofNullable(env.getProperty("ecommerce.secrets." + path + "." + key));
    }
}
