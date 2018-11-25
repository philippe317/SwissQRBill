//
// Swiss QR Bill Generator
// Copyright (c) 2017 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.qrbill.canvas;

import java.io.IOException;

/**
 * Abstract base class for simplified implementation of {@code Canvas} classes.
 */
public abstract class AbstractCanvas implements Canvas {

    protected static final double MM_TO_PT = 72 / 25.4;

    FontMetrics fontMetrics;

    protected void setupFontMetrics(String fontFamilyList) {
        fontMetrics = new FontMetrics(fontFamilyList);
    }

    @Override
    public void putTextLines(String[] lines, double x, double y, int fontSize, double leading) throws IOException {
        for (String line : lines) {
            putText(line, x, y, fontSize, false);
            y -= fontMetrics.getLineHeight(fontSize) + leading;
        }
    }

    @Override
    public double getAscender(int fontSize) {
        return fontMetrics.getAscender(fontSize);
    }

    @Override
    public double getDescender(int fontSize) {
        return fontMetrics.getDescender(fontSize);
    }

    @Override
    public double getLineHeight(int fontSize) {
        return fontMetrics.getLineHeight(fontSize);
    }

    @Override
    public double getTextWidth(CharSequence text, int fontSize, boolean isBold) {
        return fontMetrics.getTextWidth(text, fontSize, isBold);
    }

    @Override
    public String[] splitLines(String text, double maxLength, int fontSize) {
        return fontMetrics.splitLines(text, maxLength, fontSize);
    }
}
