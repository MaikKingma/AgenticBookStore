package nl.openvalue.bookstore.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import nl.openvalue.bookstore.domaininteraction.CatalogueFlow;
import nl.openvalue.bookstore.domaininteraction.InventoryFlow;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Which chat model {@link AssistantConfig} contributes for each value of {@code spring.ai.model.chat}. A small
 * context per case, no cached Spring test context.
 * The chat client builder and the tools stand in for the beans Spring AI's auto-configuration and the component
 * scan supply in the application.
 */
class AssistantConfigContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AssistantConfig.class)
            .withBean(ChatClient.Builder.class, () -> ChatClient.builder(new OfflineChatModel()))
            .withBean(BookstoreTools.class,
                    () -> new BookstoreTools(mock(CatalogueFlow.class), mock(InventoryFlow.class)));

    @Test
    void noneGivesTheOfflineChatModel() {
        contextRunner.withPropertyValues("spring.ai.model.chat=none").run(context -> {
            assertThat(context).hasSingleBean(OfflineChatModel.class);
            assertThat(context.getBeansOfType(ChatModel.class)).hasSize(1);
            assertThat(context).hasSingleBean(AssistantProperties.class);
        });
    }

    @Test
    void scriptedGivesNoOfflineChatModel() {
        contextRunner.withPropertyValues("spring.ai.model.chat=scripted").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(OfflineChatModel.class);
            assertThat(context).hasSingleBean(AssistantProperties.class);
        });
    }

    @Test
    void anthropicGivesNoOfflineChatModel() {
        contextRunner.withPropertyValues("spring.ai.model.chat=anthropic").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(OfflineChatModel.class);
            assertThat(context).hasSingleBean(AssistantProperties.class);
        });
    }

    @Test
    void absentPropertyGivesNoOfflineChatModel() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().containsProperty("spring.ai.model.chat")).isFalse();
            assertThat(context).doesNotHaveBean(OfflineChatModel.class);
            assertThat(context).hasSingleBean(AssistantProperties.class);
        });
    }
}
