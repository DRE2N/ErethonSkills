package de.erethon.spellbook.utils;

public class Point3D {
    public float x;
    public float y;
    public float z;

    public Point3D(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Point3D rotate(float rot) {
        double cos = Math.cos(rot);
        double sin = Math.sin(rot);
        return new Point3D((float)(x * cos + z * sin), y, (float)(x * -sin + z * cos));
    }

    public Point3D add(Point3D other) {
        return new Point3D(x + other.x, y + other.y, z + other.z);
    }

    public Point3D subtract(Point3D other) {
        return new Point3D(x - other.x, y - other.y, z - other.z);
    }

    public Point3D multiply(float factor) {
        return new Point3D(x * factor, y * factor, z * factor);
    }
}
