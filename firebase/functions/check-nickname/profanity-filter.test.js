import { describe, it } from 'node:test';
import assert from 'node:assert';
import { containsProfanity, getDetectedProfanity } from './profanity-filter.js';

describe('Profanity Filter', () => {
  describe('containsProfanity', () => {
    it('should detect basic profanity', () => {
      assert.strictEqual(containsProfanity('fuck'), true);
      assert.strictEqual(containsProfanity('shit'), true);
      assert.strictEqual(containsProfanity('bitch'), true);
    });

    it('should detect profanity with numbers', () => {
      assert.strictEqual(containsProfanity('fuck 2'), true);
      assert.strictEqual(containsProfanity('f4ck'), true);
      assert.strictEqual(containsProfanity('sh1t'), true);
    });

    it('should detect profanity with spaces', () => {
      assert.strictEqual(containsProfanity('f u c k'), true);
      assert.strictEqual(containsProfanity('s h i t'), true);
    });

    it('should detect profanity with special characters', () => {
      assert.strictEqual(containsProfanity('f@ck'), true);
      assert.strictEqual(containsProfanity('$hit'), true);
      assert.strictEqual(containsProfanity('f*ck'), true);
    });

    it('should detect profanity with mixed obfuscation', () => {
      assert.strictEqual(containsProfanity('f u c k 2'), true);
      assert.strictEqual(containsProfanity('f@ck you'), true);
      assert.strictEqual(containsProfanity('sh!t 123'), true);
    });

    it('should detect profanity with case variations', () => {
      assert.strictEqual(containsProfanity('FUCK'), true);
      assert.strictEqual(containsProfanity('FuCk'), true);
      assert.strictEqual(containsProfanity('ShIt'), true);
    });

    it('should detect Polish profanity', () => {
      assert.strictEqual(containsProfanity('kurwa'), true);
      assert.strictEqual(containsProfanity('k u r w a'), true);
      assert.strictEqual(containsProfanity('kurwa 123'), true);
    });

    it('should not flag clean nicknames', () => {
      assert.strictEqual(containsProfanity('John'), false);
      assert.strictEqual(containsProfanity('Player1'), false);
      assert.strictEqual(containsProfanity('CoolGamer'), false);
      assert.strictEqual(containsProfanity('Soccer123'), false);
    });

    it('should handle empty and invalid inputs', () => {
      assert.strictEqual(containsProfanity(''), false);
      assert.strictEqual(containsProfanity(null), false);
      assert.strictEqual(containsProfanity(undefined), false);
    });

    it('should detect profanity embedded in longer text', () => {
      assert.strictEqual(containsProfanity('myfuckingname'), true);
      assert.strictEqual(containsProfanity('shit4brains'), true);
    });
  });

  describe('getDetectedProfanity', () => {
    it('should return censored profanity pattern', () => {
      const result = getDetectedProfanity('fuck');
      assert.ok(result !== null);
      assert.ok(result.startsWith('f'));
      assert.ok(result.includes('*'));
    });

    it('should return null for clean text', () => {
      assert.strictEqual(getDetectedProfanity('John'), null);
      assert.strictEqual(getDetectedProfanity('Player1'), null);
    });

    it('should handle the reported issue case', () => {
      // This is the exact case from the issue report
      assert.strictEqual(containsProfanity('fuck 2'), true);
      const detected = getDetectedProfanity('fuck 2');
      assert.ok(detected !== null);
    });
  });
});
