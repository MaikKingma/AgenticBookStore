package nl.openvalue.bookstore.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicRetryableException;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import nl.openvalue.bookstore.domaininteraction.ConflictException;
import nl.openvalue.bookstore.domaininteraction.InvalidInputException;
import nl.openvalue.bookstore.domaininteraction.NotFoundException;
import nl.openvalue.bookstore.domaininteraction.UnavailableException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.AdvisorSpec;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * The chat endpoint over a mocked chat client, with the real rate limiter on a clock the test moves. Every test starts
 * a minute after the previous one, so no test shares the limiter's window with another.
 *
 * <p>The mocks are declared on the class and autowired into the fields: Spring injects the enclosing instance of the
 * nested {@link Offline} tests from the nested context, which has a mocked chat client of its own and the offline chat
 * model instead of a mocked one.
 */
@WebMvcTest(AssistantCommands.class)
@Import({AssistantRateLimiter.class, AssistantCommandsTest.LimiterSettings.class})
@MockitoBean(types = {ChatClient.class, ChatModel.class, ChatMemory.class})
class AssistantCommandsTest {

    /** The Accept header the storefront sends with every request. */
    static final String STOREFRONT_ACCEPT = "application/json, application/problem+json";

    private static final UUID FIRST_ID = UUID.fromString("6b1d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f");
    private static final UUID SECOND_ID = UUID.fromString("d0c9b8a7-f6e5-4d4c-b3a2-019f8e7d6c5b");

    /** The answer to the 21st request of a minute: no code, the storefront knows a 429 by its status. */
    private static final Map<String, Object> TOO_MANY_REQUESTS = Map.of(
            "title", "Too Many Requests",
            "status", 429,
            "detail", "Too many assistant requests: at most 20 per minute",
            "instance", "/assistant/chat");

    /** The offline answer, word for word what the storefront shows. */
    private static final Map<String, Object> OFFLINE = Map.of(
            "title", "Assistant offline",
            "status", 503,
            "detail", "Assistant offline: set BOOKSTORE_AI_API_KEY",
            "instance", "/assistant/chat",
            "code", "assistant-offline",
            "details", List.of());

    /** The answer to a failed chat call: the spec 12 text, with the shape of the offline answer. */
    private static final Map<String, Object> UNAVAILABLE = Map.of(
            "title", "Assistant unavailable",
            "status", 503,
            "detail", "The assistant is unavailable right now, try again later",
            "instance", "/assistant/chat",
            "code", "assistant-unavailable",
            "details", List.of());

    /** What a failing live call says; none of it may reach the answer. */
    private static final String FAILURE_MESSAGE = "upstream rejected key sk-ant-dummy-not-a-key";

    /** The limiter's clock and settings: 20 requests a minute, messages up to 1000 characters. */
    @TestConfiguration(proxyBeanMethods = false)
    static class LimiterSettings {

        @Bean
        MutableClock clock() {
            return new MutableClock(Instant.parse("2001-06-15T12:00:30.600Z"));
        }

        @Bean
        AssistantProperties assistantProperties() {
            return new AssistantProperties(20, 1000);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ChatModel chatModel;

    private final ChatClientRequestSpec request = mock(ChatClientRequestSpec.class);
    private final CallResponseSpec response = mock(CallResponseSpec.class);

    @BeforeEach
    void startANewMinuteWithAnAnsweringClient() {
        clock.advance(Duration.ofSeconds(61));
        when(chatClient.prompt()).thenReturn(request);
        when(request.user(anyString())).thenReturn(request);
        when(request.advisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any())).thenReturn(request);
        when(request.call()).thenReturn(response);
        when(response.content()).thenReturn("We have Emma in stock.");
    }

    // --- contract ---

    @Test
    void replyEchoesTheConversation() throws Exception {
        when(response.content()).thenReturn("Persuasion costs 9.99 EUR.");
        Map<String, Object> first = json(chat(FIRST_ID, "Do you have Persuasion?").andExpect(status().isOk()));
        when(response.content()).thenReturn(" Emma is out of stock. ");
        Map<String, Object> second = json(chat(SECOND_ID, "And Emma?").andExpect(status().isOk()));

        assertThat(first).isEqualTo(Map.of("conversationId", FIRST_ID.toString(),
                "reply", "Persuasion costs 9.99 EUR."));
        assertThat(second).isEqualTo(Map.of("conversationId", SECOND_ID.toString(),
                "reply", " Emma is out of stock. "));
    }

    @Test
    void messageReachesTheClientUnchanged() throws Exception {
        chat(FIRST_ID, " two spaces  ").andExpect(status().isOk());
        chat(FIRST_ID, "Anything by Austen?").andExpect(status().isOk());

        ArgumentCaptor<String> messages = ArgumentCaptor.captor();
        verify(request, times(2)).user(messages.capture());
        assertThat(messages.getAllValues()).containsExactly(" two spaces  ", "Anything by Austen?");
    }

    @Test
    void conversationIdIsPassedAsMemoryKey() throws Exception {
        chat(FIRST_ID, "Do you have Persuasion?").andExpect(status().isOk());
        chat(SECOND_ID, "Do you have Persuasion?").andExpect(status().isOk());

        ArgumentCaptor<Consumer<AdvisorSpec>> advisorSettings = ArgumentCaptor.captor();
        verify(request, times(2)).advisors(advisorSettings.capture());
        List<Consumer<AdvisorSpec>> settings = advisorSettings.getAllValues();
        assertThat(advisorParameters(settings.get(0))).isEqualTo(Map.of(ChatMemory.CONVERSATION_ID,
                "6b1d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f"));
        assertThat(advisorParameters(settings.get(1))).isEqualTo(Map.of(ChatMemory.CONVERSATION_ID,
                "d0c9b8a7-f6e5-4d4c-b3a2-019f8e7d6c5b"));
    }

    // --- validation and length ---

    @Test
    void missingConversationIdIs400() throws Exception {
        ResultActions result = post("{\"message\":\"Do you have Persuasion?\"}");

        assertThat(problem(result, 400)).isEqualTo(validationProblem(List.of(
                Map.of("field", "conversationId", "message", "must not be null"))));
        verifyNoInteractions(chatClient);
    }

    @Test
    void missingMessageIs400() throws Exception {
        ResultActions result = post("{\"conversationId\":\"" + FIRST_ID + "\"}");

        assertThat(problem(result, 400)).isEqualTo(validationProblem(List.of(
                Map.of("field", "message", "message", "must not be blank"))));
        verifyNoInteractions(chatClient);
    }

    @Test
    void missingConversationIdAndMessageAreListedByField() throws Exception {
        ResultActions result = post("{}");

        assertThat(problem(result, 400)).isEqualTo(validationProblem(List.of(
                Map.of("field", "conversationId", "message", "must not be null"),
                Map.of("field", "message", "message", "must not be blank"))));
        verifyNoInteractions(chatClient);
    }

    @ParameterizedTest
    // the last row is the JSON escape for U+2003, an em space: a Unicode blank that String.trim() keeps
    @ValueSource(strings = {"", "   ", "\\t\\n ", "\\u2003"})
    void blankMessageIs400(String jsonMessage) throws Exception {
        ResultActions result = chat(FIRST_ID, jsonMessage);

        assertThat(problem(result, 400)).isEqualTo(validationProblem(List.of(
                Map.of("field", "message", "message", "must not be blank"))));
        verifyNoInteractions(chatClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "6b1d2e3f-4a5b-4c6d-8e7f"})
    void malformedConversationIdIs400(String conversationId) throws Exception {
        post("{\"conversationId\":\"" + conversationId + "\",\"message\":\"Hi\"}").andExpect(status().isBadRequest());

        verifyNoInteractions(chatClient);
    }

    @Test
    void messageOf1000CharactersIsAccepted() throws Exception {
        String message = "b".repeat(1000);

        chat(FIRST_ID, message).andExpect(status().isOk());

        verify(request).user(message);
    }

    @Test
    void messageOf1001CharactersIs400() throws Exception {
        ResultActions result = chat(FIRST_ID, "b".repeat(1001));

        assertThat(problem(result, 400)).isEqualTo(Map.of(
                "title", "Validation failed",
                "status", 400,
                "detail", "The request body is not valid",
                "instance", "/assistant/chat",
                "code", "validation-failed",
                "details", List.of(Map.of("field", "message", "message", "must be at most 1000 characters"))));
        verifyNoInteractions(chatClient);
    }

    /** Lengths count UTF-16 units, as the storefront does: a book emoji counts two, an accented letter one. */
    @Test
    void lengthCountsUtf16UnitsNotCodePointsOrBytes() throws Exception {
        String emoji = Character.toString(0x1F4DA);
        String accented = Character.toString(0xE9);

        problem(chat(FIRST_ID, "c".repeat(999) + emoji), 400);
        chat(FIRST_ID, "c".repeat(999) + accented).andExpect(status().isOk());

        verify(request).user("c".repeat(999) + accented);
        verify(chatClient, times(1)).prompt();
    }

    @Test
    void trailingSpaceCountsTowardsTheLength() throws Exception {
        ResultActions result = chat(FIRST_ID, "d".repeat(1000) + " ");

        assertThat(problem(result, 400)).extractingByKey("details")
                .isEqualTo(List.of(Map.of("field", "message", "message", "must be at most 1000 characters")));
        verifyNoInteractions(chatClient);
    }

    // --- rate limit and request order ---

    @Test
    void twentyFirstRequestIs429AndNeverReachesTheModel() throws Exception {
        for (int request = 1; request <= 20; request++) {
            chat(FIRST_ID, "Question " + request).andExpect(status().isOk());
        }

        ResultActions result = chat(FIRST_ID, "Question 21");

        assertThat(problem(result, 429)).isEqualTo(TOO_MANY_REQUESTS);
        verify(chatClient, times(20)).prompt();
        verifyNoInteractions(chatModel);
    }

    @Test
    void storefrontGets429AsJson() throws Exception {
        for (int request = 1; request <= 20; request++) {
            sendFromStorefront(mockMvc, body(SECOND_ID, "Question " + request)).andExpect(status().isOk());
        }

        ResultActions result = sendFromStorefront(mockMvc, body(SECOND_ID, "Question 21"));

        assertThat(storefrontProblem(result, 429)).isEqualTo(TOO_MANY_REQUESTS);
        verify(chatClient, times(20)).prompt();
    }

    @Test
    void slotFreesAfterAMinute() throws Exception {
        for (int request = 1; request <= 20; request++) {
            chat(FIRST_ID, "Question " + request).andExpect(status().isOk());
        }
        problem(chat(FIRST_ID, "Question 21"), 429);

        clock.advance(Duration.ofSeconds(60));

        chat(FIRST_ID, "Question 22").andExpect(status().isOk());
        verify(request).user("Question 22");
    }

    @Test
    void invalidRequestsConsumeNoSlot() throws Exception {
        String tooLong = "e".repeat(1001);
        List<String> invalid = List.of(
                body(FIRST_ID, tooLong),
                body(FIRST_ID, "   "),
                "{\"message\":\"Hi\"}",
                "{\"conversationId\":\"not-a-uuid\",\"message\":\"Hi\"}");
        for (int request = 0; request < 20; request++) {
            post(invalid.get(request % invalid.size())).andExpect(status().isBadRequest());
        }

        for (int request = 1; request <= 20; request++) {
            chat(FIRST_ID, "Question " + request).andExpect(status().isOk());
        }

        problem(chat(FIRST_ID, "Question 21"), 429);
        verify(chatClient, times(20)).prompt();
    }

    @Test
    void tooLongMessageBeats429() throws Exception {
        for (int request = 1; request <= 20; request++) {
            chat(FIRST_ID, "Question " + request).andExpect(status().isOk());
        }

        ResultActions result = chat(FIRST_ID, "f".repeat(1001));

        assertThat(problem(result, 400)).extractingByKey("details")
                .isEqualTo(List.of(Map.of("field", "message", "message", "must be at most 1000 characters")));
        assertThat(problem(chat(FIRST_ID, " "), 400)).extractingByKey("details")
                .isEqualTo(List.of(Map.of("field", "message", "message", "must not be blank")));
    }

    // --- use-case exceptions ---

    static Stream<Arguments> useCaseRejections() {
        List<Map<String, String>> shelf = List.of(Map.of("shelf", "7", "aisle", "B"));
        List<Map<String, String>> lock = List.of(Map.of("lockedBy", "checkout"), Map.of("since", "09:15"));
        List<Map<String, String>> symbol = List.of(Map.of("position", "12", "reason", "unknown symbol"));
        List<Map<String, String>> retry = List.of(Map.of("retryAfter", "30"));
        return Stream.of(
                Arguments.of(new NotFoundException("shelf-not-found", "Shelf not found", "No shelf 7 in the shop",
                        shelf), 404, "shelf-not-found", "Shelf not found", "No shelf 7 in the shop", shelf),
                Arguments.of(new NotFoundException("note-not-found", "Note not found", "No note for this book"),
                        404, "note-not-found", "Note not found", "No note for this book", List.of()),
                Arguments.of(new ConflictException("basket-locked", "Basket locked", "The basket is locked", lock),
                        409, "basket-locked", "Basket locked", "The basket is locked", lock),
                Arguments.of(new ConflictException("question-repeated", "Question repeated", "Asked twice"),
                        409, "question-repeated", "Question repeated", "Asked twice", List.of()),
                Arguments.of(new InvalidInputException("odd-question", "Odd question",
                        "The question cannot be read", symbol),
                        422, "odd-question", "Odd question", "The question cannot be read", symbol),
                Arguments.of(new InvalidInputException("empty-shelf", "Empty shelf", "Nothing on that shelf"),
                        422, "empty-shelf", "Empty shelf", "Nothing on that shelf", List.of()),
                Arguments.of(new UnavailableException("catalogue-asleep", "Catalogue asleep",
                        "The catalogue cannot be reached", retry),
                        503, "catalogue-asleep", "Catalogue asleep", "The catalogue cannot be reached", retry),
                Arguments.of(new UnavailableException("stock-asleep", "Stock asleep", "The stock cannot be read"),
                        503, "stock-asleep", "Stock asleep", "The stock cannot be read", List.of()));
    }

    @ParameterizedTest
    @MethodSource("useCaseRejections")
    void useCaseExceptionsMapToTheirStatus(RuntimeException rejection, int status, String code, String title,
            String detail, List<Map<String, String>> details) throws Exception {
        when(response.content()).thenThrow(rejection);

        ResultActions result = chat(FIRST_ID, "Where is my book?");

        assertThat(problem(result, status)).isEqualTo(Map.of(
                "title", title,
                "status", status,
                "detail", detail,
                "instance", "/assistant/chat",
                "code", code,
                "details", details));
    }

    @Test
    void errorBodiesCarryNoTrace() throws Exception {
        assertNoTrace(post("{}"));
        assertNoTrace(chat(FIRST_ID, "g".repeat(1001)));
        when(response.content()).thenThrow(new UnavailableException("catalogue-asleep", "Catalogue asleep",
                "The catalogue cannot be reached", new IllegalStateException("connection refused")));
        assertNoTrace(chat(FIRST_ID, "Where is my book?"));
        for (int request = 2; request <= 20; request++) {
            chat(FIRST_ID, "Question " + request);
        }
        ResultActions tooMany = chat(FIRST_ID, "Question 21");
        tooMany.andExpect(status().isTooManyRequests());
        assertNoTrace(tooMany);
    }

    // --- a failed chat call ---

    /** Two failures of the Anthropic client and two from outside its hierarchy, each saying what must stay hidden. */
    static Stream<RuntimeException> failedChatCalls() {
        return Stream.of(
                new AnthropicIoException(FAILURE_MESSAGE),
                new AnthropicRetryableException(FAILURE_MESSAGE),
                new IllegalStateException(FAILURE_MESSAGE),
                new UncheckedIOException(FAILURE_MESSAGE, new IOException("upstream connection reset")));
    }

    @ParameterizedTest
    @MethodSource("failedChatCalls")
    void failedChatCallAnswersAssistantUnavailable(RuntimeException failure) throws Exception {
        when(response.content()).thenThrow(failure);

        ResultActions result = chat(FIRST_ID, "Where is my book?");

        Map<String, Object> problem = problem(result, 503);
        assertThat(problem).isEqualTo(UNAVAILABLE);
        assertThat(problem.keySet()).isEqualTo(OFFLINE.keySet());
        String body = result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).doesNotContain(FAILURE_MESSAGE).doesNotContain("sk-ant-dummy-not-a-key")
                .doesNotContain("upstream").doesNotContain(failure.getClass().getName())
                .doesNotContain(failure.getClass().getSimpleName());
        verify(chatClient).prompt();
    }

    @Test
    void failedChatCallIsLoggedOnceAtWarnWithItsCause() {
        IllegalStateException failure = new IllegalStateException(FAILURE_MESSAGE);
        when(response.content()).thenThrow(failure);
        List<ILoggingEvent> logEvents = new ArrayList<>();

        Throwable escaped = catchThrowable(() -> logOf(() -> chat(FIRST_ID, "Where is my book?"), logEvents));

        List<ILoggingEvent> warnings = logEvents.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .toList();
        assertThat(warnings).as("WARN lines of AssistantCommands").hasSize(1);
        assertThat(warnings.get(0).getLoggerName()).isEqualTo(AssistantCommands.class.getName());
        assertThat(warnings.get(0).getThrowableProxy().getClassName()).isEqualTo("java.lang.IllegalStateException");
        assertThat(((ThrowableProxy) warnings.get(0).getThrowableProxy()).getThrowable()).isSameAs(failure);
        assertThat(escaped).as("the failure is answered, not thrown").isNull();
    }

    /** A use-case exception from the chat call is a rejection the handler answers, not a failed model call. */
    @ParameterizedTest
    @MethodSource("useCaseRejections")
    void useCaseExceptionFromTheChatCallIsNotLoggedAsAFailure(RuntimeException rejection, int status, String code,
            String title, String detail, List<Map<String, String>> details) throws Throwable {
        when(response.content()).thenThrow(rejection);
        List<ILoggingEvent> logEvents = new ArrayList<>();

        logOf(() -> assertThat(problem(chat(FIRST_ID, "Where is my book?"), status)).isEqualTo(Map.of(
                "title", title,
                "status", status,
                "detail", detail,
                "instance", "/assistant/chat",
                "code", code,
                "details", details)), logEvents);

        assertThat(logEvents).as("log lines of AssistantCommands")
                .noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.WARN));
        verify(chatClient).prompt();
    }

    /** Runs the action with every event of the {@link AssistantCommands} logger collected into the given list. */
    private static void logOf(ThrowingCallable action, List<ILoggingEvent> logEvents) throws Throwable {
        Logger commandsLogger = (Logger) LoggerFactory.getLogger(AssistantCommands.class);
        Level originalLevel = commandsLogger.getLevel();
        boolean originalAdditive = commandsLogger.isAdditive();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        commandsLogger.setLevel(Level.TRACE);
        commandsLogger.setAdditive(false);
        commandsLogger.addAppender(appender);
        try {
            action.call();
        } finally {
            commandsLogger.detachAppender(appender);
            commandsLogger.setLevel(originalLevel);
            commandsLogger.setAdditive(originalAdditive);
            logEvents.addAll(appender.list);
        }
    }

    /** The same endpoint while no key is configured: its context holds the offline chat model, not a mock. */
    @Nested
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    @WebMvcTest(AssistantCommands.class)
    @Import({AssistantRateLimiter.class, LimiterSettings.class, OfflineChatModel.class})
    @MockitoBean(types = {ChatClient.class, ChatMemory.class})
    class Offline {

        @Autowired
        private MockMvc offlineMvc;

        @Autowired
        private ChatModel offlineChatModel;

        @Autowired
        private MutableClock offlineClock;

        @Autowired
        private ChatClient offlineChatClient;

        @BeforeEach
        void startANewMinute() {
            offlineClock.advance(Duration.ofSeconds(61));
        }

        @Test
        void contextHoldsTheOfflineChatModel() {
            assertThat(offlineChatModel).isInstanceOf(OfflineChatModel.class);
        }

        @Test
        void offlineAnswers503() throws Exception {
            ResultActions result = send(offlineMvc, body(FIRST_ID, "Do you have Persuasion?"));

            assertThat(problem(result, 503)).isEqualTo(OFFLINE);
            verifyNoInteractions(offlineChatClient);
        }

        @Test
        void storefrontGets503AsJson() throws Exception {
            ResultActions result = sendFromStorefront(offlineMvc, body(SECOND_ID, "Anything by Austen?"));

            assertThat(storefrontProblem(result, 503)).isEqualTo(OFFLINE);
            verifyNoInteractions(offlineChatClient);
        }

        @Test
        void offlineBeatsTheLimiter() throws Exception {
            for (int request = 1; request <= 25; request++) {
                ResultActions result = send(offlineMvc, body(FIRST_ID, "Question " + request));
                assertThat(problem(result, 503)).as("request %d", request).isEqualTo(OFFLINE);
            }
            verifyNoInteractions(offlineChatClient);
        }

        @Test
        void validationBeatsOffline() throws Exception {
            assertThat(problem(send(offlineMvc, body(FIRST_ID, "  ")), 400)).isEqualTo(validationProblem(List.of(
                    Map.of("field", "message", "message", "must not be blank"))));
            assertThat(problem(send(offlineMvc, body(FIRST_ID, "h".repeat(1001))), 400)).isEqualTo(
                    validationProblem(List.of(
                            Map.of("field", "message", "message", "must be at most 1000 characters"))));
            verifyNoInteractions(offlineChatClient);
        }
    }

    /**
     * Applies the advisor settings the endpoint gave to a recording spec and returns the parameters they set. Adding
     * an advisor per request, or setting a parameter twice, fails the test.
     */
    private static Map<String, Object> advisorParameters(Consumer<AdvisorSpec> settings) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        settings.accept(new AdvisorSpec() {

            @Override
            public AdvisorSpec param(String key, Object value) {
                assertThat(parameters).as("parameter %s set once", key).doesNotContainKey(key);
                parameters.put(key, value);
                return this;
            }

            @Override
            public AdvisorSpec params(Map<String, Object> values) {
                values.forEach(this::param);
                return this;
            }

            @Override
            public AdvisorSpec advisors(Advisor... advisors) {
                throw new AssertionError("the chat client's own advisors are enough");
            }

            @Override
            public AdvisorSpec advisors(List<Advisor> advisors) {
                throw new AssertionError("the chat client's own advisors are enough");
            }
        });
        return parameters;
    }

    static String body(UUID conversationId, String message) {
        return "{\"conversationId\":\"" + conversationId + "\",\"message\":\"" + message + "\"}";
    }

    /** The 400 problem of a body that breaks bean validation, with the given details. */
    static Map<String, Object> validationProblem(List<Map<String, String>> details) {
        return Map.of(
                "title", "Validation failed",
                "status", 400,
                "detail", "The request body is not valid",
                "instance", "/assistant/chat",
                "code", "validation-failed",
                "details", details);
    }

    private ResultActions chat(UUID conversationId, String message) throws Exception {
        return post(body(conversationId, message));
    }

    private ResultActions post(String json) throws Exception {
        return send(mockMvc, json);
    }

    /** Posts the JSON to the chat endpoint without an Accept header. */
    static ResultActions send(MockMvc mvc, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/assistant/chat").contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    /** Posts the JSON to the chat endpoint as the storefront does, with its Accept header. */
    static ResultActions sendFromStorefront(MockMvc mvc, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/assistant/chat").contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT, STOREFRONT_ACCEPT)
                .content(json));
    }

    /** Asserts the status and a JSON content type the storefront accepts, and returns the body as a JSON map. */
    static Map<String, Object> storefrontProblem(ResultActions result, int status) throws Exception {
        result.andExpect(status().is(status));
        assertThat(result.andReturn().getResponse().getContentType())
                .isIn("application/json", "application/problem+json");
        assertNoTrace(result);
        return json(result);
    }

    static Map<String, Object> json(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$");
    }

    /** Asserts the status and the problem content type, and returns the problem detail as a JSON map. */
    static Map<String, Object> problem(ResultActions result, int status) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(content().contentType("application/problem+json"));
        Map<String, Object> problem = json(result);
        assertNoTrace(result);
        return problem;
    }

    /** A problem body never carries a stack trace or the name of an exception. */
    static void assertNoTrace(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(json(result)).doesNotContainKey("trace");
        assertThat(body).doesNotContain("Exception").doesNotContain("\tat ").doesNotContain("\\tat ");
    }
}
