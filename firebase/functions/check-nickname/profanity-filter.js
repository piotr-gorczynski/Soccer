/**
 * Profanity filter module that normalizes text and checks against a list of inappropriate words.
 * This provides a fallback to AI-based moderation for catching variants with numbers, spaces, etc.
 */

// Common profanity words in multiple languages
// This is a basic list - expand as needed
const PROFANITY_PATTERNS = [
  // English profanity
  'fuck', 'fuk', 'fck', 'fack', // variations of fuck
  'shit', 'sht', 'shyt', 'shiit', // variations of shit  
  'bitch', 'btch', 'biatch',
  'asshole', 'ashole', 'arsehole',
  'cunt', 'cnt',
  'dick', 'dik', 'dck',
  'cock', 'cok', 'cck',
  'pussy', 'psy', 'pusy',
  'bastard', 'bstrd',
  'damn', 'dmn',
  'piss', 'pis',
  'whore', 'whor', 'hore',
  'slut', 'slt',
  'fag', 'faggot', 'fagot',
  'nigger', 'nigga', 'niga',
  'retard', 'rtrd', 'retrd',
  // Common leetspeak and variants will be caught by normalization
  
  // Polish profanity (common in this app based on logs)
  'kurwa', 'kurw', 'krwa',
  'chuj', 'huj',
  'kurde',
  'pizda', 'pzda',
  'dupa', 'dup',
  'gówno', 'gowno',
  'szmata', 'szmta',
  'dziwka', 'dzwka',
  'skurwysyn', 'skrwysyn',
  'jebać', 'jebac', 'jeba',
  'pierdol', 'perdol',
  'ciota', 'ciot',
  'pedale', 'pedal',
  'cwel',
  
  // Other common patterns
  'nazi', 'nzi',
  'hitler', 'htler',
  'kkk',
];

/**
 * Normalizes text by:
 * - Converting to lowercase
 * - Removing numbers
 * - Removing spaces
 * - Removing special characters
 * - Handling common substitutions (0->o, 1->i/l, 3->e, 4->a, 5->s, 7->t, 8->b, @->a, $->s)
 * 
 * This helps catch variants like "fuck 2", "f u c k", "f@ck", "fvck", etc.
 */
function normalizeText(text) {
  return text
    .toLowerCase()
    // Common leetspeak substitutions
    .replace(/0/g, 'o')
    .replace(/1/g, 'i')
    .replace(/3/g, 'e')
    .replace(/4/g, 'a')
    .replace(/5/g, 's')
    .replace(/7/g, 't')
    .replace(/8/g, 'b')
    .replace(/@/g, 'a')
    .replace(/\$/g, 's')
    .replace(/\+/g, 't')
    .replace(/!/g, 'i')
    .replace(/\|/g, 'i')
    // Remove all non-letter characters (spaces, numbers, punctuation)
    .replace(/[^a-z]/g, '');
}

/**
 * Checks if the given text contains profanity.
 * Returns true if profanity is detected, false otherwise.
 */
export function containsProfanity(text) {
  if (!text || typeof text !== 'string') {
    return false;
  }

  const normalized = normalizeText(text);
  
  // Check if any profanity pattern is contained in the normalized text
  for (const pattern of PROFANITY_PATTERNS) {
    if (normalized.includes(pattern)) {
      console.log(`Profanity detected: "${text}" normalized to "${normalized}" contains "${pattern}"`);
      return true;
    }
  }
  
  return false;
}

/**
 * Gets a sanitized version of the profanity that was detected (for logging purposes).
 * Returns null if no profanity is detected.
 */
export function getDetectedProfanity(text) {
  if (!text || typeof text !== 'string') {
    return null;
  }

  const normalized = normalizeText(text);
  
  for (const pattern of PROFANITY_PATTERNS) {
    if (normalized.includes(pattern)) {
      // Return censored version for logging
      return pattern.charAt(0) + '*'.repeat(pattern.length - 1);
    }
  }
  
  return null;
}
