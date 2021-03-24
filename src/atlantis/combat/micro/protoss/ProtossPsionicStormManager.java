package atlantis.combat.micro.protoss;

import atlantis.information.AMap;
import bwapi.TechType;
import java.util.List;
import atlantis.units.AUnit;
import atlantis.units.Select;
import atlantis.util.AtlantisUtilities;

public class ProtossPsionicStormManager {
	public static boolean useStorm(AUnit templar) {
		List<AUnit> enemyUnitsInAbilityRange = Select.enemy().combatUnits().inRadius(9, templar).listUnits();
		
		if (enemyUnitsInAbilityRange.isEmpty() || enemyUnitsInAbilityRange.size() < 5) {
			// There are either no units in range of the ability or not enough units for the ability to have a significant impact.
			return false;
		}
	
		AUnit bestTarget = chooseBestTarget(enemyUnitsInAbilityRange);
		
		if (bestTarget == null) {
			// No good targets in range. Enemy units are likely spread out and will not be very well affected by the storm.
			return false;
		} else {
			// We have a target within range that is surrounded by a group of other enemy units, making them a perfect target
			// for the storm. We return true to make sure this unit is not given any other orders.
			templar.useTech(TechType.Psionic_Storm, bestTarget);
			return true;
		}
	}
	
	private static AUnit chooseBestTarget(List<AUnit> enemyUnitsInRange) {
		AUnit currentBestTarget = null;
		int currentBestHits = 0;
		AUnit currentConsideration = null;
		
		
		for	(int i = 0; i < enemyUnitsInRange.size(); i++) {
			currentConsideration = enemyUnitsInRange.get(i);
			
			// A psionic storm hits in a radius of 3 units around the primary target. So in order to maximise the damage it can deal, 
			// we analyse a 3 unit radius around each target in range, counting the number of hits that a storm will achieve. A 
			// "good" storm typically hits around 6 units.
			List<AUnit> secondaryTargets = Select.enemy().combatUnits().inRadius(3, currentConsideration).listUnits();
			
			if (secondaryTargets.size() < 6) {
				continue;
			} else if (currentBestTarget == null) {
				currentBestTarget = currentConsideration;
				currentBestHits = secondaryTargets.size();
				continue;
			} else if (currentBestHits < secondaryTargets.size()) {
				currentBestTarget = currentConsideration;
				currentBestHits = secondaryTargets.size();
			}
		}
		return currentBestTarget;
	}
}
