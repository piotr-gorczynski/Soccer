import { HttpsError, onCall } from "firebase-functions/v2/https";
import { VertexAI } from "@google-cloud/vertexai";

const projectId = process.env.GCLOUD_PROJECT ?? process.env.GOOGLE_CLOUD_PROJECT;
const location = "us-central1";

// gRPC error codes
const GRPC_PERMISSION_DENIED = 7;
const GRPC_UNAVAILABLE = 14;

if (!projectId) {
  throw new Error("Project ID is not set in GCLOUD_PROJECT or GOOGLE_CLOUD_PROJECT environment variables.");
}

const vertex = new VertexAI({ project: projectId, location });
const modelInstance = vertex.getGenerativeModel({
  model: "gemini-1.5-flash",
});

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
    // Log detailed error information for debugging
    console.error("Vertex AI moderation failed for nickname check:", {
      nickname,
      error: error.message,
      code: error.code,
      details: error.details,
      stack: error.stack,
    });

    // Check for specific error types and provide appropriate fallback
    // Permission denied - API not enabled or insufficient permissions
    if (error.code === GRPC_PERMISSION_DENIED || error.message?.toUpperCase().includes("PERMISSION_DENIED")) {
      console.warn("Vertex AI permission denied - allowing nickname by default");
      return { allowed: true };
    }

    // Service unavailable - temporary outage or network issues
    if (error.code === GRPC_UNAVAILABLE || error.message?.toUpperCase().includes("UNAVAILABLE")) {
      console.warn("Vertex AI service unavailable - allowing nickname by default");
      return { allowed: true };
    }

    // Authentication issues - API key or credentials problems
    if (error.message?.toUpperCase().includes("API KEY") || 
        error.message?.toUpperCase().includes("AUTHENTICATION") ||
        error.message?.toUpperCase().includes("UNAUTHENTICATED")) {
      console.error("Vertex AI authentication error - allowing nickname by default");
      return { allowed: true };
    }

    // For unknown errors, log and allow the nickname to avoid blocking users
    console.warn("Unknown Vertex AI error - allowing nickname by default as fallback");
    return { allowed: true };
  }
});
