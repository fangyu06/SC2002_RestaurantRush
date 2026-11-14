// Works out the locked price of an order, and holds the Happy Hour state.
// Happy Hour can be switched on once per game, at the start of a turn. It
// lasts for that turn and the next. Orders taken while it is active get 20%
// off. The customer's own discount (e.g. VIP) is applied first, then Happy Hour:
// a VIP Burger during Happy Hour = $15.00 x 0.9 x 0.8 = $10.80.
public class PricingPolicy{
    public static final int HAPPY_HOUR_LENGTH=2;          // the activation turn and the next
    public static final double HAPPY_HOUR_MULTIPLIER=0.8;  // 20% off
    public static final int HAPPY_HOUR_RECOVERY=15;        // satisfaction regained per active turn

    private boolean happyHourUsed;
    private boolean happyHourActive;
    private int happyHourTurnsRemaining;

    public PricingPolicy(){
        this.happyHourUsed=false;
        this.happyHourActive=false;
        this.happyHourTurnsRemaining=0;
    }

    // Called at the start of every turn: Happy Hour is active while turns remain.
    public void startTurn(){
        happyHourActive=happyHourTurnsRemaining>0;
    }
    // Can only be switched on once per game. Returns false if it was already used.
    public boolean activateHappyHour(){
        if (happyHourUsed){
            return false;
        }
        happyHourUsed=true;
        happyHourActive=true;
        happyHourTurnsRemaining=HAPPY_HOUR_LENGTH;
        return true;
    }
    // Called at the end of every turn.
    public void endTurn(){
        if (happyHourTurnsRemaining>0){
            happyHourTurnsRemaining--;
        }
        happyHourActive = happyHourTurnsRemaining > 0;
    }
    public boolean isHappyHourUsed(){
        return this.happyHourUsed;
    }
    public boolean isHappyHourActive(){
        return this.happyHourActive;
    }
    public int getRemainingTurns(){
        return this.happyHourTurnsRemaining;
    }

    // The price locked on an order when it is taken: customer discount first, then Happy Hour.
    public int calculatePrice(Customer customer, MenuItem item){
        int price=customer.applyDiscount(item.getPriceCents());
        if (happyHourActive){
            price=(int) Math.round(price*HAPPY_HOUR_MULTIPLIER);
        }
        return price;
    }
}
