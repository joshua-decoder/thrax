// Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
package edu.jhu.thrax.hadoop.features.mapred.coc;

import static java.lang.Math.*;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Linear estimator of count of counts in log-log space.
 * 
 * y = exp(slope * log(count) + intercept)
 */
public class CountOfCountsEstimator implements Serializable {

  private static final long serialVersionUID = -7988102132725579097L;
  
  private final double slope;
  private final double intercept;

  public CountOfCountsEstimator(double slope, double intercept) {
    this.slope = slope;
    this.intercept = intercept;
  }
  
  public double getSlope() {
    return slope;
  }
  
  public double getIntercept() {
    return intercept;
  }
  
  public long getRoundedCountOfCount(long count) {
    return Math.round(getEstimatedCountOfCount(count));
  }
  
  public double getEstimatedCountOfCount(long count) {
    return exp(slope * log(count) + intercept);
  }
  
  public static CountOfCountsEstimator regress(Map<Integer, Integer> countOfCountsMap) {
    double[] counts = new double[countOfCountsMap.size()];
    double[] countOfCounts = new double[countOfCountsMap.size()];
    int idx = 0;
    for (Entry<Integer, Integer> e : countOfCountsMap.entrySet()) {
      counts[idx] = e.getKey();
      countOfCounts[idx] = e.getValue();
      idx += 1;
    }
    return regress(counts, countOfCounts);
  }
  
  /**
   * Weighted least squares regression in log-log space of count of counts data.
   * 
   * We can solve this by OLS with scaling our input data with sqrt(weight).
   * As a weight we use the count of counts of each data point.
   * This is the more often a count appears the more weight it gets.
   * 
   * We get:
   * x1: sqrt(counts of counts)
   * x2: sqrt(counts of counts) * log(counts)
   * y: sqrt(counts of counts) * log(counts of counts)
   * w: weighted by x (the counts)
   * 
   * OLS solution is:
   * (X^T X)^-1 X^Ty
   */
  public static CountOfCountsEstimator regress(double[] counts, double[] countsOfCounts) {
    if (!(counts.length == countsOfCounts.length)) {
      throw new RuntimeException("Dimensions of counts and countsOfCounts must match.");
    }

    final int numDataPoints = counts.length;
    double[] x1 = new double[numDataPoints];
    double[] x2 = new double[numDataPoints];
    double[] y = new double[numDataPoints];
    for (int i = 0; i < numDataPoints; i++) {
       double sqrt_of_weight = sqrt(countsOfCounts[i]);
       x1[i] = sqrt_of_weight * 1.0;                   // bias (for intercept)
       x2[i] = sqrt_of_weight * log(counts[i]);        // feature
       y[i] = sqrt_of_weight * log(countsOfCounts[i]); // target
    }

    //X^T X
    double xs00 = 0;
    double xs01 = 0; //symmetric matrix: xs01 == xs10
    double xs11 = 0;
    for (int j = 0; j < x1.length; j++) {
      xs00 += x1[j] * x1[j];
      xs01 += x1[j] * x2[j];
      xs11 += x2[j] * x2[j];
    }

    // matrix inverse to get (X^T X)^-1
    double denom = xs00 * xs11 - xs01 * xs01; 
    double xs00_inv = xs11 / denom;
    double xs01_inv = -xs01 / denom;
    double xs11_inv = xs00 / denom;
    
    //X^T y
    double xty0 = 0.;
    double xty1 = 0.;
    for (int j = 0; j < x1.length; j++) {
      xty0 += x1[j] * y[j];
      xty1 += x2[j] *y [j];
    }
    
    //bringing everything together: [intercept slope]^T = (X^T X)^-1 X^T y
    double intercept = xs00_inv * xty0 + xty1 * xs01_inv;
    double slope = xs01_inv * xty0 + xty1 * xs11_inv;

    return new CountOfCountsEstimator(slope, intercept);
  }

}