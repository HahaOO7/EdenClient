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

	public static int clamp(int num, int min, int max) {
		return num < min ? min : num > max ? max : num;
	}

	public static float clamp(float num, float min, float max) {
		return num < min ? min : num > max ? max : num;
	}

	public static double clamp(double num, double min, double max) {
		return num < min ? min : num > max ? max : num;
	}
}
