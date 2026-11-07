package nl.openvalue.bookstore.domain;

/** Thrown when the publication year of a book is before the earliest or after the latest allowed year. */
public class PublicationYearOutOfRangeException extends RuntimeException {

    public PublicationYearOutOfRangeException(int year, int earliest, int latest) {
        super("Publication year " + year + " is out of range: it must be between " + earliest + " and " + latest);
    }
}
