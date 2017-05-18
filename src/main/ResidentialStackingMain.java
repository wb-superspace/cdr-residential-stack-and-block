package main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import cdr.colour.HEXColour;
import cdr.colour.HSVColour;
import cdr.gui.javaFX.JavaFXGUI;
import chart.StackChart;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.legend.LegendItem;
import javafx.legend.VBoxLegend;
import javafx.scene.chart.Chart;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import model.StackAnalysis;

public class ResidentialStackingMain  extends JavaFXGUI<ResidentialStackingRenderer> implements Initializable{

	ResidentialStackingRenderer application;
	
	public BorderPane applicationBorderPane;
	
	public TitledPane statisticsTitledPane;
	public TitledPane legendTitledPane;
	
	public VBox chartVBox;
	
	public MenuItem importGeometryMenuItem;
	public MenuItem importUnitMixMenuItem;
	
	public MenuItem exportUnitDataMenuItem;
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
				
				if (application.importDXF()) {
					
				}
			}
		});
		
		importUnitMixMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				
				if (application.importCSV()) {
					
					initializeTitledPanes();
					initializeCharts();
				}
			}
		});
		
		exportUnitDataMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				
				FileChooser fc = new FileChooser();
				ExtensionFilter csvExtensionFilter = new ExtensionFilter("CSV", "*.csv") ; 
				fc.getExtensionFilters().add(csvExtensionFilter); 
				fc.setInitialDirectory(new File(System.getProperty("user.dir")));
				fc.setSelectedExtensionFilter(csvExtensionFilter);
				File file = fc.showSaveDialog(null);

				application.exportCSV(file);
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

				application.exportPNG(file, chartVBox.getScene().getRoot());
			}
		});
		
		startMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				application.start();		
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
		
		application.sm.getFlag().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue,
					Number newValue) {

				updateStatisticsTitledPane();
				updateLegendTitledPane();
			}
		});
		
		application.renderAttribute.addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				updateLegendTitledPane();
			}
		});
	}
				
	private void updateLegendTitledPane() {
		
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
					
				VBox legendVBox = new VBox();
				ObservableList<LegendItem> legendItems = FXCollections.observableArrayList();
				
				int legendCount = 10;
				String legendType = application.renderAttributes[application.renderAttribute.get()];
				
				if (legendType != "unitType") {
					
					float[] bounds = StackAnalysis.getBounds(application.se.getAnalysisStacks(), legendType);
					
					if (bounds[1] != bounds[0] && bounds[1] != 0) {
						
						for (float i = bounds[0]; i <= bounds[1]; i+= (bounds[1]-bounds[0]) / legendCount) {
							
							float value = (i - bounds[0]) / (bounds[1] - bounds[0]);	
							
							HSVColour c = new HSVColour() ;
							c.setHSV((1-(value)) * 0.6f, 1f, 1f) ;
							
							float[] col = new float[]{c.red(), c.green(), c.blue()};
							
							LegendItem legendItem = new LegendItem(null, Float.toString(i), col);
							
							legendItems.add(legendItem);
						}
					}
					
				} else {
					
					for (Map.Entry<String, Integer> entry : application.se.counts.entrySet()) {
						
						HEXColour colour = new HEXColour(application.sm.unitColors.get(entry.getKey()));
						Integer count = entry.getValue();		
						String label = entry.getKey() + " : " + count + " [" + application.sm.unitCounts.get(entry.getKey()) + "]";
						float[] col = new float[]{colour.red(), colour.green(), colour.blue()};
						
						LegendItem legendItem = new LegendItem(null, label, col);
									
						legendItems.add(legendItem);
					}
				}
					
				legendVBox.getChildren().add(new VBoxLegend<>(legendItems, 150, 3));
				legendTitledPane.setText("Legend [" + legendType + "]" );
				legendTitledPane.setContent(legendVBox);
			}
		});
	}
	
	private void updateStatisticsTitledPane() {
	
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				
				VBox statisticsVBox = new VBox();
				ObservableList<LegendItem> statisticsLegendItems = FXCollections.observableArrayList();

				float[] col = new float[]{0,0,0};
				
				LegendItem valueLegendItem = new LegendItem(null, "VALUE : " + application.se.value.get(), col);
				LegendItem deltaLegendItem = new LegendItem(null, "DELTA : " + application.se.delta.get(), col);
							
				statisticsLegendItems.add(valueLegendItem);
				statisticsLegendItems.add(deltaLegendItem);

				statisticsVBox.getChildren().add(new VBoxLegend<>(statisticsLegendItems, 150, 3));
				statisticsTitledPane.setContent(statisticsVBox);
			}
		});
	}
}
