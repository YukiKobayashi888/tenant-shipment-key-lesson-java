package com.example.logistics;

import java.io.IOException;

/** Coordinates a tenant's shipment credential and its named operator. */
public final class TenantCredentialLesson {
    private final InfraiHttpClient infrai;

    public TenantCredentialLesson(InfraiHttpClient infrai) { this.infrai = infrai; }

    public Enrollment enroll(String tenantId, String projectId, String operatorEmail)
            throws IOException, InterruptedException {
        String userId = infrai.createUser(operatorEmail, tenantId);
        try {
            InfraiHttpClient.CreatedKey key = infrai.createScopedKey(projectId, tenantId);
            return new Enrollment(tenantId, userId, key.id(), key.plaintext());
        } catch (IOException | InterruptedException | RuntimeException failure) {
            try { infrai.deleteUser(userId); }
            catch (IOException | InterruptedException | RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    public Offboarding offboard(Enrollment enrollment, int unresolvedExceptions)
            throws IOException, InterruptedException {
        if (unresolvedExceptions > 0) return Offboarding.deferred("Resolve shipment exceptions before removing access.");
        try {
            infrai.revokeKey(enrollment.keyId());
        } catch (IOException | InterruptedException | RuntimeException failure) {
            try { infrai.deleteUser(enrollment.userId()); }
            catch (IOException | InterruptedException | RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
        infrai.deleteUser(enrollment.userId());
        return Offboarding.finished();
    }

    public record Enrollment(String tenantId, String userId, String keyId, String plaintextKey) { }
    public record Offboarding(boolean completed, String message) {
        static Offboarding deferred(String message) { return new Offboarding(false, message); }
        static Offboarding finished() { return new Offboarding(true, "Credential and operator removed."); }
    }
}
