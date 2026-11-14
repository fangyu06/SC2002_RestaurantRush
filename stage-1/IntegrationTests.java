import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

// Integration tests for Stage 1
// Compile and run from the src folder:
//     javac *.java
//     java IntegrationTests
// Part A drives Restaurant and the staff directly, one turn at a time
// Part B plays the whole 18-turn game through GameController with scripted input, including one reproducible VICTORY run and one reproducible DEFEAT run
public class IntegrationTests{
    private static int passed=0;
    private static int failed=0;

    public static void main(String[] args){
        System.out.println("=== Part A: rules on Restaurant and staff ===");
        testBaseSetting();
        testSpecExampleOneCustomer();
        testRefusedChoicesDoNotUseUpTasks();
        testCashierPaysOneOldestFirst();
        testNoRevenueFromUnpaidOrAbandoned();
        testAbandonmentAfterThirteenTurns();
        testVictoryNeedsAllThreeTargets();
        System.out.println();
        System.out.println("=== Part B: whole game through GameController ===");
        testVictoryRun();
        testDefeatRun();
        testRunsAreReproducible();
        testInvalidInputIsHandled();
        System.out.println();
        System.out.println("Passed: "+passed+"   Failed: "+failed);
        if (failed>0){
            System.exit(1);
        }
    }

    private static void check(String name, boolean condition){
        if (condition){
            passed++;
            System.out.println("  PASS  "+name);
        }else{
            failed++;
            System.out.println("  FAIL  "+name);
        }
    }

    private static void checkRefused(String name, Runnable action){
        try{
            action.run();
            check(name+" (was NOT refused)", false);
        }catch (IllegalStateException|IllegalArgumentException e){
            check(name, true);
        }
    }

    private static class FixedChoiceCustomer extends Customer{
        private final int menuIndex;
        FixedChoiceCustomer(int id, int menuIndex){
            super(id);
            this.menuIndex=menuIndex;
        }
        @Override
        public MenuItem chooseItem(List<MenuItem> menu, Random rng){
            return menu.get(menuIndex);
        }
        @Override
        protected int getLossPerTurn(){
            return RegularCustomer.LOSS_PER_TURN;
        }
        @Override
        public String getTypeName(){
            return "Test";
        }
    }

    private static class Sim{
        final Restaurant restaurant;
        int turn=1;
        Sim(Restaurant restaurant){
            this.restaurant=restaurant;
        }
        void endTurn(){
            restaurant.finishEating(turn);
            restaurant.getCashier().processPayment(restaurant, turn);
            restaurant.applyWaitingPenalties();
            turn++;
        }
        void idleTurns(int count){
            for (int i=0;i<count;i++){
                endTurn();
            }
        }

        void serveCompletely(Customer customer){
            for (int guard=0;guard<40&&customer.getState()!=CustomerState.PAID&&customer.getState()!=CustomerState.LEFT;guard++){
                Waiter waiter=restaurant.getWaiter();
                if (customer.getState()==CustomerState.WAITING){
                    waiter.seat(restaurant, customer, restaurant.getAvailableTable(), turn);
                }else if (customer.getState()==CustomerState.SEATED){
                    waiter.takeOrder(restaurant, customer, turn);
                }else if (customer.getState()==CustomerState.ORDERED
                        &&customer.getOrder().getOrderStatus()==OrderStatus.READY){
                    waiter.serve(restaurant, customer, turn);
                }
                if (customer.getState()==CustomerState.ORDERED
                        &&restaurant.getOrdersInKitchen().contains(customer.getOrder())){
                    restaurant.getChef().prepare(customer.getOrder(), turn);
                }
                endTurn();
            }
        }
    }

    // Part A : rules
    private static void testBaseSetting(){
        Restaurant restaurant=new Restaurant();
        check("18 turns maximum", GameController.MAX_TURNS==18);
        check("2 tables, both free at the start", restaurant.getTables().size()==2&&restaurant.getAvailableTable()!=null);
        List<MenuItem> menu=restaurant.getMenu();
        check("menu has exactly 3 items", menu.size()==3);
        check("Salad $9.00, 1 unit", menu.get(0).getName().equals("Salad")&&menu.get(0).getPriceCents()==900&&menu.get(0).getUnits()==1);
        check("Burger $15.00, 1 unit", menu.get(1).getName().equals("Burger")&&menu.get(1).getPriceCents()==1500&&menu.get(1).getUnits()==1);
        check("Pasta $18.00, 2 units", menu.get(2).getName().equals("Pasta")&&menu.get(2).getPriceCents()==1800&&menu.get(2).getUnits()==2);
        check("targets: 4 paid, $45.00, average 60",
            Restaurant.TARGET_PAID_CUSTOMERS==4&&Restaurant.TARGET_REVENUE_CENTS==4500&&Restaurant.TARGET_AVERAGE_SATISFACTION==60);
    }

    private static void testSpecExampleOneCustomer(){
        Restaurant restaurant=new Restaurant();
        Waiter waiter=restaurant.getWaiter();
        Chef chef=restaurant.getChef();
        Sim sim=new Sim(restaurant);
        Customer customer=new FixedChoiceCustomer(1, 2);
        restaurant.addToWaitingQueue(customer);
        check("arrives with satisfaction 100, WAITING", customer.getSatisfaction()==100&&customer.getState()==CustomerState.WAITING);

        waiter.seat(restaurant, customer, restaurant.getAvailableTable(), sim.turn);   // turn 1
        sim.endTurn();
        check("turn 1: seated, satisfaction 92", customer.getState()==CustomerState.SEATED&&customer.getSatisfaction()==92);

        waiter.takeOrder(restaurant, customer, sim.turn);                              // turn 2
        chef.prepare(customer.getOrder(), sim.turn);
        sim.endTurn();
        check("turn 2: order taken, unit 1 of 2 done, satisfaction 84",
            customer.getOrder().getOrderStatus()==OrderStatus.PREPARING&&customer.getSatisfaction()==84);

        chef.prepare(customer.getOrder(), sim.turn);                                   // turn 3
        check("turn 3: order is READY", customer.getOrder().getOrderStatus()==OrderStatus.READY);
        sim.endTurn();
        check("turn 3: satisfaction 76", customer.getSatisfaction()==76);

        waiter.serve(restaurant, customer, sim.turn);                                  // turn 4
        sim.endTurn();
        check("turn 4: SERVED, no further loss", customer.getState()==CustomerState.SERVED&&customer.getSatisfaction()==76);

        sim.endTurn();                                                                 // turn 5
        check("turn 5: READY_TO_PAY, nothing paid yet", customer.getState()==CustomerState.READY_TO_PAY&&restaurant.getRevenueCents()==0);

        sim.endTurn();                                                                 // turn 6
        check("turn 6: PAID $18.00 and table freed",
            customer.getState()==CustomerState.PAID&&restaurant.getRevenueCents()==1800&&restaurant.getAvailableTable()!=null);
        check("order is PAID, satisfaction 76 recorded", customer.getOrder().getOrderStatus()==OrderStatus.PAID&&restaurant.getAverageSatisfaction()==76.0);
    }

    private static void testRefusedChoicesDoNotUseUpTasks(){
        Restaurant restaurant=new Restaurant();
        Waiter waiter=restaurant.getWaiter();
        Customer first=new FixedChoiceCustomer(1, 2);   // Pasta: needs 2 cooking units
        Customer second=new RegularCustomer(2);
        Customer third=new RegularCustomer(3);
        restaurant.addToWaitingQueue(first);
        restaurant.addToWaitingQueue(second);
        restaurant.addToWaitingQueue(third);

        checkRefused("cannot take the order of a customer who is not seated", ()->waiter.takeOrder(restaurant, first, 1));
        checkRefused("cannot serve a customer who has no order", ()->waiter.serve(restaurant, first, 1));
        check("Waiter is still available after refused choices", waiter.isAvailable(1));
        waiter.seat(restaurant, first, restaurant.getAvailableTable(), 1);
        check("a valid seating after refusals works", first.getState()==CustomerState.SEATED);
        checkRefused("Waiter cannot do a second task in the same turn", ()->waiter.seat(restaurant, second, restaurant.getAvailableTable(), 1));

        waiter.seat(restaurant, second, restaurant.getAvailableTable(), 2);
        check("both tables are now occupied", restaurant.getAvailableTable()==null);
        checkRefused("cannot seat when no table is free", ()->waiter.seat(restaurant, third, restaurant.getAvailableTable(), 3));
        check("customer stays in the queue after a refused seating",
            restaurant.getWaitingQueue().size()==1&&third.getState()==CustomerState.WAITING);
        checkRefused("cannot seat a customer who is already seated", ()->waiter.seat(restaurant, first, restaurant.getAvailableTable(), 4));

        waiter.takeOrder(restaurant, first, 3);
        Order order=first.getOrder();
        restaurant.getChef().prepare(order, 3);
        checkRefused("Chef cannot cook twice in the same turn", ()->restaurant.getChef().prepare(order, 3));
        checkRefused("a dish that is not READY cannot be served", ()->waiter.serve(restaurant, first, 4));
    }

    private static void testCashierPaysOneOldestFirst(){
        Restaurant restaurant=new Restaurant();
        Waiter waiter=restaurant.getWaiter();
        Chef chef=restaurant.getChef();
        Customer older=new FixedChoiceCustomer(1, 0);   // Salad: 1 unit
        Customer newer=new FixedChoiceCustomer(2, 0);
        restaurant.addToWaitingQueue(older);
        restaurant.addToWaitingQueue(newer);
        waiter.seat(restaurant, older, restaurant.getAvailableTable(), 1);
        waiter.seat(restaurant, newer, restaurant.getAvailableTable(), 2);
        waiter.takeOrder(restaurant, older, 3);  chef.prepare(older.getOrder(), 3);
        waiter.takeOrder(restaurant, newer, 4);  chef.prepare(newer.getOrder(), 4);
        waiter.serve(restaurant, older, 5);
        waiter.serve(restaurant, newer, 6);
        Sim sim=new Sim(restaurant);
        sim.turn=5;
        sim.endTurn();   // turn 5
        sim.endTurn();   // turn 6: older READY_TO_PAY
        check("customer served on turn 5 is READY_TO_PAY on turn 6 but not paid yet",
            older.getState()==CustomerState.READY_TO_PAY&&restaurant.getRevenueCents()==0);
        sim.endTurn();   // turn 7: older pays; newer becomes READY_TO_PAY
        check("older customer pays first on turn 7", older.getState()==CustomerState.PAID&&newer.getState()==CustomerState.READY_TO_PAY);
        check("only one payment on that turn", restaurant.getRevenueCents()==900&&restaurant.getPaidCount()==1);
        sim.endTurn();   // turn 8
        check("newer customer pays on turn 8", newer.getState()==CustomerState.PAID&&restaurant.getRevenueCents()==1800);
        checkRefused("a customer cannot pay before READY_TO_PAY", ()->new FixedChoiceCustomer(9, 0).pay());
    }

    private static void testNoRevenueFromUnpaidOrAbandoned(){
        Restaurant restaurant=new Restaurant();
        Sim sim=new Sim(restaurant);
        Customer eating=new FixedChoiceCustomer(1, 1);
        restaurant.addToWaitingQueue(eating);
        restaurant.getWaiter().seat(restaurant, eating, restaurant.getAvailableTable(), sim.turn); sim.endTurn();
        restaurant.getWaiter().takeOrder(restaurant, eating, sim.turn);
        restaurant.getChef().prepare(eating.getOrder(), sim.turn); sim.endTurn();
        restaurant.getWaiter().serve(restaurant, eating, sim.turn); sim.endTurn();
        check("served but not yet paid: revenue is still $0", restaurant.getRevenueCents()==0);

        Customer abandoning=new RegularCustomer(2);
        restaurant.addToWaitingQueue(abandoning);
        sim.idleTurns(13);
        check("abandoning customer left and brought no revenue",
            abandoning.getState()==CustomerState.LEFT&&restaurant.getUnhappyDepartures()==1);
        check("only the paid order adds to revenue", restaurant.getRevenueCents()==1500);
    }

    private static void testAbandonmentAfterThirteenTurns(){
        Restaurant queued=new Restaurant();
        Customer waiting=new RegularCustomer(1);
        queued.addToWaitingQueue(waiting);
        Sim sim=new Sim(queued);
        sim.idleTurns(12);
        check("after 12 turns in the queue the customer is still there (satisfaction 4)",
            waiting.getState()==CustomerState.WAITING&&waiting.getSatisfaction()==4);
        sim.idleTurns(1);
        check("after 13 turns the customer leaves the queue", waiting.getState()==CustomerState.LEFT&&queued.getWaitingQueue().isEmpty());

        Restaurant seated=new Restaurant();
        Customer diner=new FixedChoiceCustomer(1, 2);
        seated.addToWaitingQueue(diner);
        Sim sim2=new Sim(seated);
        seated.getWaiter().seat(seated, diner, seated.getAvailableTable(), sim2.turn); sim2.endTurn();
        seated.getWaiter().takeOrder(seated, diner, sim2.turn);
        seated.getChef().prepare(diner.getOrder(), sim2.turn); sim2.endTurn();
        sim2.idleTurns(20);
        check("seated customer with an unfinished order leaves", diner.getState()==CustomerState.LEFT);
        check("their order is CANCELLED and the table is free again",
            diner.getOrder().getOrderStatus()==OrderStatus.CANCELLED&&seated.getAvailableTable()!=null);
    }

    private static void testVictoryNeedsAllThreeTargets(){
        Restaurant fewPaid=new Restaurant();
        Sim sim1=new Sim(fewPaid);
        for (int id=1;id<=3;id++){
            Customer customer=new FixedChoiceCustomer(id, 2);
            fewPaid.addToWaitingQueue(customer);
            sim1.serveCompletely(customer);
        }
        check("3 paid, $54.00: paid-count target missed, so not a victory",
            fewPaid.getPaidCount()==3&&fewPaid.hasEnoughRevenue()&&fewPaid.hasEnoughSatisfaction()&&!fewPaid.isVictory());

        Restaurant lowRevenue=new Restaurant();
        Sim sim2=new Sim(lowRevenue);
        for (int id=1;id<=4;id++){
            Customer customer=new FixedChoiceCustomer(id, 0);
            lowRevenue.addToWaitingQueue(customer);
            sim2.serveCompletely(customer);
        }
        check("4 paid, $36.00: revenue target missed, so not a victory",
            lowRevenue.getPaidCount()==4&&!lowRevenue.hasEnoughRevenue()&&lowRevenue.hasEnoughSatisfaction()&&!lowRevenue.isVictory());

        Restaurant unhappy=new Restaurant();
        Sim sim3=new Sim(unhappy);
        for (int id=1;id<=4;id++){
            Customer customer=new FixedChoiceCustomer(id, 2);
            unhappy.addToWaitingQueue(customer);
            sim3.idleTurns(6);
            sim3.serveCompletely(customer);
        }
        check("4 paid, $72.00 but low satisfaction: satisfaction target missed, so not a victory",
            unhappy.getPaidCount()==4&&unhappy.hasEnoughRevenue()&&!unhappy.hasEnoughSatisfaction()&&!unhappy.isVictory());

        Restaurant good=new Restaurant();
        Sim sim4=new Sim(good);
        for (int id=1;id<=4;id++){
            Customer customer=new FixedChoiceCustomer(id, 2);
            good.addToWaitingQueue(customer);
            sim4.serveCompletely(customer);
        }
        check("4 paid, $72.00, good satisfaction: all targets met, so victory",
            good.getPaidCount()==4&&good.getRevenueCents()==7200&&good.isVictory());
    }

    // Part B : whole game through GameController
    private static class GameRun{
        Restaurant restaurant;
        String output;
    }

    private static GameRun playGame(String[] inputLines){
        String input="";
        for (String line:inputLines){
            input+=line+"\n";
        }
        PrintStream originalOut=System.out;
        ByteArrayOutputStream captured=new ByteArrayOutputStream();
        GameRun run=new GameRun();
        try{
            System.setOut(new PrintStream(captured));
            GameController controller=new GameController(new GameCLI(new Scanner(input)));
            controller.startGame();
            run.restaurant=controller.getRestaurant();
        }finally{
            System.setOut(originalOut);
        }
        run.output=captured.toString();
        return run;
    }

    private static final String[] VICTORY_INPUT={
        "1", "1",                   
        "2", "1", "1",              
        "3", "1",                 
        "1", "2",                  
        "2", "2", "2",            
        "3", "2",                   
        "1", "3",                  
        "2", "3", "3",              
        "3", "3",                  
        "1", "4",                  
        "2", "4", "4",             
        "3", "4",                 
        "1", "5",                   
        "2", "5", "5",              
        "0", "5",                  
        "3", "5",                   
        "1", "6",                  
        "2", "6", "6"               
    };

    private static final String[] DEFEAT_INPUT={};

    private static void testVictoryRun(){
        GameRun run=playGame(VICTORY_INPUT);
        Restaurant restaurant=run.restaurant;
        check("victory run: 5 customers paid", restaurant.getPaidCount()==5);
        check("victory run: revenue $72.00", restaurant.getRevenueCents()==7200);
        check("victory run: average satisfaction of paid customers is 82.4", Math.abs(restaurant.getAverageSatisfaction()-82.4)<0.0001);
        check("victory run: nobody left unhappy", restaurant.getUnhappyDepartures()==0);
        check("victory run: isVictory() is true", restaurant.isVictory());
        check("victory run: output shows VICTORY", run.output.contains("*** VICTORY ***"));
        check("victory run: all 18 turns were played", run.output.contains("TURN 18 / 18")&&run.output.contains("Turns used:          18"));
        check("victory run: customer's choice is shown once the order is placed", run.output.contains("C1 orders Burger ($15.00, 1 unit(s) to cook)."));
        check("victory run: a dish finished in a turn is served the next turn",
            restaurant.findCustomer(1).getServedTurn()==3);
        check("victory run: the turn-16 customer cannot finish paying within 18 turns",
            restaurant.findCustomer(6).getState()!=CustomerState.PAID);
        check("victory run: a table was freed after payment", restaurant.getAvailableTable()!=null);
    }

    private static void testDefeatRun(){
        GameRun run=playGame(DEFEAT_INPUT);
        Restaurant restaurant=run.restaurant;
        check("defeat run: nobody paid and revenue is $0.00", restaurant.getPaidCount()==0&&restaurant.getRevenueCents()==0);
        check("defeat run: average satisfaction is N/A", run.output.contains("N/A"));
        check("defeat run: two customers left unhappy", restaurant.getUnhappyDepartures()==2);
        check("defeat run: isVictory() is false", !restaurant.isVictory());
        check("defeat run: output shows DEFEAT and not VICTORY", run.output.contains("*** DEFEAT ***")&&!run.output.contains("*** VICTORY ***"));
        check("defeat run: the game still ran all 18 turns", run.output.contains("TURN 18 / 18"));
    }

    
    private static void testRunsAreReproducible(){
        GameRun first=playGame(VICTORY_INPUT);
        GameRun second=playGame(VICTORY_INPUT);
        check("two victory runs print exactly the same transcript", first.output.equals(second.output));
        check("two defeat runs print exactly the same transcript", playGame(DEFEAT_INPUT).output.equals(playGame(DEFEAT_INPUT).output));
    }

 
    private static void testInvalidInputIsHandled(){
        String[] input={
            "abc",   
            "9",     
            "2", "1",
            "1", "7",
            "1", "1" 
        };
        GameRun run=playGame(input);
        check("non-numeric input is rejected with a message", run.output.contains("Please enter a whole number."));
        check("out-of-range input is rejected with a message", run.output.contains("Please enter a number from 0 to 3."));
        check("refused order explains why", run.output.contains("is not seated and waiting to order"));
        check("unknown customer explains why", run.output.contains("There is no customer with id 7."));
        check("after the mistakes the Waiter could still seat C1 in the same turn", run.output.contains("Waiter seats C1 at Table 1."));
        check("the game still finished all 18 turns", run.output.contains("FINAL RESULT"));
    }
}
