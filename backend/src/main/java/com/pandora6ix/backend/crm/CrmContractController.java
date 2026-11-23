package com.pandora6ix.backend.crm;

import com.pandora6ix.backend.service.IdentityService;

import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@Profile("dev2")
@RequestMapping("/api/v1/crm/contracts")
class CrmContractController {
    private final IdentityService identities;
    private final CrmContracts contracts;

    CrmContractController(IdentityService identities, CrmContracts contracts) {
        this.identities = identities;
        this.contracts = contracts;
    }

    @GetMapping
    List<Map<String, Object>> list(@RequestParam(required = false) String q,
            @RequestParam(required = false) CrmContracts.Expiry expiry, @RequestParam(defaultValue = "0") int offset) {
        return contracts.list(identities.current(), q, expiry, offset);
    }

    @GetMapping("/{id}")
    Map<String, Object> get(@PathVariable String id) {
        return contracts.get(identities.current(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> create(@Valid @RequestBody CrmContracts.Input input) {
        return contracts.save(identities.current(), null, input);
    }

    @PutMapping("/{id}")
    Map<String, Object> update(@PathVariable String id, @Valid @RequestBody CrmContracts.Input input) {
        return contracts.save(identities.current(), id, input);
    }

    @PutMapping(value = "/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, Object> upload(@PathVariable String id, @RequestParam long version, @RequestParam MultipartFile file) {
        return contracts.upload(identities.current(), id, version, file);
    }

    @GetMapping("/{id}/attachment")
    ResponseEntity<byte[]> download(@PathVariable String id) {
        var attachment = contracts.download(identities.current(), id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(attachment.name(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(attachment.bytes());
    }
}
