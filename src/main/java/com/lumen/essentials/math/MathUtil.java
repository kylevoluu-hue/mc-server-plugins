package com.lumen.essentials.math;

import java.util.Collection;

/**
 * Lightweight statistical helpers used by timing/consistency checks. These are
 * intentionally allocation-light and side-effect free so they are safe to call
 * from hot packet paths.
 */
public final class MathUtil {

    private MathUtil() {
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    public static double average(Collection<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return 0.0D;
        }
        double sum = 0.0D;
        for (Number n : values) {
            sum += n.doubleValue();
        }
        return sum / values.size();
    }

    public static double variance(Collection<? extends Number> values) {
        if (values == null || values.size() < 2) {
            return 0.0D;
        }
        double mean = average(values);
        double sum = 0.0D;
        for (Number n : values) {
            double d = n.doubleValue() - mean;
            sum += d * d;
        }
        return sum / values.size();
    }

    public static double standardDeviation(Collection<? extends Number> values) {
        return Math.sqrt(variance(values));
    }

    /**
     * Computes the GCD-style outlier metric used by autoclicker checks: a low
     * standard deviation relative to the mean indicates inhuman consistency.
     *
     * @return the coefficient of variation (stddev / mean), or 0 when undefined.
     */
    public static double coefficientOfVariation(Collection<? extends Number> values) {
        double mean = average(values);
        if (mean == 0.0D) {
            return 0.0D;
        }
        return standardDeviation(values) / mean;
    }
}
