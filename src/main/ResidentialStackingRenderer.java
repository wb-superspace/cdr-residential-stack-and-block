package main;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.awt.datatransfer.FlavorTable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import javax.media.opengl.GL2;

import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.opengl.util.gl2.GLUT;
import com.sun.swing.internal.plaf.metal.resources.metal;

import cdr.colour.Colour;
import cdr.colour.HEXColour;
import cdr.colour.HSVColour;
import cdr.fileIO.dxf2.DXFDocument2;
import cdr.geometry.primitives.ArrayVector3D;
import cdr.geometry.primitives.Point3D;
import cdr.geometry.primitives.Polygon3D;
import cdr.geometry.primitives.Rectangle2D;
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
import cdr.mesh.renderer.MeshRenderer3DFlatShaded;
import cdr.mesh.renderer.MeshRenderer3DOutline;
import cdr.spacepartition.boundingObjects.BoundingBox3D;
import chart.StackChart;
import fileio.CsvReader;
import fileio.FileDialogs;
import geometry.PolygonApproximationRectangular;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import model.StackEvaluator;
import model.StackManager;
import model.StackViewer;

public class ResidentialStackingRenderer extends OpaqueRendererWithGUI{

	GeometryRenderer gr = new GeometryRenderer();
	StackManager sm;
	StackEvaluator se;
	StackChart sc;
	
	GLUT glut = new GLUT();
	
	boolean renderExploded = false;
		
	@Override
	protected GLCamera createCamera(GLFramework framework) {
		return new GLCameraAxonometric(framework);
	}
	
	@Override
	public void initialiseRenderer(GLFramework framework) {
		super.initialiseRenderer(framework);
		
		framework.getKeyListeners().add(new SimpleKeyListener(){
			
			public void keyTyped(KeyEvent e) {
				
				if(e.getKeyChar() == 't') {
					
					renderExploded = !renderExploded;
				}
			}
		});
	}

	@Override
	protected void renderGUI(GL2 gl, int width, int height) {
		
	}
	
	@Override
	protected void renderFill(GL2 gl) {
								
		if (sm != null) {

			for (Point3D footprint : sm.getFootprints()) {
								
				for (Map.Entry<String, List<Mesh3D>> units : StackViewer.getViewableStackMeshes(
						sm,
						se,
						footprint,   
						renderExploded).entrySet()) {

					String color = sm.unitColors.get(units.getKey());
					
					for (Mesh3D unit : units.getValue()) {
						
						List<Polygon3D> unitFaces = new ArrayList<>();
						
						for(Face face : unit.iterableFaces()) {
							
							unitFaces.add(unit.getPolygon(face)); 					
						}

						HEXColour colour = new HEXColour(color);
						gl.glColor3f(colour.red(), colour.green(), colour.blue());
						
						gr.renderPolygons3DFill(gl, unitFaces);
						
						gl.glLineWidth(0.1f);
						gl.glColor3f(0, 0, 0);
						
						gr.renderPolygons3DLines(gl, unitFaces);
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
		
		if (sm != null) {
			gr.renderPolygons3DLines(gl, sm.getBoundaries());
		}
		
		if (sm != null) {
							
			for (Point3D footprint : sm.getFootprints()) {
								
				gr.renderPolygons3DLines(gl, 
						StackViewer.getViewableStackPolygons(
								sm,
								se,
								footprint, 
								renderExploded));
								
				for (Map.Entry<Point3D, Float> valueEntry : StackViewer.getViewableStackValues(sm, se, footprint, renderExploded).entrySet()) {
										 
					gl.glPushMatrix();
					gl.glTranslatef(valueEntry.getKey().x()+1,valueEntry.getKey().y()+1, valueEntry.getKey().z());
					gl.glScalef(0.03f,0.03f,0.03f);
					
					gl.glColor3f(0,0,0); gl.glLineWidth(0.8f);
					
					glut.glutStrokeString(0, valueEntry.getValue().toString());		
					gl.glPopMatrix();
				}
			}
		}
	}
	
	public void initialize() {	
		
		se.initialize(sm);
	}
	
	public void evaluate() {
		
		sc.clearValues();
			
		new Thread(new Runnable() {
		    public void run() {
				if (se != null && sm != null) {					
					se.evaluate(sm);
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
					
					dxf.getPolygons3D(layerName, units);
					dxf.getPoints3D(layerName, floorplate);
					dxf.getText3D(layerName, unitTypes);
					
					if (floorplate.size() != 1) {
						System.out.println("invalid anchor - skipping");
						continue;
					}
					
					for (Polygon3D unit : units) {							
						if (unit.getPlane3D().c() < 0) {
							unit.reverseWinding();
						}
					}
												
					sm.addFloorplate(footprintType, floorplateType, floorplate.get(0), unitTypes, units);
				}
			}
		}	
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
				String unitColor = line[4].trim();

				sm.addUnit(unitType, unitCount, unitArea, unitValue, unitColor);
			}
		}
	}
}
