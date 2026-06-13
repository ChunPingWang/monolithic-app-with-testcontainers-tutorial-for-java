package com.tutorial.ecommerce.sharedkernel.port;

import java.util.Optional;

/**
 * 密鑰來源 Outbound Port — 多模組共用(支付 API key、DB 動態憑證)。
 * Real adapter: Vault。Fake: PropertyFile。替代:AwsSm。
 */
public interface SecretProvider {

    Optional<String> getString(String path, String key);

    default String requireString(String path, String key) {
        return getString(path, key).orElseThrow(() ->
            new IllegalStateException("secret not found: " + path + "#" + key));
    }
}
