const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

exports.forwardChildEventToParent = onDocumentCreated(
  "users/{childUid}/{eventCollection}/{eventId}",
  async (event) => {
    const childUid = event.params.childUid;
    const collection = event.params.eventCollection;
    const data = event.data?.data();
    if (!data || !["notifications", "telephonyEvents", "cameraCaptures"].includes(collection)) return;

    const child = await getFirestore().doc("users/" + childUid).get();
    const parentUid = child.data()?.parentUid;
    if (!parentUid) return;

    const parent = await getFirestore().doc("users/" + parentUid).get();
    const token = parent.data()?.fcmToken;
    if (!token) return;

    const title = collection === "notifications"
      ? "Child app notification"
      : collection === "telephonyEvents"
        ? "Child call/SMS event"
        : "New camera capture";

    const body = collection === "notifications"
      ? ((data.title || data.packageName || "Notification") + (data.text ? ": " + data.text : ""))
      : collection === "telephonyEvents"
        ? ((data.type || "Telephony event") + ": " + (data.contactName ? data.contactName + " — " : "") + (data.number || "number unavailable") + (data.message ? ": " + data.message : ""))
        : "A new camera photo is available.";

    await getMessaging().send({
      token,
      notification: { title, body },
      data: { childUid, eventCollection: collection, eventId: event.params.eventId },
    });
  }
);
