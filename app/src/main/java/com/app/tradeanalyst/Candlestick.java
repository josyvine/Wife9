package com.tradeanalyst.app;

public class Candlestick {
    public double open;
    public double high;
    public double low;
    public double close;
    public long timestamp;

    public Candlestick(double open, double high, double low, double close, long timestamp) {
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.timestamp = timestamp;
    }
}
