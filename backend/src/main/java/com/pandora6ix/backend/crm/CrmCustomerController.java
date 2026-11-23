package com.pandora6ix.backend.crm;

import com.pandora6ix.backend.service.IdentityService;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Profile("dev2")
@RequestMapping("/api/v1/crm")
class CrmCustomerController {
    private final IdentityService identities;
    private final CrmCustomers customers;
    private final CrmAccess access;

    CrmCustomerController(IdentityService identities, CrmCustomers customers, CrmAccess access) {
        this.identities = identities;
        this.customers = customers;
        this.access = access;
    }

    @GetMapping("/capabilities")
    Map<String, Boolean> capabilities() {
        var actor = identities.current();
        return Map.of("manageCrm", access.canManage(actor), "manageContracts", actor.has("ADMIN"));
    }

    @GetMapping("/people")
    List<Map<String, Object>> people() {
        return access.people(identities.current());
    }

    @GetMapping("/customers")
    List<Map<String, Object>> list(@RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(required = false) String q, @RequestParam(required = false) CrmCustomers.Kind kind,
            @RequestParam(defaultValue = "0") int offset) {
        return customers.list(identities.current(), mine, q, kind, offset);
    }

    @GetMapping("/customers/{id}")
    Map<String, Object> get(@PathVariable String id) {
        return customers.get(identities.current(), id);
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> create(@Valid @RequestBody CrmCustomers.Input input) {
        return customers.save(identities.current(), null, input);
    }

    @PutMapping("/customers/{id}")
    Map<String, Object> update(@PathVariable String id, @Valid @RequestBody CrmCustomers.Input input) {
        return customers.save(identities.current(), id, input);
    }
}
