// Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
package edu.jhu.thrax.hadoop.features.mapred.coc;

public class GoodTuringSmoother {
  private CountOfCountsEstimator estimator;

  public GoodTuringSmoother(CountOfCountsEstimator estimator) {
    this.estimator = estimator;
  }
  
  public double smoothedCount(int count) {
    double turingFraction = estimator.getEstimatedCountOfCount(count + 1) / estimator.getEstimatedCountOfCount(count);
    return (count + 1) * turingFraction;
  }
}