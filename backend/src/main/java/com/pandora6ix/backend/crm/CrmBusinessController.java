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
@RequestMapping("/api/v1/crm/businesses")
class CrmBusinessController {
    private final IdentityService identities;
    private final CrmBusiness businesses;

    CrmBusinessController(IdentityService identities, CrmBusiness businesses) {
        this.identities = identities;
        this.businesses = businesses;
    }

    @GetMapping
    List<Map<String, Object>> list(@RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(required = false) String q, @RequestParam(required = false) CrmBusiness.Kind kind,
            @RequestParam(required = false) CrmBusiness.Status status, @RequestParam(required = false) String customerId,
            @RequestParam(defaultValue = "0") int offset) {
        return businesses.list(identities.current(), mine, q, kind, status, customerId, offset);
    }

    @GetMapping("/{id}")
    Map<String, Object> get(@PathVariable String id) {
        return businesses.get(identities.current(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> create(@Valid @RequestBody CrmBusiness.Input input) {
        return businesses.save(identities.current(), null, input);
    }

    @PutMapping("/{id}")
    Map<String, Object> update(@PathVariable String id, @Valid @RequestBody CrmBusiness.Input input) {
        return businesses.save(identities.current(), id, input);
    }
}
