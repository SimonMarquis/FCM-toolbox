const {onCall} = require("firebase-functions/v2/https");
const {onRequest} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");

const admin = require("firebase-admin");
const {initializeApp} = require("firebase-admin/app");
const {HttpsError} = require("firebase-functions/v1/auth");

initializeApp({
  credential: admin.credential.applicationDefault(),
});

exports.ping = onRequest((request, response) => {
  logger.info("Ping!");
  response.send("Pong!");
});

exports.send = onCall((request) => {
  logger.log("Request", request);
  logger.log("Request data", request.data);

  const to = request.data.to;
  if (!to) throw new HttpsError("invalid-argument", "'to' must exist");

  const ttl = parseInt(request.data.ttl);
  if (ttl < 0 || ttl > 2419200) throw new HttpsError("invalid-argument", "'ttl' must be [0 .. 2 419 200] (28 days)");

  const priorities = ["normal", "high"];
  const priority = request.data.priority;
  if (!priorities.includes(priority)) throw new HttpsError("invalid-argument", `'priority' must be one of: ${priorities.join()}`);

  const data = request.data.data;
  if (!data) throw new HttpsError("invalid-argument", "'data' must exist");

  // FirebaseMessagingError: data must only contain string value
  const convertData = (obj) => Object.fromEntries(Object.entries(obj).map(([k, v]) => [k, (typeof v === "string" || v instanceof String) ? v : JSON.stringify(v)]));

  const message = {
    data: convertData(data),
    android: {
      priority: priority,
      ttl: ttl * 1000,
    },
  };

  // Select token or topic
  message[to.startsWith("/topics/") ? "topic" : "token"] = to;

  logger.log("Sending message:", message);
  return admin.messaging()
      .send(message)
      .then((data) => {
        logger.log("Success:", data);
        return data;
      })
      .catch((error) => {
        logger.error("Failure:", error);
        throw new HttpsError("internal", error.message, error);
      });
});
