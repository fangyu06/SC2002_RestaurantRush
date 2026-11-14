package src;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

// Holds the restaurant-wide state (waiting queue, tables, customers, revenue,
// counters) and the game rules that act on that state. It also owns the staff.
// GameController runs the turn steps and asks Restaurant to do the work;
// GameCLI only displays what Restaurant reports.
public class Restaurant{
    public static final long SEED=2002; // fixed seed so every run is the same
    public static final int TABLE_COUNT=2;
    public static final int TARGET_PAID_CUSTOMERS=4;
    public static final int TARGET_REVENUE_CENTS=4500; // money is in cents, so 4500 = $45.00
    public static final int TARGET_AVERAGE_SATISFACTION=60;

    private final Queue<Customer> waitingQueue; // customers waiting for a table, first come first served
    private final List<Table> tables;
    private final List<Customer> customers; // everyone who has arrived, even after they leave or pay    
    private final List<MenuItem> menu;
    private final Waiter waiter;
    private final Chef chef;
    private final Cashier cashier;
    private final Random rng;  // used by customers to choose food                 
    private int revenueCents;                  
    private int servedCount;
    private int unhappyDepartures;

    public Restaurant(){
        this.waitingQueue=new LinkedList<>();
        this.tables=new ArrayList<>();
        this.customers=new ArrayList<>();
        this.menu=new ArrayList<>();
        this.waiter=new Waiter();
        this.chef=new Chef();
        this.cashier=new Cashier();
        this.rng=new Random(SEED);
        for (int i=1;i<=TABLE_COUNT;i++){
            tables.add(new Table(i));
        }
        menu.add(new MenuItem("Salad", 900, 1));
        menu.add(new MenuItem("Burger", 1500, 1));
        menu.add(new MenuItem("Pasta", 1800, 2));
    }

    public Waiter getWaiter(){
        return this.waiter;
    }
    
    public Chef getChef(){
        return this.chef;
    }

    public Cashier getCashier(){
        return this.cashier;
    }

    // Unmodifiable, so other classes can look but cannot add or remove items
    public List<MenuItem> getMenu(){
        return Collections.unmodifiableList(menu);
    }

    public List<Table> getTables(){
        return Collections.unmodifiableList(tables);
    }

    // Returns a copy, so the real queue cannot be changed from outside
    public Queue<Customer> getWaitingQueue(){
        return new LinkedList<>(waitingQueue);
    }

    public void addToWaitingQueue(Customer customer){
        if (customer==null){
            throw new IllegalArgumentException("Cannot add a missing customer to the queue.");
        }
        customers.add(customer);
        waitingQueue.add(customer);
    }

    // First free table, or null if both are taken
    public Table getAvailableTable(){
        for (Table table:tables){
            if (table.isFree()){
                return table;
            }
        }
        return null;
    }

    // Both checks come first, so nothing changes if a rule is broken
    public void seatCustomer(Customer customer, Table table){
        if (!waitingQueue.contains(customer)){
            throw new IllegalStateException("Customer "+customer.getId()+" is not in the waiting queue.");
        }
        if (table==null){
            throw new IllegalStateException("No table is free.");
        }
        customer.seat(table); // seat first, so if this fails the customer is still in the queue         
        waitingQueue.remove(customer);
    }

    // The customer picks the food, not Restaurant, so no need to check Regular/VIP/Critic here
    // Any discount (e.g. VIP) is applied inside Order, so this method did not need to change for Stage 2
    public Order placeOrder(Customer customer){
        MenuItem item=customer.chooseItem(menu, rng);
        Order order=new Order(customer, item);
        customer.placeOrder(order);
        return order;
    }

    public void recordServed(){
        servedCount++;
    }

    public Customer findCustomer(int id){
        for (Customer customer:customers){
            if (customer.getId()==id){
                return customer;
            }
        }
        throw new IllegalArgumentException("There is no customer with id "+id+".");
    }

    public List<Customer> getCustomersInState(CustomerState state){
        List<Customer> result=new ArrayList<>();
        for (Customer customer:customers){
            if (customer.getState()==state){
                result.add(customer);
            }
        }
        return result;
    }

    // Orders the Chef can still cook (just placed or half done)
    public List<Order> getOrdersInKitchen(){
        List<Order> result=new ArrayList<>();
        for (Customer customer:getCustomersInState(CustomerState.ORDERED)){
            OrderStatus status=customer.getOrder().getOrderStatus();
            if (status==OrderStatus.PLACED||status==OrderStatus.PREPARING){
                result.add(customer.getOrder());
            }
        }
        return result;
    }

    // Customers whose food is cooked and waiting for the Waiter to serve
    public List<Customer> getCustomersWithReadyOrders(){
        List<Customer> result=new ArrayList<>();
        for (Customer customer:getCustomersInState(CustomerState.ORDERED)){
            if (customer.getOrder().getOrderStatus()==OrderStatus.READY){
                result.add(customer);
            }
        }
        return result;
    }

    // Served on turn S -> finishes eating on turn S+1 -> can pay from turn S+2
    // Only customers served on an EARLIER turn finish, so nobody eats in the same turn they are served
    public List<Customer> finishEating(int turn){
        List<Customer> finished=new ArrayList<>();
        for (Customer customer:getCustomersInState(CustomerState.SERVED)){
            if (customer.getServedTurn()<turn){
                customer.finishEating();
                finished.add(customer);
            }
        }
        return finished;
    }

    // The Cashier takes the customer who has been waiting to pay the longest
    // (smallest served turn). Returns null if nobody can pay yet.
    // servedTurn+1<turn means they became READY_TO_PAY on an earlier turn
    public Customer oldestEligiblePayment(int turn){
        Customer oldest=null;
        for (Customer customer:getCustomersInState(CustomerState.READY_TO_PAY)){
            if (customer.getServedTurn()+1<turn){
                if (oldest==null||customer.getServedTurn()<oldest.getServedTurn()){
                    oldest=customer;
                }
            }
        }
        return oldest;
    }

    public void addRevenue(int amountCents){
        if (amountCents<0){
            throw new IllegalArgumentException("Revenue cannot be reduced by a negative amount.");
        }
        revenueCents+=amountCents;
    }

    public int getRevenueCents(){
        return this.revenueCents;
    }

    // Every customer still waiting loses satisfaction. At 0 they leave without paying.
    // Returns who left this turn so GameController can tell the manager
    public List<Customer> applyWaitingPenalties(){
        List<Customer> departed=new ArrayList<>();
        for (Customer customer:customers){
            if (customer.isWaitingForService()){
                customer.applyWaitingPenalty();
                if (customer.getSatisfaction()==0){
                    waitingQueue.remove(customer);
                    customer.leave();
                    unhappyDepartures++;
                    departed.add(customer);
                }
            }
        }
        return departed;
    }

    public int getServedCount(){
        return this.servedCount;
    }

    public int getUnhappyDepartures(){
        return this.unhappyDepartures;
    }

    public int getPaidCount(){
        return getCustomersInState(CustomerState.PAID).size();
    }

    // Average of paid customers only. NaN if nobody has paid yet (GameCLI shows N/A)
    // A customer's satisfaction stops changing once served, so this is the value at payment
    public double getAverageSatisfaction(){
        List<Customer> paid=getCustomersInState(CustomerState.PAID);
        if (paid.isEmpty()){
            return Double.NaN;
        }
        int total=0;
        for (Customer customer:paid){
            total+=customer.getSatisfaction();
        }
        return (double) total/paid.size();
    }

    public boolean hasEnoughPaidCustomers(){
        return getPaidCount()>=TARGET_PAID_CUSTOMERS;
    }

    public boolean hasEnoughRevenue(){
        return revenueCents>=TARGET_REVENUE_CENTS;
    }

    // The paid count check is there because NaN compared with 60 is false anyway, but this makes it clear
    public boolean hasEnoughSatisfaction(){
        return getPaidCount()>0&&getAverageSatisfaction()>=TARGET_AVERAGE_SATISFACTION;
    }

    // Win only if all three targets are met
    public boolean isVictory(){
        return hasEnoughPaidCustomers()&&hasEnoughRevenue()&&hasEnoughSatisfaction();
    }
}
