package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import cdr.geometry.primitives.ArrayVector3D;
import cdr.geometry.primitives.LineSegment3D;
import cdr.geometry.primitives.Point3D;
import cdr.geometry.primitives.Polygon3D;
import cdr.geometry.primitives.Text3D;
import cdr.mesh.datastructure.Mesh3D;
import cdr.mesh.datastructure.fvMesh.FVMesh;
import cdr.mesh.toolkit.operators.MeshOperators;
import javafx.beans.property.SimpleIntegerProperty;
import lucy.MoreMeshPrimitives;
import sun.launcher.resources.launcher;

public class StackManager {
		
	/*
	 * Attributes
	 */
	
	public float maxArea = Float.MAX_VALUE; 
	
	public float maxHeight = Float.MAX_VALUE; 
	
	public float floorplateCostBase = 1250f; 							// AECOM
	
	public float floorplateCostFloorMultiplier = 35f; 					// AECOM
	
	public float unitPremiumFloorMultiplier = 0.015f; 					// AECOM (1.5% -> 2.2% (PH))
		
	public float floorToCeilingHeight = 4f;
	
	public float viewHeight = 1.8f;
	
	/*
	 * Properties
	 */
	
	List<Point3D> views = new ArrayList<>();	
	
	List<Polygon3D> boundaries = new ArrayList<>();
	
	List<Mesh3D> context = new ArrayList<>();
	
	/*
	 * Foorprint
	 */
	
	List<Point3D> footprints = new ArrayList<>();
	
	Map<Point3D, String> footprintTypes = new HashMap<>();
		
	/*
	 * Floorplate
	 */
	
	Map<String, Map<String, Point3D>> floorplates = new HashMap<>();
	
	Map<Point3D, String> floorplateTypes = new HashMap<>();
	
	Map<Point3D, List<Polygon3D>> units = new HashMap<>(); 
	
	/*
	 * Unit 
	 */
	
	Map<Polygon3D, String> unitTypes = new HashMap<>();
	
	Map<Polygon3D, Point3D> unitTagPoints = new HashMap<>();
	
	Map<Polygon3D, List<Point3D>> unitAnalysisPoints = new HashMap<>();
	
	Map<Polygon3D, Mesh3D> unitAnalysisMeshes = new HashMap<>();
	
	Map<Polygon3D, Float> unitAreas = new HashMap<>();
	
	/*
	 * Unit matrix
	 */
	
	public Map<String, Integer> unitCounts = new HashMap<>();
	
	public Map<String, String> unitColors = new HashMap<>();
	
	public Map<String, Float> unitValues = new HashMap<>();
	
	public Map<String, Float> unitCaps = new HashMap<>();
	
	/*
	 * Save
	 */
		
	private List<int[]> pointers = new ArrayList<>();
		
	private Stack<Map<Point3D, Stack<String>>> restore = new Stack<>();
	
	private Map<Point3D, Stack<String>> stacks = new HashMap<>();
	
	/*
	 * Methods
	 */
	
	public void clearStacks() {
		this.restore = new Stack<>();
		this.pointers = new ArrayList<>();
		for (Point3D footprint : this.getFootprints()) {
			this.getStack(footprint).clear();
		}
		
		this.saveStacks();
	}
	
	public void clearUnitMix() {
		this.unitCounts = new HashMap<>();
		this.unitColors = new HashMap<>();
		this.unitValues = new HashMap<>();
		this.unitCaps = new HashMap<>();
	}
		
	public void point() {
		
		pointers.clear();	
						
		for (int j=0; j<footprints.size(); j++) {	
			
			for (int k =0;k< this.getStack(footprints.get(j)).size(); k++) {
				pointers.add(new int[] {j,k});	
			}	
		}
	}
	
	public List<int[]> getPointers() {
		return this.pointers;
	}
		
	public void addFootprint(String footprintType, List<Point3D> footprints) {
		
		for (Point3D footprint : footprints) {
			this.footprints.add(footprint);
			this.footprintTypes.put(footprint, footprintType);
			this.stacks.put(footprint, new Stack<>());
		}
		this.floorplates.put(footprintType, new HashMap<>());
	}
	
	public List<Point3D> getFootprints() {
		return this.footprints;
	}
	
	public String getFootprintType(Point3D footprint) {
		return this.footprintTypes.get(footprint);
	}
	
	public float getFootprintArea(Point3D footprint) { // TODO - replace this with a polyline?
		
		float area = 0f;
		
		String footprintType = this.getFootprintType(footprint);
		String floorplateType = this.getFloorplateTypes(footprintType).iterator().next();	
		Point3D floorplate = this.getFloorplate(footprintType, floorplateType);	
		
		for (Polygon3D unit : this.getUnits(floorplate)) {
			area += this.unitAreas.get(unit);
		}
		
		return area;
	}
	
	public float getStackArea() {
		
		float area = 0f;
		
		for (Point3D footprint : this.footprints) {
			area += this.getFootprintArea(footprint) * this.getStack(footprint).size();
		}
						
		return area;
	}
	
	public void addFloorplate(String footprintType, String floorplateType, Point3D floorplate, List<Text3D> unitTypes,
			List<Polygon3D> units, List<LineSegment3D> analysisLocations) {
		
		if (!this.floorplates.containsKey(footprintType)) { // TODO - maybe return instead?
			this.addFootprint(footprintType, new ArrayList<>());
		}
		
		this.floorplates.get(footprintType).put(floorplateType, floorplate);
		this.floorplateTypes.put(floorplate, floorplateType);
		this.units.put(floorplate, units);
		
		for (Polygon3D unit : units) {
			
			Mesh3D analysisMesh = MoreMeshPrimitives.makeExtrudedMeshFromPolygon3D(unit, this.floorToCeilingHeight);
			new MeshOperators().triangulateMesh(analysisMesh);
			this.unitAnalysisMeshes.put(unit, analysisMesh);
			this.unitAreas.put(unit, unit.area());
			
			for (Text3D unitType : unitTypes) {
				if (unit.isInside(unitType.getAnchor())) {
					this.unitTypes.put(unit, unitType.getString().trim());
					this.unitTagPoints.put(unit, unitType.getAnchor());
				}
			}
			
			this.unitAnalysisPoints.put(unit, new ArrayList<>());
			
			for (LineSegment3D analysisLocation : analysisLocations) {
				
				boolean sptIsInside = unit.isInside(analysisLocation.getStartPoint());
				boolean eptIsInside = unit.isInside(analysisLocation.getEndPoint());
				
				if (sptIsInside && !eptIsInside) {
					
					Point3D analysisPoint = analysisLocation.getEndPoint();
					analysisPoint.translate(new ArrayVector3D(0, 0, viewHeight));
					this.unitAnalysisPoints.get(unit).add(analysisPoint);
					
				} else if (eptIsInside && !sptIsInside) {
					
					Point3D analysisPoint = analysisLocation.getStartPoint();
					analysisPoint.translate(new ArrayVector3D(0, 0, viewHeight));
					this.unitAnalysisPoints.get(unit).add(analysisPoint);
				}
			}
		}
	}
	
	public Set<String> getFloorplateTypes(String footprintType) {
		return this.floorplates.get(footprintType).keySet();
	}
	
	public String getFloorplateType(Point3D floorplate) {
		return this.floorplateTypes.get(floorplate);
	}
	
	public Point3D getFloorplate(String footprintType, String floorplateType) {
		return this.floorplates.get(footprintType).get(floorplateType);
	}
	
	public void swapFloors(Point3D footprint, int f1, int f2) {
		
		Stack<String> stack = this.getStack(footprint);
		
		String s1 = stack.get(f1);
		String s2 = stack.get(f2);
		
		stack.set(f1, s2);
		stack.set(f2, s1);
		
		point();
	}
	
	public void pushFloor(Point3D footprint, String floorplate) {
		
		this.getStack(footprint).push(floorplate);
		
		point();
	}
	
	public String popFloor(Point3D footprint) {
		
		String floorplateType = this.getStack(footprint).pop();
		
		point();
		
		return floorplateType;
	}
	
	public String removeFloor(Point3D footprint, int floorIndex) {
		
		String floorplateType = this.getStack(footprint).remove(floorIndex);
		
		point();
		
		return floorplateType;
	}
	
	public void addFloor(Point3D footprint, String floorplate, int floorIndex) {
		
		this.getStack(footprint).insertElementAt(floorplate, floorIndex);
		
		point();
	}
	
	public List<Polygon3D> getUnits(Point3D floorplate) {
		return this.units.containsKey(floorplate) ? this.units.get(floorplate) : new ArrayList<>();
	}
	
	public void addUnit(String type, Integer count, Float value, Float cap, String color) {
		this.unitCounts.put(type, count);
		this.unitValues.put(type, value);
		this.unitCaps.put((cap == 0 ? null : type), (cap == 0 ? Float.MAX_VALUE : cap));
		this.unitColors.put(type, color);
	}
			
	public Set<String> getUnitTypes() {
		return this.unitCounts.keySet();
	}
	
	public List<String> getUnitTypes(String footprintType, String floorplateType) {
		
		List<String> unitTypes = new ArrayList<>();
		
		for (Polygon3D unit : this.getUnits(this.getFloorplate(footprintType, floorplateType))) {
			if (this.getUnitType(unit) != null) {
				unitTypes.add(this.getUnitType(unit));
			}
		}
		
		return unitTypes;
	}
	
	public Float getUnitArea(Polygon3D unit) {
		return this.unitAreas.get(unit);
	}
		
	public String getUnitType(Polygon3D unit) {		
		return this.unitTypes.get(unit);
	}
	
	public Point3D getUnitTagPoint(Polygon3D unit) {
		return this.unitTagPoints.get(unit);
	}
	
	public List<Point3D> getUnitAnalysisPoints(Polygon3D unit) {
		return this.unitAnalysisPoints.get(unit);
	}
	
	public Mesh3D getUnitAnalysisMesh(Polygon3D unit) {
		
		Mesh3D analysisMesh = new FVMesh.D3();
				
		new MeshOperators().duplicateMesh(this.unitAnalysisMeshes.get(unit), analysisMesh);
		
		return analysisMesh;
	}
			
	public int getUnitCount(String unitType) {
		
		int count = 0;
		
		for (Point3D footprint : this.footprints) {
			
			String footprintType = this.getFootprintType(footprint);
			
			for (String floorplateType : this.getStack(footprint)) {	
				
				Point3D floorplate = this.getFloorplate(footprintType, floorplateType);
				List<Polygon3D> units = this.getUnits(floorplate);
				
				if (units != null) {
					for (String type : this.getUnitTypes(footprintType, floorplateType)) {
						if (type.equals(unitType)) {
							count++;
						}
					}
				}
			}
		}
						
		return count;
	}
					
	public Stack<String> getStack(Point3D footprint) {
		return this.stacks.get(footprint);
	}
			
	public void saveStacks() {
		restore.push(saveState());
	}
	
	public void restoreStacks() {
		this.restoreState((restore.size() > 1 ? restore.pop() : restore.peek()));
	}
	
	public Map<Point3D, Stack<String>> saveState() {
		
		Map<Point3D, Stack<String>> state = new HashMap<>();
		this.getFootprints().forEach(f -> state.put(f, cloneStack(f)));
		return state;
	}
	
	public void restoreState(Map<Point3D, Stack<String>> state) {
		
		for (Map.Entry<Point3D, Stack<String>> entry : state.entrySet()) {
			this.setStack(entry.getKey(), this.cloneStack(entry.getValue()));
		}
		
		point();
	}
	
	@SuppressWarnings("unchecked")
	private Stack<String> cloneStack(Stack<String> stack) {
		return (Stack<String>) stack.clone();
	}
		
	public Stack<String> cloneStack(Point3D footprint) {
		return (Stack<String>) this.cloneStack(this.getStack(footprint));
	}
	
	public void setStack(Point3D footprint, Stack<String> stack) {
		this.stacks.put(footprint, stack);
		
		point();
	}
	
	public List<Mesh3D> getContext() {
		return this.context;
	}
	
	public void setContext(List<Mesh3D> context) {
		this.context = context;
	}

	public List<Polygon3D> getBoundaries() {
		return this.boundaries;
	}

	public void setBoundaries(List<Polygon3D> boundaries) {
		this.boundaries = boundaries;
	}
	
	public List<Point3D> getViewPoints() {
		return this.views;
	}
	
	public void setViewPoints(List<Point3D> viewPoints) {
		this.views = viewPoints;
	}
}
