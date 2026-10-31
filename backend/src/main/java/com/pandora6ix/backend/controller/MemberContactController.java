package com.pandora6ix.backend.controller;

import com.pandora6ix.backend.service.IdentityService;
import com.pandora6ix.backend.service.MemberContactService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MemberContactController {
    private final IdentityService identity;
    private final MemberContactService contacts;

    public MemberContactController(IdentityService identity, MemberContactService contacts) {
        this.identity = identity;
        this.contacts = contacts;
    }

    @GetMapping("/users/{id}/contact")
    public Object contact(@PathVariable String id) {
        return contacts.contact(identity.current(), id);
    }
}
