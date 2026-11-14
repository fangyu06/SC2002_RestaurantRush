// A table holds at most one customer
// It stays occupied until that customer pays or leaves
// Table only knows who is sitting at it, so it knows nothing about the game rules.

public class Table{
    private final int number; // table number will be either 1 or 2 and is set once and cannot be changed
    private Customer occupant; // who is seating here, or null if free

    public Table(int number){
        this.number=number;
        this.occupant=null; // every table starts empty
    }

    public int getNumber(){
        return this.number;
    }

    // Table is free when nobody is sitting
    public boolean isFree(){
        return this.occupant==null;
    }

    public Customer getOccupant(){
        return this.occupant;
    }

    // Puts a customer at this table
    //  Check first, only changes the occupant if both checks pass
    public void seat(Customer customer){
        if (customer==null){
            throw new IllegalArgumentException("Cannot seat a missing customer.");
        }
        if (!isFree()){
            throw new IllegalStateException("Table "+number+" is already occupied by customer "+occupant.getId()+".");
        }
        this.occupant=customer;
    }

    // Free the table when the customer pays or leaves
    // It only works if this exact customer is the one sitting here, so wrong customer cannot free the table
    public void release(Customer customer){
        if (this.occupant!=customer){
            throw new IllegalStateException("Customer "+customer.getId()+" is not sitting at table "+number+".");
        }
        this.occupant=null;
    }
}