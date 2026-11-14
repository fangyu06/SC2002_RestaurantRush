import java.util.List;
import java.util.Random;

// A customer in the restaurant. Customer owns its own state (WAITING -> SEATED -> ORDERED -> SERVED -> READY_TO_PAY -> PAID, or LEFT). Every change goes through a method that checks the current state first, so the customer can never skip a step. The fields are private; other classes only use the methods.
// The class is abstract because the amount of satisfaction lost per turn is decided by the kind of customer. Stage 1 only has RegularCustomer, but the turn controller and Restaurant already talk to "a Customer", so adding a new kind later does not require changing them.
public abstract class Customer{
    public static final int STARTING_SATISFACTION=100;

    private final int id;
    private int satisfaction;
    private CustomerState state;
    private Table table;      
    private Order order;     
    private int servedTurn;  

    public Customer(int id){
        this.id=id;
        this.satisfaction=STARTING_SATISFACTION;
        this.state=CustomerState.WAITING;
        this.table=null;
        this.order=null;
        this.servedTurn=-1;
    }
    public int getId(){
        return this.id;
    }
    public int getSatisfaction(){
        return this.satisfaction;
    }
    public CustomerState getState(){
        return this.state;
    }
    public Order getOrder(){
        return this.order;
    }
    public int getServedTurn(){
        return this.servedTurn;
    }

    public MenuItem chooseItem(List<MenuItem> menu, Random rng){
        if (menu==null||menu.isEmpty()){
            throw new IllegalArgumentException("The menu is empty, so there is nothing to choose.");
        }
        return menu.get(rng.nextInt(menu.size()));
    }

    // Bill adjustment for this customer type. Regular and Critic pay full price.
    public int applyDiscount(int priceCents){
        return priceCents;
    }
    
    public boolean isWaitingForService(){
        return state==CustomerState.WAITING||state==CustomerState.SEATED||state==CustomerState.ORDERED;
    }

    public void applyWaitingPenalty(){
        if (isWaitingForService()){
            this.satisfaction=Math.max(0,this.satisfaction-getLossPerTurn());
        }
    }
    // Stage 3 (Happy Hour): customers still waiting for a table or food recover
    // satisfaction, never above the starting value of 100.
    public void recoverSatisfaction(int points){
        if (isWaitingForService()){
            this.satisfaction=Math.min(STARTING_SATISFACTION,this.satisfaction+points);
        }
    }
  
    public void seat(Table table){
        if (state!=CustomerState.WAITING){
            throw new IllegalStateException("Customer "+id+" cannot be seated as the state is "+state+".");
        }
        table.seat(this); 
        this.table=table;
        this.state=CustomerState.SEATED;
    }
    public void placeOrder(Order order){
        if (state!=CustomerState.SEATED){
            throw new IllegalStateException("Customer "+id+" cannot order as the state is "+state+".");
        }
        this.order=order;
        this.state=CustomerState.ORDERED;
    }
   
    public void serve(int turn){
        if (state!=CustomerState.ORDERED){
            throw new IllegalStateException("Customer "+id+" cannot be served as the state is "+state+".");
        }
        order.markServed(); 
        this.state=CustomerState.SERVED;
        this.servedTurn=turn;
    }
    public void finishEating(){
        if (state!=CustomerState.SERVED){
            throw new IllegalStateException("Customer "+id+" cannot finish eating as the state is "+state+".");
        }
        this.state=CustomerState.READY_TO_PAY;
    }

    public void pay(){
        if (state!=CustomerState.READY_TO_PAY){
            throw new IllegalStateException("Customer "+id+" cannot pay as the state is "+state+".");
        }
        order.markPaid();
        this.state=CustomerState.PAID;
        releaseTable();
    }

    public void leave(){
        if (!isWaitingForService()){
            throw new IllegalStateException("Customer "+id+" cannot leave because the state is "+state+".");
        }
        if (order!=null){
            order.cancel();
        }
        this.state=CustomerState.LEFT;
        releaseTable();
    }
    private void releaseTable(){
        if (table!=null){
            table.release(this);
            this.table=null;
        }
    }

    protected abstract int getLossPerTurn();

    public abstract String getTypeName();
}
