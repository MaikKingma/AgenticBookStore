package nl.openvalue.archfixtures.query;

/** Fixture: a query view whose factory method takes the price as a boxed Float parameter. */
public class BoxedPriceView {

    static BoxedPriceView of(String title, Float price) {
        return new BoxedPriceView();
    }
}
