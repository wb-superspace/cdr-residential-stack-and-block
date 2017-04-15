package main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.stream.Collectors;

import cdr.geometry.primitives.Polygon3D;
import de.erichseifert.gral.data.DummyData;

public class StackEvaluator {
	
	public float floorCost = 600f;
	public float floorCostMultiplier = 0.02f;
	
	public float unitValueThreshold = 2700000f;	
	public float unitPremiumFloorMultiplier = 0.03f;
	
	float maxArea = 81747f;
	float maxHeight = 170f;
	
	public float floorToCeilingHeight = 4f;
	
	private StackManager sm;
	private BlockManager bm;
	
	private long sleep = 0l;
	
	private Map<Polygon3D, Stack<List<String>>> restore;
	
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
	
	private float evaluateStackValue(Polygon3D footprint, List<List<String>> stack) {
		
		float value = 0;
		
		for (int i = 0; i<stack.size(); i++) {
			value += evaluateFloorValue(footprint, stack.get(i), i+1);
		}
				
		return value;
	}
			
	private float evaluateTotalValue() {
		
		float value = 0;
		
		for (String footprintType : sm.getFootprintTypes()) {
			for (Polygon3D footprint : sm.getFootprints(footprintType)) {
				value += evaluateStackValue(footprint, sm.getStack(footprint));
			}
		}
		
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
	
	private boolean isPushFloorValid(Polygon3D from , Polygon3D to) {
		
		float height = floorToCeilingHeight * (sm.getStack(to).size() + 1);
		float area = evaluateTotalArea() - from.area() + to.area();
		
		return (height < maxHeight && area < maxArea);
	}
	
	private boolean isSwapFloorValid(Polygon3D from, Polygon3D to) {
		
		float fromHeight = floorToCeilingHeight * (sm.getStack(from).size());
		float toHeight = floorToCeilingHeight * (sm.getStack(to).size());
		
		float area = evaluateTotalArea() - from.area() - to.area();
		
		float addFromHeight = (float) Math.ceil(to.area() / from.area());
		fromHeight += addFromHeight * floorToCeilingHeight;
		
		float addToHeight = (float) Math.ceil(from.area() / to.area());
		toHeight += addToHeight * floorToCeilingHeight;
		
		area += addToHeight * to.area() + addFromHeight * from.area();
		
		return (toHeight <= maxHeight && fromHeight <= maxHeight && area <= maxArea);
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
		this.restore = new HashMap<>();
		
		List<Polygon3D> footprints = new ArrayList<>();
		
		for (String footprintType : sm.getFootprintTypes()) {
			for (Polygon3D footprint : sm.getFootprints(footprintType)) {
				restore.put(footprint, new Stack<>());
				sm.getStack(footprint).clear();
				footprints.add(footprint);
			}
		}
		
		List<String> units = new ArrayList<>();
		for (String unitType : bm.getUnitTypes()) {
			for (int i = 0; i<bm.getUnit(unitType).count; i++) {
				units.add(unitType);
			}
		}
		
		int j = (int) Math.ceil((double)units.size() / (double)footprints.size());
				
		for (int i = 0; i<footprints.size(); i++) {
			
			int count = j;
			
			if ((i*j) + count >= units.size()) {
				count = units.size() - (i*j);
			}
			
			int f = (i*j);
			int t = (i*j) + count;
						
			List<String> chunk = units.subList(f,t);
			
			applyStack(footprints.get(i), chunk);
		}
	}
			
	private void applyStack(Polygon3D footprint, List<String> units) {
		
		Stack<List<String>> stack = sm.getStack(footprint);
		Stack<String> fill = new Stack<>();
		for (String unit : units) {
			fill.push(unit);
		}
		
		fill.sort(new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				
				float v1 = bm.getUnit(o1).value;
				float v2 = bm.getUnit(o2).value;
				
				if (v1 < v2) {
					return 1;
				} else if (v1 > v2) {
					return -1;
				} else {
					return 0;
				}
			}
		});
		
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
						
		for (int i=0; i<stack.size(); i++) {
			for (int j = i; j<stack.size(); j++) {
				
				List<String> fromUnitTypes = stack.get(i);
				List<String> toUnitTypes = stack.get(j);
								
				float currValue = evaluateFloorValue(footprint, fromUnitTypes, i+1) +
						 evaluateFloorValue(footprint, toUnitTypes, j+1);
								
				if (!fromUnitTypes.equals(toUnitTypes)) {
					
					for (int ii=0; ii<fromUnitTypes.size(); ii++) {
					
						for (int jj=0; jj<toUnitTypes.size(); jj++) {
							
							String fromUnitType = fromUnitTypes.get(ii);
							String toUnitType = toUnitTypes.get(jj);
							
							if (!fromUnitType.equals(toUnitType)) {
								
								List<String> testFrom = new ArrayList<>(fromUnitTypes);
								List<String> testTo = new ArrayList<>(toUnitTypes);
								
								testFrom.remove(ii);
								testTo.remove(jj);
								
								if (fitUnit(footprint, testFrom, toUnitType) &&
									fitUnit(footprint, testTo, fromUnitType)) {
									
									float testValue = evaluateFloorValue(footprint, testFrom, i+1) +
											 evaluateFloorValue(footprint, testTo, j+1);
																		
									if (testValue > currValue) {
										
										stack.set(i, testFrom);
										stack.set(j, testTo);
																				
										try {
											Thread.sleep(sleep);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
										
										currValue = testValue;
										
									}
								}
							}
						}
					}			
				}
			}
		}
	}
	
	private void applyStacks() {
		
		for (String footprintType : sm.getFootprintTypes()) {
			for (Polygon3D footprint : sm.getFootprints(footprintType)) {
				
				List<String> units = sm.getStack(footprint)
						.stream()
                        .flatMap(l -> l.stream())
                        .collect(Collectors.toList());
				
				applyStack(footprint, units);
			}
		}
	}
	
	private void saveStacks() {
		
		for (String footprintType : sm.getFootprintTypes()) {
			for (Polygon3D footprint : sm.getFootprints(footprintType)) {
				restore.get(footprint).clear();
				for (List<String> floor : sm.getStack(footprint)) {
					restore.get(footprint).add(new ArrayList<>(floor));
				}
			}
		}
	}
	
	private void restoreStacks() {
		
		for (String footprintType : sm.getFootprintTypes()) {
			for (Polygon3D footprint : sm.getFootprints(footprintType)) {
				sm.setStack(footprint, restore.get(footprint));
				restore.put(footprint, new Stack<>());
			}
		}
	}
	
	private Map<Polygon3D, Stack<List<String>>> saveState() {
		
		Map<Polygon3D, Stack<List<String>>> state = new HashMap<>();
		
		for (String footprintType : sm.getFootprintTypes()) {
			for (Polygon3D footprint : sm.getFootprints(footprintType)) {
				
				Stack<List<String>> stack = new Stack<>();
				
				for (List<String> floor : sm.getStack(footprint)) {
					stack.add(new ArrayList<>(floor));
				}
				
				state.put(footprint, stack);
			}
		}
		
		return state;
	}
	
	private void restoreState(Map<Polygon3D, Stack<List<String>>> state) {
		
		for (Map.Entry<Polygon3D, Stack<List<String>>> entry : state.entrySet()) {
			sm.setStack(entry.getKey(), entry.getValue());
		}
	}
	
	public void evaluate(StackManager sm, BlockManager bm) {
		
		initialize(sm, bm);
				
		outerLoop:
		while (true) { 
						
			float currValue = evaluateTotalValue();
			
			SortedMap<Float, Map<Polygon3D, Stack<List<String>>>> elite = new TreeMap<>();
						
			for (String footprintType : sm.getFootprintTypes()) {
			
				for (Polygon3D footprint : sm.getFootprints(footprintType)) {
					
					if (!sm.getStack(footprint).isEmpty()) {
																						
						for (String otherType : sm.getFootprintTypes()) {
														
							for (Polygon3D otherFootprint : sm.getFootprints(otherType)) {
																
								if (!footprint.equals(otherFootprint)) {
									
									int count = sm.getStack(footprint).size();
									
//									//sliding dump loop - needs to dump all permutations of the floors						
//									saveStacks();
//									
//									for (int i=count-1; i>=0; i--) {
//																				
//										for (int j=i-1; j>=0; j--) {
//										
//											List<List<String>> dump = new ArrayList<>();
//											
//											for (int k=j; k>=i; k--) {
//					
//												dump.add(sm.getStack(footprint).remove(i));
//												
//											}
//											
//										}
//									}
//									
//									restoreStacks();
									
									
									// single push loop
									if (isPushFloorValid(footprint, otherFootprint)) {
																				
										for (int i=0; i<count; i++) {
																																												
											saveStacks();
																												
											List<String> floor = sm.getStack(footprint).get(i);
														
											try {
												Thread.sleep(sleep);
											} catch (InterruptedException e) {
												e.printStackTrace();
											}
											
											sm.getStack(footprint).remove(i);
											sm.pushFloor(otherFootprint, floor);
											
											applyStacks();
											
											float testValue = evaluateTotalValue();
																							
											if (testValue > currValue) {					
												elite.put(testValue, saveState());
												
											}
											
											try {
												Thread.sleep(sleep);
											} catch (InterruptedException e) {
												e.printStackTrace();
											}
											
																								
											restoreStacks();
										}
									}
									
									
									//dump loop									
									saveStacks();
									
									for (int i=count-1; i>=0; i--) {
																				
										if (isPushFloorValid(footprint, otherFootprint)) {
											
											List<String> floor = sm.getStack(footprint).get(i);
											
											try {
												Thread.sleep(sleep);
											} catch (InterruptedException e) {
												e.printStackTrace();
											}
											
											sm.getStack(footprint).remove(i);
											sm.pushFloor(otherFootprint, floor);
											
											applyStacks();
											
											float testValue = evaluateTotalValue();
																							
											if (testValue > currValue) {					
												elite.put(testValue, saveState());	
											}
										}
									}
									
									restoreStacks();	
									
									
									// swap loop
									if (isSwapFloorValid(footprint, otherFootprint)) {
										
										swapLoop:
										for (int i=0; i<sm.getStack(footprint).size(); i++) {
											for (int j=0; j<sm.getStack(otherFootprint).size(); j++) {
																										
												saveStacks();
																																		
												if (sm.getStack(footprint).size()-1 < i) {		
													break swapLoop;
												
												} else if(sm.getStack(otherFootprint).size()-1 < j) {										
													continue swapLoop;
												
												}
												
												Set<String> s1 = new HashSet<>(sm.getStack(footprint).get(i));
												Set<String> s2 = new HashSet<>(sm.getStack(otherFootprint).get(j));
												
												if (s1.equals(s2))  {													
													continue swapLoop;
												}
												
												try {
													Thread.sleep(sleep);
												} catch (InterruptedException e) {
													e.printStackTrace();
												}
													
												sm.swapFloor(footprint, i, otherFootprint, j);
												
												applyStacks();
												
												float testValue = evaluateTotalValue();
												
												if (testValue > currValue) {									
													elite.put(testValue, saveState());
													
												}
												
												try {
													Thread.sleep(sleep);
												} catch (InterruptedException e) {
													e.printStackTrace();
												}
												
													
												restoreStacks();
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			List<Float> values = new ArrayList<>(elite.keySet());
			
			if (values.isEmpty()) {				
				break outerLoop;
			}
			
			System.out.println("increased value : " + currValue + " -> " + values.get(values.size()-1));
						
			restoreState(elite.get(values.get(values.size()-1)));
		}
		
		print();
	}		
}
