//
// Swiss QR Bill Generator
// Copyright (c) 2018 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//

package net.codecrete.qrbill.generator;

import java.text.Normalizer;


/**
 * Field validations related to Swiss Payment standards
 */
public class Payments {

    private Payments() {
        // Do not create instances
    }

    /** Result of cleaning a string value */
    static class CleaningResult {
        /** Cleaned string */
        String cleanedString;
        /** Flag indicating that unsupported characters have been replaced */
        boolean replacedUnsupportedChars;
    }

    /**
     * Cleans a string value.
     * <p>
     *     Unsupported characters (according to Swiss Payment Standards 2018, ch. 2.4.1 and appendix D)
     *     are replaced with spaces (unsupported whitespace)
     *     or dots (all other unsupported characters). Leading and trailing
     *     whitespace is removed.
     * </p>
     * <p>
     *     If characters beyond 0xff are detected, the string is first normalized
     *     such that letters with umlauts or accents expressed with two code points
     *     are merged into a single code point (if possible), some of which might
     *     become valid.
     * </p>
     * <p>
     *     If the resulting strings is all white space, {@code null} is
     *     returned and no warning is added.
     * </p>
     * @param value string value to clean
     * @param result result to be filled with cleaned string and flag 
     */
    static void cleanValue(String value, CleaningResult result) {
        result.cleanedString = null;
        result.replacedUnsupportedChars = false;
        cleanValue(value, result, false);
        if (result.cleanedString != null && result.cleanedString.length() == 0)
            result.cleanedString = null;
    }

    private static void cleanValue(String value, CleaningResult result, boolean isNormalized) {
        /* This code has cognitive complexity 30. Deal with it. */
        if (value == null)
            return;

        int len = value.length(); // length of value
        boolean justProcessedSpace = false; // flag indicating whether we've just processed a space character
        StringBuilder sb = null; // String builder for result
        int lastCopiedPos = 0; // last position (excluding) copied to the result

        // String processing pattern: Iterate all characters and focus on runs of valid characters
        // that can simply be copied. If all characters are valid, no memory is allocated.
        int pos = 0;
        while (pos < len) {
            char ch = value.charAt(pos); // current character

            if (Payments.isValidQRBillCharacter(ch)) {
                justProcessedSpace = ch == ' ';
                pos++;
                continue;
            }

            // Check for normalization
            if (ch > 0xff && !isNormalized) {
                isNormalized = Normalizer.isNormalized(value, Normalizer.Form.NFC);
                if (!isNormalized) {
                    // Normalize string and start over
                    value = Normalizer.normalize(value, Normalizer.Form.NFC);
                    cleanValue(value, result, true);
                    return;
                }
            }

            if (sb == null)
                sb = new StringBuilder(value.length());

            // copy processed characters to result before taking care of the invalid character
            if (pos > lastCopiedPos)
                sb.append(value, lastCopiedPos, pos);

            if (Character.isHighSurrogate(ch)) {
                // Proper Unicode handling to prevent surrogates and combining characters
                // from being replaced with multiples periods.
                int codePoint = value.codePointAt(pos);
                if (Character.getType(codePoint) != Character.COMBINING_SPACING_MARK)
                    sb.append('.');
                justProcessedSpace = false;
                pos++;
            } else {
                if (Character.isWhitespace(ch)) {
                    if (!justProcessedSpace)
                        sb.append(' ');
                    justProcessedSpace = true;
                } else {
                    sb.append('.');
                    justProcessedSpace = false;
                }
            }
            pos++;
            lastCopiedPos = pos;
        }

        if (sb == null) {
            result.cleanedString = value.trim();
            return;
        }

        if (lastCopiedPos < len)
            sb.append(value, lastCopiedPos, len);

        result.cleanedString = sb.toString().trim();
        result.replacedUnsupportedChars = true;
    }

    /**
     * Validates if the string is a valid IBAN number
     * <p>
     *   All whitespace must have been removed before the
     *   the validation is performed.
     * </p>
     * <p>
     *   The string is checked for valid characters, valid length
     *   and for a valid check digit.
     * </p>
     * @param iban IBAN to validate
     * @return {@code true} if the IBAN is valid, {@code false} otherwise
     */
    public static boolean isValidIBAN(String iban) {
        if (iban.length() < 5)
            return false;
        if (!isAlphaNumeric(iban))
            return false;
        if (!Character.isLetter(iban.charAt(0)) || !Character.isLetter(iban.charAt(1))
                || !Character.isDigit(iban.charAt(2)) || !Character.isDigit(iban.charAt(3)))
            return false;

        return hasValidMod97CheckDigits(iban);
    }
    
    /**
     * Formats a IBAN or creditor reference by inserting spaces.
     * <p>
     * Spaces are inserted to form groups of 4 letters/digits.
     * If a group of less than 4 letters/digits is needed, it
     * appears at the end.
     * </p>
     * 
     * @param iban IBAN or creditor reference without white space
     * @return formatted IBAN or creditor reference
     */
    public static String formatIBAN(String iban) {
        StringBuilder sb = new StringBuilder(25);
        int len = iban.length();

        for (int pos = 0; pos < len; pos += 4) {
            int endPos = pos + 4;
            if (endPos > len)
                endPos = len;
            sb.append(iban, pos, endPos);
            if (endPos != len)
                sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * Validates if the string is a valid ISO 11649 reference number.
     * <p>
     *   All whitespace must have been removed before the
     *   the validation is performed.
     * </p>
     * <p>
     *   The string is checked for valid characters, valid length
     *   and a valid check digit.
     * </p>
     * @param referenceNo ISO 11649 creditor reference to validate
     * @return {@code true} if the creditor reference is valid, {@code false} otherwise
     */
    public static boolean isValidISO11649ReferenceNo(String referenceNo) {
        if (referenceNo.length() < 5 || referenceNo.length() > 25)
            return false;

        if (!isAlphaNumeric(referenceNo))
            return false;

        if (!Character.isDigit(referenceNo.charAt(2)) || !Character.isDigit(referenceNo.charAt(3)))
            return false;

        return hasValidMod97CheckDigits(referenceNo);
    }

    /**
     * Creates a ISO11649 creditor reference from a raw string by prefixing the string with "RF"
     * and the modulo 97 checksum.
     * <p>
     * Whitespace is removed from the reference
     * </p>
     * @param rawReference The raw string
     * @return ISO11649 creditor reference
     * @throws IllegalArgumentException if {@code rawReference} contains invalid characters
     */
    public static String createISO11649Reference(String rawReference) {
        final String whiteSpaceRemoved = Strings.whiteSpaceRemoved(rawReference);
        final int modulo = Payments.calculateMod97("RF00" + whiteSpaceRemoved);
        return String.format("RF%02d", 98-modulo) + whiteSpaceRemoved;
    }

    private static boolean hasValidMod97CheckDigits(String number) {
        try {
            return calculateMod97(number) == 1;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
	 * Calculate the reference's modulo 97 checksum according to ISO11649 and IBAN standard.
     * <p>
     * The string may only contains digits and letters (A to Z, no accents)
     * </p>
	 * @param reference the reference
	 * @return the checksum (0 to 96)
     * @throws IllegalArgumentException thrown if the reference contains an invalid character
	 */
    public static int calculateMod97(String reference) {
        String rearranged = reference.substring(4) + reference.substring(0, 4);
        int len = rearranged.length();
        int sum = 0;
        for (int i = 0; i < len; i++) {
            char ch = rearranged.charAt(i);
            if (ch >= '0' && ch <= '9') {
                sum = sum * 10 + (ch - '0');
            } else if (ch >= 'A' && ch <= 'Z') {
                sum = sum * 100 + (ch - 'A' + 10);
            } else if (ch >= 'a' && ch <= 'z') {
                sum = sum * 100 + (ch - 'a' + 10);
            } else {
                throw new IllegalArgumentException("Invalid character in reference: " + ch);
            }
            if (sum > 9999999)
                sum = sum % 97;
        }

        sum = sum % 97;
        return sum;
    }
    
    private static final int[] MOD_10 = { 0, 9, 4, 6, 8, 2, 7, 1, 3, 5 };

    /**
     * Validates if the string is a valid QR reference number.
     * <p>
     *   A valid QR reference is a valid ISR reference number.
     * </p>
     * <p>
     *   All whitespace must have been removed before the
     *   the validation is performed.
     * </p>
     * <p>
     *   The string is checked for valid characters, valid length
     *   and a valid check digit.
     * </p>
     * @param referenceNo QR reference number to validate
     * @return {@code true} if the reference number is valid, {@code false} otherwise
     */
    public static boolean isValidQRReferenceNo(String referenceNo) {
        if (!isNumeric(referenceNo))
            return false;

        int carry = 0;
        int len = referenceNo.length();
        if (len != 27)
            return false;

        for (int i = 0; i < len; i++) {
            int digit = referenceNo.charAt(i) - '0';
            carry = MOD_10[(carry + digit) % 10];
        }

        return carry == 0;
    }

    /**
     * Formats a QR reference number by inserted spaces.
     * <p>
     * Spaces are inserted to create groups of 5 digits.
     * If a group of less than 5 digits is needed, it
     * appears at the start of the formatted reference number.
     * </p>
     * 
     * @param refNo reference number without white space
     * @return formatted reference number
     */
    public static String formatQRReferenceNumber(String refNo) {
        int len = refNo.length();
        StringBuilder sb = new StringBuilder();
        int t = 0;
        while (t < len) {
            int n = t + (len - t - 1) % 5 + 1;
            if (t != 0)
                sb.append(" ");
            sb.append(refNo, t, n);
            t = n;
        }

        return sb.toString();
    }

    private static boolean isNumeric(String value) {
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char ch = value.charAt(i);
            if (ch < '0' || ch > '9')
                return false;
        }
        return true;
    }

    static boolean isAlphaNumeric(String value) {
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char ch = value.charAt(i);
            if (ch >= '0' && ch <= '9')
                continue;
            if (ch >= 'A' && ch <= 'Z')
                continue;
            if (ch >= 'a' && ch <= 'z')
                continue;
            return false;
        }
        return true;
    }

    private static boolean isValidQRBillCharacter(char ch) {
        if (ch < 0x20)
            return false;
        if (ch == 0x5e)
            return false;
        if (ch <= 0x7e)
            return true;
        if (ch == 0xa3 || ch == 0xb4)
            return true;
        if (ch < 0xc0 || ch > 0xfd)
            return false;
        if (ch == 0xc3 || ch == 0xc5 || ch == 0xc6)
            return false;
        if (ch == 0xd0 || ch == 0xd5 || ch == 0xd7 || ch == 0xd8)
            return false;
        if (ch == 0xdd || ch == 0xde)
            return false;
        if (ch == 0xe3 || ch == 0xe5 || ch == 0xe6)
            return false;
        if (ch == 0xf0 || ch == 0xf5 || ch == 0xf8)
            return false;
        return true;
    }
}
