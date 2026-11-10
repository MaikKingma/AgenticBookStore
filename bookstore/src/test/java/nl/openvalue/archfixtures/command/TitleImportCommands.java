package nl.openvalue.archfixtures.command;

import nl.openvalue.archfixtures.acl.LeakyAppServiceImpl.TitleWire;
import nl.openvalue.archfixtures.acl.LeakyAppServiceImpl.TitleWire.ContributorLine;
import nl.openvalue.archfixtures.acl.LeakyAppServiceImpl.TitleWire.ContributorLine.RoleCode;
import org.springframework.web.bind.annotation.RestController;

/** Fixture: a controller outside acl that holds nested records of an AppServiceImpl and of look-alike classes. */
@RestController
public class TitleImportCommands {

    TitleWire title;

    ContributorLine contributor;

    RoleCode role;

    // Records of AppServiceImpl classes that are themselves nested: suffix on the direct enclosing class, suffix on the
    // middle enclosing class only, and a class named exactly AppServiceImpl.
    LegacyAppServiceImpl.Line legacyLine;

    ArchiveAppServiceImpl.Batch.Line archivedLine;

    AppServiceImpl.Line bareLine;

    // Controls: nested records of classes whose names end with AppService, ServiceImpl, AppServiceImplementation or
    // Appserviceimpl, and a nested class whose own name ends with AppServiceImpl.
    StockAppService.Entry prefixOnly;

    StockServiceImpl.Entry otherSuffix;

    StockAppServiceImplementation.Entry longerSuffix;

    StockAppserviceimpl.Entry caseOnly;

    StockAppServiceImpl ownName;

    ContributorLine firstContributor(TitleWire wire) {
        return null;
    }

    static class LegacyAppServiceImpl {

        record Line(String isbn) {
        }
    }

    static class ArchiveAppServiceImpl {

        static class Batch {

            record Line(String isbn) {
            }
        }
    }

    static class AppServiceImpl {

        record Line(String isbn) {
        }
    }

    static class StockAppService {

        record Entry(String isbn) {
        }
    }

    static class StockServiceImpl {

        record Entry(String isbn) {
        }
    }

    static class StockAppServiceImplementation {

        record Entry(String isbn) {
        }
    }

    static class StockAppserviceimpl {

        record Entry(String isbn) {
        }
    }

    record StockAppServiceImpl(String isbn) {
    }
}
