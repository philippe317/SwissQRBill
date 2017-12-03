//
// Swiss QR Bill Generator
// Copyright (c) 2017 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.qrbill.generator;

/**
 * Simple font metrics class to be independent of graphics subsystems and installed fonts.
 *
 * <p>Only supports Helvetica font.</p>
 */
public class FontMetrics {

    private static final double PT_TO_MM = 25.4 / 72;

    /**
     * Distance between baseline and top of highest letter.
     * @param fontSize the font size (in pt)
     * @return the distance (in mm)
     */
    public static double getAscender(int fontSize) {
        return fontSize * 0.8 * PT_TO_MM;
    }

    /**
     * Distance between the baselines of the consecutive text lines.
     * @param fontSize the font size (in pt)
     * @return the distance (in mm)
     */
    public static double getLineHeight(int fontSize) {
        return fontSize * 1.2 * PT_TO_MM;
    }

    /**
     * Extra vertical padding before label
     * @return the distance (in mm)
     */
    public static double getLabelPadding() {
        return 6 * PT_TO_MM;
    }

    /**
     * Extra vertical padding before text block
     * @return the distance (in mm)
     */
    public static double getTextPadding() {
        return 2 * PT_TO_MM;
    }
}