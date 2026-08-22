package com.wentory.coolstuff.fireball;

public enum FireballPhase {
    NORMAL,
    IGNITED,
    OVERCHARGED,
    DIVINE,
    BLACK_HOLE;

    public static FireballPhase fromCombo(int combo) {
        if (combo >= 100) return BLACK_HOLE;
        if (combo >= 20) return DIVINE;
        if (combo >= 10) return OVERCHARGED;
        if (combo >= 5) return IGNITED;
        return NORMAL;
    }
}
