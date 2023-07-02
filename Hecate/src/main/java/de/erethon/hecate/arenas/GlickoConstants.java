package de.erethon.hecate.arenas;

import java.util.List;

public class GlickoConstants {

    public static final double DEFAULT_RATING = 1500;
    /**
     * The initial rating deviation.
     */
    public static final double DEFAULT_DEVIATION = 350;
    /**
     * The initial rating volatility.
     */
    public static final double DEFAULT_VOLATILITY = 0.06D;

    /**
     * Constraints the change in volatility over time.
     *
     * <p>
     * Reasonable choices might be between 0.3 and 1.2, but it depends on the actual application.<br>
     * Smaller values prevent the volatility measures from changing by large amounts, which in turn prevent enormous
     * changes in ratings based on very improbable results. If extremely improbable collections of game outcomes are
     * expected, then it should be set to a small value.
     * </p>
     */
    public static final double VOLATILITY_CONSTRAINT = 0.5D;

    private static final double GLICKO_SCALE = 173.7178D;
    private static final double CONVERGENCE_TOLERANCE = 0.000001D;

    static double scaleRating(double rating) {
        return (rating - DEFAULT_RATING) / GLICKO_SCALE;
    }

    static double unscaleRating(double rating) {
        return (rating * GLICKO_SCALE) + DEFAULT_RATING;
    }

    static double scaleDeviation(double deviation) {
        return deviation / GLICKO_SCALE;
    }

    static double unscaleDeviation(double deviation) {
        return deviation * GLICKO_SCALE;
    }

    // Win: 1.0, Draw: 0.5, Loss: 0.0
    public static void updateRating(Rating rating, List<Rating> opponents, List<Double> scores) {
        if (rating == null || opponents == null || opponents.isEmpty() || opponents.size() != scores.size()) {
            // Don't try to update if something is messed up
            throw new IllegalArgumentException("Invalid arguments for Glicko update");
        }
        double v = 0.0D;
        double delta = 0.0D;
        for (int i = 0; i < opponents.size(); i++) {
            Rating opponent = opponents.get(i);
            double score = scores.get(i);

            double g = g(opponent.getScaledDeviation());
            double E = E(rating, opponent);

            v += (g * g * E * (1.0D - E));
            delta += (g * (score - E));
        }
        v = 1.0D / v;

        double a = Math.log(rating.getVolatility() * rating.getVolatility());
        double A = a;
        double delta2 = (delta * delta);
        double deviation2 = (rating.getScaledDeviation() * rating.getScaledDeviation());
        double B;
        if (delta2 > (deviation2 + v)) {
            B = Math.log(delta2 - (deviation2 + v));
        } else {
            int k = 1;
            while (f(a - (k * VOLATILITY_CONSTRAINT), rating.getScaledDeviation(), v, delta, a) < 0.0D) {
                k++;
            }
            B = a - (k * VOLATILITY_CONSTRAINT);
        }

        double fA = f(A, rating.getScaledDeviation(), v, delta, a);
        double fB = f(B, rating.getScaledDeviation(), v, delta, a);

        while (Math.abs(B - A) > CONVERGENCE_TOLERANCE) {
            double C = A + (A - B) * fA / (fB - fA);
            double fC = f(C, rating.getScaledDeviation(), v, delta, a);
            if ((fC * fB) < 0.0D) {
                A = B;
                fA = fB;
            } else {
                fA = (fA / 2.0D);
            }
            B = C;
            fB = fC;
        }
        double newVolatility = Math.exp(A / 2.0D);

        double newDeviation = Math.sqrt(deviation2 + (newVolatility * newVolatility));

        newDeviation = 1.0D / Math.sqrt((1.0D / (newDeviation * newDeviation)) + (1.0D / v));
        double newRating = 0.0D;
        for (int i = 0; i < opponents.size(); i++) {
            Rating opponent = opponents.get(i);
            double score = scores.get(i).doubleValue();
            newRating += (g(opponent.getScaledDeviation()) * (score - E(rating, opponent)));
        }
        newRating *= (newDeviation * newDeviation);
        newRating += rating.getScaledRating();

        newDeviation = unscaleDeviation(newDeviation);
        newRating = unscaleRating(newRating);

        rating.update(newRating, newDeviation, newVolatility);
    }

    static double g(double scaledDeviation) {
        return 1.0D / (Math.sqrt(1.0D + 3.0D * (scaledDeviation * scaledDeviation) / (Math.PI * Math.PI)));
    }

    static double E(Rating rating1, Rating rating2) {
        assert rating1 != null && rating2 != null;
        return 1.0D / (1.0D + Math.exp(-g(rating2.getScaledDeviation()) * (rating1.getScaledRating() - rating2.getScaledRating())));
    }

    static double f(double x, double scaledDeviation, double v, double delta, double a) {
        double eX = Math.pow(Math.E, x);
        double temp = ((scaledDeviation * scaledDeviation) + v + eX);
        return ((eX * ((delta * delta) - temp)) / (2.0D * temp * temp)) - ((x - a) / (VOLATILITY_CONSTRAINT * VOLATILITY_CONSTRAINT));
    }
}
