package nl.openvalue.bookstore.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The e-mail address of a customer. The value is trimmed and lower-cased with the root locale, so addresses that differ
 * only in case or surrounding spaces are equal. The normalised value has at most 254 characters and the simple shape
 * local part, at sign, domain with at least one dot.
 */
public record EmailAddress(String value) {

    private static final int MAX_LENGTH = 254;
    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public EmailAddress {
        if (value == null) {
            throw new InvalidEmailAddressException("An e-mail address is required");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH) {
            throw new InvalidEmailAddressException(
                    "An e-mail address has at most " + MAX_LENGTH + " characters, not " + value.length());
        }
        if (!SHAPE.matcher(value).matches()) {
            throw new InvalidEmailAddressException("'" + value + "' is not a valid e-mail address");
        }
    }
}
