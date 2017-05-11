package main;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.io.File;
import java.util.ArrayList;
import javax.media.opengl.GL2;

import com.jogamp.opengl.util.gl2.GLUT;

import cdr.colour.HEXColour;
import cdr.colour.HSVColour;
import cdr.fileIO.dxf2.DXFDocument2;
import cdr.geometry.primitives.LineSegment3D;
import cdr.geometry.primitives.Point3D;
import cdr.geometry.primitives.Polygon3D;
import cdr.geometry.primitives.Text3D;
import cdr.geometry.renderer.GeometryRenderer;
import cdr.joglFramework.camera.GLCamera;
import cdr.joglFramework.camera.GLCameraAxonometric;
import cdr.joglFramework.event.KeyEvent;
import cdr.joglFramework.event.listener.impl.SimpleKeyListener;
import cdr.joglFramework.frame.GLFramework;
import cdr.joglFramework.renderer.OpaqueRendererWithGUI;
import cdr.mesh.datastructure.Face;
import cdr.mesh.datastructure.Mesh3D;
import cdr.mesh.datastructure.fvMesh.FVMeshFactory;
import cdr.mesh.toolkit.operators.MeshOperators;
import chart.StackChart;
import fileio.CsvReader;
import fileio.FileDialogs;
import model.StackAnalysis.AnalysisFloor;
import model.StackAnalysis.AnalysisUnit;
import model.StackEvaluator;
import model.StackManager;


public class ResidentialStackingRenderer extends OpaqueRendererWithGUI{

	GeometryRenderer gr = new GeometryRenderer();
	StackManager sm;
	StackEvaluator se;
	StackChart sc;
	
	GLUT glut = new GLUT();
	
	/*
	 * =========================================================
	 * RENDER
	 * =========================================================
	 */
	
	String[] renderTypes = new String[] {"type", "visibility", "value"};
	
	int renderType = 0;
	
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
		
		framework.getKeyListeners().add(new SimpleKeyListener(){
			
			public void keyTyped(KeyEvent e) {
				
				if(e.getKeyChar() == 'e') {
					
					renderExploded = !renderExploded;
				}	
				
				if(e.getKeyChar() == 'd') {
					
					if (renderType == renderTypes.length-1)	renderType = 0;
					else renderType ++;					
				}
				
				if(e.getKeyChar() == 'a') {
					
					if (renderType == 0) renderType = renderTypes.length-1;
					else renderType --;					
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
			
			String attribute = renderTypes[renderType];
			
			float[] bounds = se.getBounds(analysisStacks, attribute);

			for (Point3D footprint : analysisStacks.keySet()) {
				
				for (AnalysisFloor analysisFloor : analysisStacks.get(footprint)) {
					
					for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {

						if (analysisUnit.getType() == null) {
							continue;
						}
						
						List<Polygon3D> unitFaces = new ArrayList<>();
						
						Mesh3D m = analysisUnit.getAnalysisMesh(renderExploded);
						
						for(Face face : m.iterableFaces()) {	
							unitFaces.add(m.getPolygon(face)); 					
						}
						
						String color = sm.unitColors.get(analysisUnit.getType());
						HEXColour colour = new HEXColour(color);
						gl.glColor3f(colour.red(), colour.green(), colour.blue());
						
						HSVColour c = new HSVColour() ;
						
						switch (attribute) {
						case "value":
							
							float value = (analysisUnit.value - bounds[0]) / (bounds[1] - bounds[0]);							
							c.setHSV((1-(value)) * 0.6f, 1f, 1f) ;		
							gl.glColor3f(c.red(), c.green(), c.blue());
							
							break;
							
						case "visibility":
							
							float visibility = (analysisUnit.visibility - bounds[0]) / (bounds[1] - bounds[0]);					
							c.setHSV((1-(visibility)) * 0.6f, 1f, 1f) ;		
							gl.glColor3f(c.red(), c.green(), c.blue());
							
							break;
							
						default:							
							break;
						}
						
						gr.renderPolygons3DFill(gl, unitFaces);
						
						gl.glLineWidth(0.01f);
						gl.glColor3f(0, 0, 0);
						
						gr.renderPolygons3DLines(gl, unitFaces);
					}					
				}
			}
			
			for (Mesh3D contextMesh : sm.getContext()) {
				
				List<Polygon3D> contextFaces = new ArrayList<>();
				
				for(Face face : contextMesh.iterableFaces()) {
					
					contextFaces.add(contextMesh.getPolygon(face)); 					
				}

				gl.glColor3f(0.7f, 0.7f, 0.7f);
				gr.renderPolygons3DFill(gl, contextFaces);
				
				gl.glLineWidth(0.1f);
				gl.glColor3f(0, 0, 0);
				
				gr.renderPolygons3DLines(gl, contextFaces);
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
								
				for (AnalysisFloor analysisFloor : analysisStacks.get(footprint)) {
					
					for (AnalysisUnit analysisUnit : analysisFloor.getAnalysisUnits()) {
						
						gl.glLineWidth(0.6f);
						gl.glColor3f(0f, 0f, 0f);
						
						gr.renderPolygon3DLines(gl, analysisUnit.getAnalysisGeometry(renderExploded));	
					}
				}
			}
			
			gl.glLineWidth(1.0f);
			gl.glColor3f(0f, 0f, 0f);
			
			for (Point3D viewPoint : sm.getViewPoints()) {
				gr.renderPoint3D(gl, viewPoint);
			}
		}
	}
	
	public void reset() {	
		
		sc.reset();
		se.reset(sm);
	}
	
	public void stop() {
		
		se.stop();
	}
	
	public void start() {	
		
		new Thread(new Runnable() {
		    public void run() {
				if (se != null && sm != null) {					
					se.start();
				}
		    }
		}).start();
	}
				
	public void importDXF() {
					
		sm = new StackManager();
		se = new StackEvaluator();
		sc = new StackChart();

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
				}
			}
		}	
		
		this.reset();
	}

	
	public void importCSV() {
							
		File file = FileDialogs.openFileFX("csv");
		
		if (file != null) {
			
			List<String[]> lines = CsvReader.getCSVLines(file);
			lines.remove(0);
			
			for(String [] line : lines) {
				
				String unitType = line[0].trim();
				Integer unitCount = Integer.parseInt(line[1].trim());
				Float unitArea = Float.parseFloat(line[2].trim());
				Float unitValue = Float.parseFloat(line[3].trim());
				Float unitValueCap = Float.parseFloat(line[4].trim());
				String unitColor = line[5].trim();

				sm.addUnit(unitType, unitCount, unitArea, unitValue, unitValueCap, unitColor);
			}
		}
		
		this.reset();
	}
}
