package com.example.networkscanner.util

/**
 * A simple 1D Kalman Filter for smoothing noisy sensor data like RSSI.
 */
class KalmanFilter(
    private var processNoise: Double = 0.125,
    private var measurementNoise: Double = 0.8,
    private var estimatedError: Double = 1.0,
    private var currentEstimation: Double = -100.0
) {
    private var kalmanGain: Double = 0.0

    fun update(measurement: Double): Double {
        // Prediction update
        estimatedError += processNoise

        // Measurement update
        kalmanGain = estimatedError / (estimatedError + measurementNoise)
        currentEstimation += kalmanGain * (measurement - currentEstimation)
        estimatedError *= (1.0 - kalmanGain)

        return currentEstimation
    }

    fun reset(initialValue: Double) {
        currentEstimation = initialValue
        estimatedError = 1.0
    }
}
