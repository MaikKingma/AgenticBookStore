package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import nl.openvalue.bookstore.assistant.AssistantConfig;
import nl.openvalue.bookstore.assistant.AssistantProperties;
import nl.openvalue.bookstore.assistant.OfflineChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;

/**
 * The shared application context runs as production does without a key: the test profile blanks it, so the assistant
 * is offline and no request can reach the real API.
 */
class AssistantModeIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private AssistantProperties assistantProperties;

    @Autowired
    private ChatMemory chatMemory;

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {
            };

    /** The offline answer of the chat endpoint, word for word what the storefront shows. */
    private static final Map<String, Object> OFFLINE = Map.of(
            "title", "Assistant offline",
            "status", 503,
            "detail", "Assistant offline: set BOOKSTORE_AI_API_KEY",
            "instance", "/assistant/chat",
            "code", "assistant-offline",
            "details", List.of());

    @Test
    void startsOfflineWithoutAKey() {
        assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("none");

        Map<String, ChatModel> chatModels = context.getBeansOfType(ChatModel.class);
        assertThat(chatModels).hasSize(1);
        assertThat(chatModels.values()).allSatisfy(model -> assertThat(model).isInstanceOf(OfflineChatModel.class));
        assertThat(context.getBeanNamesForType(AnthropicChatModel.class)).isEmpty();
    }

    @Test
    void assistantPropertiesComeFromTheFile() {
        assertThat(assistantProperties.maxRequestsPerMinute()).isEqualTo(20);
        assertThat(assistantProperties.maxMessageLength()).isEqualTo(1000);
    }

    @Test
    void offlineContextBuildsTheClient() {
        String[] configNames = context.getBeanNamesForType(AssistantConfig.class);
        assertThat(configNames).hasSize(1);
        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) context).getBeanFactory();

        Map<String, ChatClient> chatClients = context.getBeansOfType(ChatClient.class);
        assertThat(chatClients).hasSize(1);
        assertThat(chatClients.keySet()).allSatisfy(name ->
                assertThat(beanFactory.getBeanDefinition(name).getFactoryBeanName()).isEqualTo(configNames[0]));

        Map<String, ChatMemory> chatMemories = context.getBeansOfType(ChatMemory.class);
        assertThat(chatMemories).hasSize(1);
        assertThat(chatMemories.values()).allSatisfy(memory ->
                assertThat(memory).isInstanceOf(MessageWindowChatMemory.class));
        assertThat(chatMemories.keySet()).allSatisfy(name ->
                assertThat(beanFactory.getBeanDefinition(name).getFactoryBeanName()).isEqualTo(configNames[0]));
    }

    @Test
    void offlineSwitchComesAfterTheConfigurationFiles() {
        List<String> names = ((ConfigurableEnvironment) environment).getPropertySources().stream()
                .map(PropertySource::getName)
                .toList();

        int offlineSwitch = names.indexOf("assistantOfflineMode");
        assertThat(offlineSwitch).as("property sources %s", names).isNotNegative();
        assertThat(names).as("property sources").anyMatch(name -> name.contains("application-test.yml"));
        assertThat(names).as("property sources").anyMatch(name -> name.contains("application.yml"));
        // The configuration files only: Boot adds its own "applicationInfo" source after every post-processor.
        List<Integer> configurationFiles = IntStream.range(0, names.size())
                .filter(index -> names.get(index).startsWith("Config resource")
                        && names.get(index).contains("application"))
                .boxed()
                .toList();
        assertThat(configurationFiles).as("property sources %s", names)
                .isNotEmpty()
                .allSatisfy(index -> assertThat(index).isLessThan(offlineSwitch));
    }

    @Test
    void chatAnswers503WithoutAKey() {
        Map<String, Object> problem = restTestClient.post().uri("/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(chatBody(UUID.fromString("9c8b7a6d-5e4f-4a3b-8c2d-1e0f2a3b4c5d"), "Do you have Persuasion?"))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentType("application/problem+json")
                .expectBody(JSON_OBJECT).returnResult().getResponseBody();

        assertThat(problem).isEqualTo(OFFLINE);
    }

    @Test
    void storefrontGets503AsJsonWithoutAKey() {
        EntityExchangeResult<Map<String, Object>> result = restTestClient.post().uri("/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT, "application/json, application/problem+json")
                .body(chatBody(UUID.fromString("1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d"), "Anything by Austen?"))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody(JSON_OBJECT).returnResult();

        assertThat(String.valueOf(result.getResponseHeaders().getContentType()))
                .isIn("application/json", "application/problem+json");
        assertThat(result.getResponseBody()).isEqualTo(OFFLINE);
    }

    @Test
    void offlineRequestsAreNeverRateLimited() {
        UUID conversationId = UUID.fromString("3e4f5a6b-7c8d-4e9f-a0b1-c2d3e4f5a6b7");
        for (int request = 1; request <= 25; request++) {
            EntityExchangeResult<Map<String, Object>> answer = restTestClient.post().uri("/assistant/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(chatBody(conversationId, "Question " + request))
                    .exchange()
                    .expectBody(JSON_OBJECT).returnResult();
            assertThat(answer.getStatus().value()).as("status of request %d", request).isEqualTo(503);
            assertThat(answer.getResponseBody()).as("request %d", request).containsEntry("code", "assistant-offline");
        }
    }

    @Test
    void offlineRequestLeavesNoMemory() {
        UUID conversationId = UUID.randomUUID();

        restTestClient.post().uri("/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(chatBody(conversationId, "Remember that I like sea stories."))
                .exchange()
                .expectStatus().isEqualTo(503);

        assertThat(chatMemory.get(conversationId.toString())).isEmpty();
    }

    private static Map<String, String> chatBody(UUID conversationId, String message) {
        return Map.of("conversationId", conversationId.toString(), "message", message);
    }
}
