package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IndicatorsEngine {

    public static class MACDResult {
        public double[] macdLine;
        public double[] signalLine;
        public double[] histogram;

        public MACDResult(int size) {
            macdLine = new double[size];
            signalLine = new double[size];
            histogram = new double[size];
        }
    }

    public static class BollingerBandsResult {
        public double[] middleBand; // SMA 20
        public double[] upperBand;  // SMA 20 + 2 * StdDev
        public double[] lowerBand;  // SMA 20 - 2 * StdDev

        public BollingerBandsResult(int size) {
            middleBand = new double[size];
            upperBand = new double[size];
            lowerBand = new double[size];
        }
    }

    public static double[] calculateSMA(List<Candlestick> candles, int period) {
        int n = candles.size();
        double[] sma = new double[n];
        for (int i = 0; i < n; i++) {
            if (i < period - 1) {
                sma[i] = candles.get(i).close; // Fill with default
                continue;
            }
            double sum = 0;
            for (int j = i - period + 1; j <= i; j++) {
                sum += candles.get(j).close;
            }
            sma[i] = sum / period;
        }
        return sma;
    }

    public static double[] calculateEMA(List<Candlestick> candles, int period) {
        int n = candles.size();
        double[] ema = new double[n];
        if (n == 0) return ema;

        double multiplier = 2.0 / (period + 1);
        ema[0] = candles.get(0).close;

        for (int i = 1; i < n; i++) {
            ema[i] = (candles.get(i).close - ema[i - 1]) * multiplier + ema[i - 1];
        }
        return ema;
    }

    public static double[] calculateRSI(List<Candlestick> candles, int period) {
        int n = candles.size();
        double[] rsi = new double[n];
        if (n <= period) {
            for (int i = 0; i < n; i++) rsi[i] = 50.0; // neutral default
            return rsi;
        }

        double[] gains = new double[n];
        double[] losses = new double[n];

        for (int i = 1; i < n; i++) {
            double change = candles.get(i).close - candles.get(i - 1).close;
            if (change > 0) {
                gains[i] = change;
                losses[i] = 0;
            } else {
                gains[i] = 0;
                losses[i] = -change;
            }
        }

        // First RSI period
        double avgGain = 0;
        double avgLoss = 0;
        for (int i = 1; i <= period; i++) {
            avgGain += gains[i];
            avgLoss += losses[i];
        }
        avgGain /= period;
        avgLoss /= period;

        if (avgLoss == 0) {
            rsi[period] = 100;
        } else {
            double rs = avgGain / avgLoss;
            rsi[period] = 100 - (100 / (1 + rs));
        }

        for (int i = period + 1; i < n; i++) {
            avgGain = (avgGain * (period - 1) + gains[i]) / period;
            avgLoss = (avgLoss * (period - 1) + losses[i]) / period;

            if (avgLoss == 0) {
                rsi[i] = 100;
            } else {
                double rs = avgGain / avgLoss;
                rsi[i] = 100 - (100 / (1 + rs));
            }
        }

        // Fill leading values
        for (int i = 0; i < period; i++) {
            rsi[i] = rsi[period];
        }

        return rsi;
    }

    public static MACDResult calculateMACD(List<Candlestick> candles) {
        int n = candles.size();
        MACDResult result = new MACDResult(n);
        if (n == 0) return result;

        double[] ema12 = calculateEMA(candles, 12);
        double[] ema26 = calculateEMA(candles, 26);

        for (int i = 0; i < n; i++) {
            result.macdLine[i] = ema12[i] - ema26[i];
        }

        // Signal line: 9 EMA of MACD
        double multiplier = 2.0 / (9 + 1);
        result.signalLine[0] = result.macdLine[0];
        for (int i = 1; i < n; i++) {
            result.signalLine[i] = (result.macdLine[i] - result.signalLine[i - 1]) * multiplier + result.signalLine[i - 1];
        }

        // Histogram
        for (int i = 0; i < n; i++) {
            result.histogram[i] = result.macdLine[i] - result.signalLine[i];
        }

        return result;
    }

    public static BollingerBandsResult calculateBollingerBands(List<Candlestick> candles) {
        int n = candles.size();
        BollingerBandsResult result = new BollingerBandsResult(n);
        if (n == 0) return result;

        double[] sma20 = calculateSMA(candles, 20);

        for (int i = 0; i < n; i++) {
            result.middleBand[i] = sma20[i];
            if (i < 19) {
                result.upperBand[i] = candles.get(i).high;
                result.lowerBand[i] = candles.get(i).low;
                continue;
            }

            double varianceSum = 0;
            for (int j = i - 19; j <= i; j++) {
                double diff = candles.get(j).close - sma20[i];
                varianceSum += diff * diff;
            }
            double stdDev = Math.sqrt(varianceSum / 20);

            result.upperBand[i] = sma20[i] + (2 * stdDev);
            result.lowerBand[i] = sma20[i] - (2 * stdDev);
        }

        return result;
    }

    public static List<Double> findSupportResistance(List<Candlestick> candles) {
        List<Double> levels = new ArrayList<>();
        int n = candles.size();
        if (n < 5) return levels;

        for (int i = 2; i < n - 2; i++) {
            double currentHigh = candles.get(i).high;
            double currentLow = candles.get(i).low;

            // Simple fractal check: local peaks or troughs
            boolean isPeak = currentHigh >= candles.get(i - 1).high &&
                             currentHigh >= candles.get(i - 2).high &&
                             currentHigh >= candles.get(i + 1).high &&
                             currentHigh >= candles.get(i + 2).high;

            boolean isTrough = currentLow <= candles.get(i - 1).low &&
                              currentLow <= candles.get(i - 2).low &&
                              currentLow <= candles.get(i + 1).low &&
                              currentLow <= candles.get(i + 2).low;

            if (isPeak) {
                levels.add(currentHigh);
            }
            if (isTrough) {
                levels.add(currentLow);
            }
        }

        // Keep it clean: filter and sort to hold unique representative levels
        List<Double> distinct = new ArrayList<>();
        for (double level : levels) {
            boolean duplicate = false;
            for (double d : distinct) {
                if (Math.abs(d - level) / level < 0.02) { // within 2%
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                distinct.add(level);
            }
        }

        Collections.sort(distinct);
        return distinct;
    }
}
