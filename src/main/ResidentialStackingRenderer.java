package main;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.media.opengl.GL2;

import com.jogamp.opengl.util.gl2.GLUT;

import cdr.colour.HEXColour;
import cdr.colour.HSVColour;
import cdr.fileIO.dxf2.DXFDocument2;
import cdr.fileIO.png.PNGExporter;
import cdr.geometry.primitives.ArrayPoint3D;
import cdr.geometry.primitives.ArrayVector3D;
import cdr.geometry.primitives.LineSegment3D;
import cdr.geometry.primitives.Point3D;
import cdr.geometry.primitives.Polygon3D;
import cdr.geometry.primitives.Polyline3D;
import cdr.geometry.primitives.Text3D;
import cdr.geometry.renderer.GeometryRenderer;
import cdr.joglFramework.camera.GLCamera;
import cdr.joglFramework.camera.GLCameraAxonometric;
import cdr.joglFramework.event.KeyEvent;
import cdr.joglFramework.event.listener.impl.SimpleKeyListener;
import cdr.joglFramework.frame.GLFramework;
import cdr.joglFramework.renderer.OpaqueRendererWithGUI;
import cdr.joglFramework.snapshot.CombinedSnapshot;
import cdr.mesh.datastructure.Face;
import cdr.mesh.datastructure.Mesh3D;
import cdr.mesh.datastructure.fvMesh.FVMeshFactory;
import cdr.mesh.renderer.MeshRenderer3D;
import cdr.mesh.renderer.MeshRenderer3DFlatShaded;
import cdr.mesh.renderer.MeshRenderer3DOutline;
import cdr.mesh.renderer.MeshRenderer3DSmoothShaded;
import cdr.mesh.toolkit.operators.MeshOperators;
import chart.StackChart;
import fileio.CsvReader;
import fileio.FileDialogs;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.util.Callback;
import model.StackAnalysis;
import model.StackAnalysis.AnalysisAttribute;
import model.StackAnalysis.AnalysisFloor;
import model.StackAnalysis.AnalysisType;
import model.StackAnalysis.AnalysisFloor.AnalysisUnit;
import model.StackEvaluator;
import model.StackManager;


public class ResidentialStackingRenderer extends OpaqueRendererWithGUI{

	GeometryRenderer gr = new GeometryRenderer();
	MeshRenderer3DOutline mr = new MeshRenderer3DOutline();
	GLUT glut = new GLUT();
	GLFramework f;
	
	StackManager sm;
	StackEvaluator se;
	StackChart sc;	
	
	/*
	 * =========================================================
	 * RENDER
	 * =========================================================
	 */
	
	String[] renderAttributes = new String[] {
			"unitType", 
			"unitVisibilityPremium",
			"unitFloorPremium",
			"unitValue",
			"floorCost",
			"floorValue",
			"floorDelta"};
		
	SimpleIntegerProperty renderAttribute = new SimpleIntegerProperty(0);
	
	boolean renderExploded = false;
		
	/*
	 * =========================================================
	 * METHODS
	 * =========================================================
	 */
	
	@Override
	protected GLCamera createCamera(GLFramework framework) {
		return new GLCameraAxonometric(framework);
	}
	
	@Override
	public void initialiseRenderer(GLFramework framework) {
		super.initialiseRenderer(framework);
		
		f = framework;
		
		framework.getKeyListeners().add(new SimpleKeyListener(){
			
			public void keyTyped(KeyEvent e) {
				
				if(e.getKeyChar() == 'e') {
					
					renderExploded = !renderExploded;
				}	
				
				if(e.getKeyChar() == 'd') {
					
					if (renderAttribute.get() == renderAttributes.length-1)	renderAttribute.set(0);
					else renderAttribute.set(renderAttribute.get()+1);;		
					
					System.out.println(renderAttributes[renderAttribute.get()]);
				}
				
				if(e.getKeyChar() == 'a') {
					
					if (renderAttribute.get() == 0) renderAttribute.set(renderAttributes.length-1);
					else renderAttribute.set(renderAttribute.get()-1);;		
					
					System.out.println(renderAttributes[renderAttribute.get()]);
				}
			}
		});
	}

	@Override
	protected void renderGUI(GL2 gl, int width, int height) {
		
	}
	
	@Override
	protected void renderFill(GL2 gl) {
								
		if (sm != null && se != null) {

			Map<Point3D, Stack<AnalysisFloor>> analysisStacks = se.getAnalysisStacks();
			
			String attribute = renderAttributes[renderAttribute.get()];
			
			AnalysisAttribute analysisAttribute = StackAnalysis.getAnalysisAttribute(analysisStacks, attribute, AnalysisType.UNIT);
			
			for (Point3D footprint : analysisStacks.keySet()) {
				
				for (AnalysisFloor analysisFloor : analysisStacks.get(footprint)) {
					
					for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {

						if (analysisUnit.getUnitType() == null) {
							continue;
						}
						
						String color = sm.unitColors.get(analysisUnit.getUnitType());
						HEXColour colour = new HEXColour(color);
						gl.glColor3f(colour.red(), colour.green(), colour.blue());
						
						HSVColour c = new HSVColour() ;
						
						if (attribute != "unitType") {
							
							float value = analysisAttribute.getMappedValue(analysisUnit.getAttribute(attribute));						
							c.setHSV((1-(value)) * 0.6f, 1f, 1f) ;		
							gl.glColor3f(c.red(), c.green(), c.blue());
							
						}
															
						if (renderExploded) {
														
							gr.renderPolygon3DFill(gl, analysisUnit.getAnalysisGeometry(renderExploded));
							
							Point3D tagPoint = analysisUnit.getTagPoint(renderExploded);
							
							gl.glPushMatrix();					
							gl.glTranslatef(tagPoint.x(), tagPoint.y(), tagPoint.z());
							gl.glScalef(0.01f,0.01f,0.01f);
							gl.glColor3f(0,0,0); 
							gl.glLineWidth(0.2f);
							
							if (attribute == "unitType") {
								
								glut.glutStrokeString(0, analysisUnit.getUnitType());
								
							} else {
								
								glut.glutStrokeString(0, Float.toString(analysisUnit.getAttribute(attribute)));
							}
							
							gl.glPopMatrix();

						} else {
							
							Mesh3D m = analysisUnit.getAnalysisMesh(renderExploded);

							mr.renderFill(gl, m);
							
							gl.glLineWidth(0.01f);
							gl.glColor3f(0, 0, 0);
														
							mr.renderCornerEdges(gl, m, 0.1f);
						}
					}					
				}
			}
			
			for (Mesh3D m : sm.getContext()) {
				
				gl.glColor3f(0.7f, 0.7f, 0.7f);
				mr.renderFill(gl, m);
				
				gl.glLineWidth(0.1f);
				gl.glColor3f(0, 0, 0);			
				mr.renderCornerEdges(gl, m, 1f);
			}
		}
	}

	@Override
	protected void renderLines(GL2 gl) {
		
		if (sm != null) {
			gr.renderPolygons3DLines(gl, sm.getBoundaries());
		}
		
		if (sm != null && se != null) {
			
			gl.glPointSize(5f);
			
			Map<Point3D, Stack<AnalysisFloor>> analysisStacks = se.getAnalysisStacks();
								
			for (Point3D footprint : analysisStacks.keySet()) {
				
				Polyline3D axis = new Polyline3D(false);
				
				for (AnalysisFloor analysisFloor : analysisStacks.get(footprint)) {
					
					if (analysisFloor.getFloorplate() != null) {
						
						Point3D anchor = analysisFloor.getAnchor(renderExploded);
						
						axis.appendVertex(anchor);
													
						Point3D tagLocation = new ArrayPoint3D();
						float tagDistance = (float) Math.sqrt(analysisFloor.getFootprintArea());
						
						anchor.addVector(new ArrayVector3D(tagDistance, tagDistance, 0), tagLocation);
						
						gl.glPushMatrix();					
						gl.glTranslatef(tagLocation.x(), tagLocation.y(), tagLocation.z());
						gl.glScalef(0.03f,0.03f,0.03f);
						gl.glColor3f(0,0,0); 
						gl.glLineWidth(0.5f);
						
						String tag = analysisFloor.getFootprintType() + " : " + 
								"floor " + analysisFloor.getFloorIndex();
						
						glut.glutStrokeString(0, tag);
						
						gl.glPopMatrix();
						
						gr.renderLineSegment3D(gl, new LineSegment3D(anchor, tagLocation));
												
						for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {
							
							gl.glLineWidth(0.6f);
							gl.glColor3f(0f, 0f, 0f);
							gr.renderPolygon3DLines(gl, analysisUnit.getAnalysisGeometry(renderExploded));	
						}
					}
				}
				
				if (renderExploded) {
					
					gl.glLineWidth(0.5f);			
					gr.renderPolyline3DLines(gl, axis);
				}
			}
			
			gl.glLineWidth(1.0f);
			gl.glColor3f(0f, 0f, 0f);
			
			for (Point3D viewPoint : sm.getViewPoints()) {			
				gr.renderPoint3D(gl, viewPoint);
			}
		}
	}
	
	private void reset() {	
		
		se.reset(sm);
		sc.reset();
	}
	
	public void stop() {
		
		se.stop();
	}
	
	public void start() {	
		
		this.reset();
		
		new Thread(new Runnable() {
		    public void run() {
				if (se != null && sm != null) {					
					se.start();
				}
		    }
		}).start();
	}
	
	public void resume() {
		// TODO;
	}
				
	public boolean importDXF() {
					
		sm = new StackManager();
		se = new StackEvaluator();
		sc = new StackChart(sm, se);

		File file = FileDialogs.openFileFX("dxf");
		
		if(file != null) {
			
			DXFDocument2 dxf = new DXFDocument2(file);	
			dxf.getPolygons3D("BOUNDARY", sm.getBoundaries());
			dxf.getPoints3D("VIEW", sm.getViewPoints());
			
			List<Mesh3D> context = new ArrayList<>();
			dxf.getMeshes3D("CONTEXT", new FVMeshFactory(), context);
			for (Mesh3D m : context) {
				new MeshOperators().triangulateMesh(m);
			}
			
			sm.setContext(context);
							
			for (String layerName : dxf.getLayerNames()) {
				
				if (layerName.contains("FOOTPRINT$")) {
					
					String[] layerString = layerName.split("\\$");
					String footprintType = layerString[1];
					
					List<Point3D> locations = new ArrayList<>();
					
					dxf.getPoints3D(layerName, locations);							
					sm.addFootprint(footprintType, locations);
				}
			}
			
			for (String layerName : dxf.getLayerNames()) {
										
				if (layerName.contains("FLOORPLATE$")) {
					
					String[] layerString = layerName.split("\\$");
					String footprintType = layerString[1];
					String floorplateType = layerString[2];
					
					List<Point3D> floorplate = new ArrayList<>();
					List<Polygon3D> units = new ArrayList<>();
					List<Text3D> unitTypes = new ArrayList<>();
					List<LineSegment3D> visibilityLocations = new ArrayList<>();
					
					dxf.getPolygons3D(layerName, units);
					dxf.getPoints3D(layerName, floorplate);
					dxf.getText3D(layerName, unitTypes);
					dxf.getLineSegments3D(layerName, visibilityLocations);
										
					if (floorplate.size() != 1) {
						System.out.println("invalid anchor - skipping");
						continue;
					}
					
					for (Polygon3D unit : units) {							
						if (unit.getPlane3D().c() < 0) {
							unit.reverseWinding();
						}
					}
												
					sm.addFloorplate(footprintType, floorplateType, floorplate.get(0), unitTypes, units, visibilityLocations);
					
					System.out.println(footprintType + " -> " + floorplateType);
				}
			}
			
			return true;
		}	
		
		return false;
	}


	public void exportPNG(File file, Node root) {

		(new CombinedSnapshot()).createSnapshot(root, f, 2, new Callback<BufferedImage, Void>() {

			@Override
			public Void call(BufferedImage param) {
				try { (new PNGExporter()).exportPNG(param, file) ; }
				catch (IOException e) { e.printStackTrace() ; }
				return null ;
			}
		}) ;
	}
	
	public void exportCSV(File file) {
		
		if (se == null) return;
		
		System.out.println("writing to output...");
		
		String output = "FLOOR INDEX, FOOTPRINT TYPE, FLOOR TYPE, FLOOR VALUE, FLOOR COST, UNIT TYPE, UNIT VALUE, UNIT VISIBILITY \n";
		
		Map<Point3D, Stack<AnalysisFloor>> analysisStacks = se.getAnalysisStacks();
		
		for (Map.Entry<Point3D, Stack<AnalysisFloor>> analysisEntry : analysisStacks.entrySet()) {
			for (AnalysisFloor analysisFloor : analysisEntry.getValue()) {
				for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {
					
					if (analysisUnit.getUnitType() == null) {
						continue;
					}
					
					String unitString = "";
					
					unitString += analysisUnit.getFloorIndex() + ",";
					unitString += analysisUnit.getFootprintType() + ",";
					unitString += analysisUnit.getFloorplateType() + ",";
					unitString += analysisUnit.getAttribute("floorValue") + ",";
					unitString += analysisUnit.getAttribute("floorCost") + ",";
					unitString += analysisUnit.getUnitType() + ",";
					unitString += analysisUnit.getAttribute("unitValue") + ",";
					unitString += analysisUnit.getAttribute("unitVisibility") + "\n";
					
					output += unitString;
				}
			}
		}
		
		try {
			FileWriter writer = new FileWriter(file);
			writer.write(output);  
			writer.close();      
		} catch (IOException e) {  
			e.printStackTrace();  
		}
			
		System.out.println("...done");
	}
	
	public boolean importCSV() {
				
		File file = FileDialogs.openFileFX("csv");
		
		if (file != null) {
			
			List<String[]> lines = CsvReader.getCSVLines(file);
			lines.remove(0);
			
			for(String [] line : lines) {
				
				String unitType = line[0].trim();
				Integer unitCount = Integer.parseInt(line[1].trim());
				Float unitValue = Float.parseFloat(line[2].trim());
				Float unitValueCap = Float.parseFloat(line[3].trim());
				String unitColor = line[4].trim();

				sm.addUnit(unitType, unitCount, unitValue, unitValueCap, unitColor);
			}
			
			return true;
		}
		
		return false;
	}
}
