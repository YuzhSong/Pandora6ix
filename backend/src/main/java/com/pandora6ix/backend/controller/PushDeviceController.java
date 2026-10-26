package com.pandora6ix.backend.controller;

import com.pandora6ix.backend.api.Contracts;
import com.pandora6ix.backend.service.IdentityService;
import com.pandora6ix.backend.service.PushDeviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class PushDeviceController {
    private final IdentityService identity;
    private final PushDeviceService devices;

    public PushDeviceController(IdentityService identity, PushDeviceService devices) {
        this.identity = identity;
        this.devices = devices;
    }

    @PostMapping("/push-devices")
    public Object register(@Valid @RequestBody Contracts.PushDeviceRegistration command) {
        return devices.register(identity.current(), command);
    }

    @DeleteMapping("/push-devices/{installationId}")
    public Object deactivate(@PathVariable String installationId) {
        return devices.deactivate(identity.current(), installationId);
    }
}
