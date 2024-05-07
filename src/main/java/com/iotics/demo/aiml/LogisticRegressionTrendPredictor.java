package com.iotics.demo.aiml;

import org.apache.commons.math3.analysis.function.Sigmoid;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class LogisticRegressionTrendPredictor {
    private final RealVector weights;

    public LogisticRegressionTrendPredictor(RealVector weights) {
        this.weights = weights;
    }

    public boolean predictNextMinuteTrend(double[] features) {
        // Add bias term to the features
        double[] extendedFeatures = new double[features.length + 1];
        System.arraycopy(features, 0, extendedFeatures, 1, features.length);
        extendedFeatures[0] = 1; // bias term

        RealVector featureVector = new ArrayRealVector(extendedFeatures);
        double dotProduct = featureVector.dotProduct(this.weights);

        // Apply sigmoid function to get probability
        double probability = sigmoid(dotProduct);

        // Predict trend based on probability
        return probability >= 0.5;
    }

    private double sigmoid(double x) {
        Sigmoid sigmoidFunction = new Sigmoid();
        return sigmoidFunction.value(x);
    }

    public static void main(String[] args) {
        // Example usage
        double[] weightsArray = {0.5, -0.3, 0.2, 0.1}; // Example weights, obtained from training
        RealVector weights = new ArrayRealVector(weightsArray);
        LogisticRegressionTrendPredictor predictor = new LogisticRegressionTrendPredictor(weights);

        // Simulating feature vectors from boolean stream
        double[][] featureVectors = {
                {1, 1, 0}, // Example feature vectors (including bias term)
                {1, 0, 1},
                {1, 0, 1}
        };

        for (double[] features : featureVectors) {
            boolean prediction = predictor.predictNextMinuteTrend(features);
            System.out.println("Next minute trend prediction: " + prediction);
        }
    }
}
