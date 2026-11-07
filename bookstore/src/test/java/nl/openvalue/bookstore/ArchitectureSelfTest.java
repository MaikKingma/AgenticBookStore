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
            Map.entry("DOMAIN_DEPENDS_ON_JAVA_ONLY", List.of(FIXTURES + "domain.BookEntity")),
            Map.entry("PERSISTENCE_TYPES_ONLY_IN_DATA", List.of(
                    FIXTURES + "domain.BookEntity",
                    FIXTURES + "command.BookCommands")),
            Map.entry("REST_CLIENT_ONLY_IN_ACL", List.of(FIXTURES + "process.LeakyDelegate")),
            Map.entry("SPRING_AI_ONLY_IN_ASSISTANT", List.of(FIXTURES + "command.ChatCommands")),
            Map.entry("REST_CONTROLLERS_ONLY_IN_COMMAND_QUERY_ASSISTANT", List.of(FIXTURES + "data.BookRestController")),
            Map.entry("COMMAND_CLASS_NAMES", List.of(FIXTURES + "command.BookController")),
            Map.entry("QUERY_CLASS_NAMES", List.of(FIXTURES + "query.BookReader")),
            Map.entry("PROCESS_CLASS_NAMES", List.of(FIXTURES + "process.EventHandler")),
            Map.entry("ASSISTANT_CLASS_NAMES", List.of(FIXTURES + "assistant.BookAssistant")),
            Map.entry("DATA_CLASS_NAMES", List.of(FIXTURES + "data.BookRestController")),
            Map.entry("ACL_CLASS_NAMES", List.of(FIXTURES + "acl.DistributorClient")),
            Map.entry("DOMAININTERACTION_CLASS_NAMES", List.of(FIXTURES + "domaininteraction.CatalogueService")));

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
                FIXTURES + "domain.BookAddedEvent", "org.springframework.ai.archfixtures.StubChatClient"));
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

        assertThat(ruleNames).hasSize(20);
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

    private static List<String> detailsOf(String ruleField) {
        return rule(ruleField).evaluate(fixtures).getFailureReport().getDetails();
    }

    private static void assertReportsExpectedViolators(String ruleField) {
        EvaluationResult result = rule(ruleField).evaluate(fixtures);
        assertThat(result.hasViolation()).as("%s reports a violation", ruleField).isTrue();
        List<String> details = result.getFailureReport().getDetails();
        for (String violator : EXPECTED_VIOLATORS.get(ruleField)) {
            assertThat(details).as("%s names %s", ruleField, violator).anyMatch(detail -> detail.contains(violator));
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
