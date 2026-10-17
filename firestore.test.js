const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write any user data", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Alice can create and read her own user profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice"
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Bob cannot read or write Alice's user profile", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).set({
      uid: BOB_UID,
      email: "bob@example.com",
      displayName: "Bob"
    })
  );
});

test("Alice can create products and customers in her collection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod1").set({
      id: "prod1",
      userId: ALICE_UID,
      name: "Bamboo Coaster",
      businessType: "ENGRAVING",
      sellingPrice: 70.0,
      unitCost: 35.0,
      isActive: true
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customers").doc("cust1").set({
      id: "cust1",
      userId: ALICE_UID,
      name: "Juan Dela Cruz"
    })
  );
});

test("Alice can create valid sales transactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx1").set({
      id: "tx1",
      userId: ALICE_UID,
      orderNumber: "ENG-2026-0001",
      businessType: "ENGRAVING",
      customerName: "Juan Dela Cruz",
      grossSales: 1500.0,
      netSales: 1425.0,
      estimatedProfit: 675.0,
      paymentStatus: "PENDING",
      amountPaid: 0.0,
      outstandingBalance: 1425.0
    })
  );
});

test("Bob cannot read Alice's products or transactions", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("products").doc("prod1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx1").get());
});

test("Invalid product data is rejected (negative price)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("bad_prod").set({
      id: "bad_prod",
      userId: ALICE_UID,
      name: "Bad Product",
      businessType: "ENGRAVING",
      sellingPrice: -50.0,
      unitCost: 10.0,
      isActive: true
    })
  );
});
