package at.haha007.edenclient.utils;

public class MathUtils {
    private MathUtils() {
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;

        } catch (NumberFormatException e) {
            return false;
        }
    }
}
