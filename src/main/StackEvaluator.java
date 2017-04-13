package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import cdr.geometry.primitives.Polygon3D;

public class StackEvaluator {
	
	public float floorCost = 300f;
	public float floorCostMultiplier = 0.01f;
	
	public float unitValueThreshold = 2700000f;	
	public float unitPremiumFloorMultiplier = 0.03f;
	
	float maxArea = 81747f;
	float maxHeight = 170f;
	
	public float floorToCeilingHeight = 4f;
	
	private StackManager sm;
	private BlockManager bm;
	
	private float evaluateFloorValue(Polygon3D footprint, List<String> floor, float floorIndex) {
		
		float totalFloorCost = floorCost * footprint.area();
		totalFloorCost += totalFloorCost * floorCostMultiplier * floorIndex;
		
		float totalFloorValue = 0f;
		
		for (String unitType : floor) {
			
			float unitArea = bm.getUnit(unitType).area;
			float unitValue = unitArea * bm.getUnit(unitType).value;

			unitValue += unitValue * unitPremiumFloorMultiplier * floorIndex;
			
			if (unitValue > unitValueThreshold) {
				unitValue = unitValueThreshold;
			}
			
			totalFloorValue += unitValue;
		}
		
		return totalFloorValue - totalFloorCost;
	}
	
	private float evaluateStackValue(Polygon3D footprint) {
		
		float value = 0;
		
		for (int i = 0; i<sm.getStack(footprint).size(); i++) {
			value += evaluateFloorValue(footprint, sm.getStack(footprint).get(i), i+1);
		}
				
		return value;
	}
			
	private float evaluateTotalValue() {
		
		float value = 0;
		
		for (String footprintType : sm.getFootprintTypes()) {
			for (Polygon3D footprint : sm.getFootprints(footprintType)) {
				value += evaluateStackValue(footprint);
			}
		}
		
		System.out.println("total value: " + value);
		
		return value;
	}
	
	private float evaluateTotalArea() {
		
		float area = 0;
		
		for (String footprintType : sm.getFootprintTypes()) {
			for (Polygon3D footprint : sm.getFootprints(footprintType)) {
				area += footprint.area() * sm.getStack(footprint).size();
			}
		}
		
		return area;
	}
	
	private int evaluateTotalUnitCount(String unitType) {
		
		int count = 0;
		
		for (String footprintType : sm.getFootprintTypes()) {
			for (Polygon3D footprint : sm.getFootprints(footprintType)) {
				for (List<String> floor : sm.getStack(footprint)) {
					for (String unit : floor) {
						if (unit == unitType) {
							count ++;
						}
					}
				}
			}
		}
		
		return count;
	}
			
	private void print() {
	
	System.out.println();
	System.out.println();
	System.out.println("total area: " + evaluateTotalArea());
	System.out.println("total value: " + evaluateTotalValue());
	System.out.println();
	for (String unitType : bm.getUnitTypes()) {
		System.out.println(unitType + " : " + evaluateTotalUnitCount(unitType));
	}
	System.out.println();
	for (String footprintType : sm.getFootprintTypes()) {
		System.out.println();
		System.out.println(footprintType);
		for (Polygon3D footprint : sm.getFootprints(footprintType)) {
			System.out.println("------");
			System.out.println("height : " + sm.getStack(footprint).size() * floorToCeilingHeight);
			for (List<String> floorMix : sm.getStack(footprint)) {
				System.out.println(floorMix);
			}
		}
	}
}
	
	private boolean fitUnit(Polygon3D footprint, List<String> floor, String unit) {
		
		float area = footprint.area() - bm.getUnit(unit).area;
		
		for (String floorUnit : floor) {
			area -= bm.getUnit(floorUnit).area;
		}
		
		if (area > 0) {
			floor.add(unit);
		}
		
		return area > 0;
	}
	
	private void initialize(StackManager sm, BlockManager bm) {
		
		this.bm = bm;
		this.sm = sm;
		
		Polygon3D footprint = sm.getFootprints(sm.getFootprintTypes().iterator().next()).get(0);
		
		List<String> units = new ArrayList<>();
		for (String unitType : bm.getUnitTypes()) {
			for (int i = 0; i<bm.getUnit(unitType).count; i++) {
				units.add(unitType);
			}
		}
		
		fillStack(footprint, units);	
	}
	
	private void fillStack(Polygon3D footprint, List<String> units) {
		
		Stack<List<String>> stack = sm.getStack(footprint);
		Stack<String> fill = new Stack<>();
		for (String unit : units) {
			fill.push(unit);
		}
		
		stack.clear();
		stack.push(new ArrayList<>());
		while (!fill.isEmpty()) {
			
			String unit = fill.pop();
			List<String> floor = stack.peek();
			
			if (!fitUnit(footprint, floor, unit)) {
				stack.push(new ArrayList<>());
				stack.peek().add(unit);
			}
		}
		
		print();
	}
	
	public void evaluate(StackManager sm, BlockManager bm) {
		
		initialize(sm, bm);
	}
	
//	private boolean isUnitMixValid(Polygon3D footprint, List<String> units) {
//	
//	float area = footprint.area();
//	
//	for (String unitType : units) {
//		area -= bm.getUnit(unitType).area;
//	}
//	
//	return area >= 0;
//}
//
//private boolean isPushFloorValid(Polygon3D from , Polygon3D to) {
//	
//	float height = floorToCeilingHeight * (sm.getStack(to).size() + 1);
//	float area = evaluateTotalArea() - from.area() + to.area();
//	
//	return (height < maxHeight && area < maxArea);
//}
//
//private boolean isStackMixValid() {
//	
//	for (String unitType : bm.getUnitTypes()) {
//		if (bm.getUnit(unitType).count < evaluateTotalUnitCount(unitType)) {
//			return false;
//		}
//	}
//	
//	return true;
//}
//
//	private List<List<String>> buildUnitCombinatrix(String footprintType, Polygon3D footprint, List<List<String>> combinations) {
//		
//		List<List<String>> combinatrix = new ArrayList<>();
//		
//		for (List<String> combination : combinations) {
//			
//			List<List<String>> tests = new ArrayList<>();
//			
//			for (String unitType : bm.getUnitTypes()) {
//												
//				List<String> test = new ArrayList<>(combination);
//				test.add(unitType);
//						
//				if (isUnitMixValid(footprint, test)) {
//					tests.add(test);
//				}			
//			}
//			
//			if (tests.isEmpty()) {
//				combinatrix.add(combination);
//			} else {
//				combinatrix.addAll(buildUnitCombinatrix(footprintType, footprint, tests));
//			}
//		}
//		
//		return combinatrix;
//	}
//	
//	private void buildStack(Polygon3D footprint) {
//		
//		List<List<String>> combinatrix = new ArrayList<>();
//		for (String unitType : bm.getUnitTypes()) {
//			combinatrix.add(Arrays.asList(unitType));
//		}
//		
//		combinatrix = buildUnitCombinatrix(sm.getFootprintType(footprint), footprint, combinatrix);
//				
//		Stack<List<String>> stack = sm.getStack(footprint);
//		
//		for (int i = 0; i<stack.size(); i++) {
//			for (List<String> combination : combinatrix) {
//				
//				float v1 = evaluateFloorValue(footprint, combination, i+1);
//				float v2 = evaluateFloorValue(footprint, stack.get(i), i+1);
//				
//				if (v1 > v2) {
//					stack.set(i, combination);
//				}
//			}
//		}
//	}
//	
////	private void buildStacks() {
////
////		for (String footprintType : sm.getFootprintTypes()) {
////			for (Polygon3D footprint : sm.getFootprints(footprintType)) {
////				buildStack(footprint);
////			}
////		}
////		
////		swapStacks();
////	}
//	
////	private void swapStacks() {
////		
////		Map<String, Integer> count = new HashMap<>();
////		Map<String, Integer> max = new HashMap<>();
////		
////		Map<Polygon3D, List<List<String>>> combinatrices = new HashMap<>();
////		
////		for (String footprintType : sm.getFootprintTypes()) {
////			for (Polygon3D footprint : sm.getFootprints(footprintType)) {
////
////				List<List<String>> combinatrix = new ArrayList<>();
////				for (String unitType : bm.getUnitTypes()) {
////					combinatrix.add(Arrays.asList(unitType));
////				}
////				
////				combinatrices.put(footprint, buildUnitCombinatrix(footprintType, footprint, combinatrix));
////			}
////		}
////		
////		for (String unitType : bm.getUnitTypes()) {
////			max.put(unitType, bm.getUnit(unitType).count);
////			count.put(unitType, evaluateTotalUnitCount(unitType));
////			
////			System.out.println(unitType + " " + count.get(unitType) + " / " + max.get(unitType));
////		}
////				
////		while (!isStackMixValid()) {
////					
////			float min = Float.MAX_VALUE;
////			
////			Integer fromIndex = null;
////			Polygon3D fromFootprint = null;
////			
////			List<String> toCombination = null;
////			
////			for (String unitType : bm.getUnitTypes()) {
////				
////				if (count.get(unitType) > max.get(unitType)) {
////					
////					for (String footprintType : sm.getFootprintTypes()) {
////						for (Polygon3D footprint : sm.getFootprints(footprintType)) {
////							
////							for (int i =0; i<sm.getStack(footprint).size(); i++) {
////								
////								List<String> floor = sm.getStack(footprint).get(i);
////								
////								if (floor.contains(unitType)) {
////									
////									float v1 = evaluateFloorValue(footprint, floor, i+1);
////									
////									List<List<String>> combinatrix = new ArrayList<>(combinatrices.get(footprint));
////									
////									loop:
////									for (int j = combinatrix.size()-1; j>=0; j--) {				
////										for (Map.Entry<String, Integer> mapEntry : count.entrySet()) {					
////											if (mapEntry.getValue() >= bm.getUnit(mapEntry.getKey()).count) {
////												if (combinatrix.get(j).contains(mapEntry.getKey())) {
////													combinatrix.remove(j);
////													continue loop;
////												}
////											}
////										}
////									}
////									
////									for (List<String> combination : combinatrices.get(footprint)) {
////										
////										if (!floor.equals(combination)) {
////											
////											float v2 = evaluateFloorValue(footprint, combination, i+1);
////											
////											if (v1 - v2 < min && v1 - v2 > 0) {
////												
////												fromIndex = i;
////												fromFootprint = footprint;
////												
////												toCombination = combination;
////											}
////										}
////									}
////								}
////							}
////						}
////					}
////				}
////			}
////			
////			if (fromIndex == null || fromFootprint == null) {
////				break;
////			}
////			
////			System.out.println(sm.getStack(fromFootprint).get(fromIndex) + " -> " + toCombination);
////			
////			
////			for (String unitType : sm.getStack(fromFootprint).get(fromIndex)) {
////				count.put(unitType, count.get(unitType) - 1);
////			}
////						
////			for (String unitType : toCombination) {
////				count.put(unitType, count.get(unitType) + 1);
////			}
////			
////			sm.getStack(fromFootprint).set(fromIndex, toCombination);
////		}
////	}
//	
//	private void buildStacks() {
//		
//		Map<String, Integer> counter = new HashMap<>();
//		Map<Polygon3D, List<List<String>>> combinatrices = new HashMap<>();
//		
//		for (String footprintType : sm.getFootprintTypes()) {
//			for (Polygon3D footprint : sm.getFootprints(footprintType)) {
//				for (List<String> floor : sm.getStack(footprint)) {
//					floor.clear();
//				}
//				
//				List<List<String>> combinatrix = new ArrayList<>();
//				for (String unitType : bm.getUnitTypes()) {
//					combinatrix.add(Arrays.asList(unitType));
//				}
//				
//				combinatrices.put(footprint, buildUnitCombinatrix(footprintType, footprint, combinatrix));
//			}
//		}
//		
//		for (String unitType : bm.getUnitTypes()) {
//			counter.put(unitType, 0);
//		}
//		
//		System.out.println(counter);
//				
//		for (String footprintType : sm.getFootprintTypes()) {
//			for (Polygon3D footprint : sm.getFootprints(footprintType)) {
//				
//				List<List<String>> combinatrix = new ArrayList<>();		
//				Stack<List<String>> stack = sm.getStack(footprint);
//				
//				for (List<String> c: combinatrices.get(footprint)) {
//					combinatrix.add(new ArrayList<>(c));
//				}
//												
//				for (int j = 0; j<stack.size(); j++) {
//					
//					loop:
//					for (int i = combinatrix.size()-1; i>=0; i--) {				
//						for (Map.Entry<String, Integer> mapEntry : counter.entrySet()) {					
//							if (mapEntry.getValue() >= bm.getUnit(mapEntry.getKey()).count) {
//								if (combinatrix.get(i).contains(mapEntry.getKey())) {
//									combinatrix.remove(i);
//									continue loop;
//								}
//							}
//						}
//					}
//				
//					for ( List<String> combination : combinatrix) {
//					
//						float v1 = evaluateFloorValue(footprint, combination, j+1);
//						float v2 = evaluateFloorValue(footprint, stack.get(j), j+1);
//						
//						if (v1 > v2) {
//							
//							for (String unitType : stack.get(j)) {
//								counter.put(unitType, counter.get(unitType) - 1);
//							}
//							
//							stack.set(j, combination);
//							
//							for (String unitType : stack.get(j)) {
//								counter.put(unitType, counter.get(unitType) + 1);
//							}
//						}
//					}
//				}	
//			}
//		}
//	}
//	
//	
////	private void initialize(StackManager sm, BlockManager bm) {
////		
////		this.sm = sm;
////		this.bm = bm;
////	
////		Polygon3D init = null;
////		List<List<String>> combinatrix = new ArrayList<>();
////		Map<String, Integer> counter = new HashMap<>();
////		
////		
////		for (String footprintType : sm.getFootprintTypes()) {
////			for (Polygon3D footprint : sm.getFootprints(footprintType)) {		
////				sm.getStack(footprint).clear();
////				init = footprint;
////			}
////		}
////				
////		for (String unitType : bm.getUnitTypes()) {
////			combinatrix.add(Arrays.asList(unitType));
////			counter.put(unitType, 0);
////		}
////		
////		combinatrix = buildUnitCombinatrix(sm.getFootprintType(init), init, combinatrix);
////		
////		while (combinatrix.size() > 0) {
////				
////			loop:
////			for (int i = combinatrix.size()-1; i>=0; i--) {				
////				for (Map.Entry<String, Integer> mapEntry : counter.entrySet()) {					
////					if (mapEntry.getValue() >= bm.getUnit(mapEntry.getKey()).count) {
////						if (combinatrix.get(i).contains(mapEntry.getKey())) {
////							combinatrix.remove(i);
////							continue loop;
////						}
////					}
////				}
////			}
////						
////			sm.pushFloor(init, new ArrayList<>());
////			
////			int i = sm.getStack(init).size();
////			
////			for ( List<String> combination : combinatrix) {
////				
////				float v1 = evaluateFloorValue(init, combination, i);
////				float v2 = evaluateFloorValue(init, sm.getStack(init).get(i-1), i);
////				
////				if (v1 > v2) {
////					
////					for (String unitType : sm.getStack(init).get(i-1)) {
////						counter.put(unitType, counter.get(unitType) - 1);
////					}
////					
////					sm.getStack(init).set(i-1, combination);
////					
////					for (String unitType : sm.getStack(init).get(i-1)) {
////						counter.put(unitType, counter.get(unitType) + 1);
////					}
////
////				}
////			}
////		}
////		
////		print();
////				
////		for (int i=0; i<sm.getStack(init).size(); i++) {
////			for (int j = i+1; j<sm.getStack(init).size(); j++) {
////				
////				float v1 = evaluateFloorValue(init, sm.getStack(init).get(i), i);
////				float v2 = evaluateFloorValue(init, sm.getStack(init).get(j), i);
////				
////				if (v1 > v2) {
////					sm.swapFloor(init, i, init, j);
////				}
////			}
////		}
////		
////		print();
////	}
////	
////	public void run(StackManager sm, BlockManager bm) {
////		
////		initialize(sm, bm);		
////	}
//	
//	private void initialize(StackManager sm, BlockManager bm) {
//		
//		this.sm = sm;
//		this.bm = bm;
//		
//		for (String type : sm.getFootprintTypes()) {
//			for (Polygon3D footprint : sm.getFootprints(type)) {
//				sm.getStack(footprint).clear();
//			}
//		}
//		
//		float area = 0f;
//		
//		boolean isNotMaxArea = true;
//		
//		while (isNotMaxArea) {
//			
//			isNotMaxArea = false;
//			
//			for (String footprintType : sm.getFootprintTypes()) {
//				for (Polygon3D footprint : sm.getFootprints(footprintType)) {
//										
//					if (area + footprint.area() < maxArea) {
//						if (floorToCeilingHeight * (sm.getStack(footprint).size() + 1) < maxHeight) {
//							sm.pushFloor(footprint, new ArrayList<>());
//							
//							area += footprint.area();
//							isNotMaxArea = true;
//						}
//					}
//				}
//			}
//		}
//	}
//	
//			
//	public void run(StackManager sm, BlockManager bm) {
//		
//		initialize(sm, bm);
//		
//		boolean isChanged = true;
//		
//		outerLoop:
//		while (isChanged) {
//						
//			float currValue = evaluateTotalValue();
//			
//			isChanged = false;
//			
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//			for (String footprintType : sm.getFootprintTypes()) {
//				
//				for (Polygon3D footprint : sm.getFootprints(footprintType)) {
//					
//					if (!sm.getStack(footprint).isEmpty()) {
//						
//						for (String otherType : sm.getFootprintTypes()) {
//														
//							for (Polygon3D otherFootprint : sm.getFootprints(otherType)) {
//																
//								if (!footprint.equals(otherFootprint)  && isPushFloorValid(footprint, otherFootprint)) {
//																		
//									int count = 0;
//									
//									while(evaluateTotalArea() <= maxArea) {
//										sm.pushFloor(otherFootprint, new ArrayList<>());
//										count++;
//										
//										try {
//											Thread.sleep(20);
//										} catch (InterruptedException e) {
//											// TODO Auto-generated catch block
//											e.printStackTrace();
//										}
//									}
//									
//									count--;
//									sm.popFloor(otherFootprint);
//									
//									buildStacks();
//									
//									float testValue = evaluateTotalValue();
//									
//									if (testValue > currValue) {
//										
//										currValue = testValue;
//										isChanged = true;
//										continue outerLoop;
//										
//									} else {
//										
//										sm.pushFloor(footprint, new ArrayList<>());
//										
//										while (count>0) {
//											sm.popFloor(otherFootprint);
//											count--;
//											
//											try {
//												Thread.sleep(20);
//											} catch (InterruptedException e) {
//												// TODO Auto-generated catch block
//												e.printStackTrace();
//											}
//										}
//									}
//									
//									buildStacks();
//								}
//							}	
//						}
//					}
//				}								
//			}
//		}
//		
//		print();
//	}
}
