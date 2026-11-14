// The Chef cooks ONE preparation unit per turn. The manager chooses which
// order to advance; Chef checks the rules. As with the Waiter, a refused
// choice does not use up the Chef's turn.
public class Chef{
    private int lastActionTurn=-1; 

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
        order.prepareOneUnit(); 
        lastActionTurn=turn;
    }
}
