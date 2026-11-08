package nl.openvalue.distributor.catalog;

import java.time.LocalDate;
import java.util.List;

/**
 * One entry of {@code catalog/titles.json}. Classics carry a fixed {@code releaseDate}; new releases carry
 * {@code releasedDaysAgo} instead, so the seed never ages. Exactly one of the two is set.
 */
public record TitleSeed(
        String ean,
        String titleName,
        List<Contributor> contributors,
        List<String> subjectCodes,
        Format format,
        int listPriceCents,
        String currency,
        Availability availability,
        int warehouseQuantity,
        LocalDate releaseDate,
        Integer releasedDaysAgo) {
}
