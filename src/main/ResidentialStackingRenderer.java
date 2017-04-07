package main;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.media.opengl.GL2;

import cdr.fileIO.dxf2.DXFDocument2;
import cdr.geometry.primitives.ArrayVector3D;
import cdr.geometry.primitives.Polygon3D;
import cdr.geometry.renderer.GeometryRenderer;
import cdr.joglFramework.camera.GLCamera;
import cdr.joglFramework.camera.GLCameraAxonometric;
import cdr.joglFramework.event.KeyEvent;
import cdr.joglFramework.event.listener.impl.SimpleKeyListener;
import cdr.joglFramework.frame.GLFramework;
import cdr.joglFramework.renderer.OpaqueRendererWithGUI;
import cdr.mesh.datastructure.Mesh3D;
import cdr.mesh.datastructure.fvMesh.FVMeshFactory;
import cdr.spacepartition.boundingObjects.BoundingBox2D;
import fileio.CsvReader;
import fileio.FileDialogs;
import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import main.FloorFactory.Floor;
import model.EvaluatedBoundary;



public class ResidentialStackingRenderer extends OpaqueRendererWithGUI{

	GeometryRenderer gr = new GeometryRenderer();
	
	/*
	 * Model
	 */
	
	public List<Polygon3D> boundaries = new ArrayList<>();
	public List<Floor> floors = new ArrayList<>();
	
	public FloorFactory floorFactory;
	
	@Override
	protected GLCamera createCamera(GLFramework framework) {
		return new GLCameraAxonometric(framework);
	}

	@Override
	protected void renderGUI(GL2 arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void renderFill(GL2 gl) {
		// TODO Auto-generated method stub
		super.renderFill(gl);
	}

	@Override
	protected void renderLines(GL2 gl) {
		
		gl.glLineWidth(1.0f);
		gl.glColor3f(0f, 0f, 0f);
		
		gr.renderPolygons3DLines(gl, boundaries);
		
		renderStacks(gl);
	}
	
	private void renderStacks(GL2 gl) {
		

	}

	@Override
	public void initialiseRenderer(GLFramework framework) {
		// TODO Auto-generated method stub
		super.initialiseRenderer(framework);
	
		framework.getKeyListeners().add(new SimpleKeyListener(){
			
			public void keyTyped(KeyEvent e) {
			
				if (e.getKeyChar() == 'i') {
			
					Platform.runLater(new Runnable() {
						
						public void run() {
							
							boundaries.clear();			
							floorFactory = new FloorFactory();
							
							System.out.println("import");
							
							File file = FileDialogs.openFileFX("dxf");
							
							if(file != null) {
								
								DXFDocument2 dxf = new DXFDocument2(file);	
								dxf.getPolygons3D("BOUNDARY", boundaries);
												
								for (String layerName : dxf.getLayerNames()) {
									
									if (layerName.contains("FOOTPRINT$")) {
										
										String[] layerString = layerName.split("\\$");
										String type = layerString[1];
										
										List<Polygon3D> polygons = new ArrayList<>();
										
										dxf.getPolygons3D(layerName, polygons);
										
										if (polygons.size() == 1) {
											floorFactory.addFootprint(type, polygons.get(0));
										} else {
											System.out.println("incorrect number of footprints... ignoring");
										}
									}
								}
							}
						}
					});
				}
								
				if (e.getKeyChar() == 'b') {
					
					build();
				}
								
				if (e.getKeyChar() == 'e') {
					
					evaluate();
				}
			}
		});
	}
	
	private void build() {
		
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
								
				File file = FileDialogs.openFileFX("csv");
				
				if (file != null) {
					
					List<String[]> lines = CsvReader.getCSVLines(file);
					lines.remove(0);
					
					for(String [] line : lines) {
						
						String type = line[0];
										
						float cost = Float.parseFloat(line[1]);
						float premium = Float.parseFloat(line[2]);
						
						floorFactory.addCost(type, cost);
						floorFactory.addPremium(type, premium);
					}
				}
			}
		});
	}
	
	private void evaluate() {
		
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {

			}
		});
	}
}
