package at.haha007.edenclient.utils;

public class MathUtils {
    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static int clamp(int n, int min, int max){
        return Math.max(min, Math.min(n, max));
    }
}
