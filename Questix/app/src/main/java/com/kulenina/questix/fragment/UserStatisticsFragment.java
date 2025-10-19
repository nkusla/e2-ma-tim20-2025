package com.kulenina.questix.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import com.kulenina.questix.R;
import com.kulenina.questix.databinding.FragmentUserStatisticsBinding;
import com.kulenina.questix.model.StatisticsData;
import com.kulenina.questix.service.UserStatisticsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserStatisticsFragment extends Fragment {

    private FragmentUserStatisticsBinding binding;
    private UserStatisticsService statisticsService;

    public UserStatisticsFragment() {}

    public static UserStatisticsFragment newInstance() {
        return new UserStatisticsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        statisticsService = new UserStatisticsService();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_statistics, container, false);
        binding.setFragment(this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupCharts();
        loadStatistics();
    }

    private void setupCharts() {
        setupPieChart();
        setupBarChart();
        setupLineCharts();
    }

    private void setupPieChart() {
        PieChart pieChart = binding.donutChart;
        pieChart.setUsePercentValues(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        Description desc = new Description();
        desc.setText("");
        pieChart.setDescription(desc);

        pieChart.getLegend().setEnabled(true);
    }

    private void setupBarChart() {
        BarChart barChart = binding.categoryBarChart;
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setMaxVisibleValueCount(60);
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(false);

        Description desc = new Description();
        desc.setText("");
        barChart.setDescription(desc);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);

        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
    }

    private void setupLineCharts() {
        setupLineChart(binding.difficultyLineChart);
        setupLineChart(binding.xpProgressLineChart);
    }

    private void setupLineChart(LineChart lineChart) {
        lineChart.setDrawGridBackground(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        Description desc = new Description();
        desc.setText("");
        lineChart.setDescription(desc);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(true);
    }

    private void loadStatistics() {
        statisticsService.getUserStatistics()
            .addOnSuccessListener(this::displayStatistics)
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), getString(R.string.statistics_load_error, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void displayStatistics(StatisticsData stats) {
        binding.tvActiveDays.setText(getString(R.string.consecutive_days, stats.activeDaysCount));
        binding.tvCompletedCount.setText(String.valueOf(stats.totalCompletedTasks));
        binding.tvMissedCount.setText(String.valueOf(stats.totalMissedTasks));
        binding.tvCanceledCount.setText(String.valueOf(stats.totalCanceledTasks));

        int activeTasks = stats.totalCreatedTasks - stats.totalCompletedTasks -
                         stats.totalMissedTasks - stats.totalCanceledTasks;
        binding.tvActiveCount.setText(String.valueOf(activeTasks));

        binding.tvLongestStreak.setText(getString(R.string.days_in_row, stats.longestSuccessStreak));
        binding.tvStartedMissions.setText(String.valueOf(stats.startedSpecialMissions));
        binding.tvCompletedMissions.setText(String.valueOf(stats.completedSpecialMissions));
        
        setupPieChartData(stats);
        setupBarChartData(stats.completedTasksByCategory);
        setupDifficultyLineChart(stats.averageDifficultyData);
        setupXpProgressLineChart(stats.last7DaysXp);
    }

    private void setupPieChartData(StatisticsData stats) {
        PieChart pieChart = binding.donutChart;

        ArrayList<PieEntry> entries = new ArrayList<>();

        if (stats.totalCompletedTasks > 0) {
            entries.add(new PieEntry(stats.totalCompletedTasks, "Completed"));
        }
        if (stats.totalMissedTasks > 0) {
            entries.add(new PieEntry(stats.totalMissedTasks, "Missed"));
        }
        if (stats.totalCanceledTasks > 0) {
            entries.add(new PieEntry(stats.totalCanceledTasks, "Canceled"));
        }

        int activeTasks = stats.totalCreatedTasks - stats.totalCompletedTasks -
                         stats.totalMissedTasks - stats.totalCanceledTasks;
        if (activeTasks > 0) {
            entries.add(new PieEntry(activeTasks, "Active"));
        }

        if (entries.isEmpty()) {
            pieChart.setNoDataText("No tasks created yet");
            pieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "Task Status");
        dataSet.setColors(new int[]{
            getResources().getColor(R.color.success_color, null),
            getResources().getColor(R.color.error_color, null),
            getResources().getColor(R.color.warning_color, null),
            getResources().getColor(R.color.info_color, null)
        });
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void setupBarChartData(Map<String, Integer> categoryData) {
        BarChart barChart = binding.categoryBarChart;

        if (categoryData.isEmpty()) {
            barChart.setNoDataText(getString(R.string.no_completed_tasks));
            barChart.invalidate();
            return;
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Integer> entry : categoryData.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Tasks by Category");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);

        barChart.setData(data);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setLabelCount(labels.size());
        barChart.invalidate();
    }

    private void setupDifficultyLineChart(List<StatisticsData.DifficultyXpData> difficultyData) {
        LineChart lineChart = binding.difficultyLineChart;

        if (difficultyData.isEmpty()) {
            lineChart.setNoDataText(getString(R.string.no_completed_tasks));
            lineChart.invalidate();
            return;
        }

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < difficultyData.size(); i++) {
            StatisticsData.DifficultyXpData data = difficultyData.get(i);
            entries.add(new Entry(i, (float) data.averageXp));
            labels.add(data.difficulty);
        }

        LineDataSet dataSet = new LineDataSet(entries, "Average XP by Difficulty");
        dataSet.setColor(getResources().getColor(R.color.accent_color, null));
        dataSet.setCircleColor(getResources().getColor(R.color.accent_color, null));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);

        LineData data = new LineData(dataSet);
        lineChart.setData(data);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        lineChart.getXAxis().setLabelCount(labels.size());
        lineChart.invalidate();
    }

    private void setupXpProgressLineChart(List<StatisticsData.XpProgressData> xpData) {
        LineChart lineChart = binding.xpProgressLineChart;

        if (xpData.isEmpty()) {
            lineChart.setNoDataText(getString(R.string.no_xp_data));
            lineChart.invalidate();
            return;
        }

        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < xpData.size(); i++) {
            StatisticsData.XpProgressData data = xpData.get(i);
            entries.add(new Entry(i, data.xpEarned));
            labels.add(data.date);
        }

        LineDataSet dataSet = new LineDataSet(entries, "XP Earned");
        dataSet.setColor(getResources().getColor(R.color.accent_color, null));
        dataSet.setCircleColor(getResources().getColor(R.color.accent_color, null));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setValueTextSize(10f);
        dataSet.setFillAlpha(65);
        dataSet.setFillColor(getResources().getColor(R.color.accent_color, null));
        dataSet.setDrawFilled(true);

        LineData data = new LineData(dataSet);
        lineChart.setData(data);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        lineChart.getXAxis().setLabelCount(labels.size());
        lineChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}