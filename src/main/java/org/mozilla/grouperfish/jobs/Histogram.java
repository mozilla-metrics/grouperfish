package org.mozilla.grouperfish.jobs;


/**
 * An in-memory histogram with int categories and positive double values.
 */
public class Histogram {

  /** Use logarithmic categories (base 10) */
  public
  Histogram() {
    k_ = LOG_STEPS.length;
    categories_ = LOG_STEPS;
    counts_ = new int[k_];
  }


  /** Start N categories of the given with at the given offset. */
  public
  Histogram(double offset, double stepWidth) {
    k_ = LIN_STEPS.length;
    categories_ = new double[k_];
    for (int i = 0; i < k_; ++i) {
      categories_[i] = offset + stepWidth * LIN_STEPS[i];
    }
    counts_ = new int[k_];
  }


  /** Start N categories of the given with at the given offset. */
  public
  Histogram(double[] steps, int[] counts) {
    k_ = steps.length;
    categories_ = steps;
    counts_ = counts;

    n_ = 0;
    for (int i = k_; i --> 0;) n_ += counts_[i];
  }


  public
  void add(double sample) {
    add(sample, 1);
  }


  public
  void add(double sample, int times) {
    n_ += times;
    for (int i = k_; i --> 0;) {
      if (sample < categories_[i]) continue;
      counts_[i] += times;
      return;
    }
  }


  /** index of the largest category */
  public
  int maxIndex() {
    int maxI = 0;
    int max = 0;
    for (int i = 0; i < k_; ++i) {
      if (counts_[i] > max) {
        maxI = i;
        max = counts_[i];
      }
    }
    return maxI;
  }


  /**
   * Portion of the given category among the total (inc. tails).
   * @param index The category (0 &lte; <tt>index</tt> &lt; {@link #k()})
   */
  public
  double portion(int index) {
    if (n_ == 0) return 0;
    return counts_[index] / (double)n_;
  }


  /** total number of samples */
  public
  int n() {
    return n_;
  }


  /** number of categories */
  public
  int k() {
    return k_;
  }

  /**
   * Size of the given category.
   * @param index The category (0 &lte; <tt>index</tt> &lt; {@link #k()})
   */
  public
  int count(int index) {
    return counts_[index];
  }

  public
  double category(int index) {
    return categories_[index];
  }


  public
  double left() {
    return categories_[0];
  }


  public
  double right() {
    return categories_[k_ - 1];
  }


  @Override public
  String toString() {
    final StringBuilder builder = new StringBuilder();

    builder.append("\n");
    for (double category : categories_) {
      builder.append(String.format("% 10.2f |", category));
    }

    builder.append("\n");
    for (int i = 0; i < k_; ++i) {
      builder.append(String.format("% 10.2f |", portion(i)));
    }

    builder.append("\n");
    for (int count : counts_) {
      builder.append(String.format("% 10d |", count));
    }

    return builder.toString();
  }


  private static final double[] LOG_STEPS =
    new double[]{ 0, 0.0001, 0.001, 0.01, 0.1,   1,
                     10,     100,   1000, 10000, 100000};

  private static final double[] LIN_STEPS =
    new double[]{0, 1, 2, 3, 4, 5,
                    6, 7, 8, 9, 10};

  private final int k_;
  private int n_ = 0;

  /**
   * Steps are given by their *left* category boundary.
   * First category is the left tail.
   * Last category is the open ended right tail.
   */
  private final double[] categories_;

  /**
   * Counters for the individual steps.
   */
  private final int[] counts_;


}
