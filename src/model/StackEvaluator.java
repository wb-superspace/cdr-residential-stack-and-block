package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import cdr.geometry.primitives.Point3D;
import cdr.geometry.primitives.Polygon3D;


public class StackEvaluator {
	
	public float floorplateCostBase = 125f; 							// AECOM
	public float floorplateCostFloorMultiplier = 3f; 					// AECOM
	
	public float unitValueThreshold = Float.MAX_VALUE; //2700000f;							// PAFILLIA
	public float unitPremiumFloorMultiplier = 0.015f; 					// AECOM (1.5% -> 2.2% (PH))
	
	float maxArea = 81747f;
	float maxHeight = 170f;
	
	public float floorToCeilingHeight = 4f;
	
	private StackManager sm;
	
	private long sleep = 0l;
	
	public Map<String, Integer> counts; // TODO - make observable
	public float value; // TODO - make observable
		
	/*
	 * =========================================================
	 * EVALUATIONS
	 * =========================================================
	 */
	
	public float evaluateFloorCost(List<Polygon3D> units, float floorIndex) {
				
		float baseCost = floorplateCostFloorMultiplier * floorIndex + floorplateCostBase;
		float floorCost = 0f;

		if (units == null) {
			return floorCost;
		}
		
		for (Polygon3D unit : units) {
			
			String unitType = sm.getUnitType(unit);		
			float unitArea = unitType != null ? sm.unitAreas.get(unitType) : 0f;
			float unitCost = baseCost * unitArea;
			
			floorCost += unitCost;
		}
		
		return floorCost;
	}
	
	public float evaluateFloorValue(List<Polygon3D> units, float floorIndex) {
				
		float floorValue = 0f;
		
		if (units == null) {
			return floorValue;
		}
		
		for (Polygon3D unit : units) {
			
			String unitType = sm.getUnitType(unit);					
			float unitArea = unitType != null ? sm.unitAreas.get(unitType) : 0f;  
			float unitValue = unitType != null ? unitArea * sm.unitValues.get(unitType) : 0f;

			unitValue += unitValue * unitPremiumFloorMultiplier * floorIndex;
			
			if (unitValue > unitValueThreshold) {
				unitValue = unitValueThreshold;
			}
			
			floorValue += unitValue;
		}
		
		return floorValue;
	}
	
	public float evaluateFloorProfit(Point3D floorplate, float floorIndex) {
		
		float floorValue = evaluateFloorValue(sm.getUnits(floorplate), floorIndex);
		float floorCost = evaluateFloorCost(sm.getUnits(floorplate), floorIndex);
		
		return floorValue - floorCost;
	}
	
	public float evaluateStackProfit(Point3D footprint, Stack<String> stack) {
		
		float profit = 0;
		
		for (int i = 0; i<stack.size(); i++) {			
			profit += evaluateFloorProfit(sm.getFloorplate(sm.getFootprintType(footprint), stack.get(i)), i+1);
		}
				
		return profit;
	}
			
	public float evaluateTotalProfit() {
		
		float profit = 0;
		
		for (Point3D footprint : sm.getFootprints()) {
			profit += evaluateStackProfit(footprint, sm.getStack(footprint));
		}
		
		return profit;
	}
	
	public float evaluateTotalCost() {
		
		float cost = 0;
		
		for (Point3D footprint : sm.getFootprints()) {
			for (int i = 0; i<sm.getStack(footprint).size(); i++) {
				cost += evaluateFloorCost(sm.getUnits(sm.getFloorplate(sm.getFootprintType(footprint),
						sm.getStack(footprint).get(i))), i+1);
			}				
		}
		
		return cost;
	}
	
	private float evaluateTotalArea() {
		
		float area = 0f;
		
		for (Point3D footprint : sm.getFootprints()) {
			area += sm.getFootprintArea(footprint) * sm.getStack(footprint).size();
		}
						
		return area;
	}
	
	public int evaluateTotalDelta() {
		
		int delta = 0;
		
		for (String unitType : sm.getUnitTypes()) {
			
			int curr = evaluateTotalUnitCount(unitType);
			int count = sm.unitCounts.get(unitType);
			
			delta += curr - count;
		}
		
		return delta;
	}
	
	public int evaluateTotalUnitCount(String unitType) {
		
		int count = 0;
		
		if (sm != null) {
			
			for (Point3D footprint : sm.getFootprints()) {
				
				String footprintType = sm.getFootprintType(footprint);
				
				for (String floorplateType : sm.getStack(footprint)) {	
					
					Point3D floorplate = sm.getFloorplate(footprintType, floorplateType);
					List<Polygon3D> units = sm.getUnits(floorplate);
					
					if (units != null) {
						for (String type : sm.getUnitTypes(footprintType, floorplateType)) {
							if (type.equals(unitType)) {
								count++;
							}
						}
					}
				}
			}
		}
						
		return count;
	}
	
	/*
	 * =========================================================
	 * TESTS
	 * =========================================================
	 */
	
	private boolean isPushFloorValid(Point3D from , Point3D to) {
		
		float heightMax =  maxHeight;
		float heightTo = floorToCeilingHeight * (sm.getStack(to).size() + 1);
		
		float areaMax = maxArea;
		float areaFrom = from == null ? 0 : sm.getFootprintArea(from);
		float areaTo = to == null ? 0 : sm.getFootprintArea(to);	
		float areaTotal = evaluateTotalArea() - areaFrom + areaTo;
				
		return (from == null || sm.getStack(from).size() > 0) && (heightTo < heightMax && areaTotal < areaMax);
	}
	
	
	/*
	 * =========================================================
	 * RUN
	 * =========================================================
	 */
	
	private String swap(String footprintType, String floorplateType) {
		
		List<String> selection = new ArrayList<>();
		List<String> floorplateTypes = new ArrayList<>(sm.getFloorplateTypes(footprintType));
		
		for (String type : floorplateTypes) {

			int count = 0;
			
			for (String unitType : sm.getUnitTypes(footprintType, type)) {
				count += sm.unitCounts.get(unitType) - evaluateTotalUnitCount(unitType);
			}
			
			for (int i=0; i<count; i++) {
				selection.add(type);
			}
		}
		
		String mutation = selection.isEmpty() 
			? null
			: selection.get(new Random().nextInt(selection.size()));
		
		return mutation;
	}
	
	private void sort() {
		
		for (Point3D footprint : sm.getFootprints()) {

			for (int i=sm.getStack(footprint).size()-1; i>=0; i--) {
				if (sm.getStack(footprint).get(i) == null) {
					sm.removeFloor(footprint, i);
				}
			}
						
			for (int s=0; s<sm.getStack(footprint).size(); s++) {
				for (int t=s+1; t<sm.getStack(footprint).size(); t++) {
										
					Stack<String> clone = sm.cloneStack(footprint);
																									
					sm.swapFloors(footprint, s, t);
					
					try {
						Thread.sleep(sleep);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					float vc = evaluateStackProfit(footprint, clone);
					float vt = evaluateStackProfit(footprint, sm.getStack(footprint));
					
					if (vc > vt) {
						sm.setStack(footprint, clone);
					}
				}
			}
		}
	}
		
	private void mutate() {
		
		for (int i = new Random().nextInt(sm.getPointers().size()); i>=0 ; i--) {
			
			int[] pull = sm.getPointers().get(new Random().nextInt(sm.getPointers().size()));
			int[] push = sm.getPointers().get(new Random().nextInt(sm.getPointers().size()));
			
			if (push[0] != pull[0]) {
				
				if (isPushFloorValid(sm.getFootprints().get(push[0]), sm.getFootprints().get(pull[0]))) {

					sm.removeFloor(sm.getFootprints().get(push[0]), push[1]);
					sm.pushFloor(sm.getFootprints().get(pull[0]), null);
					
					try {
						Thread.sleep(sleep);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					i--;
				}
			}
			
			sm.point();
		}
		
		for (int[] pointer : sm.getPointers()) {
			if (sm.getStack(sm.getFootprints().get(pointer[0])).get(pointer[1]) == null) {
				sm.getStack(sm.getFootprints().get(pointer[0])).set(pointer[1], 
						swap(sm.getFootprintType(sm.getFootprints().get(pointer[0])), null));
			}
		}
		
		sort();
	}
		
	public void initialize(StackManager sm) {
		
		this.sm = sm;
		this.counts = new HashMap<>();
		this.value = 0;
		
		sm.initialize();		
				
		boolean valid = true;
		
		while (valid) {
			
			valid = false;
			
			for (Point3D footprint :  sm.getFootprints()) {	
				if (isPushFloorValid(null, footprint)) {
					sm.pushFloor(footprint, null);
					
					valid = true;
				}			
			}
		}
		
		for (String unitType : sm.getUnitTypes()) {
			this.counts.put(unitType, 0);
		}

		print();
	}
	
	private void update() {

		this.value =  evaluateTotalProfit();
		
		for (String unitType : sm.getUnitTypes()) {
			this.counts.put(unitType, evaluateTotalUnitCount(unitType));
		}
		
		sm.flag();
	}
			
	public void evaluate(StackManager sm) {
		
		initialize(sm);
				
		SortedMap<Float, Map<Point3D, Stack<String>>> elite = new TreeMap<>();
		
		int cross = 20;
		int mutate = 50;
		int pool = 100;
		
		boolean flag = false;
		
		elite.put(evaluateTotalProfit(), sm.saveState());
		
		while(!flag) {
						
			SortedMap<Float, Map<Point3D, Stack<String>>> front = new TreeMap<>(elite);
			
			elite.clear();
			
			System.out.println();
			System.out.println();
			System.out.println();
			System.out.println(" ---> new front : " + front.size());
			System.out.println();
			System.out.println();
						
			for (Map.Entry<Float, Map<Point3D, Stack<String>>> f : front.entrySet()) {
				
				sm.restoreState(f.getValue());
								
				float currValue = f.getKey();
				boolean currFlag = false;
				
				if (currValue != 0) update();
				
				System.out.println("new pool : " + currValue);
												
				for (int p = pool ; p > 0 ; p--) {
						
					sm.saveStacks();
					
					mutate();							
											
					float value = evaluateTotalProfit();
					
					if (value > currValue) {											
						elite.put(value, sm.saveState());
						currFlag = true;			
					}				

					try {
						Thread.sleep(sleep);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					sm.restoreStacks();				
				}
								
				if (!currFlag) {					
					elite.put(f.getKey(), f.getValue());
				}
			}
			
			List<Float> values = new ArrayList<>(elite.keySet());
			Collections.reverse(values);
			
			if (elite.keySet().equals(front.keySet())) {	
				
				flag = true;
				
			} else {			
				
				for (int i=values.size()-1; i>=cross; i--) {
					elite.remove(values.get(i));
				}
			}
		} 
		
		List<Float> values = new ArrayList<>(elite.keySet());
		Collections.reverse(values);	
		
		sm.restoreState(elite.get(values.get(0)));
		
		update();
								
		print();
	}
										
	private void print() {
		
		System.out.println();
		System.out.println();
		System.out.println("total area: " + evaluateTotalArea() + " / " + maxArea);
		System.out.println("total value: " + evaluateTotalProfit());
		System.out.println("total delta: " + evaluateTotalDelta());
		System.out.println();
		for (String unitType : sm.getUnitTypes()) {
			System.out.println(unitType + " : " + evaluateTotalUnitCount(unitType) + " / " + sm.unitCounts.get(unitType));
		}
		for (Point3D footprint : sm.getFootprints()) {
			System.out.println(sm.getFootprintType(footprint) + " height : " + sm.getStack(footprint).size() * floorToCeilingHeight);
		}
		System.out.println();
	}
}
