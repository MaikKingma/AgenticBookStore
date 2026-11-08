package nl.openvalue.distributor.catalog;

import java.util.List;

/**
 * One page of search results. {@code total} counts every title that matched, not only the ones on this page.
 */
public record TitlePage(List<Title> items, int page, int size, int total) {

    public TitlePage {
        items = List.copyOf(items);
    }
}
