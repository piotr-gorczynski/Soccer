import { HttpsError, onCall } from "firebase-functions/v2/https";
import { VertexAI } from "@google-cloud/vertexai";

const vertex = new VertexAI({ project: process.env.GCLOUD_PROJECT, location: "us-central1" });
const modelInstance = vertex.getGenerativeModel({ model: "publishers/google/models/text-moderation" });

export const checkNickname = onCall({ region: "us-central1" }, async (request) => {
  const nickname = (request.data?.nickname ?? "").trim();

  if (!nickname) {
    throw new HttpsError("invalid-argument", "Nickname is empty.");
  }

  try {
    const response = await modelInstance.generateContent({
      contents: [{ role: "user", parts: [{ text: nickname }] }],
    });

    const safetyRatings = response.response?.candidates?.[0]?.safetyRatings ?? [];
    const flagged = safetyRatings.some(({ probability }) => probability === "HIGH" || probability === "MEDIUM");

    if (flagged) {
      return { allowed: false, reason: "Nickname violates content rules." };
    }

    return { allowed: true };
  } catch (error) {
    console.error("Vertex moderation failed", error);
    throw new HttpsError("internal", "Failed to verify nickname.");
  }
});
