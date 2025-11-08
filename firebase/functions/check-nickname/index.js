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
  model: "gemini-2.5-flash",
});

export const checkNickname = onCall({ region: "us-central1" }, async (request) => {
  const nickname = (request.data?.nickname ?? "").trim();

  // Log the incoming request for debugging
  console.log("checkNickname called with nickname:", nickname);

  if (!nickname) {
    console.error("checkNickname: Nickname is empty");
    throw new HttpsError("invalid-argument", "Nickname is empty.");
  }

  try {
    console.log("checkNickname: Calling Vertex AI for nickname moderation:", nickname);
    
    const response = await modelInstance.generateContent({
      contents: [{ role: "user", parts: [{ text: nickname }] }],
    });

    console.log("checkNickname: Vertex AI response received for nickname:", nickname);
    
    const safetyRatings = response.response?.candidates?.[0]?.safetyRatings ?? [];
    
    // Log detailed safety ratings for debugging
    console.log("checkNickname: Safety ratings for nickname:", nickname, {
      safetyRatings: safetyRatings.map(rating => ({
        category: rating.category,
        probability: rating.probability,
      })),
    });

    const flagged = safetyRatings.some(({ probability }) => probability === "HIGH" || probability === "MEDIUM");

    if (flagged) {
      console.warn("checkNickname: Nickname BLOCKED due to content violations:", nickname, {
        flaggedRatings: safetyRatings.filter(({ probability }) => probability === "HIGH" || probability === "MEDIUM"),
      });
      return { allowed: false, reason: "Nickname violates content rules." };
    }

    console.log("checkNickname: Nickname ALLOWED:", nickname);
    return { allowed: true };
  } catch (error) {
    // Log detailed error information for debugging
    console.error("checkNickname: Vertex AI moderation FAILED for nickname:", nickname, {
      error: error.message,
      code: error.code,
      details: error.details,
      stack: error.stack,
    });

    // Check for specific error types and provide appropriate fallback
    // Permission denied - API not enabled or insufficient permissions
    if (error.code === GRPC_PERMISSION_DENIED || error.message?.toUpperCase().includes("PERMISSION_DENIED")) {
      console.warn("checkNickname: FALLBACK ACTIVATED - Vertex AI permission denied - allowing nickname by default:", nickname);
      return { allowed: true };
    }

    // Service unavailable - temporary outage or network issues
    if (error.code === GRPC_UNAVAILABLE || error.message?.toUpperCase().includes("UNAVAILABLE")) {
      console.warn("checkNickname: FALLBACK ACTIVATED - Vertex AI service unavailable - allowing nickname by default:", nickname);
      return { allowed: true };
    }

    // Authentication issues - API key or credentials problems
    if (error.message?.toUpperCase().includes("API KEY") || 
        error.message?.toUpperCase().includes("AUTHENTICATION") ||
        error.message?.toUpperCase().includes("UNAUTHENTICATED")) {
      console.error("checkNickname: FALLBACK ACTIVATED - Vertex AI authentication error - allowing nickname by default:", nickname);
      return { allowed: true };
    }

    // For unknown errors, log and allow the nickname to avoid blocking users
    console.warn("checkNickname: FALLBACK ACTIVATED - Unknown Vertex AI error - allowing nickname by default as fallback:", nickname);
    return { allowed: true };
  }
});
