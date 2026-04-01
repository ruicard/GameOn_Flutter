const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onRequest } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { Player } = require("./glicko2");

initializeApp();

async function sendInvitationNotifications({ matchId, match, playersToNotify, logPrefix }) {
  if (!Array.isArray(playersToNotify) || playersToNotify.length === 0) {
    console.log(`${logPrefix} No players to notify.`);
    return;
  }

  const createdByUserId = match.createdByUserId || "";
  const modality = match.modality || "match";
  const dateTime = match.dateTime || "";
  const location = match.location || "";

  const filteredPlayers = playersToNotify.filter((id) => id !== createdByUserId);
  if (filteredPlayers.length === 0) {
    console.log(`${logPrefix} No players to notify after excluding creator.`);
    return;
  }

  console.log(`${logPrefix} Players to notify (${filteredPlayers.length}): ${JSON.stringify(filteredPlayers)}`);

  const db = getFirestore();
  const tokenDocs = []; // { token, docId }

  // Firestore "in" queries support up to 30 items per batch
  const BATCH_SIZE = 30;
  for (let i = 0; i < filteredPlayers.length; i += BATCH_SIZE) {
    const batch = filteredPlayers.slice(i, i + BATCH_SIZE);
    console.log(`${logPrefix} Querying users with id in: ${JSON.stringify(batch)}`);
    const snapshot = await db.collection("users").where("id", "in", batch).get();
    console.log(`${logPrefix} Found ${snapshot.docs.length} user document(s) for this batch.`);
    snapshot.docs.forEach((doc) => {
      const token = doc.data().fcmToken;
      console.log(`${logPrefix}   User doc ${doc.id}: fcmToken = ${token ? token.substring(0, 20) + "..." : "MISSING"}`);
      if (token) tokenDocs.push({ token, docId: doc.id });
    });
  }

  if (tokenDocs.length === 0) {
    console.log(`${logPrefix} No FCM tokens found for invited players. Notifications not sent.`);
    return;
  }

  const tokens = tokenDocs.map((t) => t.token);
  const multicastMessage = {
    notification: {
      title: `Invite pending - ${modality}`,
      body: `You have been invited to a ${modality} match on ${dateTime} at ${location}.`,
    },
    data: {
      matchId,
      screen: "home",
    },
    tokens,
  };

  try {
    const response = await getMessaging().sendEachForMulticast(multicastMessage);
    console.log(
      `${logPrefix} Notifications sent: ${response.successCount} success, ${response.failureCount} failed.`
    );
    const invalidTokenDocIds = [];
    response.responses.forEach((resp, idx) => {
      if (!resp.success) {
        const errCode = resp.error?.code || "";
        console.warn(`${logPrefix} Token[${idx}] (doc: ${tokenDocs[idx].docId}) failed: ${resp.error?.message}`);
        if (
          errCode === "messaging/invalid-registration-token" ||
          errCode === "messaging/registration-token-not-registered"
        ) {
          invalidTokenDocIds.push(tokenDocs[idx].docId);
        }
      }
    });
    for (const docId of invalidTokenDocIds) {
      console.log(`${logPrefix} Removing stale FCM token for user doc: ${docId}`);
      await db.collection("users").doc(docId).update({ fcmToken: FieldValue.delete() });
    }
  } catch (error) {
    console.error(`${logPrefix} Error sending FCM notifications:`, error);
  }
}

/**
 * Triggered whenever a new match document is created in the "matches" collection.
 * Sends a push notification to every invited player who has an FCM token stored,
 * excluding the player who created the match.
 */
exports.sendMatchInvitationNotification = onDocumentCreated(
  "matches/{matchId}",
  async (event) => {
    const match = event.data?.data();
    if (!match) {
      console.warn("No match data found for event.");
      return;
    }

    // playerInvitations is the authoritative map of { userId -> status }
    // built from the explicitly invited players when the match was created.
    const playerInvitations = match.playerInvitations || {};
    const allInvitedIds = Object.keys(playerInvitations);

    console.log(`create:${event.params.matchId} Match created.`);
    console.log(`All invited player IDs (${allInvitedIds.length}): ${JSON.stringify(allInvitedIds)}`);
    const playersToNotify = allInvitedIds.filter(
      (id) => playerInvitations[id] === "NO_ANSWER"
    );
    await sendInvitationNotifications({
      matchId: event.params.matchId,
      match,
      playersToNotify,
      logPrefix: `create:${event.params.matchId}`,
    });
  }
);

exports.sendMatchInvitationNotificationOnUpdate = onDocumentUpdated(
  "matches/{matchId}",
  async (event) => {
    const beforeMatch = event.data?.before?.data();
    const afterMatch = event.data?.after?.data();
    if (!beforeMatch || !afterMatch) {
      console.warn(`update:${event.params.matchId} Missing before/after match data.`);
      return;
    }

    const beforeInvitations = beforeMatch.playerInvitations || {};
    const afterInvitations = afterMatch.playerInvitations || {};
    const newlyAddedPlayerIds = Object.keys(afterInvitations).filter(
      (id) => !Object.prototype.hasOwnProperty.call(beforeInvitations, id) && afterInvitations[id] === "NO_ANSWER"
    );

    if (newlyAddedPlayerIds.length === 0) {
      console.log(`update:${event.params.matchId} No newly added invitees to notify.`);
      return;
    }

    await sendInvitationNotifications({
      matchId: event.params.matchId,
      match: afterMatch,
      playersToNotify: newlyAddedPlayerIds,
      logPrefix: `update:${event.params.matchId}`,
    });
  }
);

// ════════════════════════════════════════════════════════════════════════════
//  GLICKO-2  helpers
// ════════════════════════════════════════════════════════════════════════════

/**
 * Fetches a user's current Glicko-2 stats from Firestore.
 * Falls back to default values (1500 / 350 / 0.06) if the fields are missing.
 */
async function getPlayerStats(db, userId) {
  const snap = await db.collection("users").where("id", "==", userId).limit(1).get();
  if (snap.empty) return { docRef: null, player: new Player() };
  const doc  = snap.docs[0];
  const data = doc.data();
  return {
    docRef : doc.ref,
    player : new Player(
      data.glickoRating ?? 1500,
      data.glickoRd     ?? 350,
      data.glickoVol    ?? 0.06,
    ),
  };
}

/**
 * Core Glicko-2 processing logic for a confirmed match.
 * Reads player stats, runs the algorithm, writes updated stats back.
 *
 * Outcome mapping  →  win = 1.0 | draw = 0.5 | loss = 0.0
 *
 * No-show handling: invited players whose invitation status is NOT "ACCEPTED"
 * are treated as having lost against every player who actually attended.
 */
async function processMatchGlicko2(matchId, matchData) {
  const db      = getFirestore();
  const saver   = matchData.resultsSavedByUserId || "";
  const scoreA  = matchData.scoreMyTeam  ?? 0;
  const scoreB  = matchData.scoreOpponent ?? 0;

  // ── Resolve absolute team player lists ───────────────────────────────────
  let groupA = [], groupB = [];

  if (matchData.matchType === "Team") {
    const teamsSnap = await db.collection("teams")
      .where("name", "in", [matchData.myTeam, matchData.opponent])
      .get();
    teamsSnap.docs.forEach(d => {
      const t = d.data();
      if (t.name === matchData.myTeam)    groupA = t.members || [];
      if (t.name === matchData.opponent)  groupB = t.members || [];
    });
    // scoreMyTeam is from the saver's team perspective
    if (groupB.includes(saver)) [groupA, groupB] = [groupB, groupA];
  } else {
    groupA = matchData.teamAPlayers || [];
    groupB = matchData.teamBPlayers || [];
    if (groupB.includes(saver)) [groupA, groupB] = [groupB, groupA];
  }

  if (groupA.length === 0 && groupB.length === 0) {
    console.warn(`glicko2: match ${matchId} has no resolvable teams — skipping.`);
    return 0;
  }

  // ── Identify no-shows ────────────────────────────────────────────────────
  // Players who were invited but whose status is NOT "ACCEPTED" and who are
  // not in either playing group are considered no-shows.
  const invitations = matchData.playerInvitations || {};
  const attendingSet = new Set([...groupA, ...groupB]);
  const noShowIds = Object.keys(invitations).filter(id =>
    invitations[id] !== "ACCEPTED" && !attendingSet.has(id)
  );

  // ── Outcome (from groupA's point of view) ────────────────────────────────
  // scoreA = groupA score (saver's side),  scoreB = groupB score
  const outcomeA = scoreA > scoreB ? 1.0 : scoreA === scoreB ? 0.5 : 0.0;
  const outcomeB = 1.0 - outcomeA; // symmetric

  // ── Fetch all player stats in parallel ───────────────────────────────────
  const allIds = [...new Set([...groupA, ...groupB, ...noShowIds])];
  const statsMap = {}; // userId -> { docRef, player }
  await Promise.all(allIds.map(async id => {
    statsMap[id] = await getPlayerStats(db, id);
  }));

  // ── Build opponent lists for each side ───────────────────────────────────
  const teamAStats = groupA.map(id => statsMap[id]?.player).filter(Boolean);
  const teamBStats = groupB.map(id => statsMap[id]?.player).filter(Boolean);

  const teamARatings  = teamAStats.map(p => p.rating);
  const teamARds      = teamAStats.map(p => p.rd);
  const teamBRatings  = teamBStats.map(p => p.rating);
  const teamBRds      = teamBStats.map(p => p.rd);

  // All attending players' stats (used as opponents for no-shows)
  const allAttendingStats = [...teamAStats, ...teamBStats];
  const allAttendingRatings = allAttendingStats.map(p => p.rating);
  const allAttendingRds     = allAttendingStats.map(p => p.rd);

  // ── Run Glicko-2 update ──────────────────────────────────────────────────
  const batch = db.batch();
  let updated = 0;

  // Attending players on group A face group B opponents
  for (const id of groupA) {
    const entry = statsMap[id];
    if (!entry?.docRef) continue;
    entry.player.updatePlayer(teamBRatings, teamBRds, Array(teamBRatings.length).fill(outcomeA));
    batch.update(entry.docRef, entry.player.toObject());
    updated++;
  }

  // Attending players on group B face group A opponents
  for (const id of groupB) {
    const entry = statsMap[id];
    if (!entry?.docRef) continue;
    entry.player.updatePlayer(teamARatings, teamARds, Array(teamARatings.length).fill(outcomeB));
    batch.update(entry.docRef, entry.player.toObject());
    updated++;
  }

  // No-shows: treated as a loss (0.0) against every attending player
  for (const id of noShowIds) {
    const entry = statsMap[id];
    if (!entry?.docRef) continue;
    if (allAttendingRatings.length > 0) {
      entry.player.updatePlayer(
        allAttendingRatings,
        allAttendingRds,
        Array(allAttendingRatings.length).fill(0.0) // loss for every attending opponent
      );
    } else {
      // No one attended — just increase RD uncertainty
      entry.player.didNotCompete();
    }
    batch.update(entry.docRef, entry.player.toObject());
    updated++;
  }

  // Mark match so it is never processed twice
  batch.update(db.collection("matches").doc(matchId), { glicko2Distributed: true });

  await batch.commit();
  console.log(`glicko2: processed match ${matchId} — ${updated} player(s) updated.`);
  return updated;
}

// ════════════════════════════════════════════════════════════════════════════
//  ENDPOINT 1 — Pure calculation  (no Firestore, stateless)
//
//  POST /glicko2Calculate
//  {
//    "player":    { "rating": 1500, "rd": 350, "vol": 0.06 },
//    "opponents": [
//      { "rating": 1400, "rd": 30,  "outcome": 1   },
//      { "rating": 1550, "rd": 100, "outcome": 0.5 },
//      { "rating": 1700, "rd": 300, "outcome": 0   }
//    ]
//  }
//  outcome: 1 = win | 0.5 = draw | 0 = loss
// ════════════════════════════════════════════════════════════════════════════
exports.glicko2Calculate = onRequest({ cors: true }, (req, res) => {
  if (req.method !== "POST") return res.status(405).json({ error: "POST only" });

  try {
    const { player: pd, opponents } = req.body;
    if (!pd || !Array.isArray(opponents) || opponents.length === 0) {
      return res.status(400).json({ error: "Provide player and a non-empty opponents array." });
    }

    const player = new Player(pd.rating ?? 1500, pd.rd ?? 350, pd.vol ?? 0.06);
    const ratingList  = opponents.map(o => o.rating);
    const rdList      = opponents.map(o => o.rd);
    const outcomeList = opponents.map(o => o.outcome);

    player.updatePlayer(ratingList, rdList, outcomeList);

    return res.json({ success: true, result: player.toObject() });
  } catch (err) {
    console.error("glicko2Calculate error:", err);
    return res.status(500).json({ error: err.message });
  }
});

// ════════════════════════════════════════════════════════════════════════════
//  ENDPOINT 2 — Process a match by ID  (reads + writes Firestore)
//
//  POST /glicko2ProcessMatch
//  { "matchId": "<firestoreMatchDocId>" }
// ════════════════════════════════════════════════════════════════════════════
exports.glicko2ProcessMatch = onRequest({ cors: true }, async (req, res) => {
  if (req.method !== "POST") return res.status(405).json({ error: "POST only" });

  try {
    const { matchId } = req.body;
    if (!matchId) return res.status(400).json({ error: "matchId is required." });

    const db        = getFirestore();
    const matchDoc  = await db.collection("matches").doc(matchId).get();
    if (!matchDoc.exists) return res.status(404).json({ error: "Match not found." });

    const matchData = matchDoc.data();
    if (!matchData.resultsConfirmed) {
      return res.status(400).json({ error: "Results are not yet confirmed for this match." });
    }
    if (matchData.glicko2Distributed) {
      return res.status(200).json({ success: true, message: "Already processed.", updated: 0 });
    }

    const updated = await processMatchGlicko2(matchId, matchData);
    return res.json({ success: true, updated });
  } catch (err) {
    console.error("glicko2ProcessMatch error:", err);
    return res.status(500).json({ error: err.message });
  }
});

// ════════════════════════════════════════════════════════════════════════════
//  TRIGGER — Auto-process Glicko-2 when a match result is confirmed
//
//  Fires automatically whenever a match document is updated in Firestore.
//  Runs Glicko-2 the first time resultsConfirmed flips to true.
// ════════════════════════════════════════════════════════════════════════════
exports.processGlicko2OnResultConfirmed = onDocumentUpdated(
  "matches/{matchId}",
  async (event) => {
    const before = event.data?.before?.data();
    const after  = event.data?.after?.data();
    if (!before || !after) return;

    // Only act when resultsConfirmed just became true and not yet processed
    if (before.resultsConfirmed || !after.resultsConfirmed) return;
    if (after.glicko2Distributed) return;

    try {
      await processMatchGlicko2(event.params.matchId, after);
    } catch (err) {
      console.error(`glicko2 trigger error for match ${event.params.matchId}:`, err);
    }
  }
);
