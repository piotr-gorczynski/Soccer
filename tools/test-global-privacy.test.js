// Run with: node --test tools/test-global-privacy.test.js
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const publicDir = path.join(root, 'firebase-hosting/public');
const read = name => fs.readFileSync(path.join(publicDir, name), 'utf8');
const manager = fs.readFileSync(path.join(root,
  'mobile/app/src/main/java/piotr_gorczynski/soccer2/LanguageManager.java'), 'utf8');
const languages = [...manager.matchAll(/LANGUAGE_NAME_RES_IDS\.put\("([a-z]+)"/g)]
  .map(match => match[1]).sort();

test('Global privacy covers every application language', () => {
  assert.ok(languages.length > 0);
  const actual = fs.readdirSync(publicDir)
    .filter(name => /^privacy-[a-z]+\.html$/.test(name))
    .map(name => name.slice(8, -5)).sort();
  assert.deepEqual(actual, languages);
  const sectionShape = html => [...html.matchAll(/<h2[^>]*>[\s\S]*?<\/h2>([\s\S]*?)(?=<h2|<\/div>)/g)]
    .map(match => (match[1].match(/<p>/g) || []).length);
  for (const lang of languages) {
    const html = read(`privacy-${lang}.html`);
    assert.ok(html.includes(`lang="${lang}"`), lang);
    assert.ok(html.includes('GLOBAL-privacy-2026-10-13'), lang);
    assert.ok(!html.includes('\uFFFD'), lang);
    assert.deepEqual(sectionShape(html), sectionShape(read('privacy-en.html')), lang);
    if (['ar', 'fa', 'ur'].includes(lang)) assert.ok(html.includes('dir="rtl"'), lang);
  }
});

test('all privacy translations are discoverable and local navigation resolves', () => {
  const home = read('index.html');
  for (const lang of languages) {
    assert.ok(home.includes(`href="privacy-${lang}.html"`), lang);
    const html = read(`privacy-${lang}.html`);
    const selector = html.match(/<p class="policy-languages">([\s\S]*?)<\/p>/)[1];
    const destinations = [...selector.matchAll(/href="privacy-([a-z]+)\.html"/g)]
      .map(match => match[1]).sort();
    assert.deepEqual(destinations, languages, lang);
    for (const page of [html, read(`terms-${lang}.html`)]) {
      const nav = page.match(/<nav>[\s\S]*?<\/nav>/)[0];
      assert.ok(nav.includes(`href="privacy-${lang}.html"`), lang);
    }
    for (const [, href] of html.matchAll(/href="([^"]+)"/g)) {
      if (!/^(https?:|mailto:|#)/.test(href)) {
        assert.ok(fs.existsSync(path.join(publicDir, href)), `${lang}: ${href}`);
      }
    }
  }
});

test('privacy.html remains the current English destination for immutable Terms', () => {
  assert.equal(read('privacy.html'), read('privacy-en.html'));
  for (const lang of languages) {
    assert.ok(read(`terms/GLOBAL-terms-2026-10-13/${lang}.html`)
      .includes('https://piotr-gorczynski.com/privacy.html'), lang);
  }
});
