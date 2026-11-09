package nl.openvalue.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
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
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

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
}
