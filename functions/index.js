const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

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

