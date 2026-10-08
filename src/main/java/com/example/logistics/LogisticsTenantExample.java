package com.example.logistics;

/** Run a complete enrollment lesson with: INFRAI_API_KEY, tenant id, project id, and operator email. */
public final class LogisticsTenantExample {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: LogisticsTenantExample <tenant-id> <project-id> <operator-email>");
            System.exit(2);
        }
        String apiKey = System.getenv("INFRAI_API_KEY");
        TenantCredentialLesson lesson = new TenantCredentialLesson(new InfraiHttpClient(apiKey));
        TenantCredentialLesson.Enrollment enrollment = lesson.enroll(args[0], args[1], args[2]);
        try {
            System.out.println("Tenant " + enrollment.tenantId() + " has key id " + enrollment.keyId()
                    + " and user id " + enrollment.userId());
            if (!enrollment.plaintextKey().isEmpty()) {
                System.out.println("Store this key now: " + enrollment.plaintextKey());
                System.out.println("The plaintext key is returned only once and cannot be retrieved again.");
            }
        } finally {
            lesson.offboard(enrollment, 0);
            System.out.println("Tenant key revoked and operator deleted.");
        }
    }
}
