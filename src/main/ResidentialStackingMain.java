package main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.Stack;

import com.sun.corba.se.impl.orbutil.graph.Node;
import com.sun.xml.internal.ws.policy.EffectiveAlternativeSelector;

import cdr.colour.HEXColour;
import cdr.colour.HSVColour;
import cdr.geometry.primitives.Point3D;
import cdr.gui.javaFX.JavaFXGUI;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.legend.BarChartItem;
import javafx.legend.GridPaneBarChart;
import javafx.legend.LegendItem;
import javafx.legend.VBoxLegend;
import javafx.scene.chart.Chart;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import model.StackAnalysis;
import model.StackAnalysis.AnalysisAttribute;
import model.StackAnalysis.AnalysisFloor;
import model.StackAnalysis.AnalysisFloor.AnalysisUnit;

public class ResidentialStackingMain  extends JavaFXGUI<ResidentialStackingRenderer> implements Initializable{

	ResidentialStackingRenderer application;
	
	public BorderPane applicationBorderPane;
	
	public TitledPane performanceTitledPane;
	public TitledPane stackTitledPane;
	public TitledPane legendTitledPane;

	public VBox distributionChartVBox;
	public TitledPane distributionTitledPane;
	
	public VBox generationsChartVBox;
	public TitledPane generationsChartTitledPane;
	
	public MenuItem importGeometryMenuItem;
	public MenuItem importUnitMixMenuItem;
	public MenuItem importStackMenuItem;
	
	public MenuItem exportStackMenuItem;
	public MenuItem exportImageMenuItem;
	
	public MenuItem startMenuItem;
	public MenuItem stopMenuItem;
	public MenuItem resumeMenuItem;
	
	public ResidentialStackingMain(ResidentialStackingRenderer application) {
		super(application);
		this.application = application;
	}
	
	public static void main(String[] args) {
		
		ResidentialStackingMain gui = new ResidentialStackingMain(new ResidentialStackingRenderer());
		gui.buildAndShowGUI("Residential Stacking Evaluator");
	}

	@Override
	protected Pane createPane(ResidentialStackingRenderer arg0) {
		
		Pane pane = null;
		try {
			FXMLLoader floader = new FXMLLoader(
					ResidentialStackingMain.class
							.getResource("GUITemplate.fxml"));
			floader.setController(this);
			pane = (Pane) floader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return pane;
	}
	
	@Override
	protected String getStyleSheetPath() {
		return "main/GUIStyleSheet.css";
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		initializeMenuItems();
	}

	private void initializeMenuItems() {
		
		importGeometryMenuItem.setOnAction(new EventHandler<ActionEvent>() {
			
			@Override
			public void handle(ActionEvent event) {		
				
				if (application.importGeometry()) {
					
				}
			}
		});
		
		importUnitMixMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				
				if (application.importUnitMix()) {
					
					initializeTitledPanes();
					initializeCharts();
				}
			}
		});
		
		importStackMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				if (application.importStack()) {
					
					updatePerformanceTitledPane();
				}
			}
		});
		
		exportStackMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				
				FileChooser fc = new FileChooser();
				ExtensionFilter csvExtensionFilter = new ExtensionFilter("CSV", "*.csv") ; 
				fc.getExtensionFilters().add(csvExtensionFilter); 
				fc.setInitialDirectory(new File(System.getProperty("user.dir")));
				fc.setSelectedExtensionFilter(csvExtensionFilter);
				File file = fc.showSaveDialog(null);

				application.exportStack(file);
			}
		});
		
		exportImageMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				
				FileChooser fc = new FileChooser();
				ExtensionFilter csvExtensionFilter = new ExtensionFilter("PNG", "*.png") ; 
				fc.getExtensionFilters().add(csvExtensionFilter); 
				fc.setInitialDirectory(new File(System.getProperty("user.dir")));
				fc.setSelectedExtensionFilter(csvExtensionFilter);
				File file = fc.showSaveDialog(null);

				application.exportImage(file, generationsChartVBox.getScene().getRoot());
			}
		});
		
		startMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				application.start();
				performanceTitledPane.setExpanded(true);
				generationsChartTitledPane.setExpanded(true);
			}
		});
		
		stopMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				application.stop();
			}
		});
		
		resumeMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				application.resume();
			}
		});
	}
	
	private void initializeCharts() {
		
		generationsChartVBox.setStyle("-fx-background-color: white;");
		
		Chart stacksChart = application.sc.getStacksChart();
	
		stacksChart.setPrefHeight(100);
		stacksChart.prefWidthProperty().bind(generationsChartVBox.widthProperty().multiply(0.95f));
		
		Chart valueChart = application.sc.getValueChart();
		
		valueChart.setPrefHeight(100);
		valueChart.prefWidthProperty().bind(generationsChartVBox.widthProperty().multiply(0.95f));
		
		Chart scatterChart = application.sd.getScatterChart();
		
		scatterChart.setPrefHeight(200);
		scatterChart.setMaxHeight(400);
				
		generationsChartVBox.getChildren().clear();
		generationsChartVBox.getChildren().add(valueChart);
		generationsChartVBox.getChildren().add(stacksChart);
		
		distributionChartVBox.getChildren().clear();
		distributionChartVBox.getChildren().add(scatterChart);
	}
	
	private void initializeTitledPanes() {
		
		application.se.getGeneration().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue,
					Number newValue) {

				updatePerformanceTitledPane();
			}
		});
		
		application.attributeIndex.addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				updatePerformanceTitledPane();
			}
		});
	}
	
	private String formatToValue(float value, String label) {
		
		if (label == "\u00A3") {
			return label +  NumberFormat.getNumberInstance(Locale.US).format((int)value);
		} else {
			return (int) Math.round(value) + label;
		}
	}
	
	private void updatePerformanceTitledPane() {
		
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				
				if (application.sm == null || 
					application.se == null ||
					application.se.value.get() == -Float.MAX_VALUE ||
					application.se.counts.isEmpty()) {
					return;
				}
				
				stackTitledPane.setVisible(true);
												
				Map<Point3D, Stack<AnalysisFloor>> analysisStacks = application.se.getAnalysisStacks();
					
				ObservableList<BarChartItem> stackItems = FXCollections.observableArrayList();
				ObservableList<BarChartItem> valueItems = FXCollections.observableArrayList();
				
				Map<String, List<AnalysisUnit>> unitTypes = new HashMap<>();
				
				for (String unitType : application.sm.getUnitTypes()) {
					unitTypes.put(unitType, new ArrayList<>());
				}
				
				float maxUnitValue = -Float.MAX_VALUE;
				float totalValue = 0f;
				float totalArea = 0f;
				float totalCost = 0f;
				float totalUnitCount = 0f;
				
				for (Stack<AnalysisFloor> analysisStack : analysisStacks.values()) {
					for (AnalysisFloor analysisFloor : analysisStack) {
						for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {
							
							String unitType = analysisUnit.getUnitType();
							
							if (unitType != null) {

								unitTypes.get(analysisUnit.getUnitType()).add(analysisUnit);
								
								totalValue += analysisUnit.getAttribute("unitValue-total");
								totalArea += analysisUnit.getArea();
								totalCost += analysisUnit.getAttribute("unitCost-total");
								
								if (analysisUnit.getAttribute("unitValue-total") > maxUnitValue) {
									maxUnitValue = analysisUnit.getAttribute("unitValue-total");
								}
								
								totalUnitCount ++;
							}
						}
					}
				}
				
				float totalProfit = totalValue - totalCost;
												
				for (Map.Entry<String, List<AnalysisUnit>> unitType : unitTypes.entrySet()) {
					
					AnalysisAttribute analysisAttribute = StackAnalysis.getAnalysisAttribute(unitType.getValue(), "unitValue-total");
					
					String color = application.sm.unitColors.get(unitType.getKey());
					HEXColour c = new HEXColour(color);
					
					int count = application.se.counts.get(unitType.getKey());
					int max =  application.sm.unitCounts.get(unitType.getKey());
					
					float area = 0f;
					
					for (AnalysisUnit analysisUnit : unitType.getValue()) {
						area += analysisUnit.getArea();
					}
					
					float[] col = new float[]{c.red(), c.green(), c.blue()};
					float value = analysisAttribute.getMean();
					float sum = analysisAttribute.getSum();
					
					float sumPerc = sum / totalValue;
					
					String typeLabel = "\t" + unitType.getKey() + " - " + count + " [" + max + "]";		
					String valueLabel = " - ";
					String attributeLabel = analysisAttribute.getLabel();
					
					if (sum != 0) {
						valueLabel = formatToValue(sum, attributeLabel) + " \t [" +
							formatToValue(sum / count, attributeLabel) + "/unit - " +
							formatToValue(sum / area, attributeLabel) + "/m2]";
					}
				
					BarChartItem valueItem = new BarChartItem(typeLabel, valueLabel, col, sumPerc, 100, 10);
															
					valueItem.getBeforeBarLabel().setPrefWidth(150);
								
					valueItems.add(valueItem);										
				}
												
				stackItems.add(new BarChartItem("AREA", (int)totalArea + "m2", new float[]{0,0,0}, 0, 50, 10));
				stackItems.add(new BarChartItem("UNITS", (int)totalUnitCount +"" , new float[]{0,0,0}, 0, 50, 10));	
				
				stackItems.addAll(valueItems);
				
				stackItems.add(new BarChartItem("VALUE", formatToValue(totalValue, "\u00A3") + " \t [" +
						formatToValue(totalValue / totalUnitCount, "\u00A3")  + "/unit - " +
						formatToValue(totalValue / totalArea ,"\u00A3") + "/m2]", new float[]{0,0,0}, 0, 50, 10));
				
				stackItems.add(new BarChartItem("COST", formatToValue(totalCost, "\u00A3") + " \t [" +
						formatToValue(totalCost / totalUnitCount, "\u00A3")  + "/unit - " +
						formatToValue(totalCost / totalArea, "\u00A3") + "/m2]", new float[]{0,0,0}, 0, 50, 10));
				
				stackItems.add(new BarChartItem("PROFIT", formatToValue(totalProfit, "\u00A3") + " \t [" +
						formatToValue(totalProfit / totalUnitCount, "\u00A3")  + "/unit - " +
						formatToValue(totalProfit / totalArea, "\u00A3") + "/m2]", new float[]{0,0,0}, 0, 50, 10));
				
				stackTitledPane.setContent(new GridPaneBarChart<>(stackItems));
				
				/*
				 * Legend
				 */
				
				String attribute = application.attributes[application.attributeIndex.get()];
				
				if (attribute != "unitType") {
					
					distributionTitledPane.setVisible(true);
					distributionTitledPane.setExpanded(true);
					legendTitledPane.setVisible(true);
					legendTitledPane.setExpanded(true);
					
					ObservableList<BarChartItem> legendItems = FXCollections.observableArrayList();
										
					AnalysisAttribute analysisAttribute;
																
					List<AnalysisUnit> analysisUnits = new ArrayList<>();
					
					for (Stack<AnalysisFloor> analysisStack : analysisStacks.values()) {
						for (AnalysisFloor analysisFloor : analysisStack) {
							analysisUnits.addAll(analysisFloor.getAnalysisUnits());
						}
					}
					
					analysisAttribute = StackAnalysis.getAnalysisAttribute(analysisUnits, attribute);
										
					
					Map<Float, List<Float>> bins = analysisAttribute.getBinValues(10);
					
					List<Float> values = new ArrayList<>(bins.keySet());
					
					for (int i =0; i<values.size(); i++) {
						
						Float value = values.get(i);
						List<Float> items = bins.get(value);
						
						HSVColour c = new HSVColour() ;
						c.setHSV((1-(analysisAttribute.getMappedValue(value))) * 0.6f, 1f, 1f) ;
						
						float[] col = new float[]{c.red(), c.green(), c.blue()};
						
						float perc = 0;
						
						String attributeLabel = analysisAttribute.getLabel();
						String valueLabel = formatToValue(value, attributeLabel);
						String countLabel = "\t" + items.size() + " [UNITS]";
						
						if (i != values.size() -1) {
							valueLabel += " - " + formatToValue(values.get(i+1) - 1, attributeLabel);
						}
						
						perc = ((float) items.size() / (float) totalUnitCount ) + 0.01f;

						BarChartItem legendItem = new BarChartItem(countLabel, valueLabel , col, perc, 100, 10);		
						legendItem.getBeforeBarLabel().setPrefWidth(150);
						legendItem.getAfterBarLabel().setPrefWidth(150);
						legendItems.add(legendItem);
					}
										
					legendTitledPane.setText("legend : ["+ attribute + "]");
					legendTitledPane.setContent(new GridPaneBarChart<>(legendItems));
				
				} else {
					
					distributionTitledPane.setVisible(false);
					distributionTitledPane.setExpanded(false);
					legendTitledPane.setVisible(false);
					legendTitledPane.setExpanded(false);
				}
			}
		});
	}
}
