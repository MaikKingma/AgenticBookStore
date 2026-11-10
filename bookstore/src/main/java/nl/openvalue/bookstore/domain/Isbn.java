package nl.openvalue.bookstore.domain;

/**
 * An ISBN-13. Hyphens and spaces are removed, so {@code value()} always holds the 13 digits and two spellings of the
 * same ISBN are equal.
 */
public record Isbn(String value) {

    private static final int LENGTH = 13;

    public Isbn {
        if (value == null) {
            throw new InvalidIsbnException(null, "no value");
        }
        String digits = value.replace("-", "").replace(" ", "");
        if (digits.length() != LENGTH || !onlyAsciiDigits(digits)) {
            throw new InvalidIsbnException(value, "an ISBN-13 has exactly 13 digits");
        }
        if (!digits.startsWith("978") && !digits.startsWith("979")) {
            throw new InvalidIsbnException(value, "the prefix must be 978 or 979");
        }
        if (digit(digits, LENGTH - 1) != checkDigit(digits)) {
            throw new InvalidIsbnException(value, "the check digit does not match");
        }
        value = digits;
    }

    private static boolean onlyAsciiDigits(String candidate) {
        return candidate.chars().allMatch(c -> c >= '0' && c <= '9');
    }

    // Weights 1 and 3 alternate over the first twelve digits.
    private static int checkDigit(String digits) {
        int sum = 0;
        for (int i = 0; i < LENGTH - 1; i++) {
            sum += digit(digits, i) * (i % 2 == 0 ? 1 : 3);
        }
        return (10 - sum % 10) % 10;
    }

    private static int digit(String digits, int index) {
        return digits.charAt(index) - '0';
    }
}
