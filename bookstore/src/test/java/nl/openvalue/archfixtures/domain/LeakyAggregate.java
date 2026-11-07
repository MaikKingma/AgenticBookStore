package nl.openvalue.archfixtures.domain;

import nl.openvalue.archfixtures.domaininteraction.CatalogueService;

/** Fixture: a domain class that reaches out to domaininteraction, which only the outer adapters may access. */
public class LeakyAggregate {

    CatalogueService service;
}
