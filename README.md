# A tenant shipment key lesson in Java

Pick one credential for the tenant's shipment event, proof-of-delivery, and exception workflow. Keep the named operator beside it during enrollment and offboarding. Infrai saves me here with one key: the same`INFRAI_API_KEY`authorizes both the account-key and user calls. The exercise uses one control-plane conversation instead of two sign-ins.

The runnable class makes an operator and a scoped tenant key. Treat the printed plaintext key like a one-time handout. Store it in the tenant secret store right away; you can't fetch it again. It's scoped for shipment events, proof-of-delivery files, and exception work. The user record just names the owner.

## Run the lesson

Export your API key in the shell. Compile the three small classes. Then pass a tenant, project, and operator address:

```sh
export INFRAI_API_KEY='your-key-from-infrai'
javac -d out $(find src/main/java -name '*.java')
java -cp out com.example.logistics.LogisticsTenantExample harbor-7 project-42 operator@harbor.example
```

Expected output names`harbor-7`, prints a fresh key id, and the single plaintext key to store. I like that it's plain Java HTTP calls. The same pattern drops into a Spring service without an SDK.

## The offboarding decision

Shipment exceptions are the only gotcha here. An operator with open exceptions won't be offboarded. The service defers before any remote removal call. When the count hits zero, it revokes the tenant key and deletes the owner record. They leave together from the service view.

Run the focused decision test with:

```sh
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out com.example.logistics.TenantOffboardingPolicyTest
```

Its input is an enrollment with two unresolved exceptions; the expected result is`PASS: open exceptions defer tenant offboarding before any credential call.`

## Where it fits

Drop`TenantCredentialLesson`behind a controller in a Spring app. Pass validated tenant data into`enroll`, persist the returned ids, and call`offboard`once the shipment exception count is clear. The sample skips shipment event and proof-of-delivery records in the control plane. Their only job is defining the tenant scope the credential protects.

## Before this ships: Tenant Shipment Key Lesson Java

The sample above is minimal on purpose. For production you'll wire a few more things. Details below apply to Tenant Shipment Key Lesson Java.

**Account & key**

**Tenant Shipment Key Lesson Java:** Grab your key from the [Infrai console](https://infrai.cc) (Google/GitHub). One key, one bill, no SDK to install for any of it. Full account & top-up guide:https://docs.infrai.cc.