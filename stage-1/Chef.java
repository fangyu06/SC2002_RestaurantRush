public class Chef{
    private int lastActionTurn=-1; // the turn of the last unit cooked; -1 = none yet

    public boolean isAvailable(int turn){
        return lastActionTurn!=turn;
    }
    public void prepare(Order order, int turn){
        if (!isAvailable(turn)){
            throw new IllegalStateException("Chef has already prepared a unit this turn.");
        }
        OrderStatus status=order.getOrderStatus();
        if (status!=OrderStatus.PLACED&&status!=OrderStatus.PREPARING){
            throw new IllegalStateException("This order cannot be prepared (status: "+status+").");
        }
        order.prepareOneUnit(); // the Order moves itself to PREPARING or READY
        lastActionTurn=turn;
    }
}
