package de.erethon.spellbook.utils;

import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TransformationUtil {

    public static Transformation scale(Transformation transformation, float x, float y, float z) {
        return new Transformation(new Vector3f(x, y, z), transformation.getLeftRotation(), transformation.getTranslation(), transformation.getRightRotation());
    }

    public static Transformation scale(Transformation transformation, float scale) {
        return scale(transformation, scale, scale, scale);
    }

    public static Transformation translate(Transformation transformation, float x, float y, float z) {
        return new Transformation(transformation.getScale(), transformation.getLeftRotation(), new Vector3f(x, y, z), transformation.getRightRotation());
    }

    public static Transformation leftRotation(Transformation transformation, float x, float y, float z, float w) {
        return new Transformation(transformation.getScale(), new Quaternionf(x, y, z, w), transformation.getTranslation(), transformation.getRightRotation());
    }

    public static Transformation rightRotation(Transformation transformation, float x, float y, float z, float w) {
        return new Transformation(transformation.getScale(), transformation.getLeftRotation(), transformation.getTranslation(), new Quaternionf(x, y, z, w));
    }
}
