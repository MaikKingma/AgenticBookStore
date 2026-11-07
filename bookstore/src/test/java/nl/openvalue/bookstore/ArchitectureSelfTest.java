package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Proves that no rule of {@link ArchitectureTest} is vacuous. Every rule is evaluated against the fixture classes in
 * nl.openvalue.archfixtures, which hold at least one designated violator per rule, and must name that violator.
 * Control fixtures prove the rules stay narrow. This class deliberately has no {@code @AnalyzeClasses} and no
 * {@code @ArchTest}, so the rules never run on the production import from here.
 */
class ArchitectureSelfTest {

    private static final String FIXTURES = "nl.openvalue.archfixtures.";

    // Controls: fixtures that the rules must NOT report.
    private static final String EVENT_PROCESSOR = FIXTURES + "process.BookEventProcessor";
    private static final String NESTED_CLASS = FIXTURES + "command.BookCommands$InnerThing";
    private static final String ANNOTATED_PACKAGE_INFO = FIXTURES + "query.package-info";

    // Money rule fixtures: at least one violator per guarded layer, each with a double or float field or parameter.
    private static final String PRICED_BOOK = FIXTURES + "domain.PricedBook";
    private static final String PRICE_DTO = FIXTURES + "domaininteraction.PriceDTO";
    private static final String PRICE_PAYLOAD = FIXTURES + "command.PricePayload";
    private static final String PRICE_VIEW = FIXTURES + "query.PriceView";
    // A double as the only parameter, and a float as the first parameter.
    private static final String PRICE_TAG = FIXTURES + "domain.PriceTag";
    private static final String REPRICE_PAYLOAD = FIXTURES + "command.RepricePayload";
    // Control: a double field in assistant, a layer the money rule does not guard.
    private static final String TEMPERATURE_PROPERTIES = FIXTURES + "assistant.TemperatureProperties";

    /** The four packages the money rule guards, as the anchored patterns of ArchitectureTest. */
    private static final Set<String> MONEY_RULE_PACKAGES = Set.of("nl.openvalue..domain..",
            "nl.openvalue..domaininteraction..", "nl.openvalue..command..", "nl.openvalue..query..");

    private static final Pattern COMPANY_PACKAGE_IN_DESCRIPTION = Pattern.compile("'(nl\\.openvalue\\.\\.[^']*)'");

    /** The package list each half of the money rule prints, in ArchUnit's bracket form (from a printed description). */
    private static final String MONEY_RULE_PACKAGE_LIST = "['nl.openvalue..domain..', "
            + "'nl.openvalue..domaininteraction..', 'nl.openvalue..command..', 'nl.openvalue..query..']";

    /** The rules of ArchitectureTest that are built on a layered architecture. */
    private static final List<String> LAYERED_RULES = List.of(
            "DOMAIN_IS_ONLY_ACCESSED_BY_DOMAININTERACTION",
            "DOMAININTERACTION_IS_ONLY_ACCESSED_BY_ADAPTERS",
            "COMMAND_IS_NOT_ACCESSED_BY_OTHER_LAYERS",
            "QUERY_IS_NOT_ACCESSED_BY_OTHER_LAYERS",
            "PROCESS_IS_NOT_ACCESSED_BY_OTHER_LAYERS",
            "ASSISTANT_IS_NOT_ACCESSED_BY_OTHER_LAYERS",
            "DATA_IS_NOT_ACCESSED_BY_OTHER_LAYERS",
            "ACL_IS_NOT_ACCESSED_BY_OTHER_LAYERS");

    /** Rule 3 field of ArchitectureTest to the outer adapter layer it closes off. */
    private static final Map<String, String> ADAPTER_LAYER_OF_RULE = Map.of(
            "COMMAND_IS_NOT_ACCESSED_BY_OTHER_LAYERS", "command",
            "QUERY_IS_NOT_ACCESSED_BY_OTHER_LAYERS", "query",
            "PROCESS_IS_NOT_ACCESSED_BY_OTHER_LAYERS", "process",
            "ASSISTANT_IS_NOT_ACCESSED_BY_OTHER_LAYERS", "assistant",
            "DATA_IS_NOT_ACCESSED_BY_OTHER_LAYERS", "data",
            "ACL_IS_NOT_ACCESSED_BY_OTHER_LAYERS", "acl");

    private static final String LAYER_CLAUSE = "where layer '";

    /** Rule field of ArchitectureTest to the fixture classes that must be named in its failure details. */
    private static final Map<String, List<String>> EXPECTED_VIOLATORS = Map.ofEntries(
            Map.entry("DOMAIN_IS_ONLY_ACCESSED_BY_DOMAININTERACTION", List.of(
                    FIXTURES + "query.BookQueries",
                    FIXTURES + "process.LeakyDelegate",
                    FIXTURES + "RootLevelConfig")),
            Map.entry("DOMAININTERACTION_IS_ONLY_ACCESSED_BY_ADAPTERS", List.of(FIXTURES + "domain.LeakyAggregate")),
            Map.entry("COMMAND_IS_NOT_ACCESSED_BY_OTHER_LAYERS", List.of(FIXTURES + "query.BookReader")),
            Map.entry("QUERY_IS_NOT_ACCESSED_BY_OTHER_LAYERS", List.of(FIXTURES + "data.BookDataServiceImpl")),
            Map.entry("PROCESS_IS_NOT_ACCESSED_BY_OTHER_LAYERS", List.of(FIXTURES + "acl.DistributorClient")),
            Map.entry("ASSISTANT_IS_NOT_ACCESSED_BY_OTHER_LAYERS", List.of(FIXTURES + "process.LeakyDelegate")),
            Map.entry("DATA_IS_NOT_ACCESSED_BY_OTHER_LAYERS", List.of(FIXTURES + "command.BookController")),
            Map.entry("ACL_IS_NOT_ACCESSED_BY_OTHER_LAYERS", List.of(FIXTURES + "assistant.BookAssistant")),
            Map.entry("DOMAIN_DEPENDS_ON_JAVA_ONLY", List.of(FIXTURES + "domain.BookEntity",
                    FIXTURES + "domain.LeakyAggregate")),
            Map.entry("PERSISTENCE_TYPES_ONLY_IN_DATA", List.of(
                    FIXTURES + "domain.BookEntity",
                    FIXTURES + "command.BookCommands")),
            Map.entry("REST_CLIENT_ONLY_IN_ACL", List.of(FIXTURES + "process.LeakyDelegate")),
            Map.entry("SPRING_AI_ONLY_IN_ASSISTANT", List.of(FIXTURES + "command.ChatCommands")),
            Map.entry("REST_CONTROLLERS_ONLY_IN_COMMAND_QUERY_ASSISTANT", List.of(FIXTURES + "data.BookRestController")),
            Map.entry("NO_DOUBLE_OR_FLOAT_FOR_MONEY", List.of(PRICED_BOOK, PRICE_DTO, PRICE_PAYLOAD, PRICE_VIEW,
                    REPRICE_PAYLOAD, PRICE_TAG)),
            Map.entry("COMMAND_CLASS_NAMES", List.of(FIXTURES + "command.BookController")),
            Map.entry("QUERY_CLASS_NAMES", List.of(FIXTURES + "query.BookReader")),
            Map.entry("PROCESS_CLASS_NAMES", List.of(FIXTURES + "process.EventHandler")),
            Map.entry("ASSISTANT_CLASS_NAMES", List.of(FIXTURES + "assistant.BookAssistant")),
            Map.entry("DATA_CLASS_NAMES", List.of(FIXTURES + "data.BookRestController")),
            Map.entry("ACL_CLASS_NAMES", List.of(FIXTURES + "acl.DistributorClient")),
            Map.entry("DOMAININTERACTION_CLASS_NAMES", List.of(FIXTURES + "domaininteraction.CatalogueService")));

    /** Naming rule field of ArchitectureTest to the exact suffix set of the architecture standard. */
    private static final Map<String, Set<String>> EXPECTED_SUFFIXES = Map.ofEntries(
            Map.entry("COMMAND_CLASS_NAMES", Set.of("Commands", "Payload", "CommandResult", "ExceptionHandler")),
            Map.entry("QUERY_CLASS_NAMES", Set.of("Queries", "View", "ExceptionHandler")),
            Map.entry("PROCESS_CLASS_NAMES", Set.of("EventProcessor", "Delegate")),
            Map.entry("ASSISTANT_CLASS_NAMES", Set.of("Commands", "Tools", "Config", "Properties", "Payload", "View",
                    "ExceptionHandler", "RateLimiter", "ChatModel")),
            Map.entry("DATA_CLASS_NAMES", Set.of("DataServiceImpl", "JPA", "JPAMapper", "Repository")),
            Map.entry("ACL_CLASS_NAMES", Set.of("AppServiceImpl", "Properties")),
            Map.entry("DOMAININTERACTION_CLASS_NAMES", Set.of("Flow", "DataService", "AppService", "DTO",
                    "DomainMapper", "Exception")));

    private static final Pattern SUFFIX_IN_DESCRIPTION = Pattern.compile("simple name ending with '([^']+)'");

    private static JavaClasses fixtures;

    @BeforeAll
    static void importFixtures() {
        // No import options: the fixtures live in the test output, which DoNotIncludeTests would drop.
        fixtures = new ClassFileImporter()
                .importPackages("nl.openvalue.archfixtures", "org.springframework.ai.archfixtures");
    }

    @Test
    void fixtureImportContainsEveryNamedFixture() {
        Set<String> named = new TreeSet<>(List.of(EVENT_PROCESSOR, NESTED_CLASS, ANNOTATED_PACKAGE_INFO,
                FIXTURES + "domain.BookAddedEvent", "org.springframework.ai.archfixtures.StubChatClient",
                PRICED_BOOK, PRICE_DTO, PRICE_PAYLOAD, PRICE_VIEW, TEMPERATURE_PROPERTIES, PRICE_TAG,
                REPRICE_PAYLOAD));
        EXPECTED_VIOLATORS.values().forEach(named::addAll);

        assertThat(named).allSatisfy(name -> assertThat(fixtures.contain(name)).as("fixture %s imported", name).isTrue());
    }

    @Test
    void processMayReferenceDomainEvents() {
        assertThat(fixtures.contain(EVENT_PROCESSOR)).isTrue();

        assertThat(detailsOf("DOMAIN_IS_ONLY_ACCESSED_BY_DOMAININTERACTION"))
                .noneMatch(detail -> detail.contains(EVENT_PROCESSOR));
    }

    @Test
    void namingRulesIgnoreNestedClasses() {
        assertThat(fixtures.contain(NESTED_CLASS)).isTrue();

        assertThat(detailsOf("COMMAND_CLASS_NAMES")).noneMatch(detail -> detail.contains(NESTED_CLASS));
    }

    @Test
    void namingRulesIgnorePackageInfo() {
        assertThat(fixtures.contain(ANNOTATED_PACKAGE_INFO)).isTrue();

        assertThat(detailsOf("QUERY_CLASS_NAMES")).noneMatch(detail -> detail.contains(ANNOTATED_PACKAGE_INFO));
    }

    @Test
    void commandNamingRuleRejectsBookController() {
        String bookController = FIXTURES + "command.BookController";
        EvaluationResult result = ArchitectureTest.COMMAND_CLASS_NAMES.evaluate(fixtures);
        assertThat(result.hasViolation()).isTrue();

        String bookControllerDetails = result.getFailureReport().getDetails().stream()
                .filter(detail -> detail.contains(bookController))
                .collect(Collectors.joining("\n"));

        assertThat(bookControllerDetails)
                .contains(bookController)
                .contains("'Commands'", "'Payload'", "'CommandResult'", "'ExceptionHandler'");
    }

    @Test
    void namingRulesPinExactlyTheSuffixesOfTheStandard() {
        assertThat(EXPECTED_SUFFIXES.keySet()).allSatisfy(ruleField -> {
            Matcher matcher = SUFFIX_IN_DESCRIPTION.matcher(rule(ruleField).getDescription());
            Set<String> suffixes = new TreeSet<>();
            while (matcher.find()) {
                suffixes.add(matcher.group(1));
            }
            assertThat(suffixes).as("%s suffixes", ruleField).isEqualTo(new TreeSet<>(EXPECTED_SUFFIXES.get(ruleField)));
        });
    }

    @Test
    void productionImportOfArchitectureTestHoldsOnlyProductionClasses() throws ReflectiveOperationException {
        AnalyzeClasses analyzeClasses = ArchitectureTest.class.getAnnotation(AnalyzeClasses.class);
        assertThat(analyzeClasses).isNotNull();
        assertThat(analyzeClasses.packagesOf()).isEmpty();
        assertThat(analyzeClasses.locations()).isEmpty();
        assertThat(analyzeClasses.wholeClasspath()).isFalse();
        List<ImportOption> importOptions = new ArrayList<>();
        for (Class<? extends ImportOption> importOption : analyzeClasses.importOptions()) {
            importOptions.add(importOption.getDeclaredConstructor().newInstance());
        }

        JavaClasses production = new ClassFileImporter().withImportOptions(importOptions)
                .importPackages(analyzeClasses.packages());

        List<String> names = production.stream().map(JavaClass::getName).toList();
        List<String> locations = production.stream()
                .map(javaClass -> javaClass.getSource().map(source -> source.getUri().toString()).orElse(""))
                .toList();
        assertThat(names).contains("nl.openvalue.bookstore.BookstoreApplication");
        assertThat(names).doesNotContain(ArchitectureTest.class.getName());
        assertThat(names).noneMatch(name -> name.startsWith("nl.openvalue.archfixtures"));
        assertThat(locations).noneMatch(location -> location.contains("/target/test-classes/"));
    }

    @Test
    void everyArchitectureRuleHasAnExpectedViolator() {
        List<Field> ruleFields = Arrays.stream(ArchitectureTest.class.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ArchTest.class))
                .toList();
        assertThat(ruleFields).allSatisfy(field -> {
            assertThat(Modifier.isPublic(field.getModifiers())).as("%s is public", field.getName()).isTrue();
            assertThat(Modifier.isStatic(field.getModifiers())).as("%s is static", field.getName()).isTrue();
            assertThat(field.getType()).as("%s type", field.getName()).isEqualTo(ArchRule.class);
        });
        Set<String> ruleNames = ruleFields.stream().map(Field::getName).collect(Collectors.toSet());

        assertThat(ruleNames).hasSize(21);
        assertThat(ruleNames).isEqualTo(EXPECTED_VIOLATORS.keySet());
    }

    @Test
    void domainIsOnlyAccessedByDomaininteraction() {
        assertReportsExpectedViolators("DOMAIN_IS_ONLY_ACCESSED_BY_DOMAININTERACTION");
    }

    @Test
    void domaininteractionIsOnlyAccessedByAdapters() {
        assertReportsExpectedViolators("DOMAININTERACTION_IS_ONLY_ACCESSED_BY_ADAPTERS");
    }

    @Test
    void commandIsNotAccessedByOtherLayers() {
        assertReportsExpectedViolators("COMMAND_IS_NOT_ACCESSED_BY_OTHER_LAYERS");
    }

    @Test
    void queryIsNotAccessedByOtherLayers() {
        assertReportsExpectedViolators("QUERY_IS_NOT_ACCESSED_BY_OTHER_LAYERS");
    }

    @Test
    void processIsNotAccessedByOtherLayers() {
        assertReportsExpectedViolators("PROCESS_IS_NOT_ACCESSED_BY_OTHER_LAYERS");
    }

    @Test
    void assistantIsNotAccessedByOtherLayers() {
        assertReportsExpectedViolators("ASSISTANT_IS_NOT_ACCESSED_BY_OTHER_LAYERS");
    }

    @Test
    void dataIsNotAccessedByOtherLayers() {
        assertReportsExpectedViolators("DATA_IS_NOT_ACCESSED_BY_OTHER_LAYERS");
    }

    @Test
    void aclIsNotAccessedByOtherLayers() {
        assertReportsExpectedViolators("ACL_IS_NOT_ACCESSED_BY_OTHER_LAYERS");
    }

    @Test
    void domainDependsOnJavaOnly() {
        assertReportsExpectedViolators("DOMAIN_DEPENDS_ON_JAVA_ONLY");
    }

    @Test
    void persistenceTypesOnlyInData() {
        assertReportsExpectedViolators("PERSISTENCE_TYPES_ONLY_IN_DATA");
    }

    @Test
    void restClientOnlyInAcl() {
        assertReportsExpectedViolators("REST_CLIENT_ONLY_IN_ACL");
    }

    @Test
    void springAiOnlyInAssistant() {
        assertReportsExpectedViolators("SPRING_AI_ONLY_IN_ASSISTANT");
    }

    @Test
    void restControllersOnlyInCommandQueryAssistant() {
        assertReportsExpectedViolators("REST_CONTROLLERS_ONLY_IN_COMMAND_QUERY_ASSISTANT");
    }

    @Test
    void noDoubleOrFloatForMoney() {
        assertReportsExpectedViolators("NO_DOUBLE_OR_FLOAT_FOR_MONEY");
    }

    @Test
    void commandClassNames() {
        assertReportsExpectedViolators("COMMAND_CLASS_NAMES");
    }

    @Test
    void queryClassNames() {
        assertReportsExpectedViolators("QUERY_CLASS_NAMES");
    }

    @Test
    void processClassNames() {
        assertReportsExpectedViolators("PROCESS_CLASS_NAMES");
    }

    @Test
    void assistantClassNames() {
        assertReportsExpectedViolators("ASSISTANT_CLASS_NAMES");
    }

    @Test
    void dataClassNames() {
        assertReportsExpectedViolators("DATA_CLASS_NAMES");
    }

    @Test
    void aclClassNames() {
        assertReportsExpectedViolators("ACL_CLASS_NAMES");
    }

    @Test
    void domaininteractionClassNames() {
        assertReportsExpectedViolators("DOMAININTERACTION_CLASS_NAMES");
    }

    @Test
    void moneyRuleIgnoresOtherLayers() {
        assertThat(fixtures.contain(TEMPERATURE_PROPERTIES)).isTrue();

        assertThat(detailsOf("NO_DOUBLE_OR_FLOAT_FOR_MONEY"))
                .noneMatch(detail -> detail.contains(TEMPERATURE_PROPERTIES));
    }

    @Test
    void moneyRuleDescriptionNamesTheFourLayersAndBothTypes() {
        String description = rule("NO_DOUBLE_OR_FLOAT_FOR_MONEY").getDescription();

        Matcher matcher = COMPANY_PACKAGE_IN_DESCRIPTION.matcher(description);
        Set<String> packages = new TreeSet<>();
        while (matcher.find()) {
            packages.add(matcher.group(1));
        }
        assertThat(packages).isEqualTo(new TreeSet<>(MONEY_RULE_PACKAGES));
        assertThat(description).contains("have raw type double", "have raw type float", "include double or float");
    }

    @Test
    void moneyRuleDescriptionNamesTheFourLayersInEachHalf() {
        String description = rule("NO_DOUBLE_OR_FLOAT_FOR_MONEY").getDescription();

        long packageLists = Pattern.compile(Pattern.quote(MONEY_RULE_PACKAGE_LIST)).matcher(description)
                .results().count();
        assertThat(packageLists).as("the four-package list once per half in: %s", description).isEqualTo(2);
        assertThat(description).contains(
                "no fields that are declared in classes that reside in any package " + MONEY_RULE_PACKAGE_LIST
                        + " should have raw type double",
                "no code units that are declared in classes that reside in any package " + MONEY_RULE_PACKAGE_LIST
                        + " should have raw parameter types include double or float");
    }

    @Test
    void everyLayeredRuleStatesExactlyOneLayerConstraint() {
        assertThat(LAYERED_RULES).hasSize(8).allSatisfy(ruleField -> {
            String description = rule(ruleField).getDescription();
            long constraints = Pattern.compile(Pattern.quote(LAYER_CLAUSE)).matcher(description).results().count();
            assertThat(constraints).as("%s states one layer constraint in: %s", ruleField, description).isEqualTo(1);
        });
    }

    @Test
    void architectureRulesAreFieldsOnly() {
        assertThat(ArchitectureTest.class.getDeclaredMethods())
                .noneMatch(method -> method.isAnnotationPresent(ArchTest.class));
    }

    @Test
    void domainRuleAllowsOnlyDomaininteraction() {
        assertThat(rule("DOMAIN_IS_ONLY_ACCESSED_BY_DOMAININTERACTION").getDescription())
                .contains("where layer 'domain' may only be accessed by layers ['domaininteraction']");
    }

    @Test
    void domaininteractionRuleAllowsExactlyTheSixAdapters() {
        assertThat(rule("DOMAININTERACTION_IS_ONLY_ACCESSED_BY_ADAPTERS").getDescription())
                .contains("where layer 'domaininteraction' may only be accessed by layers "
                        + "['command', 'query', 'process', 'assistant', 'data', 'acl']");
    }

    @Test
    void adapterRulesCloseOffTheirOwnLayer() {
        assertThat(ADAPTER_LAYER_OF_RULE).hasSize(6).allSatisfy((ruleField, layer) ->
                assertThat(rule(ruleField).getDescription())
                        .contains("where layer '" + layer + "' may not be accessed by any layer"));
    }

    @Test
    void frameworkRulesNameTheirExemptPackageAndForbiddenPackages() {
        assertThat(rule("PERSISTENCE_TYPES_ONLY_IN_DATA").getDescription())
                .contains("reside outside of package 'nl.openvalue..data..'")
                .contains("should depend on classes that reside in any package "
                        + "['jakarta.persistence..', 'org.springframework.data..']");
        assertThat(rule("REST_CLIENT_ONLY_IN_ACL").getDescription())
                .contains("reside outside of package 'nl.openvalue..acl..'")
                .contains("should depend on classes that reside in any package ['org.springframework.web.client..']");
        assertThat(rule("SPRING_AI_ONLY_IN_ASSISTANT").getDescription())
                .contains("reside outside of package 'nl.openvalue..assistant..'")
                .contains("should depend on classes that reside in any package ['org.springframework.ai..']");
    }

    @Test
    void restControllerRuleAcceptsControllersInCommand() {
        String bookController = FIXTURES + "command.BookController";
        assertThat(fixtures.contain(bookController)).isTrue();

        assertThat(detailsOf("REST_CONTROLLERS_ONLY_IN_COMMAND_QUERY_ASSISTANT"))
                .noneMatch(detail -> detail.contains(bookController));
    }

    @Test
    void domainJavaOnlyRuleIgnoresPrimitiveAndArrayFields() {
        String bookEntity = FIXTURES + "domain.BookEntity";
        assertThat(fixtures.contain(bookEntity)).isTrue();

        assertThat(detailsOf("DOMAIN_DEPENDS_ON_JAVA_ONLY"))
                .noneMatch(detail -> detail.contains(bookEntity + ".pages"))
                .noneMatch(detail -> detail.contains(bookEntity + ".printRuns"));
    }

    private static List<String> detailsOf(String ruleField) {
        return rule(ruleField).evaluate(fixtures).getFailureReport().getDetails();
    }

    private static void assertReportsExpectedViolators(String ruleField) {
        EvaluationResult result = rule(ruleField).evaluate(fixtures);
        assertThat(result.hasViolation()).as("%s reports a violation", ruleField).isTrue();
        List<String> details = result.getFailureReport().getDetails();
        for (String violator : EXPECTED_VIOLATORS.get(ruleField)) {
            // The violator must be the origin of the violation, not merely the target of another class's dependency.
            Pattern asOrigin = Pattern.compile(
                    "^(Class|Field|Method|Constructor|Static Initializer) <" + Pattern.quote(violator) + "[.$>].*");
            assertThat(details).as("%s names %s as origin", ruleField, violator)
                    .anyMatch(detail -> asOrigin.matcher(detail).matches());
        }
    }

    private static ArchRule rule(String ruleField) {
        try {
            return (ArchRule) ArchitectureTest.class.getField(ruleField).get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("ArchitectureTest has no public rule field " + ruleField, e);
        }
    }
}
