// The Host seats at most ONE queued customer per turn, in addition to the
// Waiter's task (the Waiter can still seat customers too). Seating goes
// through Restaurant.seatCustomer, the same rule the Waiter uses, so the
// seating rules are written only once. As with the Waiter, a refused choice
// does not use up the Host's turn.
public class Host{
    private int lastActionTurn=-1;

    public boolean isAvailable(int turn){
        return lastActionTurn!=turn;
    }

    public void seat(Restaurant restaurant, Customer customer, Table table, int turn){
        if (turn<1){
            throw new IllegalArgumentException("Turn must be 1 or later.");
        }
        if (!isAvailable(turn)){
            throw new IllegalStateException("Host has already seated a customer this turn.");
        }
        restaurant.seatCustomer(customer, table);
        lastActionTurn=turn;
    }
}
