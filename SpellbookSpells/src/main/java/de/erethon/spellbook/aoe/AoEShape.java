package de.erethon.spellbook.aoe;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Defines the different shapes an AoE can have
 */
public enum AoEShape {
    CIRCLE,
    RECTANGLE,
    CONE;

    /**
     * Checks if a location is within this shape
     * @param center The center/origin of the AoE
     * @param target The location to check
     * @param parameters Shape-specific parameters (radius for circle, width/length for rectangle, etc.)
     * @param rotation Optional rotation vector for oriented shapes
     * @return true if the target is within the shape
     */
    public boolean contains(Location center, Location target, AoEParameters parameters, Vector rotation) {
        if (!center.getWorld().equals(target.getWorld())) {
            return false;
        }

        double heightDiff = Math.abs(target.getY() - center.getY());
        if (heightDiff > parameters.getHeight() / 2) {
            return false;
        }

        Vector toTarget = target.toVector().subtract(center.toVector());
        toTarget.setY(0);

        switch (this) {
            case CIRCLE:
                return toTarget.lengthSquared() <= parameters.getRadius() * parameters.getRadius();

            case RECTANGLE:
                return isInRectangle(toTarget, parameters, rotation);

            case CONE:
                return isInCone(toTarget, parameters, rotation);

            default:
                return false;
        }
    }

    private boolean isInRectangle(Vector toTarget, AoEParameters parameters, Vector rotation) {
        if (rotation == null || rotation.lengthSquared() == 0) {
            return Math.abs(toTarget.getX()) <= parameters.getWidth() / 2 &&
                   Math.abs(toTarget.getZ()) <= parameters.getLength() / 2;
        }

        Vector rotated = rotateVector(toTarget, -getYaw(rotation));
        return Math.abs(rotated.getX()) <= parameters.getWidth() / 2 &&
               Math.abs(rotated.getZ()) <= parameters.getLength() / 2;
    }

    private boolean isInCone(Vector toTarget, AoEParameters parameters, Vector rotation) {
        if (rotation == null || rotation.lengthSquared() == 0) {
            rotation = new Vector(0, 0, 1);
        }

        double distance = toTarget.length();
        if (distance > parameters.getRadius()) {
            return false;
        }

        Vector normalizedDirection = rotation.clone().normalize();
        normalizedDirection.setY(0);
        Vector normalizedTarget = toTarget.clone().normalize();

        double dot = normalizedDirection.dot(normalizedTarget);
        double angle = Math.acos(Math.max(-1, Math.min(1, dot)));

        return angle <= Math.toRadians(parameters.getAngle() / 2);
    }

    private Vector rotateVector(Vector vector, double yawRadians) {
        double cos = Math.cos(yawRadians);
        double sin = Math.sin(yawRadians);

        double newX = vector.getX() * cos - vector.getZ() * sin;
        double newZ = vector.getX() * sin + vector.getZ() * cos;

        return new Vector(newX, vector.getY(), newZ);
    }

    private double getYaw(Vector direction) {
        return Math.atan2(-direction.getX(), direction.getZ());
    }
}
