package nl.openvalue.bookstore.domaininteraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The four use-case exceptions share no base class, so every behaviour is checked once per class. Each case below
 * builds one of the four classes and exposes its accessors as suppliers, so a check always reads the live value.
 */
class UseCaseExceptionsTest {

    private static final String CODE = "some-code";
    private static final String TITLE = "Some title";
    private static final String DETAIL = "Something specific went wrong";

    private interface ThreeArgumentConstructor {
        Subject create(String code, String title, String detail);
    }

    private interface FourArgumentConstructor {
        Subject create(String code, String title, String detail, List<Map<String, String>> details);
    }

    private record Subject(RuntimeException exception, Supplier<String> code, Supplier<String> title,
            Supplier<List<Map<String, String>>> details) {
    }

    private record UseCase(Class<? extends RuntimeException> type, ThreeArgumentConstructor withoutDetails,
            FourArgumentConstructor withDetails) {
    }

    private static Stream<Arguments> useCaseExceptions() {
        return Stream.of(
                named("NotFoundException", new UseCase(NotFoundException.class,
                        (c, t, d) -> subject(new NotFoundException(c, t, d)),
                        (c, t, d, l) -> subject(new NotFoundException(c, t, d, l)))),
                named("ConflictException", new UseCase(ConflictException.class,
                        (c, t, d) -> subject(new ConflictException(c, t, d)),
                        (c, t, d, l) -> subject(new ConflictException(c, t, d, l)))),
                named("InvalidInputException", new UseCase(InvalidInputException.class,
                        (c, t, d) -> subject(new InvalidInputException(c, t, d)),
                        (c, t, d, l) -> subject(new InvalidInputException(c, t, d, l)))),
                named("UnavailableException", new UseCase(UnavailableException.class,
                        (c, t, d) -> subject(new UnavailableException(c, t, d)),
                        (c, t, d, l) -> subject(new UnavailableException(c, t, d, l)))));
    }

    private static Arguments named(String name, UseCase useCase) {
        return Arguments.of(Named.of(name, useCase));
    }

    private static Subject subject(NotFoundException e) {
        return new Subject(e, e::code, e::title, e::details);
    }

    private static Subject subject(ConflictException e) {
        return new Subject(e, e::code, e::title, e::details);
    }

    private static Subject subject(InvalidInputException e) {
        return new Subject(e, e::code, e::title, e::details);
    }

    private static Subject subject(UnavailableException e) {
        return new Subject(e, e::code, e::title, e::details);
    }

    private static List<Map<String, String>> sampleDetails() {
        List<Map<String, String>> details = new ArrayList<>();
        details.add(Map.of("field", "isbn", "message", "is not valid"));
        return details;
    }

    @ParameterizedTest
    @MethodSource("useCaseExceptions")
    void isAPublicNonFinalRuntimeExceptionWithoutACommonBase(UseCase useCase) {
        assertThat(useCase.type().getSuperclass()).isEqualTo(RuntimeException.class);
        assertThat(Modifier.isPublic(useCase.type().getModifiers())).isTrue();
        assertThat(Modifier.isFinal(useCase.type().getModifiers())).isFalse();
    }

    @ParameterizedTest
    @MethodSource("useCaseExceptions")
    void carriesCodeTitleAndDetailAsTheMessage(UseCase useCase) {
        Subject subject = useCase.withoutDetails().create(CODE, TITLE, DETAIL);

        assertThat(subject.code().get()).isEqualTo(CODE);
        assertThat(subject.title().get()).isEqualTo(TITLE);
        assertThat(subject.exception().getMessage()).isEqualTo(DETAIL);
    }

    @ParameterizedTest
    @MethodSource("useCaseExceptions")
    void hasEmptyUnmodifiableDetailsByDefault(UseCase useCase) {
        Subject subject = useCase.withoutDetails().create(CODE, TITLE, DETAIL);

        assertThat(subject.details().get()).isEmpty();
        assertThatThrownBy(() -> subject.details().get().add(Map.of("a", "b")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("useCaseExceptions")
    void carriesTheGivenDetailsInOrderAndUnmodifiable(UseCase useCase) {
        List<Map<String, String>> given = sampleDetails();
        given.add(Map.of("field", "price", "message", "is too high"));

        Subject subject = useCase.withDetails().create(CODE, TITLE, DETAIL, given);

        assertThat(subject.code().get()).isEqualTo(CODE);
        assertThat(subject.title().get()).isEqualTo(TITLE);
        assertThat(subject.exception().getMessage()).isEqualTo(DETAIL);
        assertThat(subject.details().get()).containsExactlyElementsOf(given);
        assertThatThrownBy(() -> subject.details().get().add(Map.of("a", "b")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @MethodSource("useCaseExceptions")
    void copiesTheGivenDetailsSoLaterChangesToTheSourceListDoNotShowThrough(UseCase useCase) {
        List<Map<String, String>> given = sampleDetails();
        Subject subject = useCase.withDetails().create(CODE, TITLE, DETAIL, given);

        given.add(Map.of("field", "added", "message", "after construction"));
        given.remove(0);

        assertThat(subject.details().get()).containsExactly(Map.of("field", "isbn", "message", "is not valid"));
    }

    @ParameterizedTest
    @MethodSource("useCaseExceptions")
    void rejectsNullDetails(UseCase useCase) {
        assertThatThrownBy(() -> useCase.withDetails().create(CODE, TITLE, DETAIL, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void bookNotFoundIsANotFoundExceptionWithItsCodeTitleAndIdInTheMessage() {
        UUID id = UUID.fromString("0b8f6a52-6d4e-4a53-9f43-2f1a8f3b7c10");

        BookDataService.BookNotFoundException exception = new BookDataService.BookNotFoundException(id);

        assertThat(exception).isInstanceOf(NotFoundException.class);
        assertThat(exception.code()).isEqualTo("book-not-found");
        assertThat(exception.title()).isEqualTo("Book not found");
        assertThat(exception.getMessage()).isEqualTo("No book with id " + id);
        assertThat(exception.details()).isEmpty();
        assertThat(Modifier.isFinal(BookDataService.BookNotFoundException.class.getModifiers())).isTrue();
    }

    @Test
    void isbnAlreadyExistsIsAConflictExceptionWithItsCodeTitleAndIsbnInTheMessage() {
        BookDataService.IsbnAlreadyExistsException exception =
                new BookDataService.IsbnAlreadyExistsException("9780141439518");

        assertThat(exception).isInstanceOf(ConflictException.class);
        assertThat(exception.code()).isEqualTo("isbn-already-exists");
        assertThat(exception.title()).isEqualTo("ISBN already in catalogue");
        assertThat(exception.getMessage())
                .isEqualTo("A book with ISBN 9780141439518 is already in the catalogue");
        assertThat(exception.details()).isEmpty();
        assertThat(Modifier.isFinal(BookDataService.IsbnAlreadyExistsException.class.getModifiers())).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "9780141439518, The distributor has no title with ISBN 9780141439518",
            "9789900010050, The distributor has no title with ISBN 9789900010050"})
    void titleNotFoundAtDistributorIsANotFoundWithItsCodeTitleAndIsbnInTheMessage(String isbn, String detail) {
        DistributorAppService.TitleNotFoundAtDistributorException exception =
                new DistributorAppService.TitleNotFoundAtDistributorException(isbn);

        assertThat(exception.getClass().getSuperclass()).isEqualTo(NotFoundException.class);
        assertThat(exception.code()).isEqualTo("title-not-found-at-distributor");
        assertThat(exception.title()).isEqualTo("Title not found at distributor");
        assertThat(exception.getMessage()).isEqualTo(detail);
        assertThat(exception.details()).isNotNull().isEmpty();
        assertThat(Modifier.isFinal(DistributorAppService.TitleNotFoundAtDistributorException.class.getModifiers()))
                .isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "9789900010678, The title with ISBN 9789900010678 cannot be ordered from the distributor",
            "9789900010272, The title with ISBN 9789900010272 cannot be ordered from the distributor"})
    void titleNotOrderableIsAnInvalidInputWithItsCodeTitleAndIsbnInTheMessage(String isbn, String detail) {
        DistributorAppService.TitleNotOrderableException exception =
                new DistributorAppService.TitleNotOrderableException(isbn);

        assertThat(exception.getClass().getSuperclass()).isEqualTo(InvalidInputException.class);
        assertThat(exception.code()).isEqualTo("title-not-orderable");
        assertThat(exception.title()).isEqualTo("Title not orderable");
        assertThat(exception.getMessage()).isEqualTo(detail);
        assertThat(exception.details()).isNotNull().isEmpty();
        assertThat(Modifier.isFinal(DistributorAppService.TitleNotOrderableException.class.getModifiers())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Connection refused", "Distributor answered with status 503"})
    void distributorUnavailableIsAnUnavailableWithItsCodeTitleAndTheGivenDetail(String detail) {
        DistributorAppService.DistributorUnavailableException exception =
                new DistributorAppService.DistributorUnavailableException(detail);

        assertThat(exception.getClass().getSuperclass()).isEqualTo(UnavailableException.class);
        assertThat(exception.code()).isEqualTo("distributor-unavailable");
        assertThat(exception.title()).isEqualTo("Distributor unavailable");
        assertThat(exception.getMessage()).isEqualTo(detail);
        assertThat(exception.details()).isNotNull().isEmpty();
        assertThat(Modifier.isFinal(DistributorAppService.DistributorUnavailableException.class.getModifiers()))
                .isTrue();
    }

    @Test
    void stockItemNotFoundIsANotFoundWithTheBookNotFoundCodeAndTitle() {
        UUID bookId = UUID.fromString("5d0c1c3e-8f7a-4b21-a6d4-3c9e0b7a2f18");

        StockItemDataService.StockItemNotFoundException exception =
                new StockItemDataService.StockItemNotFoundException(bookId);

        assertThat(exception).isInstanceOf(NotFoundException.class);
        assertThat(exception.code()).isEqualTo("book-not-found");
        assertThat(exception.title()).isEqualTo("Book not found");
        assertThat(exception.getMessage()).isNotBlank().contains(bookId.toString());
        assertThat(exception.details()).isEmpty();
        assertThatThrownBy(() -> exception.details().add(Map.of("a", "b")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(Modifier.isFinal(StockItemDataService.StockItemNotFoundException.class.getModifiers())).isTrue();
    }

    @Test
    void orderNotFoundIsANotFoundWithTheOrderNotFoundCodeAndTitle() {
        UUID id = UUID.fromString("2c9a7f10-4b3d-4e8a-9d52-6a1f0c3b8e77");

        OrderDataService.OrderNotFoundException exception = new OrderDataService.OrderNotFoundException(id);

        assertThat(exception).isInstanceOf(NotFoundException.class);
        assertThat(exception.code()).isEqualTo("order-not-found");
        assertThat(exception.title()).isEqualTo("Order not found");
        assertThat(exception.getMessage()).isNotBlank().contains(id.toString());
        assertThat(exception.details()).isEmpty();
        assertThatThrownBy(() -> exception.details().add(Map.of("a", "b")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(Modifier.isFinal(OrderDataService.OrderNotFoundException.class.getModifiers())).isTrue();
    }

    @Test
    void bookNotOrderableIsAConflictWithTheBookNotOrderableCodeAndTitle() {
        UUID id = UUID.fromString("6b2e9d41-3a7c-4f58-8e10-d4c5a9b73f26");

        BookNotOrderableException exception = new BookNotOrderableException(id);

        assertThat(exception).isInstanceOf(ConflictException.class);
        assertThat(exception.code()).isEqualTo("book-not-orderable");
        assertThat(exception.title()).isEqualTo("Book not orderable");
        assertThat(exception.getMessage()).isNotBlank().contains(id.toString());
        assertThat(exception.details()).isEmpty();
        assertThatThrownBy(() -> exception.details().add(Map.of("a", "b")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(Modifier.isFinal(BookNotOrderableException.class.getModifiers())).isTrue();
        assertThat(Modifier.isPublic(BookNotOrderableException.class.getModifiers())).isTrue();
    }
}
