// The Waiter does at most ONE task per turn: seat a customer, take an order,
// or serve a READY dish. The manager chooses which task, and Waiter enforces
// the rules.

// Every task method checks all the rules BEFORE changing anything. If a rule
// is broken it throws an exception with the reason, and lastActionTurn is not
// updated, so a mistaken choice does not use up the Waiter's task.
public class Waiter{
    private int lastActionTurn=-1; 

    public boolean isAvailable(int turn){
        return lastActionTurn!=turn;
    }
  
    public void seat(Restaurant restaurant, Customer customer, Table table, int turn){
        requireAvailable(turn);
        restaurant.seatCustomer(customer, table); 
        lastActionTurn=turn;
    }
    
    public void takeOrder(Restaurant restaurant, Customer customer, int turn){
        requireAvailable(turn);
        if (customer.getState()!=CustomerState.SEATED){
            throw new IllegalStateException("Customer "+customer.getId()+" is not seated and waiting to order (state: "+customer.getState()+").");
        }
        restaurant.placeOrder(customer);
        lastActionTurn=turn;
    }
  
    public void serve(Restaurant restaurant, Customer customer, int turn){
        requireAvailable(turn);
        Order order=customer.getOrder();
        if (customer.getState()!=CustomerState.ORDERED||order==null){
            throw new IllegalStateException("Customer "+customer.getId()+" has no order waiting to be served (state: "+customer.getState()+").");
        }
        if (order.getOrderStatus()!=OrderStatus.READY){
            throw new IllegalStateException("Customer "+customer.getId()+"'s order is not READY yet (status: "+order.getOrderStatus()+").");
        }
        customer.serve(turn);
        restaurant.recordServed();
        lastActionTurn=turn;
    }
   
    private void requireAvailable(int turn){
        if (turn<1){
            throw new IllegalArgumentException("Turn must be 1 or later.");
        }
        if (!isAvailable(turn)){
            throw new IllegalStateException("Waiter has already completed a task this turn.");
        }
    }
}
