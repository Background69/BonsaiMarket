package com.bonsaimarket.backend.shipping;

import com.bonsaimarket.backend.marketplace.MarketplaceException;

public enum ShipmentState {
    CREATING,UNKNOWN,WAITING_PICKUP,PICKED_UP,IN_TRANSIT,OUT_FOR_DELIVERY,DELIVERY_FAILED,DELIVERED,
    RETURN_REQUESTED,RETURNING,RETURN_FAILED,RETURNED,CANCELLED,EXCEPTION,LOST,DAMAGED,DISPOSED;
    public static ShipmentState fromGhn(String status){
        return switch(status) {
            case "ready_to_pick","picking","money_collect_picking" -> WAITING_PICKUP;
            case "picked" -> PICKED_UP;
            case "storing","sorting","transporting" -> IN_TRANSIT;
            case "delivering","money_collect_delivering" -> OUT_FOR_DELIVERY;
            case "delivery_fail" -> DELIVERY_FAILED;
            case "delivered" -> DELIVERED;
            case "waiting_to_return","return" -> RETURN_REQUESTED;
            case "return_transporting","return_sorting","returning" -> RETURNING;
            case "return_fail" -> RETURN_FAILED;
            case "returned" -> RETURNED;
            case "cancel" -> CANCELLED;
            case "exception" -> EXCEPTION;
            case "lost" -> LOST;
            case "damage" -> DAMAGED;
            case "scrap" -> DISPOSED;
            default -> throw MarketplaceException.invalid("Trạng thái GHN chưa được hỗ trợ; cần đối chiếu với đơn vị vận chuyển.");
        };
    }
    public boolean terminal(){return switch(this){case DELIVERED,RETURNED,CANCELLED,EXCEPTION,LOST,DAMAGED,DISPOSED -> true;default -> false;};}
    public boolean accepts(ShipmentState next){
        if(this==CREATING||this==UNKNOWN)return true;
        if(terminal())return this==next;
        if(next==CANCELLED)return this==WAITING_PICKUP;
        if(next==WAITING_PICKUP)return this==WAITING_PICKUP;
        if(next==PICKED_UP)return this==WAITING_PICKUP||this==PICKED_UP;
        if(this==RETURN_REQUESTED||this==RETURNING||this==RETURN_FAILED)
            return next==RETURN_REQUESTED||next==RETURNING||next==RETURN_FAILED||next==RETURNED||next==EXCEPTION||next==LOST||next==DAMAGED||next==DISPOSED;
        return true;
    }
}
