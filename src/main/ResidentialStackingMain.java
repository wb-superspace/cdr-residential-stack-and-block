package main;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

import cdr.colour.HEXColour;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class ResidentialStackingMain  extends JavaFXGUI<ResidentialStackingRenderer> implements Initializable{

	ResidentialStackingRenderer application;
	
	public TitledPane unitMixTitledPane;
	public TitledPane statisticsTitledPane;
	
	public AnchorPane chartAnchorPane;
	
	public MenuItem importGeometryMenuItem;
	public MenuItem importUnitMixMenuItem;
	public MenuItem evaluateModelMenuItem;
	
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
				application.importDXF();
			}
		});
		
		importUnitMixMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				application.importCSV();	
				application.initialize();
				
				initializeTitledPanes();
				initializeChart();
			}
		});
		
		evaluateModelMenuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				application.evaluate();		
			}
		});
	}
	
	private void initializeChart() {
		
		Chart chart = application.sc.get();
	
		chart.setPrefHeight(100);
		chart.prefWidthProperty().bind(chartAnchorPane.widthProperty().multiply(0.9f));
		
		chartAnchorPane.getChildren().add(chart);
		
		application.sm.getFlag().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
					Boolean newValue) {
				
				updateChart();
			}
		});
	}
	
	private void initializeTitledPanes() {
		
		application.sm.getFlag().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
					Boolean newValue) {

				updateUnitMixTitledPane();
				updateStatisticsTitledPane();
			}
		});
	}
		
	private void updateChart() {
		
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				application.sc.addValue(application.se.value);
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
				
				String label = "VALUE : " + application.se.value;
				
				LegendItem legendItem = new LegendItem(null, label, col);
							
				statisticsLegendItems.add(legendItem);

				statisticsVBox.getChildren().add(new VBoxLegend<>(statisticsLegendItems, 150, 3));
				statisticsTitledPane.setContent(statisticsVBox);
			}
		});
	}
	
	private void updateUnitMixTitledPane() {
		
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				
				VBox unitMixVBox = new VBox();
				ObservableList<LegendItem> unitMixLegendItems = FXCollections.observableArrayList();
						
				for (Map.Entry<String, Integer> entry : application.se.counts.entrySet()) {
					
					HEXColour colour = new HEXColour(application.sm.unitColors.get(entry.getKey()));
					Integer count = entry.getValue();		
					String label = entry.getKey() + " : " + count + " [" + application.sm.unitCounts.get(entry.getKey()) + "]";
					float[] col = new float[]{colour.red(), colour.green(), colour.blue()};
					
					LegendItem legendItem = new LegendItem(null, label, col);
								
					unitMixLegendItems.add(legendItem);
				}

				unitMixVBox.getChildren().add(new VBoxLegend<>(unitMixLegendItems, 150, 3));
				unitMixTitledPane.setContent(unitMixVBox);
			}
		});
	}
}
