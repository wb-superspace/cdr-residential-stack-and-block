package main;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.awt.datatransfer.FlavorTable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.media.opengl.GL2;

import com.jogamp.graph.geom.opengl.SVertex;import cdr.colour.Colour;
import cdr.colour.HEXColour;
import cdr.colour.HSVColour;
import cdr.fileIO.dxf2.DXFDocument2;
import cdr.geometry.primitives.ArrayVector3D;
import cdr.geometry.primitives.Polygon3D;
import cdr.geometry.primitives.Rectangle2D;
import cdr.geometry.renderer.GeometryRenderer;
import cdr.joglFramework.camera.GLCamera;
import cdr.joglFramework.camera.GLCameraAxonometric;
import cdr.joglFramework.event.KeyEvent;
import cdr.joglFramework.event.listener.impl.SimpleKeyListener;
import cdr.joglFramework.frame.GLFramework;
import cdr.joglFramework.renderer.OpaqueRendererWithGUI;
import cdr.mesh.renderer.MeshRenderer3DFlatShaded;
import cdr.mesh.renderer.MeshRenderer3DOutline;
import fileio.CsvReader;
import fileio.FileDialogs;
import geometry.PolygonApproximationRectangular;
import javafx.application.Platform;

public class ResidentialStackingRenderer extends OpaqueRendererWithGUI{

	GeometryRenderer gr = new GeometryRenderer();
	StackViewer sv;
	StackManager sm;
	StackEvaluator se;
	BlockManager bm;
		
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
		
		MeshRenderer3DFlatShaded mr = new MeshRenderer3DFlatShaded();
						
		if (sv != null) {

			for (String type : sm.getFootprintTypes()) {
				for (Polygon3D footprint : sm.getFootprints(type)) {
					
					for (Map.Entry<String, List<Polygon3D>> unitFaces : sv.getViewableStackUnits(
							footprint, 
							sm.getStack(footprint), 
							se.floorToCeilingHeight).entrySet()) {
						

						HEXColour colour = new HEXColour(unitFaces.getKey());
						gl.glColor3f(colour.red(), colour.green(), colour.blue());
						
						gr.renderPolygons3DFill(gl, unitFaces.getValue());
						
						gl.glLineWidth(0.1f);
						gl.glColor3f(0f, 0f, 0f);
						
						gr.renderPolygons3DLines(gl, unitFaces.getValue());
					}
				}
			}
		}
	}

	@Override
	protected void renderLines(GL2 gl) {
		
		MeshRenderer3DOutline mr = new MeshRenderer3DOutline();
		
		gl.glLineWidth(1.0f);
		gl.glColor3f(0f, 0f, 0f);
		
		if (sv != null) {
						
			gr.renderPolygons3DLines(gl, sm.getBoundaries());
			
			for (String type : sm.getFootprintTypes()) {
				for (Polygon3D footprint : sm.getFootprints(type)) {
									
//					gr.renderPolygons3DLines(gl, 
//							sv.getViewableStack(
//									footprint, 
//									sm.getStack(footprint), 
//									se.floorToCeilingHeight));
										
//					gr.renderPolygons3DLines(gl, 
//							sv.getViewableStackUnits(
//									footprint, 
//									sm.getStack(footprint), 
//									se.floorToCeilingHeight));
					
//					mr.renderEdges(gl, sv.getViewableStackUnits(
//							footprint, 
//							sm.getStack(footprint), 
//							se.floorToCeilingHeight));
			
				}
			}
		}
	}
	
	@Override
	public void initialiseRenderer(GLFramework framework) {

		super.initialiseRenderer(framework);
	
		framework.getKeyListeners().add(new SimpleKeyListener(){
			
			public void keyTyped(KeyEvent e) {
			
				if (e.getKeyChar() == 'i') {
			
					Platform.runLater(new Runnable() {
						
						public void run() {

							sm = new StackManager();
							se = new StackEvaluator();
							sv = new StackViewer();
														
							importDXF();
						}
					});
				}
				
				if (e.getKeyChar() == 'd') {
					
					Platform.runLater(new Runnable() {
						
						@Override
						public void run() {
							
							bm = new BlockManager();
							
							importCSV();
						}
					});
				}
																
				if (e.getKeyChar() == 'e') {
					
					Platform.runLater(new Runnable() {
						
						@Override
						public void run() {
							
							se.evaluate(sm, bm);
						}
					});
				}
			}
		});
	}
			
	private void importDXF() {
		
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {

				File file = FileDialogs.openFileFX("dxf");
				
				if(file != null) {
					
					DXFDocument2 dxf = new DXFDocument2(file);	
					dxf.getPolygons3D("BOUNDARY", sm.getBoundaries());
									
					for (String layerName : dxf.getLayerNames()) {
						
						if (layerName.contains("FOOTPRINT$")) {
							
							String[] layerString = layerName.split("\\$");
							String footprintType = layerString[1];
							
							List<Polygon3D> polygons = new ArrayList<>();
												
							dxf.getPolygons3D(layerName, polygons);
							sm.addFootPrint(footprintType, polygons);
						}
					}
					
					sv.setStackManager(sm);
				}	
			}
		});
	}
	
	private void importCSV() {
		
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
								
				File file = FileDialogs.openFileFX("csv");
				
				if (file != null) {
					
					List<String[]> lines = CsvReader.getCSVLines(file);
					String[] keys = lines.get(0);
					lines.remove(0);
					
					for(String [] line : lines) {
						
						String unitType = line[0];
						Integer unitCount = Integer.parseInt(line[1]);
						Float unitArea = Float.parseFloat(line[2]);
						Float unitValue = Float.parseFloat(line[3]);
						String unitColor = line[4];

						bm.addUnit(unitType, unitCount, unitArea, unitValue, unitColor);
					}
					
					sv.setBlockManager(bm);
				}
			}
		});
	}
}
