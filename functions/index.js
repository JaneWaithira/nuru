const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const RATE_LIMIT = 5; // Maximum allowed login attempts
const TIME_WINDOW = 5 * 60 * 1000;

exports.limitLoginAttempts = functions.https.onCall(async (data, context) => {
  const ip = context.rawRequest.ip;
  const timestamp = admin.firestore.FieldValue.serverTimestamp();

  const rateLimitRef = admin.firestore().collection("rateLimits").doc(ip);
  const doc = await rateLimitRef.get();

  if (!doc.exists || doc.data().timestamp.toMillis() < Date.now() -
  TIME_WINDOW) {
    await rateLimitRef.set({attempts: 1, timestamp});
    return {allowed: true};
  } else {
    const attempts = doc.data().attempts;
    if (attempts < RATE_LIMIT) {
      await rateLimitRef.update({
        attempts: admin.firestore.FieldValue.increment(1),
      });
      return {allowed: true};
    } else {
      return {
        allowed: false,
        message: "Too many attempts, please try again later.",
      };
    }
  }
});
