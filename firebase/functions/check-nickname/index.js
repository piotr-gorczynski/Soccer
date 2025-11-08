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
  safetySettings: [
    {
      category: "HARM_CATEGORY_HATE_SPEECH",
      threshold: "BLOCK_LOW_AND_ABOVE",
    },
    {
      category: "HARM_CATEGORY_DANGEROUS_CONTENT",
      threshold: "BLOCK_LOW_AND_ABOVE",
    },
    {
      category: "HARM_CATEGORY_HARASSMENT",
      threshold: "BLOCK_LOW_AND_ABOVE",
    },
    {
      category: "HARM_CATEGORY_SEXUALLY_EXPLICIT",
      threshold: "BLOCK_LOW_AND_ABOVE",
    },
  ],
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
    
    // Use a moderation prompt to ask the model to evaluate the content
    const prompt = `Evaluate if the following text is appropriate as a user nickname. Check for profanity, vulgar language, hate speech, offensive content, or inappropriate material. The text may contain obfuscation like spaces, numbers, or special characters mixed in with offensive words (e.g., "f u c k" or "fuck 2"). Respond with only "APPROPRIATE" or "INAPPROPRIATE".\n\nText to evaluate: "${nickname}"`;
    
    const response = await modelInstance.generateContent({
      contents: [{ role: "user", parts: [{ text: prompt }] }],
    });

    console.log("checkNickname: Vertex AI response received for nickname:", nickname);
    
    const safetyRatings = response.response?.candidates?.[0]?.safetyRatings ?? [];
    const textResponse = response.response?.candidates?.[0]?.content?.parts?.[0]?.text?.trim().toUpperCase() ?? "";
    
    // Log detailed safety ratings for debugging
    console.log("checkNickname: Safety ratings for nickname:", nickname, {
      safetyRatings: safetyRatings.map(rating => ({
        category: rating.category,
        probability: rating.probability,
      })),
      modelResponse: textResponse,
    });

    // Check if blocked by safety filters (HIGH or MEDIUM probability)
    const flaggedBySafety = safetyRatings.some(({ probability }) => probability === "HIGH" || probability === "MEDIUM");
    
    // Check if the model itself flagged it as inappropriate
    const flaggedByModel = textResponse.includes("INAPPROPRIATE");

    if (flaggedBySafety || flaggedByModel) {
      console.warn("checkNickname: Nickname BLOCKED due to content violations:", nickname, {
        flaggedBySafety,
        flaggedByModel,
        flaggedRatings: safetyRatings.filter(({ probability }) => probability === "HIGH" || probability === "MEDIUM"),
      });
      return { allowed: false, reason: "Nickname contains inappropriate language." };
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
