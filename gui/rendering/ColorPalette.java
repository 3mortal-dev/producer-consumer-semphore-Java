package gui.rendering;

import java.awt.Color;

/**
 * NES-inspired 16-color palette for 8-bit aesthetic.
 * Index matches classic NES palette ordering.
 */
public final class ColorPalette {
    private ColorPalette() {}

    // Core 16 colors
    public static final Color BLACK       = new Color(0x00, 0x00, 0x00);   // 0
    public static final Color WHITE       = new Color(0xFF, 0xFF, 0xFF);   // 1
    public static final Color GRAY_LIGHT  = new Color(0x8C, 0x8C, 0x8C);   // 2
    public static final Color GRAY_DARK   = new Color(0x44, 0x44, 0x44);   // 3
    public static final Color BLUE_DARK   = new Color(0x00, 0x58, 0xA8);   // 4 - Hospital walls
    public static final Color BLUE_LIGHT  = new Color(0x00, 0x8C, 0xF0);   // 5 - UI accents
    public static final Color OFF_WHITE   = new Color(0xF8, 0xF8, 0xF8);   // 6 - Patient gown
    public static final Color RED         = new Color(0xD8, 0x20, 0x10);   // 7 - Emergency cross
    public static final Color ORANGE      = new Color(0xF8, 0x78, 0x00);   // 8 - Doctor coat
    public static final Color SKIN        = new Color(0xF0, 0xD0, 0xB0);   // 9 - Skin tone
    public static final Color GREEN       = new Color(0x3C, 0xBC, 0x3C);   // 10 - Treated/available
    public static final Color YELLOW      = new Color(0xE0, 0xE0, 0x20);   // 11 - Warning/timeout
    public static final Color METAL       = new Color(0xB8, 0xB8, 0xB8);   // 12 - Equipment
    public static final Color GRAY_MED    = new Color(0x58, 0x58, 0x58);   // 13 - Dark furniture
    public static final Color BROWN       = new Color(0xA8, 0x50, 0x00);   // 14 - Chairs/wood
    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);         // 15 - Sprite key

    // Semantic aliases for readability
    public static final Color FLOOR       = GRAY_LIGHT;
    public static final Color WALL        = BLUE_DARK;
    public static final Color WALL_LIGHT  = BLUE_LIGHT;
    public static final Color SEAT_EMPTY  = BROWN;
    public static final Color SEAT_OCCUPIED = SKIN;
    public static final Color DOCTOR_COAT = ORANGE;
    public static final Color PATIENT_GOWN = OFF_WHITE;
    public static final Color BED_SHEET   = WHITE;
    public static final Color BED_FRAME   = GRAY_MED;
    public static final Color MONITOR_BG  = GRAY_DARK;
    public static final Color MONITOR_LINE = GREEN;
    public static final Color TEXT_PRIMARY = WHITE;
    public static final Color TEXT_SECONDARY = GRAY_LIGHT;
    public static final Color TEXT_WARNING = YELLOW;
    public static final Color TEXT_DANGER  = RED;
    public static final Color TEXT_SUCCESS = GREEN;
    public static final Color PANEL_BG    = new Color(0x10, 0x10, 0x20);  // Dark blue-black for panels
    public static final Color PANEL_BORDER = GRAY_DARK;

    // Array for indexed access
    public static final Color[] PALETTE = {
        BLACK, WHITE, GRAY_LIGHT, GRAY_DARK,
        BLUE_DARK, BLUE_LIGHT, OFF_WHITE, RED,
        ORANGE, SKIN, GREEN, YELLOW,
        METAL, GRAY_MED, BROWN, TRANSPARENT
    };
}