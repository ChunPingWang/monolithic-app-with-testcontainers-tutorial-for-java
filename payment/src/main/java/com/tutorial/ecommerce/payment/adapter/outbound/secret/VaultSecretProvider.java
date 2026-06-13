package com.tutorial.ecommerce.payment.adapter.outbound.secret;

import com.tutorial.ecommerce.sharedkernel.port.SecretProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "ecommerce.adapter.secret", havingValue = "vault", matchIfMissing = true)
public class VaultSecretProvider implements SecretProvider {

    private final VaultTemplate vault;

    public VaultSecretProvider(VaultTemplate vault) {
        this.vault = vault;
    }

    @Override
    public Optional<String> getString(String path, String key) {
        VaultResponse resp = vault.read(path);
        if (resp == null || resp.getData() == null) return Optional.empty();
        Map<String, Object> data = resp.getData();
        // KV v2 把 payload 包在 "data" 鍵下
        if (data.containsKey("data") && data.get("data") instanceof Map<?, ?> nested) {
            data = castOrEmpty(nested);
        }
        Object value = data.get(key);
        return Optional.ofNullable(value).map(Object::toString);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castOrEmpty(Map<?, ?> nested) {
        return (Map<String, Object>) nested;
    }
}
