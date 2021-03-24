package atlantis.combat.micro.terran;

import atlantis.information.AMap;
import atlantis.units.AUnit;
import atlantis.units.Select;
import atlantis.util.AtlantisUtilities;
import bwta.Chokepoint;

/**
 *
 * @author Rafal Poniatowski <ravaelles@gmail.com>
 * Updated and edited by Edward Buckle <psyeb4@nottingham.ac.uk>
 */
public class TerranSiegeTankManager {
	private static int tankThreatValue = 3;

    public static boolean update(AUnit tank) {
        if (!tank.isInterruptible()) {
            tank.setTooltip("Can't interrupt");
            return true;
        }
        
        // =========================================================
        
        AUnit nearestAttackableEnemy = Select.enemy().combatUnits().canBeAttackedBy(tank).nearestTo(tank);
        Select<AUnit> allEnemiesWithinRange = Select.enemy().combatUnits().canBeAttackedBy(tank);      
        Select<?> allFriendlyTanksWithinRange = Select.ourTanks().inRadius(14, tank);
        
        double distanceToEnemy = nearestAttackableEnemy != null ? tank.distanceTo(nearestAttackableEnemy) : -1;
        
        if (tank.isSieged()) {
            return shouldTankUnsiege(tank, nearestAttackableEnemy, distanceToEnemy, allEnemiesWithinRange, allFriendlyTanksWithinRange);
        }
        else {
            return shouldTankSiege(tank, nearestAttackableEnemy, distanceToEnemy, allEnemiesWithinRange, allFriendlyTanksWithinRange);
        }
    }

    // =========================================================
    
    /**
     * Sieged
     */
    private static boolean shouldTankUnsiege(AUnit tank, AUnit enemy, double distanceToEnemy, Select<AUnit> allEnemiesWithinRange, Select<?> allFriendlyTanksWithinRange) {
        if (enemy == null || distanceToEnemy < 0 || distanceToEnemy >= 14) {
            tank.setTooltip("Considers unsiege");
            
            if (tank.getSquad().isMissionDefend() && (enemy == null || distanceToEnemy >= 14)) {
            	return false;
            }
            
            if (distanceToEnemy < 0 && (enemy != null && enemy.isMeleeUnit())) {
            	// There is a melee enemy unit attacking our tank, so it cannot fire back. It should unsiege and retreat.
            	tank.unsiege();
            	tank.setTooltip("Unsiege");
            	return true;
            }
            
            if (!tank.getSquad().isMissionDefend() && allFriendlyTanksWithinRange.count() >= 3) {
            	// The closest enemy unit is currently out of range and the tank is not set to defend, so we unsiege to chase.
            	// Will only give chase if there are allied tanks nearby to potentially assist. The enemy could be leading us into
            	// a trap by luring our tanks forwards.
                tank.unsiege();
                tank.setTooltip("Unsiege");
                return true;
            }
        } else if (calculateThreat(tank, allEnemiesWithinRange) >= (allFriendlyTanksWithinRange.count() * tankThreatValue)) {
        	// The "threat value" of all the nearby enemies to this tank is greater than the combined strength of all the
        	// nearby friendly tanks, so the tank should unsiege to retreat.
        	return true;
        }
        return false;
    }

    /**
     * Not sieged
     */
    private static boolean shouldTankSiege(AUnit tank, AUnit nearestAttackableEnemy, double distanceToEnemy, Select<AUnit> allEnemiesWithinRange, Select<?> allFriendlyTanksWithinRange) {
        
        // === Siege on hold =======================================
        
        // If tank is holding position, siege
        if (tank.isHoldingPosition() && canSiegeHere(tank)) {
            tank.siege();
            tank.setTooltip("Hold & siege");
            return true;
        }
        
        // === Enemy is BUILDING ========================================
        
        if (nearestAttackableEnemy != null) {

            // === Enemy is COMBAT UNIT ========================================
            
            if (Select.ourCombatUnits().inRadius(10, tank).count() >= 4) {
                return nearestEnemyIsUnit(tank, nearestAttackableEnemy, distanceToEnemy);
            }
            
            // Enemy is BUILDING
            else if (nearestAttackableEnemy.isBuilding()) {
                return nearestEnemyIsBuilding(tank, nearestAttackableEnemy, distanceToEnemy);
            } 
        }
        
        return false;
    }
    
    // =========================================================
    
    private static boolean nearestEnemyIsBuilding(AUnit tank, AUnit nearestAttackableEnemy, double distanceToEnemy) {
        if (distanceToEnemy <= 10.3) {
            tank.siege();
            tank.setTooltip("Siege - building");
            return true;
        }

        return false;
    }

    private static boolean nearestEnemyIsUnit(AUnit tank, AUnit enemy, double distanceToEnemy) {
        
        // Don't siege when enemy is too close
        if (distanceToEnemy < 5) {
            tank.setTooltip("Dont siege");
            return false;
        }
        
        // =========================================================
        
        if (distanceToEnemy < 14) {
            if ((AtlantisUtilities.rand(1, 100) < 8 || enemy.getType().isDangerousGroundUnit()) && canSiegeHere(tank)) {
                tank.siege();
                tank.setTooltip("Better siege");
                return true;
            }
        }

        if (distanceToEnemy <= 10.8 && canSiegeHere(tank)) {
            tank.siege();
            tank.setTooltip("Siege!");
            return true;
        }

        return false;
    }
    
    // =========================================================
    
    private static boolean canSiegeHere(AUnit tank) {
        Chokepoint choke = AMap.getNearestChokepoint(tank.getPosition());
        if (choke == null) {
            return true;
        }
        else {
            return tank.distanceTo(choke.getCenter()) > 1 || choke.getWidth() / 32 > 3.5;
        }
    }

    private static int calculateThreat(AUnit tank, Select<AUnit> enemiesInRange) {
    	if (enemiesInRange.canBeAttackedBy(tank) == null && enemiesInRange.canAttack(tank) != null) {
    		// There is a flying unit in range that the tanks cannot attack but the unit has a method of 
    		// attacking and destroying out tanks. This is reason to unsiege and retreat.
    		return Integer.MAX_VALUE;
    	}
    	
    	// This calculation could potentially be far more complex, taking into account specific unit counters and high ground etc but
    	// this would be non-exhaustive. After testing, the method below allows tanks to retreat when faced with overwhelming odds, 
    	// treating melee units as less of a threat due to their inability to fight back. This ultimately results in cautious 
    	// behaviour, which is perfect for Timmy's personality.
    	return enemiesInRange.melee().count() + ((enemiesInRange.count() - enemiesInRange.melee().count()) * 2); 
    }
}
