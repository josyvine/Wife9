package com.tradeanalyst.app;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements VoiceAssistantBottomSheet.AssistantListener {

    private AppDatabase mDb;
    private TradingPreferences mPrefs;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    // Views
    private CandlestickChartView mChartView;
    private TextView mChartPriceText;
    private CheckBox mEmaCheck, mSmaCheck, mBbCheck, mSrCheck;
    private TextView mRsiText, mTrendText, mAlertsCountText;
    private EditText mAlertPriceInput;
    private Button mCreateAlertBtn;
    
    private Button mTabNewsBtn, mTabTradesBtn;
    private SwipeRefreshLayout mSwipeRefresh;
    private RecyclerView mFeedRecycler;
    private FeedAdapter mFeedAdapter;
    private FloatingActionButton mVoiceAssistantFab;

    // State Variables
    private List<Candlestick> mCandles = new ArrayList<>();
    private double mCurrentPrice = 64812.50;
    private int mActiveTab = 0; // 0 for News, 1 for Trades
    private final Random mRandom = new Random();

    // Simulated market news
    private List<FeedItem> mNewsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mPrefs = new TradingPreferences(this);
        
        // Dynamically configure theme on start based on user preferences
        if (mPrefs.isDarkThemeEnabled()) {
            setTheme(R.style.Theme_MyApplication);
        } else {
            setTheme(R.style.Theme_MyApplication); // Standard DayNight allows fallback
        }

        setContentView(R.layout.activity_main);

        mDb = AppDatabase.getDatabase(this);

        // Bind Views
        initViews();

        // Load Simulated Candles
        generateInitialCandles();

        // Apply theme styles to Custom Chart
        applyThemeStyles(mPrefs.isDarkThemeEnabled());

        // Setup Listener Events
        setupListeners();

        // Generate news updates
        generateSimulatedNews();

        // Default Load list
        refreshFeedList();

        // Start Price Ticker Simulator (live-updates price, candlestick updates and checks Alerts!)
        startLivePriceSimulator();
    }

    private void initViews() {
        mChartView = findViewById(R.id.candlestick_chart);
        mChartPriceText = findViewById(R.id.chart_price);
        
        mEmaCheck = findViewById(R.id.checkbox_ema20);
        mSmaCheck = findViewById(R.id.checkbox_sma20);
        mBbCheck = findViewById(R.id.checkbox_bb);
        mSrCheck = findViewById(R.id.checkbox_sr);

        mRsiText = findViewById(R.id.stat_rsi);
        mTrendText = findViewById(R.id.stat_trend);
        mAlertsCountText = findViewById(R.id.stat_alerts);

        mAlertPriceInput = findViewById(R.id.input_alert_price);
        mCreateAlertBtn = findViewById(R.id.btn_create_alert);

        mTabNewsBtn = findViewById(R.id.btn_tab_news);
        mTabTradesBtn = findViewById(R.id.btn_tab_trades);
        mSwipeRefresh = findViewById(R.id.swipe_refresh);
        mFeedRecycler = findViewById(R.id.feed_recycler_view);

        mFeedRecycler.setLayoutManager(new LinearLayoutManager(this));
        mFeedAdapter = new FeedAdapter();
        mFeedRecycler.setAdapter(mFeedAdapter);

        mVoiceAssistantFab = findViewById(R.id.btn_voice_assistant);
    }

    private void applyThemeStyles(boolean isDark) {
        mChartView.setTheme(isDark);
        View rootLayout = findViewById(R.id.main_coordinator);
        View toolbar = findViewById(R.id.toolbar);
        TextView title = findViewById(R.id.toolbar_title);

        if (isDark) {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.bg_dark_emerald, null));
            toolbar.setBackgroundColor(getResources().getColor(R.color.surface_dark_emerald, null));
            title.setTextColor(getResources().getColor(R.color.text_dark_theme, null));
        } else {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.bg_light_sage, null));
            toolbar.setBackgroundColor(getResources().getColor(R.color.surface_light_sage, null));
            title.setTextColor(getResources().getColor(R.color.text_light_theme, null));
        }
    }

    private void generateInitialCandles() {
        double startPrice = 64200.0;
        long time = System.currentTimeMillis() - (100 * 3600 * 1000L);

        for (int i = 0; i < 40; i++) {
            double change = (mRandom.nextDouble() - 0.48) * 600; // slightly upward bias
            double open = startPrice;
            double close = startPrice + change;
            double high = Math.max(open, close) + mRandom.nextDouble() * 200;
            double low = Math.min(open, close) - mRandom.nextDouble() * 200;

            mCandles.add(new Candlestick(open, high, low, close, time));
            startPrice = close;
            time += 3600 * 1000;
        }

        mCurrentPrice = startPrice;
        mChartPriceText.setText(String.format("$%,.2f", mCurrentPrice));
        mChartView.setCandles(mCandles);

        // Update local technical states
        double[] rsi = IndicatorsEngine.calculateRSI(mCandles, 14);
        double latestRsi = rsi[rsi.length - 1];
        mRsiText.setText(String.format("%.1f (%s)", latestRsi, latestRsi > 70 ? "Overbought" : (latestRsi < 30 ? "Oversold" : "Neutral")));

        double[] ema20 = IndicatorsEngine.calculateEMA(mCandles, 20);
        double latestEma = ema20[ema20.length - 1];
        if (mCurrentPrice > latestEma) {
            mTrendText.setText("Bullish EMA");
            mTrendText.setTextColor(Color.parseColor("#10B981"));
        } else {
            mTrendText.setText("Bearish EMA");
            mTrendText.setTextColor(Color.parseColor("#EF4444"));
        }
    }

    private void generateSimulatedNews() {
        mNewsList.clear();
        mNewsList.add(new FeedItem("Bitcoin Breakthrough: Key Support Level Maintained", "Calculated SMA 20 holding steady | AI Confidence: 82% Grounded", "82% BUY", Color.parseColor("#10B981"), System.currentTimeMillis()));
        mNewsList.add(new FeedItem("Standard Securities Commission Index Approval Likely", "Over-the-counter institutional inflows surge on expectations", "92% UP", Color.parseColor("#10B981"), System.currentTimeMillis() - 4 * 3600 * 1000));
        mNewsList.add(new FeedItem("Technical Indicators Alert: RSI Oversold Zone Hit", "Bitcoin local bottom confirmed at EMA support bands", "OVERSOLD", Color.parseColor("#14B8A6"), System.currentTimeMillis() - 12 * 3600 * 1000));
        mNewsList.add(new FeedItem("Macro Reports Push Trading Volume to Historic highs", "Global market liquidity surges by 4.2% overnight", "VOLUME", Color.parseColor("#14B8A6"), System.currentTimeMillis() - 24 * 3600 * 1000));
    }

    private void setupListeners() {
        // Settings dialog launch
        findViewById(R.id.btn_settings).setOnClickListener(v -> showSettingsDialog());

        // Indicator togglers
        View.OnClickListener indicatorToggleListener = v -> {
            mChartView.setEnabledIndicators(
                mEmaCheck.isChecked(),
                mSmaCheck.isChecked(),
                mBbCheck.isChecked(),
                mSrCheck.isChecked()
            );
        };
        mEmaCheck.setOnClickListener(indicatorToggleListener);
        mSmaCheck.setOnClickListener(indicatorToggleListener);
        mBbCheck.setOnClickListener(indicatorToggleListener);
        mSrCheck.setOnClickListener(indicatorToggleListener);

        // Alert Creator button
        mCreateAlertBtn.setOnClickListener(v -> {
            String alertValStr = mAlertPriceInput.getText().toString().trim();
            if (alertValStr.isEmpty()) {
                Toast.makeText(this, "Please enter a valid price threshold", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double targetPrice = Double.parseDouble(alertValStr);
                boolean isAbove = targetPrice > mCurrentPrice;

                mExecutor.execute(() -> {
                    PriceAlertEntity alert = new PriceAlertEntity("BTC/USD", targetPrice, isAbove, true);
                    mDb.tradeDao().insertPriceAlert(alert);
                    
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Price alert created at $" + targetPrice, Toast.LENGTH_SHORT).show();
                        mAlertPriceInput.setText("");
                        updateAlertCount();
                    });
                });
            } catch (Exception e) {
                Toast.makeText(this, "Enter correct decimal values.", Toast.LENGTH_SHORT).show();
            }
        });

        // Tab selection filters
        mTabNewsBtn.setOnClickListener(v -> {
            mActiveTab = 0;
            mTabNewsBtn.setTextColor(Color.parseColor("#10B981"));
            mTabTradesBtn.setTextColor(getResources().getColor(R.color.text_secondary_dark, null));
            refreshFeedList();
        });

        mTabTradesBtn.setOnClickListener(v -> {
            mActiveTab = 1;
            mTabTradesBtn.setTextColor(Color.parseColor("#10B981"));
            mTabNewsBtn.setTextColor(getResources().getColor(R.color.text_secondary_dark, null));
            refreshFeedList();
        });

        // Swipe Pull action
        mSwipeRefresh.setOnRefreshListener(() -> {
            if (mActiveTab == 0) {
                // Simulate refresh news items
                generateSimulatedNews();
            }
            refreshFeedList();
            mSwipeRefresh.setRefreshing(false);
        });

        // Voice button click
        mVoiceAssistantFab.setOnClickListener(v -> {
            VoiceAssistantBottomSheet sheet = VoiceAssistantBottomSheet.newInstance();
            sheet.setListener(this);
            sheet.setChartMetrics(mCandles, mCurrentPrice);
            sheet.show(getSupportFragmentManager(), "VoiceAssistantBottomSheet");
        });

        // Setup Alert counter initial state
        updateAlertCount();
    }

    private void updateAlertCount() {
        mExecutor.execute(() -> {
            List<PriceAlertEntity> alerts = mDb.tradeDao().getActivePriceAlerts();
            final int count = alerts.size();
            runOnUiThread(() -> mAlertsCountText.setText(count + " active alerts"));
        });
    }

    private void refreshFeedList() {
        if (mActiveTab == 0) {
            mFeedAdapter.setItems(mNewsList);
        } else {
            // Load from paper trades table from database!
            mExecutor.execute(() -> {
                List<PaperTradeTransaction> trades = mDb.tradeDao().getAllPaperTrades();
                List<FeedItem> tradeItems = new ArrayList<>();
                for (PaperTradeTransaction t : trades) {
                    int indicatorColor = "BUY".equalsIgnoreCase(t.action) ? Color.parseColor("#10B981") : Color.parseColor("#EF4444");
                    tradeItems.add(new FeedItem(
                        String.format("ORDER CONFIRMED: %s %s", t.action, t.symbol),
                        String.format("Executed at target price: $%,.2f | Confidence: %.0f%%", t.entryPrice, t.confidence),
                        t.action,
                        indicatorColor,
                        t.timestamp
                    ));
                }

                if (tradeItems.isEmpty()) {
                    tradeItems.add(new FeedItem(
                        "No Automated Trade Logged yet",
                        "Hold mic button below and voice order high-confidence BUY/SELL commands",
                        "EMPTY",
                        Color.parseColor("#14B8A6"),
                        System.currentTimeMillis()
                    ));
                }

                runOnUiThread(() -> mFeedAdapter.setItems(tradeItems));
            });
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Bind Settings Views
        EditText keyInput = dialogView.findViewById(R.id.settings_api_key_input);
        Spinner modelSpinner = dialogView.findViewById(R.id.settings_model_spinner);
        Button fetchModelsBtn = dialogView.findViewById(R.id.settings_btn_fetch_models);
        SwitchMaterial groundingSwitch = dialogView.findViewById(R.id.switch_grounding);
        SwitchMaterial themeSwitch = dialogView.findViewById(R.id.switch_theme);
        Button cancelBtn = dialogView.findViewById(R.id.settings_btn_cancel);
        Button saveBtn = dialogView.findViewById(R.id.settings_btn_save);

        // Read preconfigured configurations
        keyInput.setText(mPrefs.getApiKey());
        groundingSwitch.setChecked(mPrefs.isGroundingEnabled());
        themeSwitch.setChecked(mPrefs.isDarkThemeEnabled());

        // Fill Spinner models locally configured
        List<String> modelDisplayNames = new ArrayList<>();
        modelDisplayNames.add("Gemini 2.5 Flash Native Audio Preview");
        modelDisplayNames.add("Gemini 3.1 Flash Live Preview");

        List<String> modelIds = new ArrayList<>();
        modelIds.add("gemini-2.5-flash-native-audio-preview-12-2025");
        modelIds.add("gemini-3.1-flash-live-preview");

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modelDisplayNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(spinnerAdapter);

        // Preselect current
        String savedModel = mPrefs.getModel();
        int savedIdx = modelIds.indexOf(savedModel);
        if (savedIdx >= 0) {
            modelSpinner.setSelection(savedIdx);
        }

        // Fetch Online Options
        fetchModelsBtn.setOnClickListener(v -> {
            String apiKey = keyInput.getText().toString().trim();
            if (apiKey.isEmpty()) {
                Toast.makeText(this, "Provide a Gemini key first before fetching online!", Toast.LENGTH_SHORT).show();
                return;
            }

            fetchModelsBtn.setText("Fetching...");
            GeminiRetrofitClient.getService().listModels(apiKey).enqueue(new Callback<GeminiRetrofitClient.ModelsQueryResponse>() {
                @Override
                public void onResponse(Call<GeminiRetrofitClient.ModelsQueryResponse> call, Response<GeminiRetrofitClient.ModelsQueryResponse> response) {
                    fetchModelsBtn.setText("Fetch Models Online");
                    if (response.isSuccessful() && response.body() != null && response.body().models != null) {
                        List<GeminiRetrofitClient.GeminiModelInfo> fetched = response.body().models;
                        
                        modelDisplayNames.clear();
                        modelIds.clear();

                        for (GeminiRetrofitClient.GeminiModelInfo m : fetched) {
                            // Filter valid generative models
                            if (m.name.contains("gemini") && m.supportedGenerationMethods.contains("generateContent")) {
                                String cleanName = m.name.replace("models/", "");
                                modelDisplayNames.add(m.displayName + " (" + cleanName + ")");
                                modelIds.add(cleanName);
                            }
                        }

                        // Rebind spinner
                        spinnerAdapter.notifyDataSetChanged();
                        Toast.makeText(MainActivity.this, "Successfully synced over-the-air Models!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Unauthorized API Key or invalid response.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<GeminiRetrofitClient.ModelsQueryResponse> call, Throwable t) {
                    fetchModelsBtn.setText("Fetch Models Online");
                    Toast.makeText(MainActivity.this, "Connection failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        saveBtn.setOnClickListener(v -> {
            String key = keyInput.getText().toString().trim();
            mPrefs.saveApiKey(key);
            mPrefs.saveGroundingEnabled(groundingSwitch.isChecked());
            
            boolean themeChanged = mPrefs.isDarkThemeEnabled() != themeSwitch.isChecked();
            mPrefs.saveDarkThemeEnabled(themeSwitch.isChecked());

            int selectedPos = modelSpinner.getSelectedItemPosition();
            if (selectedPos < modelIds.size()) {
                mPrefs.saveModel(modelIds.get(selectedPos));
            }

            dialog.dismiss();
            Toast.makeText(this, "AI Configuration Updated!", Toast.LENGTH_SHORT).show();

            if (themeChanged) {
                // Instantly swap styling modes on the fly
                applyThemeStyles(themeSwitch.isChecked());
            }
        });

        dialog.show();
    }

    private void startLivePriceSimulator() {
        // Timed runnable simulating live cryptocurrency tickers
        android.os.Handler handler = new android.os.Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Tweak price by small random fraction
                double deviation = (mRandom.nextDouble() - 0.5) * 80;
                mCurrentPrice += deviation;

                mChartPriceText.setText(String.format("$%,.2f", mCurrentPrice));

                // Update last candle close & high/low
                if (!mCandles.isEmpty()) {
                    Candlestick last = mCandles.get(mCandles.size() - 1);
                    last.close = mCurrentPrice;
                    if (mCurrentPrice > last.high) last.high = mCurrentPrice;
                    if (mCurrentPrice < last.low) last.low = mCurrentPrice;
                    mChartView.invalidate();
                }

                // Check alert triggers
                checkPriceAlerts();

                handler.postDelayed(this, 5000); // cycle every 5 seconds
            }
        }, 5000);
    }

    private void checkPriceAlerts() {
        mExecutor.execute(() -> {
            List<PriceAlertEntity> active = mDb.tradeDao().getActivePriceAlerts();
            for (PriceAlertEntity alert : active) {
                boolean trigger = false;
                if (alert.isAbove && mCurrentPrice >= alert.targetPrice) {
                    trigger = true;
                } else if (!alert.isAbove && mCurrentPrice <= alert.targetPrice) {
                    trigger = true;
                }

                if (trigger) {
                    mDb.tradeDao().setPriceAlertActive(alert.id, false);
                    final double val = alert.targetPrice;
                    runOnUiThread(() -> {
                        Toast.makeText(this, "🔔 PRICE ALERT TRIGGERED: BTC/USD is now $" + String.format("%.2f", mCurrentPrice) + " (Target was $" + val + ")", Toast.LENGTH_LONG).show();
                        updateAlertCount();
                    });
                }
            }
        });
    }

    // --- VoiceAssistantBottomSheet Callback Handlers ---

    @Override
    public void onAutomaticOrderExecuted(PaperTradeTransaction trade) {
        // Automatically inserts are triggered by bottom sheet, we just update the views!
        refreshFeedList();
        
        // Let's add it to News list feed too to make user feel the execution
        mNewsList.add(0, new FeedItem(
            "[AUTO BOT EXECUTION] LONG BTC/USD SUCCESSFUL",
            "Automatic paper trading engine successfully triggered at target Entry: $" + trade.entryPrice,
            "BOT EXEC",
            Color.parseColor("#10B981"),
            System.currentTimeMillis()
        ));
        refreshFeedList();
    }

    @Override
    public void onCustomIndicatorGenerated(String label, double price, int color) {
        // Draw the indicator custom level generated by AI instantly onto the chart canvas!
        mChartView.addCustomLine(label, price, color);
    }

    @Override
    public void onRefreshRequired() {
        refreshFeedList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdown();
    }
}
