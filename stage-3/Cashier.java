// The Cashier works automatically: at most ONE payment per turn, always the
// customer who has been waiting to pay the longest. The manager makes no
// choice here.
public class Cashier{
    private int lastPaymentTurn=-1; 

    
    public boolean processPayment(Restaurant restaurant, int turn){
        if (lastPaymentTurn==turn){
            return false; 
        }
        Customer customer=restaurant.oldestEligiblePayment(turn);
        if (customer==null){
            return false; 
        }
        int bill=customer.getOrder().getPriceCents(); 
        customer.pay();                               
        restaurant.addRevenue(bill);                 
        lastPaymentTurn=turn;
        return true;
    }
}
