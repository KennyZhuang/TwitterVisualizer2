package com.example.kenny.twittertest;

import java.util.ArrayList;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendPosition;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.Highlight;
import com.github.mikephil.charting.utils.PercentFormatter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class WordcloudActivity extends Activity {

	private RelativeLayout mainLayout;
	private PieChart mChart;
	// we're going to display pie chart for smartphones martket shares
	//private float[] yData = { 5, 10, 15, 30, 40 };
	//private String[] xData = { "Sony", "Huawei", "LG", "Apple", "Samsung" };
	private float[] yData = new float[3];
	private String[] xData = { "Positive", "Neutral", "Negative" };
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wordcloud);
		mainLayout = (RelativeLayout) findViewById(R.id.wordcloud_layout);
		Intent intent = getIntent();
		Bundle b = getIntent().getExtras();
		yData[0] = b.getInt("pos");
		yData[1] = b.getInt("neu");
		yData[2] = b.getInt("neg");
		String lName = intent.getStringExtra("lname");
		mChart = new PieChart(this);
		// add pie chart to main layout
		mainLayout.addView(mChart);
		mainLayout.setBackgroundColor(Color.parseColor("#55656C"));

		// configure pie chart
		mChart.setUsePercentValues(true);
		mChart.setDescription("Smartphones Market Share");

		// enable hole and configure
		mChart.setDrawHoleEnabled(true);
		mChart.setHoleColorTransparent(true);
		mChart.setHoleRadius(7);
		mChart.setTransparentCircleRadius(10);

		// enable rotation of the chart by touch
		mChart.setRotationAngle(0);
		mChart.setRotationEnabled(true);

		// set a chart value selected listener


		// add data
		addData();

		// customize legends
		Legend l = mChart.getLegend();
		l.setPosition(LegendPosition.RIGHT_OF_CHART);
		l.setXEntrySpace(7);
		l.setYEntrySpace(5);
	}

	private void addData() {
		ArrayList<Entry> yVals1 = new ArrayList<Entry>();

		for (int i = 0; i < yData.length; i++)
			yVals1.add(new Entry(yData[i], i));

		ArrayList<String> xVals = new ArrayList<String>();

		for (int i = 0; i < xData.length; i++)
			xVals.add(xData[i]);

		// create pie data set
		PieDataSet dataSet = new PieDataSet(yVals1, "Market Share");
		dataSet.setSliceSpace(3);
		dataSet.setSelectionShift(5);

		// add many colors
		ArrayList<Integer> colors = new ArrayList<Integer>();

		for (int c : ColorTemplate.VORDIPLOM_COLORS)
			colors.add(c);

		for (int c : ColorTemplate.JOYFUL_COLORS)
			colors.add(c);

		for (int c : ColorTemplate.COLORFUL_COLORS)
			colors.add(c);

		for (int c : ColorTemplate.LIBERTY_COLORS)
			colors.add(c);

		for (int c : ColorTemplate.PASTEL_COLORS)
			colors.add(c);

		colors.add(ColorTemplate.getHoloBlue());
		dataSet.setColors(colors);

		// instantiate pie data object now
		PieData data = new PieData(xVals, dataSet);
		data.setValueFormatter(new PercentFormatter());
		data.setValueTextSize(11f);
		data.setValueTextColor(Color.GRAY);

		mChart.setData(data);

		// undo all highlights
		mChart.highlightValues(null);

		// update pie chart
		mChart.invalidate();
	}

}