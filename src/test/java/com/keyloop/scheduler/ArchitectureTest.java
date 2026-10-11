package com.keyloop.scheduler;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.keyloop.scheduler", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule baseModulesDoNotDependOnAvailabilityOrBooking =
            noClasses().that().resideInAnyPackage("..catalog..", "..customer..", "..common..")
                    .should().dependOnClassesThat().resideInAnyPackage("..availability..", "..booking..");

    @ArchTest
    static final ArchRule availabilityDoesNotDependOnBooking =
            noClasses().that().resideInAPackage("..availability..")
                    .should().dependOnClassesThat().resideInAPackage("..booking..");

    @ArchTest
    static final ArchRule controllersDoNotUseRepositoriesOrQueries =
            noClasses().that().areAnnotatedWith(RestController.class)
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                    .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Queries");

    @ArchTest
    static final ArchRule pureClassesHaveNoSpringOrJpa =
            noClasses().that().haveSimpleName("SchedulingRules")
                    .or().haveSimpleName("AvailabilityCalculator")
                    .or().haveSimpleName("Slot")
                    .or().haveSimpleName("BusyInterval")
                    .or().haveSimpleName("DailyHours")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule noLocalDateTimeNowWithoutArguments =
            noClasses().should().callMethod(LocalDateTime.class, "now");

    @ArchTest
    static final ArchRule noLocalDateNowWithoutArguments =
            noClasses().should().callMethod(LocalDate.class, "now");

    @ArchTest
    static final ArchRule noCyclesBetweenModules =
            slices().matching("com.keyloop.scheduler.(*)..").should().beFreeOfCycles();
}
