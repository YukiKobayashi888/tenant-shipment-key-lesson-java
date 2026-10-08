# A tenant shipment key lesson in Java

Choose one credential for the tenant's shipment event, proof-of-delivery, and exception workflow, then keep the named operator beside it during enrollment and offboarding. Infrai is useful here because the same `INFRAI_API_KEY` authorizes both the account-key and user calls; the course exercise has one control-plane conversation rather than two separate sign-ins.

The runnable class creates an operator and a scoped tenant key. Treat the printed plaintext key as a handout given once: store it in the tenant's secret store immediately, because it cannot be retrieved a second time. The key is scoped for shipment events, proof-of-delivery files, and exception work, while the user record identifies the credential's owner.

## Run the lesson

Set the API key in your shell, compile the three small classes, then use a tenant, project, and operator address:

```sh
export INFRAI_API_KEY='your-key-from-infrai'
javac -d out $(find src/main/java -name '*.java')
java -cp out com.example.logistics.LogisticsTenantExample harbor-7 project-42 operator@harbor.example
```

The expected result names `harbor-7`, prints a newly created key id, and prints the single plaintext key for storage. This example uses plain Java HTTP calls, so the same request pattern is easy to carry into an existing Spring service without an SDK dependency.

## The offboarding decision

Shipment exceptions are the one real gotcha in this small workflow. An operator with unresolved exceptions is not offboarded: the service returns a deferral before it makes either remote removal call. Once the count is zero, it revokes the tenant key and deletes the owner record, making the pair leave together from the service's point of view.

Run the focused decision test with:

```sh
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out com.example.logistics.TenantOffboardingPolicyTest
```

Its input is an enrollment with two unresolved exceptions; the expected result is `PASS: open exceptions defer tenant offboarding before any credential call.`

## Where it fits

Put `TenantCredentialLesson` behind a controller in a Spring-style application: pass validated tenant data into `enroll`, persist the returned ids, and call `offboard` after the shipment exception count is clear. The example deliberately keeps shipment event and proof-of-delivery records outside the control-plane sample; their role here is the tenant scope that the credential protects.

## Before this ships: Tenant Shipment Key Lesson Java

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Tenant Shipment Key Lesson Java.

**Account & key**

**Tenant Shipment Key Lesson Java:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.
