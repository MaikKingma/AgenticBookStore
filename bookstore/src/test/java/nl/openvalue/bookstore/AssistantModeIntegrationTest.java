package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import nl.openvalue.bookstore.assistant.AssistantProperties;
import nl.openvalue.bookstore.assistant.OfflineChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

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
}
