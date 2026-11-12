package by.bsu.imageproc.buffer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EdgeModeTest {

    @Test
    void clampRepeatsBorderPixel() {
        assertEquals(0, EdgeMode.CLAMP.index(-1, 5));
        assertEquals(0, EdgeMode.CLAMP.index(-3, 5));
        assertEquals(4, EdgeMode.CLAMP.index(5, 5));
        assertEquals(4, EdgeMode.CLAMP.index(9, 5));
        assertEquals(2, EdgeMode.CLAMP.index(2, 5));
    }

    @Test
    void reflectMirrorsWithoutRepeatingBorder() {
        assertEquals(1, EdgeMode.REFLECT.index(-1, 5));
        assertEquals(2, EdgeMode.REFLECT.index(-2, 5));
        assertEquals(3, EdgeMode.REFLECT.index(5, 5));
        assertEquals(2, EdgeMode.REFLECT.index(6, 5));
        assertEquals(0, EdgeMode.REFLECT.index(8, 5));
    }

    @Test
    void paddedRowOfSmallImage() {
        GrayImage image = new GrayImage(3, 1, new int[]{10, 20, 30});
        int[] clamp = new int[7];
        int[] reflect = new int[7];
        for (int x = -2; x <= 4; x++) {
            clamp[x + 2] = image.get(x, 0, EdgeMode.CLAMP);
            reflect[x + 2] = image.get(x, 0, EdgeMode.REFLECT);
        }
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[]{10, 10, 10, 20, 30, 30, 30}, clamp);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[]{30, 20, 10, 20, 30, 20, 10}, reflect);
    }
}
