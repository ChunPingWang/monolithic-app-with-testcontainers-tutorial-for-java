package com.tutorial.ecommerce.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "com.tutorial.ecommerce",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class LayerArchitectureTest {

    /** 每模組的 Hexagonal 分層 — domain ← application ← adapter,反向禁止。 */
    @ArchTest
    static final ArchRule hexagonalLayersWithinEachModule =
        Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .withOptionalLayers(true)
            .layer("Domain").definedBy("..(product|payment|inventory)..domain..")
            .layer("Application").definedBy("..(product|payment|inventory)..application..")
            .layer("Adapter").definedBy("..(product|payment|inventory)..adapter..")
            .whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter");

    /** Domain 層禁用 Spring / JPA / Jakarta — 純 POJO,可獨立測試。 */
    @ArchTest
    static final ArchRule domainIsFrameworkAgnostic =
        noClasses()
            .that().resideInAPackage("..(product|payment|inventory)..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "jakarta.transaction..",
                "org.hibernate.."
            )
            .as("Domain 層不得依賴 Spring / JPA — 影響到的話請把實作搬到 adapter 層");

    /** Adapter 不得互相依賴 — 通訊要走 Port + Integration Event。 */
    @ArchTest
    static final ArchRule adapterDoesNotDependOnOtherAdapter =
        noClasses()
            .that().resideInAPackage("..product..adapter..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..payment..adapter..",
                "..inventory..adapter.."
            );

    @ArchTest
    static final ArchRule paymentAdapterIsolation =
        noClasses()
            .that().resideInAPackage("..payment..adapter..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..product..adapter..",
                "..inventory..adapter.."
            );

    @ArchTest
    static final ArchRule inventoryAdapterIsolation =
        noClasses()
            .that().resideInAPackage("..inventory..adapter..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..product..adapter..",
                "..payment..adapter.."
            );

    /** Shared Kernel 不得依賴任何業務模組(避免迴圈)。 */
    @ArchTest
    static final ArchRule sharedKernelHasNoBusinessModuleDependency =
        noClasses()
            .that().resideInAPackage("..sharedkernel..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..product..",
                "..payment..",
                "..inventory.."
            );

    /** 業務模組不得 import 其他業務模組的 domain.model(只能透過 Integration Event 通訊)。 */
    @ArchTest
    static final ArchRule productDoesNotTouchOtherModulesDomain =
        noClasses()
            .that().resideInAPackage("..product..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..payment..domain..",
                "..inventory..domain.."
            );

    @ArchTest
    static final ArchRule paymentDoesNotTouchOtherModulesDomain =
        noClasses()
            .that().resideInAPackage("..payment..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..product..domain..",
                "..inventory..domain.."
            );

    @ArchTest
    static final ArchRule inventoryDoesNotTouchOtherModulesDomain =
        noClasses()
            .that().resideInAPackage("..inventory..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..product..domain..",
                "..payment..domain.."
            );

    /** 命名約定:Outbound Port 頂層型別必須是介面(內嵌 DTO records 不受限制)。 */
    @ArchTest
    static final ArchRule outboundPortsAreInterfaces =
        classes()
            .that().resideInAPackage("..domain..port.outbound..")
            .and().areTopLevelClasses()
            .should().beInterfaces();

    @ArchTest
    static final ArchRule inboundPortsAreInterfaces =
        classes()
            .that().resideInAPackage("..domain..port.inbound..")
            .and().areTopLevelClasses()
            .should().beInterfaces();
}
