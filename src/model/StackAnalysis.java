package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

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
	
	public static Stack<AnalysisFloor> getAnalysisStack(StackManager sm, Point3D footprint, boolean copy) {
		
		Stack<AnalysisFloor> analysisStack = new Stack<>();
						
		for (int i=0; i<sm.getStack(footprint).size(); i++) {			
			analysisStack.push(getAnalysisFloor(sm, footprint, 
					sm.getFloorplate(sm.getFootprintType(footprint), sm.getStack(footprint).get(i)), i));

		}
		
		return analysisStack;
	}
	
	/*
	 * =========================================================
	 * FLOOR
	 * =========================================================
	 */
	
	public class AnalysisFloor {
		
		private List<AnalysisUnit> analysisUnits;
				
		public AnalysisFloor() {
			this.analysisUnits = new ArrayList<>();
		}
		
		public AnalysisFloor clone() {
			
			AnalysisFloor clone = new AnalysisFloor();
			
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
				
		public float getCost() {
			
			float cost = 0;	
			for (AnalysisUnit analysisUnit : this.analysisUnits) cost += analysisUnit.cost;
			return cost;
		}
		
		public float getValue() {
			
			float value = 0;
			for (AnalysisUnit analysisUnit : this.analysisUnits) value += analysisUnit.value;
			return value;
		}
		
		public float getDelta() {
			return this.getValue() - this.getCost();
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
				
		public float value;
		public float cost;
		public float visibility;
						
		public AnalysisUnit(StackManager sm, Point3D footprint, Point3D floorplate, Polygon3D unit, int floorIndex) {
			
			this.sm = sm;
			this.unit = unit;
			this.footprint = footprint;
			this.floorplate = floorplate;
			this.floorIndex = floorIndex;
		}
				
		@Override
		public String toString() {
			return this.getType();
		}
		
		public AnalysisUnit clone() {
			
			AnalysisUnit clone = new AnalysisUnit(this.sm, this.footprint, this.floorplate, this.unit, this.floorIndex);
			
			clone.value = this.value;
			clone.cost = this.cost;
			clone.visibility = this.visibility;
			
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
						
		public String getType() {
			return this.sm.getUnitType(this.unit);
		}
		
		public List<Point3D> getAnalysisPoints(boolean exploded) {
			
			if (this.analysisPoints == null) {
				this.analysisPoints = new ArrayList<>();

				for (Point3D unitAnalysisPoint : this.sm.getUnitAnalysisPoints(this.unit)) {
					
					Point3D translated = new ArrayPoint3D();
					unitAnalysisPoint.addVector(this.getTranslation(exploded), translated);				
					analysisPoints.add(translated);
				}
			}
			
			return analysisPoints;
		}
		
		public Mesh3D getAnalysisMesh(boolean exploded) {
			
			if (this.analysisMesh == null) {
				this.analysisMesh = this.sm.getUnitAnalysisMesh(this.unit);
				this.analysisMesh.translate(this.getTranslation(exploded));
			}
						
			return analysisMesh;
		}
				
		public Polygon3D getAnalysisGeometry(boolean exploded) {
			
			if (this.analysisGeometry == null) {
				this.analysisGeometry = new Polygon3D(this.unit.iterablePoints());
				this.analysisGeometry.translate(this.getTranslation(exploded));
			}
			
			return this.analysisGeometry;
		}
	}
}
