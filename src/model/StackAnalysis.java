package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import cdr.geometry.primitives.ArrayPoint3D;
import cdr.geometry.primitives.ArrayVector3D;
import cdr.geometry.primitives.Point3D;
import cdr.geometry.primitives.Polygon3D;
import cdr.geometry.primitives.Vector3D;
import cdr.mesh.datastructure.Mesh3D;

public class StackAnalysis {

	private static float D = 3;
	
	/*
	 * =========================================================
	 * BOUNDS
	 * =========================================================
	 */
	
	public static float[] getBounds( Map<Point3D, Stack<AnalysisFloor>> analysisStacks, String attribute) {
		
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;
		
		for (Stack<AnalysisFloor> analysisStack : analysisStacks.values()) {
		
			for (AnalysisFloor analysisFloor: analysisStack) {
				for (AnalysisUnit analysisUnit: analysisFloor.getAnalysisUnits()) {
					
					if (analysisUnit.getUnitType() != null) {
						
						float value = analysisUnit.getAttribute(attribute);
												
						if (value > max) max = value;
						if (value < min) min = value;
					}	
				}
			}
		}
		
		return new float[] {min, max};
	}
	
	/*
	 * =========================================================
	 * METHODS
	 * =========================================================
	 */
	
	private static AnalysisFloor getAnalysisFloor(StackManager sm, Point3D footprint, Point3D floorplate, int floorIndex) {
		
		StackAnalysis stackAnalysis = new StackAnalysis();
		AnalysisFloor analysisFloor = stackAnalysis.new AnalysisFloor();
		
		for (Polygon3D unit : sm.getUnits(floorplate)) {
			analysisFloor.addAnalysisUnit(stackAnalysis.new AnalysisUnit(sm, footprint, floorplate, unit, floorIndex));
		}
		
		return analysisFloor;
	}
	
	public static Stack<AnalysisFloor> getAnalysisStack(StackManager sm, Point3D footprint) {
		
		Stack<AnalysisFloor> analysisStack = new Stack<>();
						
		for (int i=0; i<sm.getStack(footprint).size(); i++) {			
			analysisStack.push(getAnalysisFloor(sm, footprint, 
					sm.getFloorplate(sm.getFootprintType(footprint), sm.getStack(footprint).get(i)), i));

		}
		
		return analysisStack;
	}
	
	public static Map<Point3D, Stack<AnalysisFloor>> getAnalysisStacks(StackManager sm) {
		
		Map<Point3D, Stack<AnalysisFloor>> analysisStacks = new HashMap<>();
		
		for (Point3D footprint : sm.getFootprints()) {
			analysisStacks.put(footprint, getAnalysisStack(sm, footprint));
		}
		
		return analysisStacks;
	}
	
	/*
	 * =========================================================
	 * FLOOR
	 * =========================================================
	 */
	
	public class AnalysisFloor {
		
		private List<AnalysisUnit> analysisUnits;
		private Map<String, Float> attributes;
		
		public AnalysisFloor() {
			this.analysisUnits = new ArrayList<>();
			this.attributes = new HashMap<>();
		}
		
		public AnalysisFloor clone() {
			
			AnalysisFloor clone = new AnalysisFloor();
			clone.attributes = new HashMap<>(this.attributes);
			
			for (AnalysisUnit analysisUnit : this.analysisUnits) {
				clone.addAnalysisUnit(analysisUnit.clone());
			}
			
			return clone;
		}
		
		public List<AnalysisUnit> getAnalysisUnits() {
			return this.analysisUnits;
		}
		
		public void addAnalysisUnit(AnalysisUnit unit) {
			this.analysisUnits.add(unit);
		}
			
		public void addAttribute(String attribute, float value) {
			this.attributes.put(attribute, value);
		}

		public float getAttribute(String attribute) {
			return this.attributes.containsKey(attribute) ? this.attributes.get(attribute) : 0f;
		}
				
		public void setFloorIndex(int floorIndex) {	
			this.analysisUnits.forEach(u -> u.setFloorIndex(floorIndex));
		}
	}
	
	/*
	 * =========================================================
	 * UNIT
	 * =========================================================
	 */
	
	public class AnalysisUnit {
		
		private final StackManager sm;
		
		private final Polygon3D unit;
		private final Point3D footprint;
		private final Point3D floorplate;
		
		private int floorIndex;
		
		private Mesh3D analysisMesh;
		private Polygon3D analysisGeometry;
		private List<Point3D> analysisPoints;
		
		private Map<String, Float> attributes;
						
		public AnalysisUnit(StackManager sm, Point3D footprint, Point3D floorplate, Polygon3D unit, int floorIndex) {
			
			this.sm = sm;
			this.unit = unit;
			this.footprint = footprint;
			this.floorplate = floorplate;
			this.floorIndex = floorIndex;
			
			this.attributes = new HashMap<>();
		}
				
		@Override
		public String toString() {
			return this.getUnitType();
		}
		
		public AnalysisUnit clone() {
			
			AnalysisUnit clone = new AnalysisUnit(this.sm, this.footprint, this.floorplate, this.unit, this.floorIndex);
			clone.attributes = new HashMap<>(this.attributes);
			
			return clone;
		}
		
		private Vector3D getTranslation(boolean exploded) {
						
			float z = exploded ? sm.floorToCeilingHeight*this.floorIndex*D : sm.floorToCeilingHeight*this.floorIndex; 	
			return new ArrayVector3D(footprint.x()-floorplate.x(), footprint.y()-floorplate.y(), z);
		}
		
		public int getFloorIndex() {
			return this.floorIndex;
		}
		
		public void setFloorIndex(int floorIndex) {
			this.floorIndex = floorIndex;
			this.analysisMesh = null;
			this.analysisPoints = null;
			this.analysisGeometry = null;
		}
		
		public String getFootprintType() {
			return this.sm.getFootprintType(this.footprint);
		}
		
		public String getFloorplateType() {
			return this.sm.getFloorplateType(this.floorplate);
		}
						
		public String getUnitType() {
			return this.sm.getUnitType(this.unit);
		}
		
		public Float getArea() {
			return this.sm.getUnitArea(this.unit);
		}
		
		public void addAttribute(String attribute, float value) {
			this.attributes.put(attribute, value);
		}
		
		public Float getAttribute(String attribute) {
			return this.attributes.containsKey(attribute) ? this.attributes.get(attribute) : 0f;
		}
		
		public Point3D getAnchor(boolean exploded) {
			
			Point3D anchor = new ArrayPoint3D();
			this.floorplate.addVector(this.getTranslation(exploded), anchor);
			
			return anchor;
		}
		
		public List<Point3D> getAnalysisPoints(boolean exploded) {
			
			if (exploded) {
				List<Point3D> analysisPoints = new ArrayList<>();
				
				for (Point3D unitAnalysisPoint : this.sm.getUnitAnalysisPoints(this.unit)) {
					
					Point3D translated = new ArrayPoint3D();
					unitAnalysisPoint.addVector(this.getTranslation(exploded), translated);				
					analysisPoints.add(translated);
				}
				
				return analysisPoints;
			}
			
			if (this.analysisPoints == null) {
				this.analysisPoints = new ArrayList<>();

				for (Point3D unitAnalysisPoint : this.sm.getUnitAnalysisPoints(this.unit)) {
					
					Point3D translated = new ArrayPoint3D();
					unitAnalysisPoint.addVector(this.getTranslation(exploded), translated);				
					this.analysisPoints.add(translated);
				}
			}
			
			return analysisPoints;
		}
		
		public Mesh3D getAnalysisMesh(boolean exploded) {
			
			if (exploded) {
				Mesh3D analysisMesh = this.sm.getUnitAnalysisMesh(this.unit);
				analysisMesh.translate(this.getTranslation(exploded));
				return analysisMesh;
			}
			
			if (this.analysisMesh == null) {
				this.analysisMesh = this.sm.getUnitAnalysisMesh(this.unit);
				this.analysisMesh.translate(this.getTranslation(exploded));
			}
						
			return analysisMesh;
		}
				
		public Polygon3D getAnalysisGeometry(boolean exploded) {
			
			if (exploded) {
				Polygon3D analysisGeometry = new Polygon3D(this.unit.iterablePoints());
				analysisGeometry.translate(this.getTranslation(exploded));		
				return analysisGeometry;
			}
			
			if (this.analysisGeometry == null) {
				this.analysisGeometry = new Polygon3D(this.unit.iterablePoints());
				this.analysisGeometry.translate(this.getTranslation(exploded));
			}
			
			return this.analysisGeometry;
		}
	}
}
