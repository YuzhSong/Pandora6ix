package com.pandora6ix.backend.controller;

import com.pandora6ix.backend.api.Contracts;
import com.pandora6ix.backend.service.*;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class IdentityController {
    private final IdentityService identity;
    private final DirectoryService directory;

    public IdentityController(IdentityService identity, DirectoryService directory) {
        this.identity = identity;
        this.directory = directory;
    }

    @PostMapping("/auth/login")
    public Object login(@Valid @RequestBody Contracts.Login c) {
        return identity.login(c);
    }

    @GetMapping("/me")
    public Object me() {
        return identity.profile(identity.current());
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> password(@Valid @RequestBody Contracts.Password c) {
        identity.password(identity.current(), c);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        identity.logout(identity.current());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/enterprise-initializations")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Object initialize(
            @Valid @RequestBody Contracts.EnterpriseCreate c,
            @RequestHeader(value = "X-Bootstrap-Token", required = false) String bootstrap,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return identity.initialize(c, bootstrap, key);
    }

    @GetMapping("/admin/enterprise")
    public Object enterprise() {
        return directory.enterprise(identity.current());
    }

    @PatchMapping("/admin/enterprise")
    public Object enterprisePatch(@Valid @RequestBody Contracts.EnterprisePatch c) {
        return directory.enterprisePatch(identity.current(), c);
    }

    @GetMapping("/admin/org-units")
    public Object orgs(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return directory.orgs(identity.current(), cursor, limit);
    }

    @PostMapping("/admin/org-units")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Object org(@Valid @RequestBody Contracts.OrgCreate c) {
        return directory.createOrg(identity.current(), c);
    }

    @PatchMapping("/admin/org-units/{id}")
    public Object orgPatch(@PathVariable String id, @Valid @RequestBody Contracts.OrgPatch c) {
        return directory.patchOrg(identity.current(), id, c);
    }

    @GetMapping("/admin/users")
    public Object users(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return directory.users(identity.current(), q, cursor, limit);
    }

    @PostMapping("/admin/users")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Object user(@Valid @RequestBody Contracts.UserCreate c) {
        return directory.createUser(identity.current(), c);
    }

    @PatchMapping("/admin/users/{id}")
    public Object userPatch(@PathVariable String id, @Valid @RequestBody Contracts.UserPatch c) {
        return directory.patchUser(identity.current(), id, c);
    }
}
