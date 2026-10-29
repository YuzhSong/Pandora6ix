## ADDED Requirements
### Requirement: Disabled account recovery guidance
Web SHALL distinguish departure-disabled and in-service disabled accounts, explicitly report unavailable disable reasons, and provide confirmed versioned reactivation only for in-service disabled accounts. Restoration SHALL preserve roles, identity and password and never treat departed accounts as eligible.
#### Scenario: Reactivate an in-service account
- **WHEN** an administrator confirms restoration of an in-service disabled account
- **THEN** Web sends a versioned user PATCH, updates the view only after a verified successful response, and shows errors without claiming success
### Requirement: Guided manager succession
Web SHALL guide blocked manager departures and manager job adjustments through existing atomic succession, retaining the intended next action and requiring separate confirmation after successful handover.
#### Scenario: Manager departure after succession
- **WHEN** an administrator starts departure requiring succession
- **THEN** Web first offers successor selection and after successful replacement opens the former manager's departure form with the returned version without automatically submitting it
