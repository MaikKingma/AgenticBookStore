package nl.openvalue.bookstore.domaininteraction;

import java.math.BigDecimal;
import java.util.List;

/**
 * A title as the distributor offers it, already translated to bookstore terms: JDK types only. The genre is the name
 * of a bookstore genre constant. The recommended price has scale 2. The currency is always EUR, because the port
 * rejects every other currency. The authors keep their order; {@code orderable} is false when the distributor no
 * longer supplies the title.
 */
public record DistributorTitleDTO(String isbn, String title, List<String> authors, String genre,
        BigDecimal recommendedPrice, String currency, int publicationYear, boolean orderable) {
}
