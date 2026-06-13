package com.tutorial.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/**
 * 單體應用啟動類。
 *
 * @Modulithic 把根 package 標為 Spring Modulith 的應用範圍 — 它會掃描
 * com.tutorial.ecommerce.* 下標 @ApplicationModule 的 package 作為模組。
 *
 * shared-kernel 不是模組(沒標 @ApplicationModule),所有模組都可依賴它。
 * infrastructure 同樣是平台層,不算業務模組。
 */
@SpringBootApplication
@Modulithic(systemName = "ecommerce-monolith")
public class ECommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ECommerceApplication.class, args);
    }
}
