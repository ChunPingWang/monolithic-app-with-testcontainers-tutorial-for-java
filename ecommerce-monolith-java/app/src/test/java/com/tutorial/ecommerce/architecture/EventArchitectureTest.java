package com.tutorial.ecommerce.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
    packages = "com.tutorial.ecommerce",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class EventArchitectureTest {

    /** Integration Event 必須是 record(不可變)且實作 DomainEvent。 */
    @ArchTest
    static final ArchRule integrationEventsAreRecords =
        classes()
            .that().resideInAPackage("..sharedkernel.event..")
            .and().areTopLevelClasses()
            .and().areNotInterfaces()
            .should().beRecords();

    /** 業務模組不得直接 import 其他模組的 application / adapter 套件。 */
    @ArchTest
    static final ArchRule moduleApplicationLayersAreIsolated =
        noClasses()
            .that().resideInAPackage("..product..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..payment..application..",
                "..payment..adapter..",
                "..inventory..application..",
                "..inventory..adapter..");

    @ArchTest
    static final ArchRule paymentApplicationLayerIsolated =
        noClasses()
            .that().resideInAPackage("..payment..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..product..application..",
                "..product..adapter..",
                "..inventory..application..",
                "..inventory..adapter..");

    @ArchTest
    static final ArchRule inventoryApplicationLayerIsolated =
        noClasses()
            .that().resideInAPackage("..inventory..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..product..application..",
                "..product..adapter..",
                "..payment..application..",
                "..payment..adapter..");

    /** Adapter 不得宣告 @Transactional(交易邊界屬於 application 層)。 */
    @ArchTest
    static final ArchRule transactionalNotInAdapter =
        noClasses()
            .that().resideInAPackage("..adapter..")
            .should().beAnnotatedWith(org.springframework.transaction.annotation.Transactional.class);
}
