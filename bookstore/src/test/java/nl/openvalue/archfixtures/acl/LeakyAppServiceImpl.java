package nl.openvalue.archfixtures.acl;

/** Fixture: an acl implementation whose nested payload records are not private, so classes outside acl can use them. */
public class LeakyAppServiceImpl {

    String titleOf(TitleWire wire) {
        return wire.titleName() + wire.contributor().name() + wire.contributor().role().code();
    }

    public record TitleWire(String titleName, ContributorLine contributor) {

        public record ContributorLine(String name, RoleCode role) {

            public record RoleCode(String code) {
            }
        }
    }
}
