package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import cdr.geometry.primitives.Point3D;
import cdr.geometry.primitives.Polygon3D;
import cdr.geometry.primitives.Text3D;
import javafx.beans.property.SimpleBooleanProperty;

public class StackManager {
	
	// TODO - make these classes
	
	/*
	 * Boundary
	 */
	
	List<Polygon3D> boundaries = new ArrayList<>();
	
	/*
	 * Foorprint
	 */
	
	List<Point3D> footprints = new ArrayList<>();
	
	Map<Point3D, String> footprintTypes = new HashMap<>();
		
	/*
	 * Floorplate
	 */
	
	Map<String, Map<String, Point3D>> floorplates = new HashMap<>();
	
	Map<Point3D, List<Polygon3D>> units = new HashMap<>();
	
	/*
	 * Unit 
	 */
	
	Map<Polygon3D, String> unitTypes = new HashMap<>();
		
	public Map<String, Integer> unitCounts = new HashMap<>();
	
	public Map<String, Float> unitAreas = new HashMap<>();
	
	public Map<String, String> unitColors = new HashMap<>();
	
	public Map<String, Float> unitValues = new HashMap<>();
	
	/*
	 * Save
	 */
	
	private SimpleBooleanProperty flag = new SimpleBooleanProperty(false);
		
	private List<int[]> pointers;
	
	private Stack<Map<Point3D, Stack<String>>> restore;
	
	private Map<Point3D, Stack<String>> stacks = new HashMap<>();
	
	/*
	 * Run
	 */
	
	public void initialize() {
		
		restore = new Stack<>();
		
		pointers = new ArrayList<>();
		
		for (Point3D footprint : this.getFootprints()) {
			this.getStack(footprint).clear();
		
		}
		
		this.saveStacks();
	}
	
	public void flag() {
		this.flag.set(!this.flag.get());
	}
	
	public SimpleBooleanProperty getFlag() {
		return this.flag;
	}
	
	public void point() {
		
		pointers.clear();	
				
		for (int j=0; j<footprints.size(); j++) {		
			for (int k =0;k<this.getStack(footprints.get(j)).size(); k++) {
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
	
	public float getFootprintArea(Point3D footprint) { // TODO - replace this with a polyline
		
		float area = 0f;
		
		String footprintType = this.getFootprintType(footprint);
		String floorplateType = this.getFloorplateTypes(footprintType).iterator().next();	
		Point3D floorplate = this.getFloorplate(footprintType, floorplateType);	
		
		for (Polygon3D unit : this.getUnits(floorplate)) {
			area += unit.area();
		}
		
		return area;
	}
	
	public void addFloorplate(String footprintType, String floorplateType, Point3D floorplate, List<Text3D> unitTypes, List<Polygon3D> units) {
		
		this.floorplates.get(footprintType).put(floorplateType, floorplate);
		this.units.put(floorplate, units);
		
		for (Polygon3D unit : units) {
			for (Text3D unitType : unitTypes) {
				if (unit.isInside(unitType.getAnchor())) {
					this.unitTypes.put(unit, unitType.getString().trim());
				}
			}
		}
	}
	
	public Set<String> getFloorplateTypes(String footprintType) {
		return this.floorplates.get(footprintType).keySet();
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
	
	public List<Polygon3D> getUnits(Point3D floorplate) {
		return this.units.get(floorplate);
	}
	
	public void addUnit(String type, Integer count, Float area, Float value, String color) {
		this.unitCounts.put(type, count);
		this.unitAreas.put(type, area);
		this.unitValues.put(type, value);
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
		
	public String getUnitType(Polygon3D unit) {	
		return this.unitTypes.get(unit);
	}
	
	public Integer getStackHash() {
		
		String stackString = "";
		
		for (int[] pointer : this.pointers) {
			stackString += pointer[0] + "-" + 
				pointer[1] + "-" + 
				this.getStack(this.getFootprints().get(pointer[0])).get(pointer[1]) + "-";
		}
		
		return stackString.hashCode();
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

	public List<Polygon3D> getBoundaries() {
		return boundaries;
	}

	public void setBoundaries(List<Polygon3D> boundaries) {
		this.boundaries = boundaries;
	}
}
