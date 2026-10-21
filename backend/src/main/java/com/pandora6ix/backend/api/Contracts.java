package com.pandora6ix.backend.api;

import com.pandora6ix.backend.domain.task.TaskKind;
import com.pandora6ix.backend.domain.task.TimeKind;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/** Command fields are intentionally explicit: tenant and actor never come from JSON. */
public final class Contracts {
    private Contracts() {}

    public record Login(
            @NotBlank @Size(max = 128) String enterpriseDomain,
            @NotBlank @Size(max = 128) String loginName,
            @NotBlank @Size(max = 72) String password) {}

    public record Password(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 10, max = 72) String newPassword) {}

    public record EnterpriseCreate(
            @NotBlank @Size(max = 128) @Pattern(regexp = "[a-z0-9][a-z0-9-]{0,127}")
                    String loginDomain,
            @NotBlank @Size(max = 128) String name,
            @Size(max = 64) String businessTimezone,
            @NotBlank @Size(max = 128) String adminLoginName,
            @NotBlank @Size(min = 10, max = 72) String adminPassword,
            @NotBlank @Size(max = 128) String adminDisplayName) {}

    public record EnterprisePatch(
            @NotNull @Min(0) Long expectedVersion,
            @Size(min = 1, max = 128) String name,
            @Size(max = 64) String businessTimezone) {}

    public record OrgCreate(
            @NotBlank @Size(max = 128) String name, @NotBlank String kind, String parentId) {}

    public record OrgPatch(
            @NotNull @Min(0) Long expectedVersion,
            @NotBlank @Size(max = 128) String name,
            @NotBlank String kind,
            String parentId) {}

    public record OrgDelete(@NotNull @Min(0) Long expectedVersion) {}

    public record UserPasswordReset(
            @NotNull @Min(0) Long expectedVersion,
            @NotBlank @Size(min = 10, max = 72) String newPassword) {}

    public record UserDeparture(@NotNull @Min(0) Long expectedVersion, @NotNull LocalDate departureDate) {}

    public record ManagerReplacement(@NotNull @Min(0) Long expectedVersion,
            @NotBlank String successorUserId, @NotNull @Min(0) Long successorExpectedVersion,
            @NotBlank @Size(max=128) String successorJobTitle, @NotBlank @Size(max=128) String formerJobTitle) {}

    public record UserJobChange(
            @NotNull @Min(0) Long expectedVersion,
            @Size(max = 128) String jobTitle, String orgUnitId, String supervisorUserId,
            @NotEmpty List<@NotBlank String> roles) {}

    public record UserCreate(
            @NotBlank @Size(max = 128) String loginName,
            @NotBlank @Size(min = 10, max = 72) String password,
            @NotBlank @Size(max = 128) String displayName,
            String orgUnitId,
            String supervisorUserId,
            @Size(max = 128) String jobTitle,
            @NotEmpty List<@NotBlank String> roles,
            @NotBlank @Pattern(regexp = "MALE|FEMALE") String gender,
            @NotBlank @Pattern(regexp = "[0-9]{15}|[0-9]{17}[0-9X]") String idNumber,
            @NotBlank @Size(min = 6, max = 24) @Pattern(regexp = "[+]?[0-9 ()-]+") String phoneNumber) {}

    public record UserPatch(
            @NotNull @Min(0) Long expectedVersion,
            @NotBlank @Size(max = 128) String displayName,
            String orgUnitId,
            String supervisorUserId,
            @Size(max = 128) String jobTitle,
            @NotBlank String status,
            @NotEmpty List<@NotBlank String> roles,
            @Pattern(regexp = "MALE|FEMALE") String gender,
            @Pattern(regexp = "[0-9]{15}|[0-9]{17}[0-9X]") String idNumber,
            @Size(min = 6, max = 24) @Pattern(regexp = "[+]?[0-9 ()-]+") String phoneNumber,
            @Size(min = 1, max = 128) String loginName) {}

    public record TagCreate(
            @NotBlank @Size(max = 64) String name,
            @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}") String colorHex) {}

    public record TagPatch(
            @NotNull @Min(0) Long expectedVersion,
            @Size(min = 1, max = 64) String name,
            @Pattern(regexp = "#[0-9A-Fa-f]{6}") String colorHex) {}

    public record TaskCreate(
            @NotNull TaskKind kind,
            @NotBlank @Size(max = 255) String title,
            @Size(max = 20000) String description,
            @NotNull @Min(1) @Max(5) Integer priorityLevel,
            @NotNull TimeKind timeKind,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            String startTime,
            String endTime,
            @Size(max = 100) List<@NotBlank String> recipientIds,
            @Size(max = 100) List<@NotBlank String> tagIds,
            String primaryTagId) {}

    public record TaskPatch(
            @NotNull @Min(0) Long expectedVersion,
            @NotBlank @Size(max = 255) String title,
            @Size(max = 20000) String description,
            @NotNull @Min(1) @Max(5) Integer priorityLevel,
            @NotNull TimeKind timeKind,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            String startTime,
            String endTime,
            @Size(max = 100) List<@NotBlank String> tagIds,
            String primaryTagId) {}

    public record Progress(
            @NotNull @Min(0) Long expectedVersion,
            @NotNull @Min(0) @Max(100) Integer progressPercent,
            @Size(max = 20000) String note) {}

    public record Complete(@NotNull @Min(0) Long expectedAssignmentVersion) {}

    public record ReviewDecision(
            @NotNull @Min(0) Long expectedVersion,
            @NotBlank @Pattern(regexp = "APPROVE|REJECT") String decision,
            @Size(max = 20000) String comment) {}

    public record CompletionRequest(
            @NotNull @Min(0) Long expectedVersion, @Size(max = 20000) String note) {}

    public record Decision(
            @NotNull @Min(0) Long expectedVersion,
            @NotBlank @Pattern(regexp = "ACCEPT|REJECT") String decision,
            @Size(max = 20000) String comment) {}

    public record EntryCreate(
            @NotBlank @Size(max = 128) String clientEntryKey,
            @NotBlank @Size(max = 20000) String content) {}

    public record JournalCreate(
            @NotNull LocalDate workDate,
            @NotEmpty @Size(max = 100) List<@NotNull @Valid EntryCreate> entries,
            @Size(max = 100) List<@NotBlank String> tagIds,
            String primaryTagId) {}

    public record EntryUpsert(
            String entryId,
            @Size(max = 128) String clientEntryKey,
            @NotBlank @Size(max = 20000) String content) {}

    public record JournalPatch(
            @NotNull @Min(0) Long expectedVersion,
            @Size(max = 100) List<@NotNull @Valid EntryUpsert> upsertEntries,
            @Size(max = 100) List<@NotBlank String> deleteEntryIds,
            @Size(max = 100) List<@NotBlank String> tagIds,
            String primaryTagId) {}

    public record ItemCreate(
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 20000) String content,
            @NotNull Integer displayOrder) {}

    public record ItemPatch(
            @NotNull @Min(0) Long expectedVersion,
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 20000) String content,
            @NotNull Integer displayOrder) {}

    public record Selection(
            @NotNull @Min(0) Long expectedVersion,
            @NotNull @Size(max = 10) List<@NotBlank String> entryIds) {}
}
