package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.HeadlessException;
import java.awt.image.BufferedImage;

import org.bars4j.encode.InvalidAtributeException;
import org.junit.jupiter.api.Test;

/**
 * BarcodeComponent: null-safe setText, stale-image handling on failed regeneration and
 * preferred-size reporting from the rendered image.
 */
class BarcodeComponentTest {

    private static BarcodeComponent component() {
        return new BarcodeComponent(BarcodeFactory.getInstance().createEAN13());
    }

    private static boolean headless() {
        return java.awt.GraphicsEnvironment.isHeadless();
    }

    @Test
    void setTextNullIsRejected() throws Exception {
        BarcodeComponent c = component();
        c.setText("978851500401");
        assertThrows(InvalidAtributeException.class, () -> c.setText(null));
        assertEquals("978851500401", c.getText(), "text unchanged after null attempt");
    }

    @Test
    void invalidTextKeepsPreviousImage() throws Exception {
        if (headless()) {
            return; // imaging requires a graphics environment
        }
        BarcodeComponent c = component();
        c.setText("978851500401");
        BufferedImage before = c.currentImage();
        assertNotNull(before, "image rendered for valid text");
        assertThrows(InvalidAtributeException.class, () -> c.setText("12a3"));
        assertEquals("978851500401", c.getText(), "text unchanged after invalid attempt");
        assertSame(before, c.currentImage(), "image not replaced by a failed regeneration");
    }

    @Test
    void emptyTextKeepsImageNull() throws Exception {
        BarcodeComponent c = component();
        assertNull(c.currentImage(), "no image before any text");
        assertEquals("", c.getText());
    }

    @Test
    void preferredSizeFollowsImage() throws Exception {
        if (headless()) {
            return;
        }
        BarcodeComponent c = component();
        c.setText("978851500401");
        BufferedImage img = c.currentImage();
        assertNotNull(img);
        assertEquals(img.getWidth(), c.getPreferredSize().width, "preferred width");
        assertEquals(img.getHeight(), c.getPreferredSize().height, "preferred height");
    }

    @Test
    void checkSumIsExposed() throws Exception {
        BarcodeComponent c = component();
        c.setText("978851500401");
        assertNotNull(c.getCheckSum(), "checksum computed on setText");
    }
}
