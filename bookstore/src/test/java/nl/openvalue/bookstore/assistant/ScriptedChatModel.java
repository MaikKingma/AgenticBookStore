package nl.openvalue.bookstore.assistant;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

/**
 * A chat model that answers from a script instead of calling a real model. Each step is a function of the prompt the
 * model receives, so a reply can quote what a tool returned. Every received prompt is recorded as an immutable copy.
 *
 * <p>A plain class on purpose: tests create it themselves or add it by {@code @Import}. As a scanned bean it would sit
 * next to the application's own chat model in every full context.
 */
public class ScriptedChatModel implements ChatModel {

    /** What the model received in one call: the messages in order and the names of the tools it was offered. */
    public record ReceivedPrompt(List<Message> messages, List<String> toolNames) {

        public ReceivedPrompt {
            messages = List.copyOf(messages);
            toolNames = List.copyOf(toolNames);
        }
    }

    private final Deque<Function<Prompt, AssistantMessage>> steps = new ArrayDeque<>();
    private final List<ReceivedPrompt> received = new ArrayList<>();

    /** Next step: answer with this text. */
    public ScriptedChatModel thenReply(String text) {
        return thenReply(prompt -> text);
    }

    /** Next step: answer with a text made from the prompt the model receives. */
    public ScriptedChatModel thenReply(Function<Prompt, String> text) {
        steps.add(prompt -> AssistantMessage.builder().content(text.apply(prompt)).build());
        return this;
    }

    /** Next step: ask for one tool call with these JSON arguments. */
    public ScriptedChatModel thenCallTool(String toolName, String argumentsJson) {
        steps.add(prompt -> AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("call-" + received.size(), "function", toolName,
                        argumentsJson)))
                .build());
        return this;
    }

    /** The prompts received so far, oldest first. */
    public List<ReceivedPrompt> received() {
        return List.copyOf(received);
    }

    /** Forgets the received prompts and the steps not yet used. */
    public void reset() {
        received.clear();
        steps.clear();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        received.add(new ReceivedPrompt(prompt.getInstructions(), toolNames(prompt.getOptions())));
        Function<Prompt, AssistantMessage> step = steps.poll();
        if (step == null) {
            throw new IllegalStateException("No scripted step left for call " + received.size());
        }
        return new ChatResponse(List.of(new Generation(step.apply(prompt))));
    }

    /**
     * Tool-calling options are required. Spring AI adds the tools to a request, and runs the tool loop, only when the
     * model's options are {@link ToolCallingChatOptions}; with the default options it drops every tool silently.
     */
    @Override
    public ChatOptions getOptions() {
        return ToolCallingChatOptions.builder().build();
    }

    private static List<String> toolNames(ChatOptions options) {
        if (options instanceof ToolCallingChatOptions toolOptions && toolOptions.getToolCallbacks() != null) {
            return toolOptions.getToolCallbacks().stream()
                    .map(callback -> callback.getToolDefinition().name())
                    .toList();
        }
        return List.of();
    }
}
