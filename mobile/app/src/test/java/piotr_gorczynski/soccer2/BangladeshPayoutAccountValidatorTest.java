package piotr_gorczynski.soccer2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BangladeshPayoutAccountValidatorTest {

    @Test
    public void normalizesInternationalAndFormattedBangladeshNumbers() {
        assertEquals("01712345678", BangladeshPayoutAccountValidator.normalize("+880 1712-345678"));
        assertEquals("01712345678", BangladeshPayoutAccountValidator.normalize("8801712345678"));
        assertEquals("01712345678", BangladeshPayoutAccountValidator.normalize("01712 345 678"));
    }

    @Test
    public void validatesBkashAndNagadMobileNumbers() {
        assertTrue(BangladeshPayoutAccountValidator.isValid("bkash", "01712345678"));
        assertTrue(BangladeshPayoutAccountValidator.isValid("nagad", "01312345678"));
        assertFalse(BangladeshPayoutAccountValidator.isValid("bkash", "01212345678"));
        assertFalse(BangladeshPayoutAccountValidator.isValid("nagad", "017123456789"));
    }

    @Test
    public void validatesRocketAccountNumberWithCheckDigit() {
        assertTrue(BangladeshPayoutAccountValidator.isValid("rocket", "017123456789"));
        assertFalse(BangladeshPayoutAccountValidator.isValid("rocket", "01712345678"));
        assertFalse(BangladeshPayoutAccountValidator.isValid("rocket", "012123456789"));
    }

    @Test
    public void retainsGenericValidationForFutureRegulationMethods() {
        assertTrue(BangladeshPayoutAccountValidator.isValid("future_wallet", "1234567890"));
        assertFalse(BangladeshPayoutAccountValidator.isValid("future_wallet", "123"));
    }
}
