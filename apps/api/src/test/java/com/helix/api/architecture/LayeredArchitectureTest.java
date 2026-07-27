package com.helix.api.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class LayeredArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.helix.api");

    @Test
    void domainMustNotDependOnSpring() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
            .because("Domain objects should remain framework-independent")
            .check(classes);
    }

    @Test
    void inboundHttpAdaptersMustNotDependOnPersistence() {
        noClasses()
            .that().resideInAnyPackage("..adapter.in.http..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter.out.persistence..")
            .because("Web adapters should call use-case services, not persistence directly")
            .check(classes);
    }
}
