package nl.openvalue.bookstore.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GenreTest {

    @Test
    void hasTheTenGenresInSpecOrder() {
        assertThat(Genre.values()).containsExactly(
                Genre.FICTION,
                Genre.SCIENCE_FICTION,
                Genre.FANTASY,
                Genre.MYSTERY,
                Genre.ROMANCE,
                Genre.HISTORY,
                Genre.SCIENCE,
                Genre.TECHNOLOGY,
                Genre.CHILDREN,
                Genre.POETRY);
    }

    @Test
    void parsesAnyCase() {
        assertThat(Genre.parse("fiction")).isEqualTo(Genre.FICTION);
        assertThat(Genre.parse("FICTION")).isEqualTo(Genre.FICTION);
        assertThat(Genre.parse("Poetry")).isEqualTo(Genre.POETRY);
    }

    @Test
    void parsesAHyphenLikeAnUnderscore() {
        assertThat(Genre.parse("science-fiction")).isEqualTo(Genre.SCIENCE_FICTION);
        assertThat(Genre.parse("Science_Fiction")).isEqualTo(Genre.SCIENCE_FICTION);
        assertThat(Genre.parse("SCIENCE_FICTION")).isEqualTo(Genre.SCIENCE_FICTION);
    }

    @Test
    void ignoresSurroundingWhitespace() {
        assertThat(Genre.parse(" fiction ")).isEqualTo(Genre.FICTION);
        assertThat(Genre.parse("\tscience-fiction\n")).isEqualTo(Genre.SCIENCE_FICTION);
    }

    @Test
    void rejectsAnUnknownGenre() {
        assertThatThrownBy(() -> Genre.parse("cookbooks"))
                .isInstanceOfSatisfying(UnknownGenreException.class, e -> {
                    assertThat(e.value()).isEqualTo("cookbooks");
                    assertThat(e.getMessage()).isNotBlank();
                });
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> Genre.parse(null))
                .isInstanceOfSatisfying(UnknownGenreException.class, e -> {
                    assertThat(e.value()).isNull();
                    assertThat(e.getMessage()).isNotBlank();
                });
    }

    @Test
    void rejectsABlankValue() {
        assertThatThrownBy(() -> Genre.parse("   "))
                .isInstanceOfSatisfying(UnknownGenreException.class, e -> {
                    assertThat(e.value()).isEqualTo("   ");
                    assertThat(e.getMessage()).isNotBlank();
                });
        assertThatThrownBy(() -> Genre.parse(""))
                .isInstanceOf(UnknownGenreException.class);
    }
}
