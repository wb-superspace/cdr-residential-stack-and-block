package main;

import java.awt.datatransfer.FlavorTable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import com.sun.org.apache.bcel.internal.generic.NEW;

import cdr.geometry.primitives.Point;
import cdr.geometry.primitives.Point3D;
import cdr.geometry.primitives.Points;
import cdr.geometry.primitives.Polygon3D;


public class StackEvaluator {
	
	public float floorCost = 600f;
	public float floorCostMultiplier = 0.01f;
	
	public float unitValueThreshold = 2700000f;	
	public float unitPremiumFloorMultiplier = 0.03f;
	
	float maxArea = 81747f;
	float maxHeight = 170f;
	
	public float floorToCeilingHeight = 4f;
	
	private StackManager sm;
	
	private long sleep = 0l;
	
	
	/*
	 * =========================================================
	 * EVALUATIONS
	 * =========================================================
	 */
	
	private float evaluateFloorValue(List<Polygon3D> units, float floorIndex) {
				
		float totalFloorValue = 0f;
		float totalFloorCost = 0f;
		
		if (units == null) {
			return totalFloorValue;
		}
		
		for (Polygon3D unit : units) {
			
			String unitType = sm.getUnitType(unit);
						
			float unitArea = unitType != null ? sm.areas.get(unitType) :0f;  
			float unitValue = unitType != null ? unitArea * sm.values.get(unitType) : 0f;
			float unitCost = unitArea * floorCost;

			unitValue += unitValue * unitPremiumFloorMultiplier * floorIndex;
			unitCost += unitCost *  floorCostMultiplier * floorIndex;
			
			if (unitValue > unitValueThreshold) {
				unitValue = unitValueThreshold;
			}
			
			totalFloorValue += unitValue;
			totalFloorCost += unitCost;
		}
		
		return totalFloorValue - totalFloorCost;
	}
	
	private float evaluateStackValue(Point3D footprint, Stack<String> stack) {
		
		float value = 0;
		
		for (int i = 0; i<stack.size(); i++) {
			
			Point3D floorplate = sm.getFloorplate(sm.getFootprintType(footprint), stack.get(i));
			
			value += evaluateFloorValue(sm.getUnits(floorplate), i+1);
		}
				
		return value;
	}
			
	private float evaluateTotalValue() {
		
		float value = 0;
		
		for (Point3D footprint : sm.getFootprints()) {
			value += evaluateStackValue(footprint, sm.getStack(footprint));
		}

		return value;
	}
	
	private float evaluateTotalArea() {
		
		float area = 0f;
		
		for (Point3D footprint : sm.getFootprints()) {
			area += sm.getFootprintArea(footprint) * sm.getStack(footprint).size();
		}
				
		return area;
	}
	
	private int evaluateTotalDelta() {
		
		int delta = 0;
		
		for (String unitType : sm.getUnitTypes()) {
			
			int curr = evaluateTotalUnitCount(unitType);
			int count = sm.counts.get(unitType);
			
			delta += curr - count;
		}
		
		return delta;
	}
	
	private int evaluateTotalUnitCount(String unitType) {
		
		int count = 0;
		
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
				
		return count;
	}
	
	/*
	 * =========================================================
	 * TESTS
	 * =========================================================
	 */
	
	private boolean isPushFloorValid(Point3D from , Point3D to) {
		
		float height = floorToCeilingHeight * (sm.getStack(to).size() + 1);
		float area = evaluateTotalArea() - sm.getFootprintArea(from) + sm.getFootprintArea(to);
				
		return sm.getStack(from).size() > 0 && (height < maxHeight && area < maxArea);
	}
	
	private boolean isMutateValid(List<String> unitTypes) {
		
		for (String unitType : unitTypes) {
			if (evaluateTotalUnitCount(unitType) > sm.counts.get(unitType)) {
				return false;
			}
		}
		
		return true;
	}
	
	/*
	 * =========================================================
	 * RUN
	 * =========================================================
	 */
	
	private void initialize(StackManager sm) {
		
		this.sm = sm;
		
		sm.initialize();		
		
		float area = 0f;
		
		loop:
		while (true) {			
			for (Point3D footprint :  sm.getFootprints()) {			
				float fa = sm.getFootprintArea(footprint);
				if (area + fa >= maxArea) break loop;
				area += fa;				
				sm.getStack(footprint).push(null);
			}
		}

		print();
	}
		
	public void evaluate(StackManager stackManager) {
		
		initialize(stackManager);
				
		List<List<Integer>> pointers = new ArrayList<>();		
		List<List<String>> options = new ArrayList<>();				
		List<Point3D> footprints = new ArrayList<>(sm.getFootprints());
		
		int cross = 50;
		int mutate = 50;
		int pool = 200;
		
		SortedMap<Float, Map<Point3D, Stack<String>>> elite = new TreeMap<>();
		
		elite.put(evaluateTotalValue(), sm.saveState());
		
		while(!elite.isEmpty()) {
						
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
				
				System.out.println("new value : " + currValue);
				
				for (int l = 0; l<pool; l++) {
								
					for (Point3D push : sm.getFootprints()) {
						for (Point3D pull : sm.getFootprints()) {
							
							sm.saveStacks();
							
							int mut = mutate;
							int cot = new Random().nextInt(sm.getStack(push).size());
							
							for (int m = 0; m<cot; m++) {
								if (!push.equals(pull) && isPushFloorValid(push, pull)) {
									sm.getStack(pull).push(null);
									sm.getStack(push).pop();
								}
							}
							
							for (int m = 0 ; m<mut; m++) {
								
								pointers.clear();	
								options.clear();
								
								for (int j=0; j<footprints.size(); j++) {
									
									Stack<String> stack = sm.getStack(footprints.get(j));		
									List<String> option = new ArrayList<>(sm.getFloorplateTypes(sm.getFootprintType(footprints.get(j))));
									
									for (int k =0;k<stack.size(); k++) {

										List<Integer> pointer = Arrays.asList(j,k);
										
										pointers.add(pointer);			
										options.add(option);	
										
										if (stack.get(k) == null) {
											stack.set(k, option.get(0));
										}
									}	
								}

								int index = new Random().nextInt(pointers.size());
								
								List<String> option = options.get(index);			
								List<Integer> pointer = pointers.get(index);
								
								Point3D testFootprint = footprints.get(pointer.get(0));
								
								String testFloorplateType = option.get(new Random().nextInt(option.size()));
								String currFloorplateType = sm.getStack(testFootprint).get(pointer.get(1));
								
								if (!testFloorplateType.equals(currFloorplateType)) {
									
									sm.getStack(testFootprint).set(pointer.get(1), testFloorplateType);	
									
								} else {
									
									m--;
									
									continue;
								}
							}
							
							float value = evaluateTotalValue();
							
							if (value > currValue) {											
								elite.put(value, sm.saveState());	
							}
							
							try {
								Thread.sleep(sleep);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							
							
							sm.restoreStacks();	
						}
					}	
				}			
			}
			
			List<Float> values = new ArrayList<>(elite.keySet());
			Collections.reverse(values);
			
			for (int i=values.size()-1; i>=cross; i--) {
				elite.remove(values.get(i));
			}
		} 
						
		print();
	}
										
	private void print() {
		
		System.out.println();
		System.out.println();
		System.out.println("total area: " + evaluateTotalArea() + " / " + maxArea);
		System.out.println("total value: " + evaluateTotalValue());
		System.out.println("total delta: " + evaluateTotalDelta());
		System.out.println();
		for (String unitType : sm.getUnitTypes()) {
			System.out.println(unitType + " : " + evaluateTotalUnitCount(unitType));
		}
		for (Point3D footprint : sm.getFootprints()) {
			System.out.println(sm.getFootprintType(footprint) + " height : " + sm.getStack(footprint).size() * floorToCeilingHeight);
		}
		System.out.println();
	}
}
