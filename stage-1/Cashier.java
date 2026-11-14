public class Cashier{
    private int lastPaymentTurn=-1; // the turn of the last payment; -1 = none yet
    
    // Returns true if a payment was collected this turn.
    public boolean processPayment(Restaurant restaurant, int turn){
        if (lastPaymentTurn==turn){
            return false; // already took a payment this turn
        }
        Customer customer=restaurant.oldestEligiblePayment(turn);
        if (customer==null){
            return false; // nobody is allowed to pay yet
        }
        int bill=customer.getOrder().getPriceCents(); 
        customer.pay();                               
        restaurant.addRevenue(bill);                 
        lastPaymentTurn=turn;
        return true;
    }
}
