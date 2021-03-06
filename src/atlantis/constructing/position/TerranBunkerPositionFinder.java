package atlantis.constructing.position;

import atlantis.combat.squad.missions.MissionDefend;
import atlantis.constructing.ConstructionOrder;
import atlantis.information.AMap;
import atlantis.position.APosition;
import atlantis.units.AUnit;
import atlantis.units.AUnitType;
import atlantis.units.Select;
import bwta.BaseLocation;
import bwta.Chokepoint;

/**
 *
 * @author Rafal Poniatowski <ravaelles@gmail.com>
 * Edited by Edward Buckle
 */
public class TerranBunkerPositionFinder {

    public static APosition findPosition(AUnitType building, AUnit builder, ConstructionOrder order) {
        APosition nearTo = null;
        
        if (order != null && order.getProductionOrder() != null && order.getProductionOrder().getModifier() != null) {
            String locationModifier = order.getProductionOrder().getModifier();
            nearTo = defineBunkerPosition(locationModifier);
        }
        else {
            nearTo = defineBunkerPosition(ASpecialPositionFinder.AT_NATURAL);
        }
        
        // =========================================================
        
        if (nearTo == null) {
            AUnit existingBunker = Select.ourOfType(AUnitType.Terran_Bunker).first();
            if (existingBunker != null) {
                nearTo = existingBunker.getPosition();
                APosition defendPoint = MissionDefend.getInstance().getFocusPoint();
                if (defendPoint != null) {
                    nearTo = nearTo.translateTowards(defendPoint, 15);
                }
            }
            else {
                AUnit mainBase = Select.mainBase();
                if (mainBase != null) {
                    nearTo = Select.mainBase().getPosition();
                }
            }
        }
        
        // Find position near specified place
        return APositionFinder.findStandardPosition(builder, building, nearTo, 30);
    }
    
    // =========================================================

    private static APosition defineBunkerPosition(String locationModifier) {
        AUnit mainBase = Select.mainBase();
        
        // There are two possible "choke points" to locate a defensive building like this. Most bases
        // in StarCraft have a narrow entrance that is considererd the MAIN_CHOKEPOINT. Some maps also
        // have narrow pathways near to the main base that are excellent locations for a bunker. This
        // is the "natural" choke point.
        

        // Bunker at MAIN CHOKEPOINT
        if (locationModifier.equals(ASpecialPositionFinder.NEAR_MAIN_CHOKEPOINT)) {
            Chokepoint chokepointForNaturalBase = AMap.getChokepointForMainBase();
            if (chokepointForNaturalBase != null) {
                return APosition.create(chokepointForNaturalBase.getCenter())
                        .translateTowards(mainBase.getPosition(), 5);
            }
        }

        // Bunker at NATURAL CHOKEPOINT
        else if (locationModifier.equals(ASpecialPositionFinder.AT_NATURAL)){
            Chokepoint chokepointForNaturalBase = AMap.getChokepointForNaturalBase();
            
            if (chokepointForNaturalBase != null && mainBase != null) {
                BaseLocation naturalBase = AMap.getNaturalBaseLocation(Select.mainBase().getPosition());
                return APosition.create(chokepointForNaturalBase.getCenter())
                        .translateTowards(naturalBase, 25);
            }
        }
        
        // Invalid location
        System.err.println("Can't define bunker location: " + locationModifier);
        return null;
    }
    
}
