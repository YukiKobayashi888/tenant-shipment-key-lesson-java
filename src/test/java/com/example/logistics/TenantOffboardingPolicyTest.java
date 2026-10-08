package com.example.logistics;

/** Focused business test: open shipment exceptions keep both identities intact. */
public final class TenantOffboardingPolicyTest {
    public static void main(String[] args) throws Exception {
        TenantCredentialLesson lesson = new TenantCredentialLesson(null);
        TenantCredentialLesson.Enrollment enrollment = new TenantCredentialLesson.Enrollment("harbor-7", "user-7", "key-7", "once-only");
        TenantCredentialLesson.Offboarding result = lesson.offboard(enrollment, 2);
        if (result.completed()) throw new AssertionError("Open exceptions must defer offboarding");
        if (!result.message().equals("Resolve shipment exceptions before removing access.")) {
            throw new AssertionError("Expected the operator-facing deferral message");
        }
        System.out.println("PASS: open exceptions defer tenant offboarding before any credential call.");
    }
}
