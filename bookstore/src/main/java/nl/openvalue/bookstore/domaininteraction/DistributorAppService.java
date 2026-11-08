package nl.openvalue.bookstore.domaininteraction;

import java.util.Optional;

/**
 * The bookstore's view of the distributor, the external system that holds title data and takes restock orders. It
 * speaks DTOs and JDK types only; the distributor's own wire shapes stay behind it.
 */
public interface DistributorAppService {

    /**
     * Looks up a title by ISBN. The result is empty when the distributor does not know the ISBN.
     *
     * @throws TitleNotOrderableException when the distributor's data cannot be taken over, for example a price in a
     *         currency other than EUR
     * @throws DistributorUnavailableException when the distributor cannot be reached or answers unusably
     */
    Optional<DistributorTitleDTO> findTitle(String isbn);

    /**
     * Asks the distributor to restock a title.
     *
     * @throws TitleNotFoundAtDistributorException when the distributor does not know the ISBN
     * @throws TitleNotOrderableException when the distributor refuses to supply the title
     * @throws DistributorUnavailableException when the distributor cannot be reached or answers unusably
     */
    RestockConfirmationDTO requestRestock(String isbn, int quantity, String retailerReference);

    /** The distributor has no title with the requested ISBN. */
    final class TitleNotFoundAtDistributorException extends NotFoundException {

        public TitleNotFoundAtDistributorException(String isbn) {
            super("title-not-found-at-distributor", "Title not found at distributor",
                    "The distributor has no title with ISBN " + isbn);
        }
    }

    /** The distributor knows the title but it cannot be taken over or ordered. */
    final class TitleNotOrderableException extends InvalidInputException {

        public TitleNotOrderableException(String isbn) {
            super("title-not-orderable", "Title not orderable",
                    "The title with ISBN " + isbn + " cannot be ordered from the distributor");
        }
    }

    /** The distributor cannot be reached or does not answer with something usable. */
    final class DistributorUnavailableException extends UnavailableException {

        public DistributorUnavailableException(String detail) {
            super("distributor-unavailable", "Distributor unavailable", detail);
        }
    }
}
