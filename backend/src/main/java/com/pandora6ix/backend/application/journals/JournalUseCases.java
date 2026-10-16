package com.pandora6ix.backend.application.journals;

public interface JournalUseCases {
    java.util.Map<String, Object> create(
            com.pandora6ix.backend.service.IdentityService.Actor actor,
            com.pandora6ix.backend.api.Contracts.JournalCreate command);

    java.util.Map<String, Object> patch(
            com.pandora6ix.backend.service.IdentityService.Actor actor,
            String id,
            com.pandora6ix.backend.api.Contracts.JournalPatch command);

    java.util.Map<String, Object> detail(
            com.pandora6ix.backend.service.IdentityService.Actor actor, String id);

    java.util.Map<String, Object> entry(
            com.pandora6ix.backend.service.IdentityService.Actor actor, String id);

    java.util.Map<String, Object> selection(
            com.pandora6ix.backend.service.IdentityService.Actor actor);

    java.util.Map<String, Object> saveSelection(
            com.pandora6ix.backend.service.IdentityService.Actor actor,
            com.pandora6ix.backend.api.Contracts.Selection command);
}
