package nl.openvalue.distributor.api;

import nl.openvalue.distributor.catalog.RestockOrder;
import nl.openvalue.distributor.catalog.RestockService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restock-orders")
public class RestockOrdersController {

    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 500;

    private final RestockService restockService;

    public RestockOrdersController(RestockService restockService) {
        this.restockService = restockService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestockOrderResponse place(@RequestBody RestockOrderRequest request) {
        // Request checks first: a bad request never reaches the business rules (404 or 422).
        if (request.ean() == null || request.ean().isBlank()) {
            throw new InvalidRequestException("ean must not be blank");
        }
        Integer quantity = request.quantity();
        if (quantity == null || quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new InvalidRequestException(
                    "quantity must be a number between " + MIN_QUANTITY + " and " + MAX_QUANTITY);
        }
        RestockOrder order = restockService.placeOrder(request.ean(), quantity, request.retailerReference());
        return RestockOrderResponse.of(order);
    }

    @GetMapping("/{id}")
    public RestockOrderResponse fetch(@PathVariable String id) {
        return restockService.findOrder(id).map(RestockOrderResponse::of)
                .orElseThrow(() -> new RestockOrderNotFoundException(id));
    }
}
