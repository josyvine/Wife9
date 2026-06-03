package com.tradeanalyst.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class CandlestickChartView extends View {

    public static class CustomIndicatorLine {
        public String label;
        public double level;
        public int color;

        public CustomIndicatorLine(String label, double level, int color) {
            this.label = label;
            this.level = level;
            this.color = color;
        }
    }

    private List<Candlestick> mCandles = new ArrayList<>();
    private List<CustomIndicatorLine> mCustomLines = new ArrayList<>();
    
    // Toggleable built-in indicators
    private boolean mShowEMA20 = true;
    private boolean mShowSMA20 = true;
    private boolean mShowBollingerBands = true;
    private boolean mShowSupportResistance = true;

    // Paints
    private Paint mCandleUpPaint;
    private Paint mCandleDownPaint;
    private Paint mLinePaint;
    private Paint mTextPaint;
    private Paint mEmaPaint;
    private Paint mSmaPaint;
    private Paint mBbPaint;
    private Paint mSrPaint;
    
    // Colors
    private int mBgColor = Color.parseColor("#060B08"); // Velvet Dark Emerald Default
    private int mGridColor = Color.parseColor("#1B2F25");

    public CandlestickChartView(Context context) {
        super(context);
        init();
    }

    public CandlestickChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mCandleUpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCandleUpPaint.setColor(Color.parseColor("#10B981")); // Emerald Green
        mCandleUpPaint.setStyle(Paint.Style.FILL);

        mCandleDownPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCandleDownPaint.setColor(Color.parseColor("#EF4444")); // Red Rose
        mCandleDownPaint.setStyle(Paint.Style.FILL);

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setStrokeWidth(2f);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(24f);

        mEmaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mEmaPaint.setColor(Color.parseColor("#3B82F6")); // Blue EMA
        mEmaPaint.setStrokeWidth(3f);
        mEmaPaint.setStyle(Paint.Style.STROKE);

        mSmaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSmaPaint.setColor(Color.parseColor("#F59E0B")); // Orange SMA
        mSmaPaint.setStrokeWidth(3f);
        mSmaPaint.setStyle(Paint.Style.STROKE);

        mBbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBbPaint.setColor(Color.parseColor("#8B5CF6")); // Purple BB
        mBbPaint.setStrokeWidth(2f);
        mBbPaint.setStyle(Paint.Style.STROKE);

        mSrPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSrPaint.setColor(Color.parseColor("#14B8A6")); // Teal support/resistance
        mSrPaint.setStrokeWidth(2f);
        mSrPaint.setStyle(Paint.Style.STROKE);
        mSrPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
    }

    public void setTheme(boolean isDark) {
        if (isDark) {
            mBgColor = Color.parseColor("#060B08"); // Velvet Dark Emerald
            mGridColor = Color.parseColor("#1B2F25");
            mTextPaint.setColor(Color.parseColor("#A3B899"));
        } else {
            mBgColor = Color.parseColor("#F2F6F3"); // Ivory Sage Light
            mGridColor = Color.parseColor("#D4DDD6");
            mTextPaint.setColor(Color.parseColor("#1E382A"));
        }
        invalidate();
    }

    public void setCandles(List<Candlestick> candles) {
        mCandles = candles;
        invalidate();
    }

    public List<Candlestick> getCandles() {
        return mCandles;
    }

    public void addCustomLine(String label, double level, int color) {
        mCustomLines.add(new CustomIndicatorLine(label, level, color));
        invalidate();
    }

    public void clearCustomLines() {
        mCustomLines.clear();
        invalidate();
    }

    public void setEnabledIndicators(boolean ema, boolean sma, boolean bb, boolean sr) {
        mShowEMA20 = ema;
        mShowSMA20 = sma;
        mShowBollingerBands = bb;
        mShowSupportResistance = sr;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(mBgColor);

        int width = getWidth();
        int height = getHeight();

        if (mCandles == null || mCandles.isEmpty()) {
            mTextPaint.setTextSize(36f);
            canvas.drawText("No Candlestick Data Loaded", width / 2f - 200, height / 2f, mTextPaint);
            return;
        }

        mTextPaint.setTextSize(24f);

        // Find min and max values to fit chart on screen
        double maxPrice = Double.MIN_VALUE;
        double minPrice = Double.MAX_VALUE;

        // Give some margin (top/bottom)
        for (Candlestick candle : mCandles) {
            if (candle.high > maxPrice) maxPrice = candle.high;
            if (candle.low < minPrice) minPrice = candle.low;
        }

        // Include Bollinger Band extrema or custom lines in calculation so they don't clip
        if (mShowBollingerBands && mCandles.size() >= 20) {
            IndicatorsEngine.BollingerBandsResult bb = IndicatorsEngine.calculateBollingerBands(mCandles);
            for (int i = 0; i < mCandles.size(); i++) {
                if (bb.upperBand[i] > maxPrice) maxPrice = bb.upperBand[i];
                if (bb.lowerBand[i] < minPrice && bb.lowerBand[i] > 0) minPrice = bb.lowerBand[i];
            }
        }

        for (CustomIndicatorLine line : mCustomLines) {
            if (line.level > maxPrice) maxPrice = line.level;
            if (line.level < minPrice) minPrice = line.level;
        }

        double priceRange = maxPrice - minPrice;
        if (priceRange == 0) priceRange = 1.0;

        // Add 5% padding to top and bottom of price range
        maxPrice += priceRange * 0.05;
        minPrice -= priceRange * 0.05;
        priceRange = maxPrice - minPrice;

        int numCandles = mCandles.size();
        float candleWidth = (float) width / numCandles;

        // Draw horizontal grid lines and prices
        int gridLinesCount = 5;
        for (int i = 0; i <= gridLinesCount; i++) {
            float y = (float) height * i / (float) gridLinesCount;
            // Map y coordinate back to price value
            double priceVal = maxPrice - (y / (float) height) * priceRange;
            
            // Grid line paint
            mLinePaint.setColor(mGridColor);
            mLinePaint.setStrokeWidth(1f);
            canvas.drawLine(0, y, width, y, mLinePaint);

            // Draw label
            canvas.drawText(String.format("$%.2f", priceVal), 15, y - 5, mTextPaint);
        }

        // Render Bollinger Bands (Lower layer)
        if (mShowBollingerBands && numCandles >= 20) {
            IndicatorsEngine.BollingerBandsResult bb = IndicatorsEngine.calculateBollingerBands(mCandles);
            for (int i = 1; i < numCandles; i++) {
                float startX = (i - 1) * candleWidth + candleWidth / 2f;
                float endX = i * candleWidth + candleWidth / 2f;

                float startUpperY = getPriceY(bb.upperBand[i - 1], minPrice, maxPrice, height);
                float endUpperY = getPriceY(bb.upperBand[i], minPrice, maxPrice, height);
                canvas.drawLine(startX, startUpperY, endX, endUpperY, mBbPaint);

                float startLowerY = getPriceY(bb.lowerBand[i - 1], minPrice, maxPrice, height);
                float endLowerY = getPriceY(bb.lowerBand[i], minPrice, maxPrice, height);
                canvas.drawLine(startX, startLowerY, endX, endLowerY, mBbPaint);
            }
        }

        // Render EMA 20 and SMA 20
        double[] ema20 = IndicatorsEngine.calculateEMA(mCandles, 20);
        double[] sma20 = IndicatorsEngine.calculateSMA(mCandles, 20);

        for (int i = 1; i < numCandles; i++) {
            float startX = (i - 1) * candleWidth + candleWidth / 2f;
            float endX = i * candleWidth + candleWidth / 2f;

            if (mShowEMA20) {
                float startEMA = getPriceY(ema20[i - 1], minPrice, maxPrice, height);
                float endEMA = getPriceY(ema20[i], minPrice, maxPrice, height);
                canvas.drawLine(startX, startEMA, endX, endEMA, mEmaPaint);
            }

            if (mShowSMA20) {
                float startSMA = getPriceY(sma20[i - 1], minPrice, maxPrice, height);
                float endSMA = getPriceY(sma20[i], minPrice, maxPrice, height);
                canvas.drawLine(startX, startSMA, endX, endSMA, mSmaPaint);
            }
        }

        // Render Support & Resistance lines
        if (mShowSupportResistance) {
            List<Double> srLevels = IndicatorsEngine.findSupportResistance(mCandles);
            for (double level : srLevels) {
                float y = getPriceY(level, minPrice, maxPrice, height);
                canvas.drawLine(0, y, width, y, mSrPaint);
                canvas.drawText(String.format("S/R: $%.1f", level), width - 180, y - 5, mTextPaint);
            }
        }

        // Draw Candlesticks (Wick and Body)
        for (int i = 0; i < numCandles; i++) {
            Candlestick candle = mCandles.get(i);
            float startX = i * candleWidth;
            float endX = (i + 1) * candleWidth;
            float centerX = startX + candleWidth / 2f;

            float highY = getPriceY(candle.high, minPrice, maxPrice, height);
            float lowY = getPriceY(candle.low, minPrice, maxPrice, height);
            float openY = getPriceY(candle.open, minPrice, maxPrice, height);
            float closeY = getPriceY(candle.close, minPrice, maxPrice, height);

            // Draw Wick
            mLinePaint.setColor(mTextPaint.getColor());
            mLinePaint.setStrokeWidth(2f);
            canvas.drawLine(centerX, highY, centerX, lowY, mLinePaint);

            // Draw Body
            float top = Math.min(openY, closeY);
            float bottom = Math.max(openY, closeY);
            float bodyWidthFactor = 0.8f; // fill 18% space on left/right
            float bodyLeft = centerX - (candleWidth * bodyWidthFactor / 2f);
            float bodyRight = centerX + (candleWidth * bodyWidthFactor / 2f);

            // Prevent hairline bodies from vanishing
            if (Math.abs(top - bottom) < 2) {
                bottom = top + 2;
            }

            if (candle.close >= candle.open) {
                canvas.drawRect(bodyLeft, top, bodyRight, bottom, mCandleUpPaint);
            } else {
                canvas.drawRect(bodyLeft, top, bodyRight, bottom, mCandleDownPaint);
            }
        }

        // Render custom (AI-driven) indicator lines
        for (CustomIndicatorLine line : mCustomLines) {
            float y = getPriceY(line.level, minPrice, maxPrice, height);
            mLinePaint.setColor(line.color);
            mLinePaint.setStrokeWidth(3f);
            canvas.drawLine(0, y, width, y, mLinePaint);

            Paint textP = new Paint(Paint.ANTI_ALIAS_FLAG);
            textP.setColor(line.color);
            textP.setTextSize(24f);
            canvas.drawText(line.label + String.format(" ($%.1f)", line.level), 15, y - 8, textP);
        }
    }

    private float getPriceY(double price, double minPrice, double maxPrice, int canvasHeight) {
        double relativePrice = (price - minPrice) / (maxPrice - minPrice);
        return (float) (canvasHeight - (relativePrice * canvasHeight));
    }
}
