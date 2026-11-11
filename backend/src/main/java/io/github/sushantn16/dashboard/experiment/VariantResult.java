package io.github.sushantn16.dashboard.experiment;

/**
 * The result for one variant. Both counts are of unique visitors, so reloads and repeated clicks
 * count once.
 *
 * @param saw visitors who saw the button
 * @param clicked visitors who clicked it at least once
 * @param clickRate clicked / saw, or 0 while nobody has seen the button
 */
public record VariantResult(Variant variant, int saw, int clicked, double clickRate) {

    static VariantResult of(Variant variant, int saw, int clicked) {
        double clickRate = saw == 0 ? 0 : (double) clicked / saw;
        return new VariantResult(variant, saw, clicked, clickRate);
    }
}
