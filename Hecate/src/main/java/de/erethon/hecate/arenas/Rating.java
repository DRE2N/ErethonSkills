package de.erethon.hecate.arenas;

import java.util.UUID;

public class Rating {

    private UUID uuid;
    private double rating;
    private double deviation;
    private double volatility;

    private double ratingScaled;
    private double deviationScaled;

    public Rating(UUID uuid) {
        this.update(GlickoConstants.DEFAULT_RATING, GlickoConstants.DEFAULT_DEVIATION, GlickoConstants.DEFAULT_VOLATILITY);
        this.uuid = uuid;
    }

    public Rating(UUID uuid, double rating, double deviation, double volatility) {
        this.update(rating, deviation, volatility);
        this.uuid = uuid;
    }

    public final void update(double rating, double deviation, double volatility) {
        this.rating = rating;
        this.deviation = deviation;
        this.volatility = volatility;

        this.ratingScaled = GlickoConstants.scaleRating(rating);
        this.deviationScaled = GlickoConstants.scaleDeviation(deviation);
    }

    public UUID getUUID() {
        return uuid;
    }

    public double getRating() {
        return rating;
    }

    double getScaledRating() {
        return ratingScaled;
    }

    public double getDeviation() {
        return deviation;
    }

    double getScaledDeviation() {
        return deviationScaled;
    }

    public double getVolatility() {
        return volatility;
    }
}
