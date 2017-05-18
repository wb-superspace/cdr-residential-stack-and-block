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
import model.StackAnalysis.AnalysisFloor;
import model.StackAnalysis.AnalysisFloor.AnalysisUnit;

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
		
	public static Stack<AnalysisFloor> getAnalysisStack(StackManager sm, Point3D footprint) {
		
		StackAnalysis stackAnalysis = new StackAnalysis();
		Stack<AnalysisFloor> analysisStack = new Stack<>();
						
		for (int i=0; i<sm.getStack(footprint).size(); i++) {			
			analysisStack.push(stackAnalysis.new AnalysisFloor(sm, footprint, 
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
		
		private final StackManager sm;
		
		private Point3D footprint;
		private Point3D floorplate;
		
		private int floorIndex;
		
		private List<AnalysisUnit> analysisUnits;
		
		private Map<String, Float> attributes;
		
		public AnalysisFloor(StackManager sm, Point3D footprint, Point3D floorplate, int floorIndex) {
			
			this.sm = sm;
			this.footprint = footprint;
			this.floorplate = floorplate;
			this.floorIndex = floorIndex;
				
			this.attributes = new HashMap<>();
			this.setAnalysisUnits();
		}
		
		public AnalysisFloor clone() {
			
			AnalysisFloor clone = new AnalysisFloor(this.sm, this.footprint, this.floorplate, this.floorIndex);
			clone.copy(this);
						
			return clone;
		}
		
		public void copy(AnalysisFloor other) {
			
			this.attributes = new HashMap<>(other.attributes);
			this.analysisUnits = new ArrayList<>();
			
			for (AnalysisUnit analysisUnit : other.analysisUnits) {
				this.analysisUnits.add(analysisUnit.clone(this));
			}
		}
				
		private Vector3D getTranslation(boolean exploded) {
			
			float z = exploded
					? this.sm.floorToCeilingHeight*this.floorIndex*D
					: this.sm.floorToCeilingHeight*this.floorIndex; 	
			
			return new ArrayVector3D(
					this.footprint.x()-this.floorplate.x(), 
					this.footprint.y()-this.floorplate.y(),
					z);
		}
		
		public Point3D getAnchor(boolean exploded) {
			
			Point3D anchor = new ArrayPoint3D();
			AnalysisFloor.this.floorplate.addVector(AnalysisFloor.this.getTranslation(exploded), anchor);
			
			return anchor;
		}
				
		public List<AnalysisUnit> getAnalysisUnits() {
			return this.analysisUnits;
		}
		
		private void setAnalysisUnits() {
			
			this.analysisUnits = new ArrayList<>();
			
			for (Polygon3D unit : this.sm.getUnits(this.floorplate)) {
				this.analysisUnits.add(this.new AnalysisUnit(unit));
			}
		}
		
		public void addAttribute(String attribute, float value) {
			this.attributes.put(attribute, value);
		}

		public float getAttribute(String attribute) {
			return this.attributes.containsKey(attribute) ? this.attributes.get(attribute) : 0f;
		}
		
		public Point3D getFootprint() {
			return this.footprint;
		}
		
		public String getFootprintType() {
			return this.sm.getFootprintType(this.footprint);
		}
		
		public float getFootprintArea() {
			return this.sm.getFootprintArea(this.footprint);
		}
		
		public void setFloorPlate(Point3D floorplate) {
			this.floorplate = floorplate;	
			this.setAnalysisUnits();
		}
		
		public Point3D getFloorplate() {
			return this.floorplate;
		}
		
		public String getFloorplateType() {
			return this.sm.getFloorplateType(this.floorplate);
		}
				
		public int getFloorIndex() {
			return this.floorIndex;
		}
				
		public void setFloorIndex(int floorIndex) {	
			this.floorIndex = floorIndex;
			this.analysisUnits.forEach(u -> u.updateFloorIndex());
		}
		
		public String getHashString() {
			
			List<Point3D> footprints = this.sm.getFootprints();
							
			String identifier = this.getFloorplateType() + "|";
			
			int floorIndex = this.floorIndex;
			int footprintIndex = footprints.indexOf(this.footprint);
			
			identifier += footprintIndex + "|";
			identifier += floorIndex + "|";
			
			for (int j = 0; j<footprints.size(); j++) {
			
				int stackSize = sm.getStack(footprints.get(j)).size();
				int relationship = 0;
				
				if (j != footprintIndex) {					
					relationship = floorIndex - stackSize;
				}
				
				if (relationship < 0) { // TODO - this assumes that all points are looking down
					relationship = -1;
				}
				
				identifier += relationship + "|";
			}
			
			return identifier;
		}
				
		/*
		 * =========================================================
		 * UNIT
		 * =========================================================
		 */
		
		public class AnalysisUnit {
			
			private final Polygon3D unit;
			
			private Mesh3D analysisMesh;
			private Polygon3D analysisGeometry;
			private List<Point3D> analysisPoints;
			
			private Map<String, Float> attributes;
							
			public AnalysisUnit(Polygon3D unit) {
				
				this.unit = unit;			
				this.attributes = new HashMap<>();
			}
					
			@Override
			public String toString() {
				return this.getUnitType();
			}
			
			public AnalysisUnit clone(AnalysisFloor analysisFloor) {
				
				AnalysisUnit clone = analysisFloor.new AnalysisUnit(this.unit);
				clone.attributes = new HashMap<>(this.attributes);
				
				return clone;
			}
						
			public int getFloorIndex() {
				return AnalysisFloor.this.floorIndex;
			}
			
			public void updateFloorIndex() {
				this.analysisMesh = null;
				this.analysisPoints = null;
				this.analysisGeometry = null;
			}
			
			public String getFootprintType() {
				return AnalysisFloor.this.getFootprintType();
			}
			
			public String getFloorplateType() {
				return AnalysisFloor.this.getFloorplateType();
			}
							
			public String getUnitType() {
				return AnalysisFloor.this.sm.getUnitType(this.unit);
			}
			
			public Float getArea() {
				return AnalysisFloor.this.sm.getUnitArea(this.unit);
			}
			
			public void addAttribute(String attribute, float value) {
				this.attributes.put(attribute, value);
			}
			
			public Float getAttribute(String attribute) {
				return this.attributes.containsKey(attribute) ? this.attributes.get(attribute) : 0f;
			}
			
			public Point3D getTagPoint(boolean exploded) {
				
				Point3D tagPoint = new ArrayPoint3D();
				AnalysisFloor.this.sm.getUnitTagPoint(this.unit).addVector(AnalysisFloor.this.getTranslation(exploded), tagPoint);
				
				return tagPoint;
			}
						
			public List<Point3D> getAnalysisPoints(boolean exploded) {
				
				if (exploded) {
					List<Point3D> analysisPoints = new ArrayList<>();
					
					for (Point3D unitAnalysisPoint : AnalysisFloor.this.sm.getUnitAnalysisPoints(this.unit)) {
						
						Point3D translated = new ArrayPoint3D();
						unitAnalysisPoint.addVector(AnalysisFloor.this.getTranslation(exploded), translated);				
						analysisPoints.add(translated);
					}
					
					return analysisPoints;
				}
				
				if (this.analysisPoints == null) {
					this.analysisPoints = new ArrayList<>();

					for (Point3D unitAnalysisPoint : AnalysisFloor.this.sm.getUnitAnalysisPoints(this.unit)) {
						
						Point3D translated = new ArrayPoint3D();
						unitAnalysisPoint.addVector(AnalysisFloor.this.getTranslation(exploded), translated);				
						this.analysisPoints.add(translated);
					}
				}
				
				return analysisPoints;
			}
			
			public Mesh3D getAnalysisMesh(boolean exploded) {
				
				if (exploded) {
					Mesh3D analysisMesh = AnalysisFloor.this.sm.getUnitAnalysisMesh(this.unit);
					analysisMesh.translate(AnalysisFloor.this.getTranslation(exploded));
					return analysisMesh;
				}
				
				if (this.analysisMesh == null) {
					this.analysisMesh = AnalysisFloor.this.sm.getUnitAnalysisMesh(this.unit);
					this.analysisMesh.translate(AnalysisFloor.this.getTranslation(exploded));
				}
							
				return analysisMesh;
			}
					
			public Polygon3D getAnalysisGeometry(boolean exploded) {
				
				if (exploded) {
					Polygon3D analysisGeometry = new Polygon3D(this.unit.iterablePoints());
					analysisGeometry.translate(AnalysisFloor.this.getTranslation(exploded));		
					return analysisGeometry;
				}
				
				if (this.analysisGeometry == null) {
					this.analysisGeometry = new Polygon3D(this.unit.iterablePoints());
					this.analysisGeometry.translate(AnalysisFloor.this.getTranslation(exploded));
				}
				
				return this.analysisGeometry;
			}
		}
	}
}
