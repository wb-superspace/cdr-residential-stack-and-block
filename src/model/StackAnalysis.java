package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import com.sun.corba.se.spi.orb.StringPair;

import cdr.geometry.primitives.ArrayPoint3D;
import cdr.geometry.primitives.ArrayVector3D;
import cdr.geometry.primitives.Point3D;
import cdr.geometry.primitives.Polygon3D;
import cdr.geometry.primitives.Vector3D;
import cdr.mesh.datastructure.Mesh3D;
import model.StackAnalysis.AnalysisFloor.AnalysisUnit;

public class StackAnalysis {

	private static float D = 3;
	
	/*
	 * =========================================================
	 * STATIC METHODS
	 * =========================================================
	 */
	
	public static AnalysisAttribute getAnalysisAttributeUnit(List<AnalysisUnit> analysisUnits, String attribute) {
		
		AnalysisAttribute analysisAttribute = new StackAnalysis().new AnalysisAttribute(attribute);
		
		for (AnalysisUnit analysisUnit : analysisUnits) {
			if (analysisUnit.getUnitType() != null) {
				analysisAttribute.addValue(analysisUnit.getAttribute(attribute));
			}
		}
		
		return analysisAttribute;
	}
	
	public static AnalysisAttribute getAnalysisAttributeFloor(List<AnalysisFloor> analysisFloors, String attribute) {
		
		AnalysisAttribute analysisAttribute = new StackAnalysis().new AnalysisAttribute(attribute);
		
		for (AnalysisFloor analysisFloor : analysisFloors) {
			analysisAttribute.addValue(analysisFloor.getAttribute(attribute));
		}
		
		return analysisAttribute;
	}
	
	public static Map<Point3D, Stack<String>> unHashStack(StackManager sm, String hashString) {
		
		Map<Point3D, Stack<String>> stacks = new HashMap<>();
		Map<Point3D, String[]> unHash = new HashMap<>();
		
		String[] floors = hashString.split("\n");
		
		
		for (int i=1; i<floors.length; i++) {
			
			String floor = floors[i];
			
			System.out.println(floor);
			
			String[] floorData = floor.split(",");
			
			int footprintIndex = Integer.parseInt(floorData[0]);
			int floorplateIndex = Integer.parseInt(floorData[1]);
			
			String footprintType = floorData[2];
			String floorplateType = floorData[3];
			
			Point3D footprint = sm.getFootprints().get(footprintIndex);
			
			if (!unHash.containsKey(footprint)) {
				unHash.put(footprint, new String[floors.length]);
			}
			
			unHash.get(footprint)[floorplateIndex] = floorplateType;
		}
		
		for (Map.Entry<Point3D, String[]> hashStack : unHash.entrySet()) {
			
			stacks.put(hashStack.getKey(), new Stack<>());
			
			for (String floorplateType : hashStack.getValue()) {
				if (floorplateType != null) {
					stacks.get(hashStack.getKey()).push(floorplateType);
				}
			}
		}
		
		return stacks;
	}
				
	public static String hashAnalysisStack( Map<Point3D, Stack<AnalysisFloor>> analysisStacks) {
		
		String hashString = "";
		
		for (Stack<AnalysisFloor> analysisStack : analysisStacks.values()) {
			for (AnalysisFloor analysisFloor: analysisStack) {
				hashString += analysisFloor.getHashString() +"\n";
			}
		}
		
		return hashString;
	}
		
	private static Stack<AnalysisFloor> getAnalysisStack(StackManager sm, Point3D footprint) {
		
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
	 * ATTRIBUTE
	 * =========================================================
	 */
	
	public class AnalysisAttribute {
		
		private String attribute;
		
		private Float[] bounds;
		private List<Float> values;
		
		public AnalysisAttribute(String attribute) {
			
			this.attribute = attribute;
			this.bounds = new Float[]{Float.MAX_VALUE, -Float.MAX_VALUE};
			this.values = new ArrayList<>();
		}
		
		public String getAttribute() {
			return this.attribute;
		}
		
		public Float[] getBounds() {
			return this.bounds;
		}
		
		public List<Float> getValues() {
			return this.values;
		}
		
		public float getMean() {	
			return getSum() / this.values.size();
		}
		
		public float getSum() {
			
			float sum = 0;
			
			for (float value : this.values) {
				sum += value;
			}
			
			return sum;
		}
		
		public void addValue(Float value) {
			
			if (value < bounds[0]) bounds[0] = value;
			if (value > bounds[1]) bounds[1] = value; 
			
			this.values.add(value);
		}
		
		public void setValues(List<Float> values) {
			
			this.bounds = new Float[]{Float.MAX_VALUE, -Float.MAX_VALUE};
			this.values = new ArrayList<>();
			
			for (Float value : values) {
				this.addValue(value);
			}
		}
		
		public float getMappedValue(float value) {
			return (value - bounds[0]) / (bounds[1] - bounds[0]);	
		}
		
		public Map<Float, List<Float>> getBinValues(int numBins) {
			
			SortedMap<Float, List<Float>> bins = new TreeMap<>();
			
			if (this.bounds[1] != this.bounds[0] && this.bounds[1] != 0) {
				
				for (float i = this.bounds[0]; i <= this.bounds[1]; i+= (this.bounds[1]-this.bounds[0]) / numBins) {
															
					bins.put(i, new ArrayList<>());
				}
				
				for (int i = 0; i < this.values.size(); i++) {
					
					List<Float> curr = null;
					
					for (Map.Entry<Float, List<Float>> bin : bins.entrySet()) {
						
						if (bin.getKey() > this.values.get(i)) break;
						
						curr = bin.getValue();
					}
					
					if (curr != null) {
						curr.add(this.values.get(i));
					}	
				}
			}
						
			return bins;
		}
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
		
		public int getFootprintIndex() {
			return sm.getFootprints().indexOf(this.footprint);
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
												
			String hashString = "";
			
			hashString += this.getFootprintIndex() + ",";
			hashString += this.floorIndex + ",";
			hashString += this.getFootprintType() + ",";
			hashString += this.getFloorplateType();
						
			return hashString;
		}
		
		public int[] getRelativPosition() {
			
			int[] position = new int[this.sm.getFootprints().size()]; 
						
			for (int j = 0; j< this.sm.getFootprints().size(); j++) {
				
				int relationship = 0;
				
				if (j != this.getFootprintIndex()) {					
					relationship = this.floorIndex - sm.getStack(this.sm.getFootprints().get(j)).size();
				}
				
				if (relationship < 0) { // TODO - this assumes that all points are looking down
					relationship = -1;
				}
				
				position[j] = relationship;
			}
			
			return position;
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
