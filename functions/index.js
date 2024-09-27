const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendPushNotification = functions.https.onCall(async (data, context) => {
  const {guruId, guruFCMToken, callRequestId, callerName, callType} = data;

  // Ensure the user is authenticated
  if (!context.auth || !context) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated to send notifications.",
    );
  }

  try {
    if (!guruFCMToken) {
      // If FCM token is not provided, fetch it from Firestore
      const guruDoc = await admin.firestore()
          .collection("Users")
          .doc(guruId)
          .get();
      if (!guruDoc.exists) {
        throw new Error("Guru not found");
      }
      const fetchedToken = guruDoc.data().fcmToken;
      if (!fetchedToken) {
        throw new Error("FCM token not found for the guru");
      }
      data.guruFCMToken = fetchedToken;
    }

    const message = {
      notification: {
        title: "Incoming Call",
        body: `${callerName} is requesting a ${callType} call`,
      },
      data: {
        callRequestId: callRequestId,
        callType: callType,
        callerName: callerName,
      },
      token: data.guruFCMToken,
    };

    // Send the message
    const response = await admin.messaging().send(message);
    console.log("Successfully sent message:", response);
    return {success: true, message: "Notification sent successfully"};
  } catch (error) {
    console.error("Error sending notification:", error);
    throw new functions.https.HttpsError("internal",
        "Error sending notification", error);
  }
});

// Add a blank line here (after this comment, in the actual code)
