package com.devpath.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureBoundaryTest {
    private final JavaClasses classes = new ClassFileImporter()
        .withImportOption(new ImportOption.DoNotIncludeTests())
        .importPackages("com.devpath");

    @Test
    void domainPackagesDoNotDependOnSpringOrProviderSdkPackages() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "javax.persistence..",
                "com.openai..",
                "dev.langchain4j..",
                "software.amazon.awssdk..",
                "com.github.."
            )
            .check(classes);
    }

    @Test
    void domainPackagesDoNotDependOnAdapters() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..")
            .check(classes);
    }

    @Test
    void inboundAdaptersDoNotDependOnOutboundAdapters() {
        noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
            .check(classes);
    }

    @Test
    void moduleAdaptersDoNotImportOtherModuleAdapters() {
        noClasses()
            .that().resideInAPackage("com.devpath.identity.adapter..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "com.devpath.repository.adapter..",
                "com.devpath.analysis.adapter..",
                "com.devpath.rule.adapter..",
                "com.devpath.career.adapter..",
                "com.devpath.knowledge.adapter..",
                "com.devpath.ai.adapter..",
                "com.devpath.portfolio.adapter..",
                "com.devpath.learning.adapter.."
            )
            .because("module adapters must communicate through application boundaries, not each other's internals")
            .check(classes);
    }

    @Test
    void webAdaptersDoNotDependOnJpa() {
        noClasses()
            .that().resideInAPackage("..adapter.in.web..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework.data.jpa..",
                "jakarta.persistence..",
                "..adapter.out.persistence.."
            )
            .check(classes);
    }
}
