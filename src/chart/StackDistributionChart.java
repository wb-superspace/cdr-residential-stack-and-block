package chart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.sun.jndi.toolkit.ctx.StringHeadTail;

import cdr.colour.HEXColour;
import cdr.geometry.primitives.Point3D;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import model.StackEvaluator;
import model.StackManager;
import model.StackAnalysis.AnalysisFloor;
import model.StackAnalysis.AnalysisFloor.AnalysisUnit;

public class StackDistributionChart {

	private final StackManager sm;
	private final StackEvaluator se;
	
	/*
	 * =========================================================
	 * SCATTER CHART
	 * =========================================================
	 */
	
	private ScatterChart<Number, Number> scatterChart;
	private NumberAxis scatterValueAxis;
	private NumberAxis scatterHeightAxis;
	
	private SimpleStringProperty attribute = new SimpleStringProperty();
	
	private Runnable update = new Runnable() {
		
		@Override
		public void run() {
			addValues(StackDistributionChart.this.se.getAnalysisStacks());
		}
	};
	
	public StackDistributionChart(StackManager sm, StackEvaluator se, String attribute) {
		
		this.sm = sm;
		this.se = se;
		
		this.attribute.set(attribute);
				
		//---------------------------
		
		this.scatterValueAxis = new NumberAxis();
		this.scatterHeightAxis = new NumberAxis();
		this.scatterHeightAxis.setLabel("unitFloor ");
		
		this.scatterChart = new ScatterChart<>(scatterHeightAxis, scatterValueAxis);
		this.scatterChart.setAnimated(false);
		this.scatterChart.setLegendVisible(false);
		this.scatterChart.setHorizontalGridLinesVisible(true);
		this.scatterChart.setVerticalGridLinesVisible(true);
		this.scatterChart.setTranslateX(-20);
				
		this.se.getGeneration().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				Platform.runLater(update);
			}
		});
		
		this.attribute.addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				Platform.runLater(update);
			}
		});
	}
	
	public void clearCharts() {
		this.scatterChart.getData().clear();
	}
	
	public void setAttribute(String attribute) {
		this.attribute.set(attribute);
	}
	
	public ScatterChart<Number, Number> getScatterChart() {
		return this.scatterChart;
	}
	
	private void addValues(Map<Point3D, Stack<AnalysisFloor>> analysisStacks) {
				
		this.scatterChart.getData().clear();
		this.scatterValueAxis.setLabel(this.attribute.get() + " [\u00A3]");
		
		Map<String, XYChart.Series<Number, Number>> scatterSeries = new HashMap<>();
		
		for (String unitType : this.sm.getUnitTypes()) {
			scatterSeries.put(unitType, new XYChart.Series<>());
		}
		
		for (Map.Entry<Point3D, Stack<AnalysisFloor>> analysisEntry : analysisStacks.entrySet()) {	
						
			List<AnalysisUnit> analysisUnits = new ArrayList<>();
			for (AnalysisFloor analysisFloor : analysisEntry.getValue()) {			
				for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {
					
					if (analysisUnit.getUnitType() != null) {
						
						analysisUnits.add(analysisUnit);
						
						int height = analysisUnit.getFloorIndex();
						float delta = analysisUnit.getAttribute(attribute.get());
						
				        XYChart.Data<Number, Number> dt=  new XYChart.Data<Number, Number>(height, delta);
				        Rectangle rect1 = new Rectangle(5, 5);
				        
				        HEXColour x = new HEXColour(sm.unitColors.get(analysisUnit.getUnitType()));	        
				        Color c = new Color(x.red(), x.green(), x.blue(), 1);
				        
				        rect1.setFill(c);
				        rect1.setStrokeWidth(0.1);
				        rect1.setStroke(Color.BLACK);
				        dt.setNode(rect1);
						
						scatterSeries.get(analysisUnit.getUnitType()).getData().add(dt);
					}
				}
			}
		}		
		
		for (Map.Entry<String, XYChart.Series<Number, Number>> series : scatterSeries.entrySet()) {			
			series.getValue().setName(series.getKey());
			this.scatterChart.getData().add(series.getValue());
		}
	}
}
