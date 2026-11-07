package nl.openvalue.bookstore;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noCodeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;
import java.util.List;

/**
 * Encodes the dependency rules (section 2) and the naming rules (section 3) of the architecture standard, one named
 * rule each. Rule numbers in the comments are the numbers of section 2.
 */
@AnalyzeClasses(packages = "nl.openvalue.bookstore", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // Layer patterns are anchored at the company root, so framework packages such as
    // org.springframework.data.domain never match.
    private static final String DOMAIN = "nl.openvalue..domain..";
    private static final String DOMAININTERACTION = "nl.openvalue..domaininteraction..";
    private static final String COMMAND = "nl.openvalue..command..";
    private static final String QUERY = "nl.openvalue..query..";
    private static final String PROCESS = "nl.openvalue..process..";
    private static final String ASSISTANT = "nl.openvalue..assistant..";
    private static final String DATA = "nl.openvalue..data..";
    private static final String ACL = "nl.openvalue..acl..";

    private static final String DOMAIN_LAYER = "domain";
    private static final String DOMAININTERACTION_LAYER = "domaininteraction";
    private static final String COMMAND_LAYER = "command";
    private static final String QUERY_LAYER = "query";
    private static final String PROCESS_LAYER = "process";
    private static final String ASSISTANT_LAYER = "assistant";
    private static final String DATA_LAYER = "data";
    private static final String ACL_LAYER = "acl";

    // Rule 1, with rule 9 as its only exception: domain may only be accessed by domaininteraction.
    @ArchTest
    public static final ArchRule DOMAIN_IS_ONLY_ACCESSED_BY_DOMAININTERACTION = layers()
            .whereLayer(DOMAIN_LAYER).mayOnlyBeAccessedByLayers(DOMAININTERACTION_LAYER)
            // Rule 9, the one documented exception: process may reference domain event classes,
            // because the event type is the contract of the listener. Nothing else in domain is allowed.
            .ignoreDependency(resideInAPackage(PROCESS), resideInAPackage(DOMAIN).and(simpleNameEndingWith("Event")))
            .allowEmptyShould(true);

    // Rule 2: domaininteraction may only be accessed by the six outer adapters.
    @ArchTest
    public static final ArchRule DOMAININTERACTION_IS_ONLY_ACCESSED_BY_ADAPTERS = layers()
            .whereLayer(DOMAININTERACTION_LAYER)
            .mayOnlyBeAccessedByLayers(COMMAND_LAYER, QUERY_LAYER, PROCESS_LAYER, ASSISTANT_LAYER, DATA_LAYER, ACL_LAYER)
            .allowEmptyShould(true);

    // Rule 3, one rule per outer adapter: no outer adapter may be accessed by any other layer.
    @ArchTest
    public static final ArchRule COMMAND_IS_NOT_ACCESSED_BY_OTHER_LAYERS = layers()
            .whereLayer(COMMAND_LAYER).mayNotBeAccessedByAnyLayer()
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule QUERY_IS_NOT_ACCESSED_BY_OTHER_LAYERS = layers()
            .whereLayer(QUERY_LAYER).mayNotBeAccessedByAnyLayer()
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule PROCESS_IS_NOT_ACCESSED_BY_OTHER_LAYERS = layers()
            .whereLayer(PROCESS_LAYER).mayNotBeAccessedByAnyLayer()
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule ASSISTANT_IS_NOT_ACCESSED_BY_OTHER_LAYERS = layers()
            .whereLayer(ASSISTANT_LAYER).mayNotBeAccessedByAnyLayer()
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule DATA_IS_NOT_ACCESSED_BY_OTHER_LAYERS = layers()
            .whereLayer(DATA_LAYER).mayNotBeAccessedByAnyLayer()
            .allowEmptyShould(true);

    @ArchTest
    public static final ArchRule ACL_IS_NOT_ACCESSED_BY_OTHER_LAYERS = layers()
            .whereLayer(ACL_LAYER).mayNotBeAccessedByAnyLayer()
            .allowEmptyShould(true);

    // Rule 4: domain depends on java.. only.
    @ArchTest
    public static final ArchRule DOMAIN_DEPENDS_ON_JAVA_ONLY = classes()
            .that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN)
            .allowEmptyShould(true);

    // Rule 5: Jakarta Persistence and Spring Data only in data.
    @ArchTest
    public static final ArchRule PERSISTENCE_TYPES_ONLY_IN_DATA = noClasses()
            .that().resideOutsideOfPackage(DATA)
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework.data..")
            .allowEmptyShould(true);

    // Rule 6: RestClient and the rest of org.springframework.web.client only in acl.
    @ArchTest
    public static final ArchRule REST_CLIENT_ONLY_IN_ACL = noClasses()
            .that().resideOutsideOfPackage(ACL)
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web.client..")
            .allowEmptyShould(true);

    // Rule 7: Spring AI only in assistant.
    @ArchTest
    public static final ArchRule SPRING_AI_ONLY_IN_ASSISTANT = noClasses()
            .that().resideOutsideOfPackage(ASSISTANT)
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.ai..")
            .allowEmptyShould(true);

    // Rule 8: @RestController classes only live in command, query and assistant.
    // The annotation is named by its fully qualified name, so the rule needs no compile dependency on it.
    @ArchTest
    public static final ArchRule REST_CONTROLLERS_ONLY_IN_COMMAND_QUERY_ASSISTANT = classes()
            .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
            .should().resideInAnyPackage(COMMAND, QUERY, ASSISTANT)
            .allowEmptyShould(true);

    // Money rule, from the requirement "Money is exact" and the standard's "Never use double or float for money":
    // no field and no method or constructor parameter of type double or float in domain, domaininteraction,
    // command and query. Prices are Money in the domain and BigDecimal plus a currency code outside it.
    @ArchTest
    public static final ArchRule NO_DOUBLE_OR_FLOAT_FOR_MONEY = CompositeArchRule
            .of(noFields()
                    .that().areDeclaredInClassesThat().resideInAnyPackage(DOMAIN, DOMAININTERACTION, COMMAND, QUERY)
                    .should().haveRawType(double.class)
                    .orShould().haveRawType(float.class))
            .and(noCodeUnits()
                    .that().areDeclaredInClassesThat().resideInAnyPackage(DOMAIN, DOMAININTERACTION, COMMAND, QUERY)
                    .should().haveRawParameterTypes(includeDoubleOrFloat()))
            .allowEmptyShould(true);

    // Naming rules: top-level class names per package must end with one of these suffixes.
    @ArchTest
    public static final ArchRule COMMAND_CLASS_NAMES = classNames(COMMAND,
            "Commands", "Payload", "CommandResult", "ExceptionHandler");

    @ArchTest
    public static final ArchRule QUERY_CLASS_NAMES = classNames(QUERY,
            "Queries", "View", "ExceptionHandler");

    @ArchTest
    public static final ArchRule PROCESS_CLASS_NAMES = classNames(PROCESS,
            "EventProcessor", "Delegate");

    @ArchTest
    public static final ArchRule ASSISTANT_CLASS_NAMES = classNames(ASSISTANT,
            "Commands", "Tools", "Config", "Properties", "Payload", "View", "ExceptionHandler", "RateLimiter",
            "ChatModel");

    @ArchTest
    public static final ArchRule DATA_CLASS_NAMES = classNames(DATA,
            "DataServiceImpl", "JPA", "JPAMapper", "Repository");

    @ArchTest
    public static final ArchRule ACL_CLASS_NAMES = classNames(ACL,
            "AppServiceImpl", "Properties");

    @ArchTest
    public static final ArchRule DOMAININTERACTION_CLASS_NAMES = classNames(DOMAININTERACTION,
            "Flow", "DataService", "AppService", "DTO", "DomainMapper", "Exception");

    /**
     * Top-level classes of the package must end with one of the suffixes. Nested, anonymous and synthetic classes
     * and {@code package-info} are excluded.
     */
    private static ArchRule classNames(String layerPackage, String firstSuffix, String... otherSuffixes) {
        ClassesShouldConjunction rule = classes()
                .that().resideInAPackage(layerPackage)
                .and().areTopLevelClasses()
                .and().doNotHaveSimpleName("package-info")
                .should().haveSimpleNameEndingWith(firstSuffix);
        for (String suffix : otherSuffixes) {
            rule = rule.orShould().haveSimpleNameEndingWith(suffix);
        }
        return rule.allowEmptyShould(true);
    }

    /**
     * True when any parameter, in any position, is a double or a float, so a floating-point price next to other
     * parameters is caught too.
     */
    private static DescribedPredicate<List<JavaClass>> includeDoubleOrFloat() {
        return DescribedPredicate.describe("include double or float", parameterTypes -> parameterTypes.stream()
                .anyMatch(type -> type.isEquivalentTo(double.class) || type.isEquivalentTo(float.class)));
    }

    /**
     * A fresh layered architecture for every rule, so the exception of rule 1 never leaks into another rule.
     * It considers all dependencies, so classes outside the eight layers (the root package) are checked too.
     */
    private static LayeredArchitecture layers() {
        return layeredArchitecture().consideringAllDependencies()
                .layer(DOMAIN_LAYER).definedBy(DOMAIN)
                .layer(DOMAININTERACTION_LAYER).definedBy(DOMAININTERACTION)
                .layer(COMMAND_LAYER).definedBy(COMMAND)
                .layer(QUERY_LAYER).definedBy(QUERY)
                .layer(PROCESS_LAYER).definedBy(PROCESS)
                .layer(ASSISTANT_LAYER).definedBy(ASSISTANT)
                .layer(DATA_LAYER).definedBy(DATA)
                .layer(ACL_LAYER).definedBy(ACL);
    }
}
