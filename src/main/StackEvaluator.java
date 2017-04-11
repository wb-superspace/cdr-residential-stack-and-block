package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import cdr.geometry.primitives.Polygon3D;

public class StackEvaluator {
	
	public float floorCost = 100f;
	public float floorCostMultiplier = 0.01f;
	
	public float unitValueThreshold = 2700000f;
	
	public float unitPremiumFloorMultiplier = 0.03f;
//	public float unitPremiumViewMultiplier = 0.005f;
//	public float unitPremiumDaylightMultiplier = 0.005f;
	
	float amenetiesPercentage = 0.2f;
	
	float netSiteArea = 19483f;
	float netBuildingArea = 81747f;
	
	public float targetFAR = netBuildingArea / netSiteArea;
	
	public float floorToCeilingHeight = 4f;
	
	
	
	private void initialize(StackManager sm, BlockManager bm) {
		
		float totatSiteArea = 0;
		float totalFloorArea = 0;
		
		for (Polygon3D siteBoundary : sm.getBoundaries()) {
			totatSiteArea += siteBoundary.area();
		}
		
		for (String type : sm.getFootprintTypes()) {
			for (Polygon3D footprint : sm.getFootprints(type)) {
				sm.getStack(footprint).clear();
			}
		}
		
		boolean isNotTargetFAR = true;
		
		while (isNotTargetFAR) {
			
			isNotTargetFAR = false;
			
			for (String type : sm.getFootprintTypes()) {
				for (Polygon3D footprint : sm.getFootprints(type)) {
						
					float currFAR = (totalFloorArea + footprint.area()) / totatSiteArea;
					
					if (currFAR > targetFAR) {
						continue;
					}

					sm.getStack(footprint).push(new ArrayList<>());

					totalFloorArea += footprint.area();
					isNotTargetFAR = true;	
				}
			}
		}
		
		System.out.println("initial FAR: " + totalFloorArea / totatSiteArea);
	}
		
	public float evaluateStack(String type, Polygon3D footprint, Stack<List<Unit>> stack) {
		
		float premium = 0;
		int floor = 1;
		
		for (List<Unit> floorMix : stack) {
			
			float cost = floorCost * footprint.area();
			cost += cost * floorCostMultiplier * floor;
			
			for (Unit unit : floorMix) {
				
				float area = footprint.area() / floorMix.size();
				float value = area * unit.getProperty("VAL");

				value += value * unitPremiumFloorMultiplier * floor;
				
				if (value > unitValueThreshold) {
					value = unitValueThreshold;
				}
				
				premium += (value - cost);
				
//				System.out.println(type + " " +
//						floorMix + " " +
//						floor + " unit premium: " +
//						(value - cost));
			}
			
			floor++;
		}
		
//		System.out.println("stack premium " + premium);
		
		return premium;
	}
	
	public float evaluatePremium(StackManager sm) {
		
		float premium = 0;
		
		for (String type : sm.getFootprintTypes()) {
			for (Polygon3D footprint : sm.getFootprints(type)) {
				premium += evaluateStack(type, footprint, sm.getStack(footprint));
			}
		}
		
		System.out.println("total premium: " + premium);
		
		return premium;
	}
	
	public float evaluateFAR(StackManager sm)  {
		
		float totatSiteArea = 0;
		float totalFloorArea = 0;
		
		for (Polygon3D siteBoundary : sm.getBoundaries()) {
			totatSiteArea += siteBoundary.area();
		}
		
		for (String type : sm.getFootprintTypes()) {
			for (Polygon3D footprint : sm.getFootprints(type)) {
				totalFloorArea += footprint.area() * sm.getStack(footprint).size(); 
			}
			
		}
		
		float FAR = totalFloorArea / totatSiteArea;
		
//		System.out.println("FAR: " + FAR);
		
		return FAR;
	}
		
	private void buildFloorkMix(String type, Polygon3D footprint, Stack<List<Unit>> stack) {
				
		List<List<Unit>> floorMix = new ArrayList<>();
		
		
		for (List<Unit> floor : stack) {
			floor.clear();
			for (Unit unit : floorMix.get(0)) {
				floor.add(unit);
			}
		}
		
		float currPremium = evaluateStack(type, footprint, stack);
		
		for (List<Unit> floor : stack) {
			
			List<Unit> currFloor = new ArrayList<>(floor);

			for (List<Unit> testFloor : floorMix) {
				
				floor.clear();
				for (Unit unit : testFloor) {
					floor.add(unit);
				}
				
				float testPremium = evaluateStack(type, footprint, stack);
				
				if (testPremium > currPremium) {
					currPremium = testPremium;
					currFloor = new ArrayList<>(floor);
				} else {
					floor.clear();
					for (Unit unit : currFloor) {
						floor.add(unit);
					}
				}
			}
		}
	}
	
	private void buildFloorkMix(StackManager sm) {
		
		for (String type : sm.getFootprintTypes()) {
			for (Polygon3D footprint : sm.getFootprints(type)) {
				buildFloorkMix(	type,footprint,	sm.getStack(footprint));
			}
		}
	}
	
	public void buildStackMix(StackManager sm, BlockManager bm) {
		
		initialize(sm, bm);
		buildFloorkMix(sm);
		
		boolean isChanged = true;
		
		outerLoop:
		while (isChanged) {
						
			float currPremium = evaluatePremium(sm);
			
			isChanged = false;
			
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			for (String type : sm.getFootprintTypes()) {
				
				for (Polygon3D footprint : sm.getFootprints(type)) {
					
					if (!sm.getStack(footprint).isEmpty()) {
						
						for (String other : sm.getFootprintTypes()) {
							
							if (!other.equals(type)) {
								
								List<Unit> floor = sm.popFloor(type);
								
								int count = 0;
								
								while(evaluateFAR(sm) <= targetFAR) {
									sm.pushFloor(other, floor);
									count++;
									
									try {
										Thread.sleep(20);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								
								count--;
								sm.popFloor(other);
								
								buildFloorkMix(sm);
								
								float testPremium = evaluatePremium(sm);
								
								if (testPremium > currPremium) {
									currPremium = testPremium;
									isChanged = true;
									continue outerLoop;
								} else {
									sm.pushFloor(type, floor);
									
									while (count>0) {
										sm.popFloor(other);
										count--;
										
										try {
											Thread.sleep(20);
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								}
							}
						}
					}
				}								
			}
		}
		
		System.out.println();
		System.out.println("final premium: " + evaluatePremium(sm));
		System.out.println("final FAR: " + evaluateFAR(sm));
		System.out.println();
		for (String type : sm.getFootprintTypes()) {
			System.out.println();
			System.out.println(type);
			for (Polygon3D footprint : sm.getFootprints(type)) {
				for (List<Unit> floorMix : sm.getStack(footprint)) {
					System.out.println(floorMix);
				}
			}
		}
	}
}
