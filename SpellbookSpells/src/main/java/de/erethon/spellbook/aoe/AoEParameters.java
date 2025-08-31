package de.erethon.spellbook.aoe;

/**
 * Parameters for defining AoE shapes
 */
public class AoEParameters {
    private final double radius;
    private final double width;
    private final double length;
    private final double height;
    private final double angle;

    // Private constructor for circle
    private AoEParameters(double radius, double height, boolean isCircle) {
        this.radius = radius;
        this.height = height;
        this.width = 0;
        this.length = 0;
        this.angle = 0;
    }

    // Private constructor for rectangle
    private AoEParameters(double width, double length, double height, boolean isRectangle) {
        this.width = width;
        this.length = length;
        this.height = height;
        this.radius = 0;
        this.angle = 0;
    }

    // Private constructor for cone
    private AoEParameters(double radius, double angle, double height, int isCone) {
        this.radius = radius;
        this.angle = angle;
        this.height = height;
        this.width = 0;
        this.length = 0;
    }

    // Static factory methods for clarity
    public static AoEParameters circle(double radius, double height) {
        return new AoEParameters(radius, height, true);
    }

    public static AoEParameters rectangle(double width, double length, double height) {
        return new AoEParameters(width, length, height, true);
    }

    public static AoEParameters cone(double radius, double angle, double height) {
        return new AoEParameters(radius, angle, height, 0);
    }

    // Getters
    public double getRadius() { return radius; }
    public double getWidth() { return width; }
    public double getLength() { return length; }
    public double getHeight() { return height; }
    public double getAngle() { return angle; }
}
