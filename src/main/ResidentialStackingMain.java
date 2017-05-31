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
import java.util.ResourceBundle;
import java.util.Stack;

import com.sun.corba.se.impl.orbutil.graph.Node;

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
	
	public TitledPane chartTitledPane;
	public TitledPane performanceTitledPane;

	public VBox chartVBox;
	
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

				application.exportImage(file, chartVBox.getScene().getRoot());
			}
		});
		
		startMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				application.start();
				chartTitledPane.setExpanded(true);
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
		
		chartVBox.setStyle("-fx-background-color: white;");
		
		Chart distributionChart = application.sc.getDistributionChart();
	
		distributionChart.setPrefHeight(100);
		distributionChart.prefWidthProperty().bind(chartVBox.widthProperty().multiply(0.95f));
		
		Chart valueChart = application.sc.getValueChart();
		
		valueChart.setPrefHeight(100);
		valueChart.prefWidthProperty().bind(chartVBox.widthProperty().multiply(0.95f));
		
		chartVBox.getChildren().clear();
		chartVBox.getChildren().add(valueChart);
		chartVBox.getChildren().add(distributionChart);
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
	
	private String formatToValue(float value) {
		return "\u00A3" +  NumberFormat.getNumberInstance(Locale.US).format((int)value);
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
								
				Map<Point3D, Stack<AnalysisFloor>> analysisStacks = application.se.getAnalysisStacks();
				
				VBox performanceVBox = new VBox();
				performanceTitledPane.setContent(performanceVBox);
										
				ObservableList<BarChartItem> unitValueItems = FXCollections.observableArrayList();
				ObservableList<BarChartItem> unitSumItems = FXCollections.observableArrayList();
				ObservableList<BarChartItem> unitMixItems = FXCollections.observableArrayList();
				
				Map<String, List<AnalysisUnit>> unitTypes = new HashMap<>();
				
				for (String unitType : application.sm.getUnitTypes()) {
					unitTypes.put(unitType, new ArrayList<>());
				}
				
				float maxUnitValue = -Float.MAX_VALUE;
				float totalUnitCount = 0f;
				float totalFloorCount = 0f;
				
				for (Stack<AnalysisFloor> analysisStack : analysisStacks.values()) {
					for (AnalysisFloor analysisFloor : analysisStack) {
						for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {
							
							String unitType = analysisUnit.getUnitType();
							
							if (unitType != null) {

								unitTypes.get(analysisUnit.getUnitType()).add(analysisUnit);
								
								if (analysisUnit.getAttribute("unitValue") > maxUnitValue) {
									maxUnitValue = analysisUnit.getAttribute("unitValue");
								}
								
								totalUnitCount ++;
							}
						}
						
						totalFloorCount ++;
					}
				}
				
				for (Map.Entry<String, List<AnalysisUnit>> unitType : unitTypes.entrySet()) {
					
					AnalysisAttribute analysisAttribute = StackAnalysis.getAnalysisAttributeUnit(unitType.getValue(), "unitValue");
					
					String color = application.sm.unitColors.get(unitType.getKey());
					HEXColour c = new HEXColour(color);
					
					int count = application.se.counts.get(unitType.getKey());
					int max =  application.sm.unitCounts.get(unitType.getKey());
					
					float[] col = new float[]{c.red(), c.green(), c.blue()};
					float value = analysisAttribute.getMean();
					float sum = analysisAttribute.getSum();
					
					float valuePerc = value / maxUnitValue;
					float sumPerc = sum / application.se.value.get();
					float mixPerc = (float) count / (float) max;
					
					String typeLabel = unitType.getKey();
					String typeCount = "[" + count + " | " + max + "]";
					String valueLabel = formatToValue(value);
					String sumLabel = formatToValue(sum);
					
					BarChartItem sumItem = new BarChartItem(typeLabel, sumLabel, col, sumPerc, 50, 10);
					BarChartItem valueItem = new BarChartItem(typeLabel, valueLabel, col, valuePerc, 50, 10);
					BarChartItem mixItem = new BarChartItem(typeLabel, typeCount, col, 0.2f, 50, 10);
															
					sumItem.getBeforeBarLabel().setPrefWidth(150);
					sumItem.getAfterBarLabel().setPrefWidth(100);
					
					valueItem.getBeforeBarLabel().setPrefWidth(150);
					valueItem.getAfterBarLabel().setPrefWidth(100);
					
					mixItem.getBeforeBarLabel().setPrefWidth(150);
					mixItem.getAfterBarLabel().setPrefWidth(100);
					
					unitValueItems.add(valueItem);
					unitSumItems.add(sumItem);
					unitMixItems.add(mixItem);
				}
				
				
				TitledPane unitMixTitledPane = new TitledPane();
				unitMixTitledPane.setText("unitMix : "  + (int) totalUnitCount );
				unitMixTitledPane.setAnimated(false);
				unitMixTitledPane.setContent(new GridPaneBarChart<>(unitMixItems));

				performanceVBox.getChildren().add(unitMixTitledPane);
				
				
				TitledPane avgTitledPane = new TitledPane();
				avgTitledPane.setText("unitValue : [avg]");
				avgTitledPane.setAnimated(false);
				avgTitledPane.setContent(new GridPaneBarChart<>(unitValueItems));
				
				performanceVBox.getChildren().add(avgTitledPane);

				TitledPane totalTitledPane = new TitledPane();
				totalTitledPane.setText("totalValue : " +  formatToValue(application.se.value.get()));
				totalTitledPane.setAnimated(false);
				totalTitledPane.setContent(new GridPaneBarChart<>(unitSumItems));
				
				performanceVBox.getChildren().add(totalTitledPane);
				
				String attribute = application.attributes[application.attributeIndex.get()];
				
				if (attribute != "unitType") {
																							
					ObservableList<BarChartItem> legendItems = FXCollections.observableArrayList();
										
					AnalysisAttribute analysisAttribute;
					
					String unit = null;
					
					if (attribute.contains("floor")) {
						
						List<AnalysisFloor> analysisFloors = new ArrayList<>();
						
						for (Stack<AnalysisFloor> analysisStack : analysisStacks.values()) {
							analysisFloors.addAll(analysisStack);
						}
						
						analysisAttribute = StackAnalysis.getAnalysisAttributeFloor(analysisFloors, attribute);
						
						unit = "floors";
						
					} else { 
						
						List<AnalysisUnit> analysisUnits = new ArrayList<>();
						
						for (Stack<AnalysisFloor> analysisStack : analysisStacks.values()) {
							for (AnalysisFloor analysisFloor : analysisStack) {
								analysisUnits.addAll(analysisFloor.getAnalysisUnits());
							}
						}
						
						analysisAttribute = StackAnalysis.getAnalysisAttributeUnit(analysisUnits, attribute);
						
						unit = "units";
					}
					
					Map<Float, List<Float>> bins = analysisAttribute.getBinValues(10);
					
					List<Float> values = new ArrayList<>(bins.keySet());
					
					for (int i =0; i<values.size(); i++) {
						
						Float value = values.get(i);
						List<Float> items = bins.get(value);
						
						HSVColour c = new HSVColour() ;
						c.setHSV((1-(analysisAttribute.getMappedValue(value))) * 0.6f, 1f, 1f) ;
						
						float[] col = new float[]{c.red(), c.green(), c.blue()};
						
						float perc = 0;
						
						String valueLabel = formatToValue(value);
						String countLabel = items.size() + " " + unit;
						
						if (i != values.size() -1) {
							valueLabel += " - " + formatToValue(values.get(i+1) - 1);
						}
						
						if (unit == "floors") {
							perc = (float) items.size() / (float) totalFloorCount;
						} else {
							perc = (float) items.size() / (float) totalUnitCount;
						}
						
						perc += 0.01;
						
						BarChartItem legendItem = new BarChartItem(valueLabel, countLabel, col, perc, 50, 10);		
						legendItem.getBeforeBarLabel().setPrefWidth(150);
						legendItem.getAfterBarLabel().setPrefWidth(100);
						legendItems.add(legendItem);
					}
					
					TitledPane legendTitledPane = new TitledPane();
					legendTitledPane.setText(attribute + " : ");
					legendTitledPane.setAnimated(false);
					legendTitledPane.setContent(new GridPaneBarChart<>(legendItems));
					
					performanceVBox.getChildren().add(legendTitledPane);
				}
			}
		});
	}
}
