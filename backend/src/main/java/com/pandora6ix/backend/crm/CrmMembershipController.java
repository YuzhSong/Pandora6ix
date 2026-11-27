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
class CrmMembershipController {
    private final IdentityService identities;
    private final CrmMemberships memberships;

    CrmMembershipController(IdentityService identities, CrmMemberships memberships) {
        this.identities = identities;
        this.memberships = memberships;
    }

    @GetMapping("/memberships")
    List<Map<String, Object>> list(@RequestParam(required = false) String q,
            @RequestParam(required = false) String customerId, @RequestParam(defaultValue = "0") int offset) {
        return memberships.list(identities.current(), q, customerId, offset);
    }

    @GetMapping("/memberships/{id}")
    Map<String, Object> get(@PathVariable String id) {
        return memberships.get(identities.current(), id);
    }

    @PostMapping("/memberships")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> create(@Valid @RequestBody CrmMemberships.MembershipInput input) {
        return memberships.saveMembership(identities.current(), null, input);
    }

    @PutMapping("/memberships/{id}")
    Map<String, Object> update(@PathVariable String id, @Valid @RequestBody CrmMemberships.MembershipInput input) {
        return memberships.saveMembership(identities.current(), id, input);
    }

    @GetMapping("/memberships/{id}/benefits")
    List<Map<String, Object>> benefits(@PathVariable String id, @RequestParam(defaultValue = "0") int offset) {
        return memberships.benefits(identities.current(), id, offset);
    }

    @PostMapping("/memberships/{id}/benefits")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> createBenefit(@PathVariable String id, @Valid @RequestBody CrmMemberships.BenefitInput input) {
        return memberships.saveBenefit(identities.current(), id, null, input);
    }

    @PutMapping("/memberships/{id}/benefits/{benefit}")
    Map<String, Object> updateBenefit(@PathVariable String id, @PathVariable String benefit,
            @Valid @RequestBody CrmMemberships.BenefitInput input) {
        return memberships.saveBenefit(identities.current(), id, benefit, input);
    }

    @GetMapping("/benefits/{id}/uses")
    List<Map<String, Object>> uses(@PathVariable String id, @RequestParam(defaultValue = "0") int offset) {
        return memberships.uses(identities.current(), id, offset);
    }

    @PostMapping("/benefits/{id}/uses")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> createUse(@PathVariable String id, @Valid @RequestBody CrmMemberships.UseInput input) {
        return memberships.saveUse(identities.current(), id, null, input);
    }

    @PutMapping("/benefits/{id}/uses/{use}")
    Map<String, Object> updateUse(@PathVariable String id, @PathVariable String use,
            @Valid @RequestBody CrmMemberships.UseInput input) {
        return memberships.saveUse(identities.current(), id, use, input);
    }
}
