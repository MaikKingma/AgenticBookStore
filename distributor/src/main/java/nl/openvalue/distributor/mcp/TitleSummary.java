package nl.openvalue.distributor.mcp;

import java.util.List;
import java.util.stream.Collectors;
import nl.openvalue.distributor.catalog.Availability;
import nl.openvalue.distributor.catalog.ContributorRole;
import nl.openvalue.distributor.catalog.Title;

/**
 * The compact form of a title in a {@code search_titles} result. {@code authors} holds the names of the AUTHOR
 * contributors in their catalogue order, joined with ", "; editors, translators and illustrators are left out.
 */
public record TitleSummary(
        String ean,
        String titleName,
        String authors,
        List<String> subjectCodes,
        Availability availability,
        int listPriceCents) {

    public static TitleSummary of(Title title) {
        String authors = title.contributors().stream()
                .filter(contributor -> contributor.role() == ContributorRole.AUTHOR)
                .map(contributor -> contributor.name())
                .collect(Collectors.joining(", "));
        return new TitleSummary(title.ean(), title.titleName(), authors, title.subjectCodes(), title.availability(),
                title.listPriceCents());
    }
}
