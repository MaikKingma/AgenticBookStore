package nl.openvalue.bookstore.data;

import nl.openvalue.bookstore.domaininteraction.BookDTO;

/** Maps between {@link BookDTO} and {@link BookJPA}. Id and ISBN are written only when an entity is created. */
final class BookJPAMapper {

    private BookJPAMapper() {
    }

    static BookJPA toNewEntity(BookDTO dto) {
        BookJPA entity = new BookJPA(dto.id(), dto.isbn());
        copyInto(dto, entity);
        return entity;
    }

    /**
     * Copies every changeable field into the entity. The authors are replaced only when they differ, so an unchanged
     * book leaves the collection, and with it the version, untouched.
     */
    static void copyInto(BookDTO dto, BookJPA entity) {
        entity.setTitle(dto.title());
        if (!entity.getAuthors().equals(dto.authors())) {
            entity.replaceAuthors(dto.authors());
        }
        entity.setGenre(dto.genre());
        entity.setPriceAmount(dto.priceAmount());
        entity.setPriceCurrency(dto.priceCurrency());
        entity.setPublicationYear(dto.publicationYear());
        entity.setStatus(dto.status());
    }

    static BookDTO toDTO(BookJPA entity) {
        return new BookDTO(entity.getId(), entity.getIsbn(), entity.getTitle(), entity.getAuthors(),
                entity.getGenre(), entity.getPriceAmount(), entity.getPriceCurrency(), entity.getPublicationYear(),
                entity.getStatus());
    }
}
