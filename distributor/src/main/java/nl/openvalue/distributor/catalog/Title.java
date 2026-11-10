package nl.openvalue.distributor.catalog;

import java.time.LocalDate;
import java.util.List;

/**
 * A title as the distributor publishes it on the wire: exactly the ten keys of the catalogue model.
 */
public record Title(
        String ean,
        String titleName,
        List<Contributor> contributors,
        List<String> subjectCodes,
        Format format,
        int listPriceCents,
        String currency,
        Availability availability,
        int warehouseQuantity,
        LocalDate releaseDate) {

    public Title {
        contributors = List.copyOf(contributors);
        subjectCodes = List.copyOf(subjectCodes);
    }
}
