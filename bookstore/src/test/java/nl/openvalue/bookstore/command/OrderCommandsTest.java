package nl.openvalue.bookstore.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import nl.openvalue.bookstore.domaininteraction.BookDataService;
import nl.openvalue.bookstore.domaininteraction.BookNotOrderableException;
import nl.openvalue.bookstore.domaininteraction.ConflictException;
import nl.openvalue.bookstore.domaininteraction.InvalidInputException;
import nl.openvalue.bookstore.domaininteraction.OrderDataService;
import nl.openvalue.bookstore.domaininteraction.OrderFlow;
import nl.openvalue.bookstore.domaininteraction.PlaceOrderLineDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(OrderCommands.class)
class OrderCommandsTest {

    private static final String EMAIL = "Reader.One@Example.COM";
    private static final String OTHER_EMAIL = "SECOND@Shop.Example.Org";

    // Three books whose ids (5, 1, 3) and whose requested quantities (4, 9, 2) are sorted in neither direction.
    private static final UUID BOOK_A = UUID.fromString("5a0d3c71-8e24-4b19-a6f3-c2d9e1b47a58");
    private static final UUID BOOK_B = UUID.fromString("1c7e92f4-3b86-4d05-8a1e-7f4b0d6e2c93");
    private static final UUID BOOK_C = UUID.fromString("3e4f8a26-d1b7-4c92-b5a0-9e6d3f1c8b74");
    // Two more books for the duplicate test: the ids in request order (4, 2, 4) are sorted in neither direction.
    private static final UUID BOOK_D = UUID.fromString("4b2e7d93-0a1c-4f58-9d36-e8b1c5a07f24");
    private static final UUID BOOK_E = UUID.fromString("2d9a6c40-7e13-4b85-a2f7-3c0d8e19b6a5");

    private static final UUID SHIP_ID = UUID.fromString("c4a8e1f2-6b39-4d70-9e52-1f8d3a6b0c47");
    private static final UUID OTHER_SHIP_ID = UUID.fromString("e7b30d95-2a6c-4f18-b4d1-8c5e9a0f3d62");
    private static final UUID THIRD_SHIP_ID = UUID.fromString("0f6d2b83-91e4-4a7c-8d35-b2c7e4a1f906");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderFlow flow;

    // --- place: accepted ---

    @Test
    void placeReturns202WithTheOrderIdAndPassesTheLinesInOrder() throws Exception {
        List<PlaceOrderLineDTO> expected = List.of(
                new PlaceOrderLineDTO(BOOK_A, 4), new PlaceOrderLineDTO(BOOK_B, 9), new PlaceOrderLineDTO(BOOK_C, 2));
        when(flow.placeOrder(eq(EMAIL), eq(expected)))
                .thenReturn(UUID.fromString("9b1f6e20-4c7d-4a83-8e51-d0a3f27c6b19"));

        postPlace(placeBody(EMAIL, line(BOOK_A, 4), line(BOOK_B, 9), line(BOOK_C, 2)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("9b1f6e20-4c7d-4a83-8e51-d0a3f27c6b19"))
                .andExpect(jsonPath("$.length()").value(1));

        ArgumentCaptor<List<PlaceOrderLineDTO>> lines = ArgumentCaptor.captor();
        verify(flow).placeOrder(eq("Reader.One@Example.COM"), lines.capture());
        assertThat(lines.getValue()).containsExactly(
                new PlaceOrderLineDTO(UUID.fromString("5a0d3c71-8e24-4b19-a6f3-c2d9e1b47a58"), 4),
                new PlaceOrderLineDTO(UUID.fromString("1c7e92f4-3b86-4d05-8a1e-7f4b0d6e2c93"), 9),
                new PlaceOrderLineDTO(UUID.fromString("3e4f8a26-d1b7-4c92-b5a0-9e6d3f1c8b74"), 2));
        verifyNoMoreInteractions(flow);
    }

    @Test
    void placePassesDuplicateLinesUnmerged() throws Exception {
        List<PlaceOrderLineDTO> expected = List.of(
                new PlaceOrderLineDTO(BOOK_D, 3), new PlaceOrderLineDTO(BOOK_E, 7), new PlaceOrderLineDTO(BOOK_D, 5));
        when(flow.placeOrder(eq(OTHER_EMAIL), eq(expected)))
                .thenReturn(UUID.fromString("2a7c5e19-b0d4-4683-9f2e-6d1b8a3c4e75"));

        postPlace(placeBody(OTHER_EMAIL, line(BOOK_D, 3), line(BOOK_E, 7), line(BOOK_D, 5)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("2a7c5e19-b0d4-4683-9f2e-6d1b8a3c4e75"))
                .andExpect(jsonPath("$.length()").value(1));

        ArgumentCaptor<List<PlaceOrderLineDTO>> lines = ArgumentCaptor.captor();
        verify(flow).placeOrder(eq("SECOND@Shop.Example.Org"), lines.capture());
        assertThat(lines.getValue()).containsExactly(
                new PlaceOrderLineDTO(UUID.fromString("4b2e7d93-0a1c-4f58-9d36-e8b1c5a07f24"), 3),
                new PlaceOrderLineDTO(UUID.fromString("2d9a6c40-7e13-4b85-a2f7-3c0d8e19b6a5"), 7),
                new PlaceOrderLineDTO(UUID.fromString("4b2e7d93-0a1c-4f58-9d36-e8b1c5a07f24"), 5));
        verifyNoMoreInteractions(flow);
    }

    @Test
    void placeAcceptsA254CharacterEmailAndPassesItUnchanged() throws Exception {
        String email = "Z".repeat(242) + "@example.com";
        assertThat(email).hasSize(254);

        postPlace(placeBody(email, line(BOOK_C, 6)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> sent = ArgumentCaptor.captor();
        verify(flow).placeOrder(sent.capture(), eq(List.of(new PlaceOrderLineDTO(BOOK_C, 6))));
        assertThat(sent.getValue()).hasSize(254).isEqualTo("Z".repeat(242) + "@example.com");
        verifyNoMoreInteractions(flow);
    }

    // The tab is written as the JSON escape, so the request body stays valid JSON; the flow gets the real tab.
    @ParameterizedTest
    @ValueSource(strings = {" reader@example.com", "reader@example.com ", "\\treader@example.com", "reader@example.com\\t"})
    void placePassesASpacePaddedEmailToTheFlowUnchanged(String email) throws Exception {
        postPlace(placeBody(email, line(BOOK_A, 4)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> sent = ArgumentCaptor.captor();
        verify(flow).placeOrder(sent.capture(), eq(List.of(new PlaceOrderLineDTO(BOOK_A, 4))));
        assertThat(sent.getValue()).isEqualTo(email.replace("\\t", "\t")).hasSize(19);
        verifyNoMoreInteractions(flow);
    }

    @Test
    void placeAcceptsA254CharacterEmailWithOneSpaceOnEachSideAndPassesItUnchanged() throws Exception {
        String address = "Z".repeat(242) + "@example.com";
        assertThat(address).hasSize(254);

        postPlace(placeBody(" " + address + " ", line(BOOK_C, 6)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> sent = ArgumentCaptor.captor();
        verify(flow).placeOrder(sent.capture(), eq(List.of(new PlaceOrderLineDTO(BOOK_C, 6))));
        assertThat(sent.getValue()).hasSize(256).isEqualTo(" " + address + " ");
        verifyNoMoreInteractions(flow);
    }

    @Test
    void placeAcceptsALeadingNonAsciiSpaceAndPassesItUnchanged() throws Exception {
        String email = Character.toString(0x2003) + "a@b.co";

        postPlace(placeBody(email, line(BOOK_E, 8)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> sent = ArgumentCaptor.captor();
        verify(flow).placeOrder(sent.capture(), eq(List.of(new PlaceOrderLineDTO(BOOK_E, 8))));
        assertThat(sent.getValue()).hasSize(7).startsWith(Character.toString(0x2003)).endsWith("a@b.co");
        verifyNoMoreInteractions(flow);
    }

    @Test
    void placeAcceptsACharacterJustAboveTheSpaceAndPassesItUnchanged() throws Exception {
        postPlace(placeBody("!a@b.co", line(BOOK_B, 5)))
                .andExpect(status().isAccepted());

        verify(flow).placeOrder("!a@b.co", List.of(new PlaceOrderLineDTO(BOOK_B, 5)));
        verifyNoMoreInteractions(flow);
    }

    // U+001F is a control character below the space: String.trim() strips it, so it may pad the address.
    @Test
    void placePassesAnEmailPaddedWithAControlCharacterToTheFlowUnchanged() throws Exception {
        String padded = Character.toString(0x1F) + "reader@example.com" + Character.toString(0x1F);

        postPlace(placeBody("\\u001f" + "reader@example.com" + "\\u001f", line(BOOK_A, 4)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> sent = ArgumentCaptor.captor();
        verify(flow).placeOrder(sent.capture(), eq(List.of(new PlaceOrderLineDTO(BOOK_A, 4))));
        assertThat(sent.getValue()).isEqualTo(padded).hasSize(20);
        verifyNoMoreInteractions(flow);
    }

    // The length limit counts UTF-16 chars, as String.trim().length() and EmailAddress do: 121 x U+1F600 is 242 chars.
    @Test
    void placeAcceptsA254CharacterEmailMadeOfSurrogatePairsAndPassesItUnchanged() throws Exception {
        String email = Character.toString(0x1F600).repeat(121) + "@example.com";
        assertThat(email).hasSize(254);

        postPlace(placeBody("\\ud83d\\ude00".repeat(121) + "@example.com", line(BOOK_C, 6)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> sent = ArgumentCaptor.captor();
        verify(flow).placeOrder(sent.capture(), eq(List.of(new PlaceOrderLineDTO(BOOK_C, 6))));
        assertThat(sent.getValue()).isEqualTo(email);
        verifyNoMoreInteractions(flow);
    }

    // U+2028 is a line terminator for the regex dot; it inside the address must still be accepted.
    @Test
    void placeAcceptsALineSeparatorInsideTheAddressAndPassesItUnchanged() throws Exception {
        String email = "a" + Character.toString(0x2028) + "b@c.de";

        postPlace(placeBody("a\\u2028b@c.de", line(BOOK_B, 5)))
                .andExpect(status().isAccepted());

        verify(flow).placeOrder(email, List.of(new PlaceOrderLineDTO(BOOK_B, 5)));
        verifyNoMoreInteractions(flow);
    }

    @Test
    void placeAcceptsASingleLine() throws Exception {
        postPlace(placeBody(EMAIL, line(BOOK_A, 6)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<List<PlaceOrderLineDTO>> lines = ArgumentCaptor.captor();
        verify(flow).placeOrder(eq(EMAIL), lines.capture());
        assertThat(lines.getValue()).containsExactly(new PlaceOrderLineDTO(BOOK_A, 6));
        verifyNoMoreInteractions(flow);
    }

    @Test
    void placeAccepts20LinesAndPassesAll20() throws Exception {
        List<String> lines = new ArrayList<>();
        List<PlaceOrderLineDTO> expected = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            UUID bookId = UUID.fromString("%08x-aaaa-4bbb-8ccc-%012x".formatted(i + 1, 7 * i + 3));
            int quantity = (i * 3) % 10 + 1;
            lines.add(line(bookId, quantity));
            expected.add(new PlaceOrderLineDTO(bookId, quantity));
        }

        postPlace(placeBody(EMAIL, lines.toArray(String[]::new)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<List<PlaceOrderLineDTO>> passed = ArgumentCaptor.captor();
        verify(flow).placeOrder(eq(EMAIL), passed.capture());
        assertThat(passed.getValue()).hasSize(20).containsExactlyElementsOf(expected);
        verifyNoMoreInteractions(flow);
    }

    @Test
    void placeAcceptsLineQuantities1And10() throws Exception {
        postPlace(placeBody(OTHER_EMAIL, line(BOOK_D, 1), line(BOOK_E, 10)))
                .andExpect(status().isAccepted());

        ArgumentCaptor<List<PlaceOrderLineDTO>> lines = ArgumentCaptor.captor();
        verify(flow).placeOrder(eq(OTHER_EMAIL), lines.capture());
        assertThat(lines.getValue())
                .containsExactly(new PlaceOrderLineDTO(BOOK_D, 1), new PlaceOrderLineDTO(BOOK_E, 10));
        verifyNoMoreInteractions(flow);
    }

    // --- place: customer e-mail rejected ---

    @Test
    void placeRejectsAMissingCustomerEmailWith400() throws Exception {
        ResultActions result = postPlace("{\"lines\":[%s]}".formatted(line(BOOK_A, 2)));

        expectRejected(result, "customerEmail");
    }

    @Test
    void placeRejectsAMissingEmailAndMissingLinesWith400ListingBoth() throws Exception {
        ResultActions result = postPlace("{}");

        expectRejected(result, "customerEmail", "lines");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "reader",
            "reader@example",
            "a b@c.de",
            "a@b c.de",
            "a@b.c d",
            "a\\tb@c.de",
            "a@b@c.de",
            "a@b.c@d",
            "@example.com",
            "reader@.com",
            "reader@example."
    })
    void placeRejectsAMalformedCustomerEmailWith400(String email) throws Exception {
        ResultActions result = postPlace(placeBody(email, line(BOOK_A, 2)));

        expectRejected(result, "customerEmail");
    }

    @Test
    void placeRejectsACustomerEmailOf255CharactersWith400() throws Exception {
        String email = "Z".repeat(243) + "@example.com";
        assertThat(email).hasSize(255);

        ResultActions result = postPlace(placeBody(email, line(BOOK_A, 2)));

        expectRejected(result, "customerEmail");
    }

    @Test
    void placeRejectsASpacePaddedCustomerEmailWhoseTrimmedLengthIs255With400() throws Exception {
        String address = "Z".repeat(243) + "@example.com";
        assertThat(address).hasSize(255);

        ResultActions result = postPlace(placeBody(" " + address + " ", line(BOOK_A, 2)));

        expectRejected(result, "customerEmail");
    }

    // 122 x U+1F600 + "@example.com" is 134 code points but 256 UTF-16 chars, over the limit.
    @Test
    void placeRejectsA256CharacterCustomerEmailMadeOfSurrogatePairsWith400() throws Exception {
        String email = Character.toString(0x1F600).repeat(122) + "@example.com";
        assertThat(email).hasSize(256);

        ResultActions result = postPlace(placeBody("\\ud83d\\ude00".repeat(122) + "@example.com", line(BOOK_A, 2)));

        expectRejected(result, "customerEmail");
    }

    // U+2003 is above the space, so String.trim() keeps it: the trimmed address is 255 characters long.
    @Test
    void placeRejectsANonAsciiSpaceBeforeA254CharacterCustomerEmailWith400() throws Exception {
        String address = "Z".repeat(242) + "@example.com";
        assertThat(address).hasSize(254);

        ResultActions result = postPlace(placeBody(Character.toString(0x2003) + address, line(BOOK_A, 2)));

        expectRejected(result, "customerEmail");
    }

    // U+2003 is above the space: String.trim() keeps it, so it cannot pad the address next to an ASCII space.
    @Test
    void placeRejectsANonAsciiSpaceNextToAnAsciiSpaceAroundTheAddressWith400() throws Exception {
        String em = Character.toString(0x2003);

        for (String email : List.of(em + " a@b.co", "a@b.co " + em)) {
            expectRejected(postPlace(placeBody(email, line(BOOK_A, 2))), "customerEmail");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "\\t", " \\t "})
    void placeRejectsABlankCustomerEmailWith400(String email) throws Exception {
        ResultActions result = postPlace(placeBody(email, line(BOOK_A, 2)));

        expectRejected(result, "customerEmail");
    }

    @Test
    void placeRejectsAnEmailBreakingBothTheLengthAndThePatternWith400ListingItTwice() throws Exception {
        String email = "x".repeat(248) + " b@c.de";
        assertThat(email).hasSize(255);

        ResultActions result = postPlace(placeBody(email, line(BOOK_A, 2)));

        expectRejected(result, "customerEmail", "customerEmail");
    }

    // --- place: lines rejected ---

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"customerEmail\":\"a@b.co\"}",
            "{\"customerEmail\":\"a@b.co\",\"lines\":null}",
            "{\"customerEmail\":\"a@b.co\",\"lines\":[]}"
    })
    void placeRejectsMissingOrEmptyLinesWith400(String body) throws Exception {
        ResultActions result = postPlace(body);

        expectRejected(result, "lines");
    }

    @Test
    void placeRejects21LinesWith400() throws Exception {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            lines.add(line(UUID.fromString("%08x-bbbb-4ccc-8ddd-%012x".formatted(i + 1, 5 * i + 1)), i % 10 + 1));
        }

        ResultActions result = postPlace(placeBody(EMAIL, lines.toArray(String[]::new)));

        expectRejected(result, "lines");
    }

    @Test
    void placeRejectsANullLineWith400() throws Exception {
        ResultActions result = postPlace(
                "{\"customerEmail\":\"a@b.co\",\"lines\":[%s,null]}".formatted(line(BOOK_A, 2)));

        expectRejected(result, "lines[1]");
    }

    @Test
    void placeRejectsALineWithoutBookIdWith400() throws Exception {
        ResultActions result = postPlace(placeBody(EMAIL, line(BOOK_A, 2), "{\"quantity\":5}", line(BOOK_C, 3)));

        expectRejected(result, "lines[1].bookId");
    }

    @Test
    void placeRejectsAMissingLineQuantityWith400() throws Exception {
        ResultActions result = postPlace(placeBody(EMAIL, line(BOOK_A, 2),
                "{\"bookId\":\"1c7e92f4-3b86-4d05-8a1e-7f4b0d6e2c93\"}", line(BOOK_C, 3)));

        expectRejected(result, "lines[1].quantity");
    }

    @Test
    void placeListsEveryFaultyLineFieldInOrder() throws Exception {
        ResultActions result = postPlace(placeBody(EMAIL,
                "{\"bookId\":\"%s\",\"quantity\":12}".formatted(BOOK_A),
                line(BOOK_B, 4),
                "{}"));

        expectRejected(result, "lines[2].bookId", "lines[2].quantity");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11})
    void placePassesALineQuantityOutside1To10ToTheFlow(int quantity) throws Exception {
        List<PlaceOrderLineDTO> lines = List.of(new PlaceOrderLineDTO(BOOK_A, quantity));
        doThrow(new InvalidInputException("quantity-out-of-range", "Quantity out of range",
                "The quantity %d is not between 1 and 10".formatted(quantity),
                List.of(Map.of("field", "lines", "message", "The quantity %d is not between 1 and 10".formatted(quantity)))))
                .when(flow).placeOrder(EMAIL, lines);

        ResultActions result = postPlace(placeBody(EMAIL, line(BOOK_A, quantity)));

        verify(flow).placeOrder(EMAIL, lines);
        verifyNoMoreInteractions(flow);
        expectProblem(result, 422, "quantity-out-of-range", "Quantity out of range")
                .andExpect(jsonPath("$.details").value(equalTo(List.of(Map.of("field", "lines", "message",
                        "The quantity %d is not between 1 and 10".formatted(quantity))))));
    }

    // --- place: mapping of use-case exceptions ---

    @Test
    void placeMapsBookNotFoundTo404() throws Exception {
        List<PlaceOrderLineDTO> lines = List.of(new PlaceOrderLineDTO(BOOK_B, 3));
        doThrow(new BookDataService.BookNotFoundException(BOOK_B)).when(flow).placeOrder(EMAIL, lines);

        ResultActions result = postPlace(placeBody(EMAIL, line(BOOK_B, 3)));

        expectProblem(result, 404, "book-not-found", "Book not found")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void placeMapsBookNotOrderableTo409() throws Exception {
        List<PlaceOrderLineDTO> lines = List.of(new PlaceOrderLineDTO(BOOK_C, 7));
        doThrow(new BookNotOrderableException(BOOK_C)).when(flow).placeOrder(OTHER_EMAIL, lines);

        ResultActions result = postPlace(placeBody(OTHER_EMAIL, line(BOOK_C, 7)));

        expectProblem(result, 409, "book-not-orderable", "Book not orderable")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void placeMapsInsufficientStockTo409WithTheShortagesInDetails() throws Exception {
        List<PlaceOrderLineDTO> lines = List.of(new PlaceOrderLineDTO(BOOK_A, 9), new PlaceOrderLineDTO(BOOK_C, 5));
        doThrow(new ConflictException("insufficient-stock", "Insufficient stock",
                "Not enough stock for 2 books",
                List.of(Map.of("bookId", BOOK_C.toString(), "requested", "5", "available", "2"),
                        Map.of("bookId", BOOK_A.toString(), "requested", "9", "available", "4"))))
                .when(flow).placeOrder(EMAIL, lines);

        ResultActions result = postPlace(placeBody(EMAIL, line(BOOK_A, 9), line(BOOK_C, 5)));

        expectProblem(result, 409, "insufficient-stock", "Insufficient stock")
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.details").value(equalTo(List.of(
                        Map.of("bookId", "3e4f8a26-d1b7-4c92-b5a0-9e6d3f1c8b74",
                                "requested", "5", "available", "2"),
                        Map.of("bookId", "5a0d3c71-8e24-4b19-a6f3-c2d9e1b47a58",
                                "requested", "9", "available", "4")))));
    }

    @Test
    void placeMapsQuantityOutOfRangeTo422() throws Exception {
        List<PlaceOrderLineDTO> lines = List.of(new PlaceOrderLineDTO(BOOK_D, 6), new PlaceOrderLineDTO(BOOK_D, 6));
        doThrow(new InvalidInputException("quantity-out-of-range", "Quantity out of range",
                "The quantity 12 is not between 1 and 10",
                List.of(Map.of("field", "lines", "message", "The quantity 12 is not between 1 and 10"))))
                .when(flow).placeOrder(EMAIL, lines);

        ResultActions result = postPlace(placeBody(EMAIL, line(BOOK_D, 6), line(BOOK_D, 6)));

        expectProblem(result, 422, "quantity-out-of-range", "Quantity out of range")
                .andExpect(jsonPath("$.details.length()").value(1))
                .andExpect(jsonPath("$.details").value(equalTo(List.of(
                        Map.of("field", "lines", "message", "The quantity 12 is not between 1 and 10")))));
    }

    // --- ship ---

    @Test
    void shipReturns202WithoutBodyAndPassesTheId() throws Exception {
        postShip(SHIP_ID)
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verify(flow).ship(UUID.fromString("c4a8e1f2-6b39-4d70-9e52-1f8d3a6b0c47"));
        verifyNoMoreInteractions(flow);
    }

    @Test
    void shipMapsOrderNotFoundTo404() throws Exception {
        doThrow(new OrderDataService.OrderNotFoundException(OTHER_SHIP_ID)).when(flow).ship(OTHER_SHIP_ID);

        ResultActions result = postShip(OTHER_SHIP_ID);

        expectProblem(result, 404, "order-not-found", "Order not found")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void shipMapsOrderAlreadyShippedTo409() throws Exception {
        doThrow(new ConflictException("order-already-shipped", "Order already shipped",
                "The order with id " + THIRD_SHIP_ID + " is already shipped"))
                .when(flow).ship(THIRD_SHIP_ID);

        ResultActions result = postShip(THIRD_SHIP_ID);

        expectProblem(result, 409, "order-already-shipped", "Order already shipped")
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    // --- helpers ---

    private ResultActions postPlace(String body) throws Exception {
        return mockMvc.perform(post("/orders/commands/place")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions postShip(UUID id) throws Exception {
        return mockMvc.perform(post("/orders/{id}/commands/ship", id));
    }

    private static String placeBody(String email, String... lines) {
        return "{\"customerEmail\":\"%s\",\"lines\":[%s]}".formatted(email, String.join(",", lines));
    }

    private static String line(UUID bookId, int quantity) {
        return "{\"bookId\":\"%s\",\"quantity\":%d}".formatted(bookId, quantity);
    }

    private ResultActions expectRejected(ResultActions result, String... fields) throws Exception {
        expectValidationFailed(result, fields);
        verifyNoInteractions(flow);
        return result;
    }

    private static ResultActions expectValidationFailed(ResultActions result, String... fields) throws Exception {
        return expectProblem(result, 400, "validation-failed", "Validation failed")
                .andExpect(jsonPath("$.details.length()").value(fields.length))
                .andExpect(jsonPath("$.details[*].field").value(contains(fields)))
                .andExpect(jsonPath("$.details[*].message").value(everyItem(not(emptyOrNullString()))));
    }

    private static ResultActions expectProblem(ResultActions result, int status, String code, String title)
            throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.details").exists());
    }
}
