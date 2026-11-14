import java.util.Collection;
import java.util.Scanner;

// Boundary class: it ONLY reads the manager's input and displays the game
// state. No game rules live here. Everything it shows comes from Restaurant
// (and the objects inside it), and everything it reads is handed back to
// GameController as plain numbers.
public class GameCLI{
    private static final String THICK_LINE="==============================================================================";
    private static final String THIN_LINE ="------------------------------------------------------------------------------";
    private static final String ROW_FORMAT="  %-9s %-11s %3s  %-12s  %-14s %7s  %s";

    private final Scanner scanner;
    private final boolean echoInput;

    public GameCLI(Scanner scanner){
        this.scanner=scanner;
        this.echoInput=System.console()==null;
    }

    public static String formatCents(int cents){
        return String.format("$%d.%02d", cents/100, cents%100);
    }

    private static String formatAverage(Restaurant restaurant){
        double average=restaurant.getAverageSatisfaction();
        return Double.isNaN(average)?"N/A":String.format("%.1f", average);
    }


    public void displayMessage(String message){
        System.out.println("  "+message);
    }

    public void displayError(String reason){
        System.out.println("  ! Invalid choice: "+reason+" Try again.");
    }
    public void displaySection(String title){
        System.out.println();
        System.out.println("  -- "+title+" --");
    }
    public void displayWelcome(int maxTurns){
        System.out.println(THICK_LINE);
        System.out.println("  RESTAURANT RUSH");
        System.out.println(THICK_LINE);
        System.out.println("  You are the manager. The game runs for all "+maxTurns+" turns.");
        System.out.println("  To win, by the end you need:");
        System.out.println("    - at least "+Restaurant.TARGET_PAID_CUSTOMERS+" paid customers");
        System.out.println("    - revenue of at least "+formatCents(Restaurant.TARGET_REVENUE_CENTS));
        System.out.println("    - average satisfaction of paid customers of at least "+Restaurant.TARGET_AVERAGE_SATISFACTION);
        System.out.println("  Each turn the Waiter does one task and the Chef cooks one unit.");
        System.out.println("  Customers are shown as C1, C2, ...; type just the number when asked.");
    }
    public void displayTurnHeader(int turn, int maxTurns){
        System.out.println();
        System.out.println();
        System.out.println(THICK_LINE);
        System.out.println("  TURN "+turn+" / "+maxTurns);
        System.out.println(THICK_LINE);
    }

    public void displayStatus(Restaurant restaurant){
        System.out.println();
        System.out.println("  Revenue: "+formatCents(restaurant.getRevenueCents())
            +"    Paid customers: "+restaurant.getPaidCount()
            +"    Avg satisfaction (paid): "+formatAverage(restaurant));
        System.out.println();
        printRow("WHERE", "CUSTOMER", "SAT", "STATE", "ORDER", "PRICE", "ORDER STATUS");
        System.out.println("  "+THIN_LINE.substring(2));
        Collection<Customer> queue=restaurant.getWaitingQueue();
        if (queue.isEmpty()){
            printRow("Queue", "(empty)", "", "", "", "", "");
        }
        for (Customer customer:queue){
            printCustomerRow("Queue", customer);
        }
        for (Table table:restaurant.getTables()){
            if (table.isFree()){
                printRow("Table "+table.getNumber(), "(free)", "", "", "", "", "");
            }else{
                printCustomerRow("Table "+table.getNumber(), table.getOccupant());
            }
        }
        System.out.println();
        displayAvailableTasks(restaurant);
    }
    private void printRow(Object... columns){
        System.out.println(String.format(ROW_FORMAT, columns).stripTrailing());
    }
    private void printCustomerRow(String where, Customer customer){
        String item="-";
        String price="";
        String status="";
        Order order=customer.getOrder();
        if (order!=null){
            item=order.getItem().getName();
            price=formatCents(order.getPriceCents());
            status=order.getOrderStatus().toString();
            if (order.getOrderStatus()==OrderStatus.PLACED||order.getOrderStatus()==OrderStatus.PREPARING){
                status+=" ("+order.getRemainingUnits()+" left)";
            }
        }
        printRow(where, "C"+customer.getId()+" "+customer.getTypeName(),
            customer.getSatisfaction(), customer.getState(), item, price, status);
    }

    private void displayAvailableTasks(Restaurant restaurant){
        boolean canSeat=!restaurant.getWaitingQueue().isEmpty()&&restaurant.getAvailableTable()!=null;
        System.out.println("  Waiter can  seat: "+(canSeat?idList(restaurant.getWaitingQueue()):"-")
            +"   take order: "+idList(restaurant.getCustomersInState(CustomerState.SEATED))
            +"   serve: "+idList(restaurant.getCustomersWithReadyOrders()));
        String kitchen="";
        for (Order order:restaurant.getOrdersInKitchen()){
            kitchen+=(kitchen.isEmpty()?"":", ")+"C"+order.getCustomer().getId()+" "+order.getItem().getName();
        }
        System.out.println("  Chef can    cook: "+(kitchen.isEmpty()?"-":kitchen));
    }
    private String idList(Collection<Customer> customers){
        if (customers.isEmpty()){
            return "-";
        }
        String text="";
        for (Customer customer:customers){
            text+=(text.isEmpty()?"":", ")+"C"+customer.getId();
        }
        return text;
    }

    public int getWaiterChoice(){
        System.out.println("  Waiter task:  [1] Seat   [2] Take order   [3] Serve   [0] Wait");
        return readInt("Waiter > ", 0, 3);
    }
    // Chef: the customer whose order to cook, or 0 to wait.
    public int getChefChoice(){
        return readInt("Chef: cook for which customer? (0 = wait) > ", 0, Integer.MAX_VALUE);
    }
    public int readCustomerId(String prompt){
        return readInt(prompt+"(customer number) > ", 0, Integer.MAX_VALUE);
    }

    public void displayOrderPlaced(Order order){
        displayMessage("C"+order.getCustomer().getId()+" orders "+order.getItem().getName()
            +" ("+formatCents(order.getPriceCents())+", "+order.getItem().getUnits()+" unit(s) to cook).");
    }
    public void displayPreparation(Order order){
        if (order.getOrderStatus()==OrderStatus.READY){
            displayMessage("Chef finishes "+order.getItem().getName()+" for C"+order.getCustomer().getId()+" -> READY (serve it next turn).");
        }else{
            displayMessage("Chef cooks one unit of "+order.getItem().getName()+" for C"
                +order.getCustomer().getId()+" ("+order.getRemainingUnits()+" unit(s) left).");
        }
    }

    public void displayTurnSummary(Restaurant restaurant, int turn, int maxTurns){
        System.out.println();
        System.out.println("  End of turn "+turn+"/"+maxTurns+":  served "+restaurant.getServedCount()
            +" | left unhappy "+restaurant.getUnhappyDepartures()
            +" | revenue "+formatCents(restaurant.getRevenueCents())
            +" | avg satisfaction (paid) "+formatAverage(restaurant));
    }
    public void displayResult(Restaurant restaurant, boolean victory, int turnsUsed){
        System.out.println();
        System.out.println();
        System.out.println(THICK_LINE);
        System.out.println("  FINAL RESULT");
        System.out.println(THICK_LINE);
        System.out.println("  Turns used:          "+turnsUsed);
        System.out.println("  Served:              "+restaurant.getServedCount());
        System.out.println("  Left unhappy:        "+restaurant.getUnhappyDepartures());
        System.out.println();
        System.out.printf("  %-26s %-10s %s%n", "TARGET", "RESULT", "NEEDED");
        System.out.printf("  %-26s %-10s %-12s %s%n", "Paid customers",
            restaurant.getPaidCount(), ">= "+Restaurant.TARGET_PAID_CUSTOMERS, metLabel(restaurant.hasEnoughPaidCustomers()));
        System.out.printf("  %-26s %-10s %-12s %s%n", "Revenue",
            formatCents(restaurant.getRevenueCents()), ">= "+formatCents(Restaurant.TARGET_REVENUE_CENTS), metLabel(restaurant.hasEnoughRevenue()));
        System.out.printf("  %-26s %-10s %-12s %s%n", "Avg satisfaction (paid)",
            formatAverage(restaurant), ">= "+Restaurant.TARGET_AVERAGE_SATISFACTION, metLabel(restaurant.hasEnoughSatisfaction()));
        System.out.println();
        System.out.println(victory?"  *** VICTORY ***  All three targets met.":"  *** DEFEAT ***  Not every target was met.");
        System.out.println(THICK_LINE);
    }
    private String metLabel(boolean met){
        return met?"met":"MISSED";
    }

    private int readInt(String prompt, int min, int max){
        while (true){
            System.out.print("  "+prompt);
            if (!scanner.hasNextLine()){
                System.out.println("(no more input, using 0)");
                return 0;
            }
            String line=scanner.nextLine().trim();
            if (echoInput){
                System.out.println(line);
            }
            try{
                int value=Integer.parseInt(line);
                if (value>=min&&value<=max){
                    return value;
                }
                System.out.println("  ! Please enter a number from "+min+(max==Integer.MAX_VALUE?" upwards.":" to "+max+"."));
            }catch (NumberFormatException e){
                System.out.println("  ! Please enter a whole number.");
            }
        }
    }
}
