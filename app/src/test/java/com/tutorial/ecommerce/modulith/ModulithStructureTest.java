package com.tutorial.ecommerce.modulith;

import com.tutorial.ecommerce.ECommerceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith 自動驗證:
 *   - 模組間依賴方向(禁止循環依賴)
 *   - 模組只能 import 對方的 NamedInterface(此教程 = Integration Event)
 *   - 內部 package 不得被外部直接引用
 *
 * 同時產出 Documenter 文件:模組關係圖、事件流程圖、C4 圖。
 */
class ModulithStructureTest {

    private final ApplicationModules modules = ApplicationModules.of(ECommerceApplication.class);

    @Test
    void verifyModuleBoundaries() {
        modules.verify();
    }

    @Test
    void printModuleStructure() {
        modules.forEach(System.out::println);
    }

    @Test
    void generateDocumentation() {
        new Documenter(modules)
            .writeDocumentation()
            .writeIndividualModulesAsPlantUml()
            .writeModulesAsPlantUml();
    }
}
