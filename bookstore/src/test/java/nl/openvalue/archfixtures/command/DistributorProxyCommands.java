package nl.openvalue.archfixtures.command;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/** Fixture: a controller outside acl that calls the distributor with RestClient itself and uses nothing from acl. */
@RestController
public class DistributorProxyCommands {

    RestClient restClient;
}
