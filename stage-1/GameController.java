import java.util.List;

// Control class: runs the six steps of every turn in the required order
// It asks GameCLI for the manager's choices and hands them to the staff and Restaurant, which enforce the rules

public class GameController{
    public static final int MAX_TURNS=18;
    private static final int[] ARRIVAL_TURNS={1,4,7,10,13,16};

    private static final int WAITER_WAIT=0;
    private static final int WAITER_SEAT=1;
    private static final int WAITER_TAKE_ORDER=2;
    private static final int WAITER_SERVE=3;

    private final Restaurant restaurant;
    private final GameCLI cli;
    private int currentTurn;

    public GameController(GameCLI cli){
        this.cli=cli;
        this.restaurant=new Restaurant();
        this.currentTurn=0;
    }

    public Restaurant getRestaurant(){
        return this.restaurant;
    }

    public void startGame(){
        cli.displayWelcome(MAX_TURNS);
        for (currentTurn=1;currentTurn<=MAX_TURNS;currentTurn++){
            processTurn();
        }
        checkResult();
    }

    private void processTurn(){
        cli.displayTurnHeader(currentTurn, MAX_TURNS);
        processArrivals();                         
        cli.displayStatus(restaurant);             
        cli.displaySection("Your choices");
        processWaiterTask();                       
        processChefTask();
        cli.displaySection("What happened");
        boolean paymentEvents=processPayments();   
        boolean departures=updateSatisfaction();   
        if (!paymentEvents&&!departures){
            cli.displayMessage("Nothing else happened this turn.");
        }
        cli.displayTurnSummary(restaurant, currentTurn, MAX_TURNS);
    }

    // Step 1: the scheduled customer joins the queue (satisfaction starts at 100)
    private void processArrivals(){
        for (int i=0;i<ARRIVAL_TURNS.length;i++){
            if (ARRIVAL_TURNS[i]==currentTurn){
                Customer arrival=new RegularCustomer(i+1); 
                restaurant.addToWaitingQueue(arrival);
                cli.displayMessage("+ C"+arrival.getId()+" ("+arrival.getTypeName()+") arrives and joins the waiting queue.");
            }
        }
    }

    // Step 2 (Waiter): ask for one task. If a rule is broken, show the reason and ask again
    private void processWaiterTask(){
        Waiter waiter=restaurant.getWaiter();
        while (true){
            int choice=cli.getWaiterChoice();
            try{
                if (choice==WAITER_WAIT){
                    cli.displayMessage("Waiter waits.");
                }else if (choice==WAITER_SEAT){
                    Customer customer=restaurant.findCustomer(cli.readCustomerId("Seat which customer? "));
                    Table table=restaurant.getAvailableTable();
                    waiter.seat(restaurant, customer, table, currentTurn);
                    cli.displayMessage("Waiter seats C"+customer.getId()+" at Table "+table.getNumber()+".");
                }else if (choice==WAITER_TAKE_ORDER){
                    Customer customer=restaurant.findCustomer(cli.readCustomerId("Take whose order? "));
                    waiter.takeOrder(restaurant, customer, currentTurn);
                    cli.displayOrderPlaced(customer.getOrder());
                }else if (choice==WAITER_SERVE){
                    Customer customer=restaurant.findCustomer(cli.readCustomerId("Serve which customer? "));
                    waiter.serve(restaurant, customer, currentTurn);
                    cli.displayMessage("Waiter serves "+customer.getOrder().getItem().getName()+" to C"+customer.getId()+" -> SERVED.");
                }
                return;
            }catch (IllegalStateException|IllegalArgumentException e){
                cli.displayError(e.getMessage());
            }
        }
    }

    // Step 3 (Chef): cook one unit of one order
    private void processChefTask(){
        Chef chef=restaurant.getChef();
        while (true){
            List<Order> kitchen=restaurant.getOrdersInKitchen();
            if (kitchen.isEmpty()){
                cli.displayMessage("Chef has nothing to cook and waits.");
                return;
            }
            int customerId=cli.getChefChoice();
            if (customerId==0){
                cli.displayMessage("Chef waits.");
                return;
            }
            try{
                Order order=findKitchenOrder(kitchen, customerId);
                chef.prepare(order, currentTurn);
                cli.displayPreparation(order);
                return;
            }catch (IllegalStateException|IllegalArgumentException e){
                cli.displayError(e.getMessage());
            }
        }
    }
    private Order findKitchenOrder(List<Order> kitchen, int customerId){
        for (Order order:kitchen){
            if (order.getCustomer().getId()==customerId){
                return order;
            }
        }
        throw new IllegalArgumentException("C"+customerId+" has no order waiting in the kitchen.");
    }

    // Step 4: customers served earlier finish eating, then the Cashier collects at most one payment
    private boolean processPayments(){
        List<Customer> finished=restaurant.finishEating(currentTurn);
        for (Customer customer:finished){
            cli.displayMessage("C"+customer.getId()+" finishes eating -> READY_TO_PAY.");
        }
        Customer payer=restaurant.oldestEligiblePayment(currentTurn);
        if (restaurant.getCashier().processPayment(restaurant, currentTurn)){
            cli.displayMessage("$ Cashier collects "+GameCLI.formatCents(payer.getOrder().getPriceCents())
                +" from C"+payer.getId()+" -> PAID, table freed (satisfaction "+payer.getSatisfaction()+" recorded).");
            return true;
        }
        return !finished.isEmpty();
    }

    // Step 5: waiting customers lose satisfaction; anyone at 0 leaves
    private boolean updateSatisfaction(){
        List<Customer> departed=restaurant.applyWaitingPenalties();
        for (Customer customer:departed){
            cli.displayMessage("! C"+customer.getId()+" ("+customer.getTypeName()+") reached 0 satisfaction and LEFT without paying.");
        }
        return !departed.isEmpty();
    }

    // Step 6: after turn 18, Victory only if all three targets are met
    private void checkResult(){
        cli.displayResult(restaurant, restaurant.isVictory(), MAX_TURNS);
    }
}
