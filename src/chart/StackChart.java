package chart;

import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;

public class StackChart {
	
	private CategoryAxis generationAxis;
	private NumberAxis valueAxis;
	
	private BarChart<String, Number> chart;
	private XYChart.Series<String, Number> series;
	
	private int generation;
	
	private SimpleFloatProperty minValue = new SimpleFloatProperty(Float.MAX_VALUE);
	private SimpleFloatProperty maxValue = new SimpleFloatProperty(Float.MIN_VALUE);
	
	public StackChart() {
			
		valueAxis = new NumberAxis();
		generationAxis = new CategoryAxis();
		chart = new BarChart<>(generationAxis, valueAxis);	
		
		chart.setAnimated(false);
		
		valueAxis.setLabel("Total Value");
		valueAxis.setAutoRanging(false);
				
		series = new Series<String, Number>();
		series.setName("Generation");
		
		chart.getData().add(series);	
		chart.setCategoryGap(0.1);
		chart.setBarGap(0.1);
		//chart.setStyle("CHART_COLOR_1: #000000;");
		
		ChangeListener<Number> updateListener = new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				valueAxis.setTickUnit((int)((maxValue.get() - minValue.get()) / 10f));
			}
		};
	
		minValue.addListener(updateListener);
		maxValue.addListener(updateListener);
		
		reset();
	}
	
	public BarChart<String, Number> get() {
		return chart;
	}
	
	public void reset() {
		
		generation = 0;
		
		series.getData().clear();
		
		valueAxis.setLowerBound(0);
		valueAxis.setUpperBound(1);
		
		minValue.set(Float.MAX_VALUE);
		maxValue.set(Float.MIN_VALUE);
	}
	
	public void addValue(Float value) {
		
		if (series.getData().size() > 200) {
			series.getData().remove(0);
		}
				
		if (value < minValue.get()) {
			minValue.set(value);
			valueAxis.setLowerBound(value);
		}
		
		if (value > maxValue.get()) {
			maxValue.set(value);
			valueAxis.setUpperBound(value);
		}
		
		series.getData().add(new Data<String, Number>(Integer.toString(generation), value));
		generation++;
	}
}
