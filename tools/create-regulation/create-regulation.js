'use strict';

const fs = require('fs');
const path = require('path');

const ENVIRONMENTS = new Set(['dev', 'test', 'prod']);
const REGULATION_STATUSES = new Set(['draft', 'active', 'inactive', 'archived']);
const MARKET_PATTERN = /^[A-Z]{2}$/;
const CURRENCY_PATTERN = /^[A-Z]{3}$/;
const LANGUAGE_PATTERN = /^[a-z]{2,3}(?:-[A-Z]{2})?$/;
const PAYOUT_METHOD_PATTERN = /^[a-z][a-z0-9_]*$/;

function fail(message) {
  throw new Error(message);
}

function isPlainObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function requireNonEmptyString(value, fieldName, maxLength = 500) {
  if (typeof value !== 'string' || value.trim().length === 0) {
    fail(`\`${fieldName}\` must be a non-empty string.`);
  }
  if (value.length > maxLength) {
    fail(`\`${fieldName}\` must not exceed ${maxLength} characters.`);
  }
  return value.trim();
}

function validateRules(rules, fieldName) {
  if (!Array.isArray(rules) || rules.length === 0) {
    fail(`\`${fieldName}\` must be a non-empty array.`);
  }

  return rules.map((rule, index) =>
    requireNonEmptyString(rule, `${fieldName}[${index}]`, 5000)
  );
}

function validateTranslations(translations) {
  if (!isPlainObject(translations) || Object.keys(translations).length === 0) {
    fail('`translations` must be a non-empty object keyed by language code.');
  }

  const validated = {};
  for (const [language, translation] of Object.entries(translations)) {
    if (!LANGUAGE_PATTERN.test(language)) {
      fail(`Invalid translation language code: ${language}.`);
    }
    if (!isPlainObject(translation)) {
      fail(`\`translations.${language}\` must be an object.`);
    }

    validated[language] = {
      rules: validateRules(translation.rules, `translations.${language}.rules`),
    };
  }

  return validated;
}

function validatePrizePool(prizePool) {
  if (!isPlainObject(prizePool)) {
    fail('`prizeRules.prizePool` must be an object.');
  }
  if (!Number.isSafeInteger(prizePool.totalAmount) ||
      prizePool.totalAmount <= 0) {
    fail('`prizeRules.prizePool.totalAmount` must be a positive integer.');
  }
  if (!Array.isArray(prizePool.awards) || prizePool.awards.length === 0) {
    fail('`prizeRules.prizePool.awards` must be a non-empty array.');
  }

  const awards = prizePool.awards.map((award, index) => {
    if (!isPlainObject(award)) {
      fail(`\`prizeRules.prizePool.awards[${index}]\` must be an object.`);
    }
    if (!Number.isSafeInteger(award.place) || award.place <= 0) {
      fail(`\`prizeRules.prizePool.awards[${index}].place\` must be a positive integer.`);
    }
    if (!Number.isSafeInteger(award.amount) || award.amount <= 0) {
      fail(`\`prizeRules.prizePool.awards[${index}].amount\` must be a positive integer.`);
    }
    return { place: award.place, amount: award.amount };
  });

  const places = awards.map(award => award.place);
  if (new Set(places).size !== places.length) {
    fail('`prizeRules.prizePool.awards` must not contain duplicate places.');
  }

  const allocatedAmount = awards.reduce((sum, award) => sum + award.amount, 0);
  if (allocatedAmount !== prizePool.totalAmount) {
    fail('The sum of award amounts must equal `prizeRules.prizePool.totalAmount`.');
  }

  return {
    totalAmount: prizePool.totalAmount,
    awards: awards.sort((left, right) => left.place - right.place),
  };
}

function validatePrizeRules(prizeRules, market, minimumAge) {
  if (prizeRules === undefined) return undefined;
  if (!isPlainObject(prizeRules)) {
    fail('`prizeRules` must be an object.');
  }
  if (typeof prizeRules.cashPrizesEnabled !== 'boolean') {
    fail('`prizeRules.cashPrizesEnabled` must be a boolean.');
  }

  const validated = {
    cashPrizesEnabled: prizeRules.cashPrizesEnabled,
  };

  if (!prizeRules.cashPrizesEnabled) {
    return validated;
  }

  if (!market) {
    fail('`market` is required when cash prizes are enabled.');
  }
  if (minimumAge === undefined) {
    fail('`minimumAge` is required when cash prizes are enabled.');
  }
  if (typeof prizeRules.currency !== 'string' ||
      !CURRENCY_PATTERN.test(prizeRules.currency)) {
    fail('`prizeRules.currency` must be a three-letter uppercase currency code.');
  }
  if (!Array.isArray(prizeRules.payoutMethods) ||
      prizeRules.payoutMethods.length === 0) {
    fail('`prizeRules.payoutMethods` must be a non-empty array when cash prizes are enabled.');
  }

  const payoutMethods = prizeRules.payoutMethods.map((method, index) => {
    if (typeof method !== 'string' || !PAYOUT_METHOD_PATTERN.test(method)) {
      fail(`Invalid payout method at \`prizeRules.payoutMethods[${index}]\`.`);
    }
    return method;
  });

  if (new Set(payoutMethods).size !== payoutMethods.length) {
    fail('`prizeRules.payoutMethods` must not contain duplicates.');
  }

  validated.currency = prizeRules.currency;
  validated.payoutMethods = payoutMethods;
  validated.prizePool = validatePrizePool(prizeRules.prizePool);
  return validated;
}

function validateRegulation(input) {
  if (!isPlainObject(input)) {
    fail('The regulation JSON root must be an object.');
  }

  const name = requireNonEmptyString(input.name, 'name', 200);
  const status = input.status || 'draft';
  if (!REGULATION_STATUSES.has(status)) {
    fail(`\`status\` must be one of: ${[...REGULATION_STATUSES].join(', ')}.`);
  }

  let market;
  if (input.market !== undefined) {
    market = requireNonEmptyString(input.market, 'market', 2);
    if (!MARKET_PATTERN.test(market)) {
      fail('`market` must be an ISO 3166-1 alpha-2 uppercase country code.');
    }
  }

  let minimumAge;
  if (input.minimumAge !== undefined) {
    if (!Number.isInteger(input.minimumAge) ||
        input.minimumAge < 0 || input.minimumAge > 120) {
      fail('`minimumAge` must be an integer between 0 and 120.');
    }
    minimumAge = input.minimumAge;
  }

  const translations = validateTranslations(input.translations);
  const prizeRules = validatePrizeRules(input.prizeRules, market, minimumAge);

  let body;
  if (input.body !== undefined) {
    body = requireNonEmptyString(input.body, 'body', 100000);
  } else if (translations.en) {
    body = translations.en.rules.map(rule => `• ${rule}`).join('\n\n');
  } else {
    const firstLanguage = Object.keys(translations)[0];
    body = translations[firstLanguage].rules.map(rule => `• ${rule}`).join('\n\n');
  }

  return {
    root: {
      name,
      body,
      status,
      ...(market !== undefined ? { market } : {}),
      ...(minimumAge !== undefined ? { minimumAge } : {}),
      ...(prizeRules !== undefined ? { prizeRules } : {}),
    },
    translations,
  };
}

function parseArguments(argv) {
  const args = [...argv];
  const environment = args.shift();
  const jsonPath = args.shift();
  const dryRun = args.length === 1 && args[0] === '--dry-run';

  if (!ENVIRONMENTS.has(environment) || !jsonPath ||
      (args.length > 0 && !dryRun)) {
    fail('Usage: node create-regulation.js <dev|test|prod> <regulation.json> [--dry-run]');
  }

  return { environment, jsonPath, dryRun };
}

function loadJson(jsonPath) {
  const absolutePath = path.resolve(jsonPath);
  let contents;
  try {
    contents = fs.readFileSync(absolutePath, 'utf8');
  } catch (error) {
    fail(`Cannot read regulation file ${absolutePath}: ${error.message}`);
  }

  try {
    return { absolutePath, data: JSON.parse(contents) };
  } catch (error) {
    fail(`Invalid JSON in ${absolutePath}: ${error.message}`);
  }
}

async function createRegulation(db, admin, regulation) {
  const regulationRef = db.collection('regulations').doc();
  const batch = db.batch();

  batch.create(regulationRef, {
    ...regulation.root,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  for (const [language, translation] of Object.entries(regulation.translations)) {
    batch.create(regulationRef.collection(language).doc('rules'), {
      rules: translation.rules,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }

  await batch.commit();
  return regulationRef.id;
}

async function main(argv = process.argv.slice(2)) {
  const { environment, jsonPath, dryRun } = parseArguments(argv);
  const { absolutePath, data } = loadJson(jsonPath);
  const regulation = validateRegulation(data);

  console.log(`Validated regulation: ${regulation.root.name}`);
  console.log(`Source: ${absolutePath}`);
  console.log(`Environment: ${environment}`);

  if (dryRun) {
    console.log('Dry run complete. No Firestore data was written.');
    return;
  }

  const serviceAccountPath = path.join(
    __dirname,
    '..',
    '..',
    'secrets',
    `serviceAccountKey.${environment}.json`
  );
  if (!fs.existsSync(serviceAccountPath)) {
    fail(`Service account key not found: ${serviceAccountPath}`);
  }

  const admin = require('firebase-admin');
  const serviceAccount = require(serviceAccountPath);
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });

  const regulationId = await createRegulation(admin.firestore(), admin, regulation);
  console.log(`Created regulation with Firestore ID: ${regulationId}`);
}

if (require.main === module) {
  main().catch(error => {
    console.error(`Failed to create regulation: ${error.message}`);
    process.exitCode = 1;
  });
}

module.exports = {
  createRegulation,
  parseArguments,
  validateRegulation,
};
