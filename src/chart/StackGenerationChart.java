package chart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import cdr.colour.HEXColour;
import cdr.geometry.primitives.Point3D;
import javafx.application.Platform;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import model.StackAnalysis;
import model.StackAnalysis.AnalysisFloor;
import model.StackAnalysis.AnalysisFloor.AnalysisUnit;
import model.StackEvaluator;
import model.StackManager;

public class StackGenerationChart {
	
	private final StackManager sm;
	private final StackEvaluator se;
		
	/*
	 * =========================================================
	 * VALUE CHART
	 * =========================================================
	 */
	
	private StackedBarChart<String, Number> valueChart;
	private CategoryAxis valueGenerationAxis;
	private NumberAxis valueValueAxis;
	private XYChart.Series<String, Number> valueSeries;
	
	/*
	 * =========================================================
	 * DISTRIBUTION CHART
	 * =========================================================
	 */
	
	private StackedBarChart<String, Number> distributionChart;
	private CategoryAxis distributionGenerationAxis;
	private NumberAxis distributionValueAxis;
	private Map<Point3D, XYChart.Series<String, Number>> distributionSeries;
		
	/*
	 * =========================================================
	 * CHART PROPERTIES
	 * =========================================================
	 */
	
	private SortedMap<Integer, String> generations = new TreeMap<>();
	
	private SimpleFloatProperty minValue = new SimpleFloatProperty(Float.MAX_VALUE);
	private SimpleFloatProperty maxValue = new SimpleFloatProperty(-Float.MAX_VALUE);
	

	public StackGenerationChart(StackManager sm, StackEvaluator se) {
		
		this.sm = sm;
		this.se = se;
		
		//---------------------------
		
		this.distributionValueAxis = new NumberAxis();
		this.distributionValueAxis.setLabel("Stack Distribution [\u00A3]");
		this.distributionValueAxis.setAutoRanging(false);

		this.distributionGenerationAxis = new CategoryAxis();
		
		this.distributionChart = new StackedBarChart<>(distributionGenerationAxis, distributionValueAxis);		
		this.distributionChart.setAnimated(false);
		this.distributionChart.setCategoryGap(0.1);
			
		this.distributionSeries = new HashMap<>();
		
		//---------------------------

		this.valueValueAxis = new NumberAxis();
		this.valueValueAxis.setLabel("Value Increase [\u00A3]");
		this.valueValueAxis.setAutoRanging(false);
		
		this.valueGenerationAxis = new CategoryAxis();
		
		this.valueChart = new StackedBarChart<>(valueGenerationAxis, valueValueAxis);		
		this.valueChart.setAnimated(false);
		this.valueChart.setCategoryGap(0.1);
		this.valueChart.setStyle("CHART_COLOR_1: #899bb7;");
		
		this.valueSeries = new XYChart.Series<>();	
		this.valueChart.getData().add(valueSeries);	
			
		/*
		 * =========================================================
		 * LISTENERS
		 * =========================================================
		 */
		
		ChangeListener<Number> updateListener = new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				valueValueAxis.setTickUnit((int)((maxValue.get() - minValue.get()) / 10f));
				distributionValueAxis.setTickUnit((int)((maxValue.get() - 0) / 10f));
			}
		};
	
		this.minValue.addListener(updateListener);
		this.maxValue.addListener(updateListener);
		
		this.se.getGeneration().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				
				Platform.runLater(new Runnable() {
					
					@Override
					public void run() {		
						if (oldValue.intValue() < newValue.intValue()) {
							addValues(StackGenerationChart.this.se.getAnalysisStacks(), (int) newValue);
						}
					}
				});
			}
		});
		
		clearCharts();
	}
	
	public StackedBarChart<String, Number> getStacksChart() {
		return this.distributionChart;
	}
	
	public StackedBarChart<String, Number> getValueChart() {
		return this.valueChart;
	}
			
	public void clearCharts() {
					
		this.distributionSeries.clear();
		this.distributionChart.getData().clear();
		this.distributionGenerationAxis.setCategories(FXCollections.observableArrayList());
		this.distributionValueAxis.setLowerBound(0);
		this.distributionValueAxis.setUpperBound(1);
		
		this.valueChart.getData().clear();
		this.valueSeries = new XYChart.Series<>();	
		this.valueChart.getData().add(valueSeries);
		this.valueGenerationAxis.setCategories(FXCollections.observableArrayList());
		this.valueValueAxis.setLowerBound(0);
		this.valueValueAxis.setUpperBound(1);		
		
		this.generations = new TreeMap<>();
		
		this.minValue.set(Float.MAX_VALUE);
		this.maxValue.set(-Float.MAX_VALUE);
	}
	
	private void addValues(Map<Point3D, Stack<AnalysisFloor>> analysisStacks, int generation) {
		
		float value = 0;
		
		for (Map.Entry<Point3D, Stack<AnalysisFloor>> analysisEntry : analysisStacks.entrySet()) {	
			
			if (!distributionSeries.containsKey(analysisEntry.getKey())) {
				distributionSeries.put(analysisEntry.getKey(), new XYChart.Series<>());
				distributionChart.getData().add(distributionSeries.get(analysisEntry.getKey()));
			}
			
			List<AnalysisUnit> analysisUnits = new ArrayList<>();
			for (AnalysisFloor analysisFloor : analysisEntry.getValue()) {			
				for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {
					
					if (analysisUnit.getUnitType() != null) {						
						analysisUnits.add(analysisUnit);
					}
				}
			}
						
			float v = StackAnalysis.getAnalysisAttribute(analysisUnits, "unitDelta-total").getSum();
			this.distributionSeries.get(analysisEntry.getKey()).getData()
				.add(new Data<String, Number>(Integer.toString(generation), v));

			value += v;
		}		
					
		if (value < minValue.get()) {
			this.minValue.set(value);
			this.valueValueAxis.setLowerBound(value);
		}
		
		if (value > maxValue.get()) {
			this.maxValue.set(value);
			this.valueValueAxis.setUpperBound(value);
			this.distributionValueAxis.setUpperBound(value);
		}

			
		this.distributionGenerationAxis.getCategories().add(Integer.toString(generation));
		this.valueGenerationAxis.getCategories().add(Integer.toString(generation));
		
		this.valueSeries.getData().add(new Data<String, Number>(Integer.toString(generation), value));
		this.generations.put(generation, StackAnalysis.hashAnalysisStack(analysisStacks));
	}
}
