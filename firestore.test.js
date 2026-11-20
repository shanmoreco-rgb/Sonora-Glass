const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_sonora_123";
const BOB_UID = "bob_sonora_456";

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

function validProfilePayload(uid, pastDate) {
  return {
    userId: uid,
    displayName: "Elena Vance",
    selectedTheme: "LIQUID_GLASS",
    offlineModeEnabled: false,
    gaplessEnabled: true,
    hiResOutputEnabled: true,
    activeEqPresetId: "preset_studio_flat",
    createdAt: pastDate,
    updatedAt: pastDate,
  };
}

function validTrackPayload(uid, pastDate) {
  return {
    userId: uid,
    title: "Liquid Aurora (24-bit Master)",
    artist: "Sonora Ensemble",
    album: "Glass Harmonics",
    durationMs: 10000,
    formatBadge: "WAV 24-bit / 48 kHz",
    sampleRateHz: 48000,
    bitDepth: 24,
    codec: "WAV",
    sourceType: "BUNDLED_HIRES",
    localFileName: "track_liquid_aurora",
    coverKey: "liquid_aurora",
    favorite: false,
    offlineAvailable: true,
    playCount: 0,
    createdAt: pastDate,
    updatedAt: pastDate,
  };
}

function validPlaylistPayload(uid, pastDate) {
  return {
    userId: uid,
    name: "Hi-Res Nocturne",
    description: "Sesión espacial en alta resolución",
    coverKey: "liquid_aurora",
    trackIds: ["track_1", "track_2"],
    offlinePinned: true,
    createdAt: pastDate,
    updatedAt: pastDate,
  };
}

function validEqPresetPayload(uid, pastDate) {
  return {
    userId: uid,
    name: "Audiophile Liquid",
    bandGainsDb: [2.5, 1.8, 0.5, -0.5, 0.0, 0.8, 1.5, 2.2, 3.0, 2.5],
    bassBoostStrength: 420,
    virtualizerStrength: 550,
    preampDb: 1.5,
    loudnessGainMb: 300,
    reverbPreset: 2,
    enabled: true,
    createdAt: pastDate,
    updatedAt: pastDate,
  };
}

test("Unauthenticated user cannot read or write any user profile, tracks, playlists, or EQ presets", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, past)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tracks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("playlists").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("eq_presets").get());
});

test("Owner can create, update, and list UserProfile, AudioTrack, Playlist, and EqPreset", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  // User profile
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID, created)));
  await assertSucceeds(profileRef.update({
    selectedTheme: "OBSIDIAN_AMOLED",
    offlineModeEnabled: true,
    updatedAt: updated,
  }));

  // Tracks (empty list first, then create & partial updates)
  const tracksCol = profileRef.collection("tracks");
  await assertSucceeds(tracksCol.get());
  await assertSucceeds(tracksCol.doc("track_1").set(validTrackPayload(ALICE_UID, created)));
  await assertSucceeds(tracksCol.doc("track_1").update({ favorite: true, updatedAt: updated }));
  await assertSucceeds(tracksCol.doc("track_1").update({ playCount: 1, updatedAt: updated }));

  // Playlists
  const playlistsCol = profileRef.collection("playlists");
  await assertSucceeds(playlistsCol.get());
  await assertSucceeds(playlistsCol.doc("pl_1").set(validPlaylistPayload(ALICE_UID, created)));
  await assertSucceeds(playlistsCol.doc("pl_1").update({ trackIds: ["track_2", "track_1"], updatedAt: updated }));
  await assertSucceeds(playlistsCol.doc("pl_1").update({ offlinePinned: false, updatedAt: updated }));

  // EqPresets
  const eqCol = profileRef.collection("eq_presets");
  await assertSucceeds(eqCol.get());
  await assertSucceeds(eqCol.doc("eq_1").set(validEqPresetPayload(ALICE_UID, created)));
  await assertSucceeds(eqCol.doc("eq_1").update({
    bandGainsDb: [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0],
    enabled: false,
    updatedAt: updated,
  }));
});

test("Cross-user access is denied across all subcollections", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const past = new Date(Date.now() - 10000);
    const bobRef = ctx.firestore().collection("users").doc(BOB_UID);
    await bobRef.set(validProfilePayload(BOB_UID, past));
    await bobRef.collection("tracks").doc("bob_t1").set(validTrackPayload(BOB_UID, past));
    await bobRef.collection("playlists").doc("bob_p1").set(validPlaylistPayload(BOB_UID, past));
    await bobRef.collection("eq_presets").doc("bob_eq1").set(validEqPresetPayload(BOB_UID, past));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobRef = aliceDb.collection("users").doc(BOB_UID);

  await assertFails(bobRef.get());
  await assertFails(bobRef.collection("tracks").get());
  await assertFails(bobRef.collection("playlists").get());
  await assertFails(bobRef.collection("eq_presets").get());
  await assertFails(bobRef.collection("tracks").doc("bob_t1").update({ favorite: true, updatedAt: new Date(Date.now() - 1000) }));
});

test("Rejects ghost fields, @DocumentId id field, immutable createdAt edits, invalid theme, and out-of-range EQ gains", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  // Reject ghost field & @DocumentId 'id'
  await assertFails(profileRef.set({ ...validProfilePayload(ALICE_UID, past), id: ALICE_UID }));
  await assertFails(profileRef.collection("tracks").doc("t_bad").set({ ...validTrackPayload(ALICE_UID, past), ghostAdmin: true }));

  // Reject invalid theme
  await assertFails(profileRef.set({ ...validProfilePayload(ALICE_UID, past), selectedTheme: "NEON_HACK" }));

  // Reject out-of-range EQ gain (> 12.0 dB) or wrong band count
  await assertFails(profileRef.collection("eq_presets").doc("eq_bad").set({
    ...validEqPresetPayload(ALICE_UID, past),
    bandGainsDb: [18.0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  }));
  await assertFails(profileRef.collection("eq_presets").doc("eq_short").set({
    ...validEqPresetPayload(ALICE_UID, past),
    bandGainsDb: [0.0, 0.0],
  }));

  // Reject future timestamps and createdAt tampering
  await assertFails(profileRef.set(validProfilePayload(ALICE_UID, new Date(Date.now() + 60000))));
  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID, past)));
  await assertFails(profileRef.update({ createdAt: new Date(Date.now() - 1000), updatedAt: new Date(Date.now() - 500) }));
});
