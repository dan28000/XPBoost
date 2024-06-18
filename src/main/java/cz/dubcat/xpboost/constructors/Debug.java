package cz.dubcat.xpboost.constructors;

public enum Debug {
    OFF(0), NORMAL(1), ALL(2);

    private final int value;

    Debug(int val) {
        value = val;
    }

    public int getValue() {
        return value;
    }
}
