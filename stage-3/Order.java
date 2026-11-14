// One customer's order for one menu item.
// The Order guards its own status: every change goes through a method that
// first checks the current status, so an order can never skip a step
// (for example, it cannot be served before it is READY). This is encapsulation.
public class Order{
    private final Customer customer;
    private final MenuItem item;
    private final int priceCents;  
    private int remainingUnits;     
    private OrderStatus status;

    // Stage 3: the price is worked out by PricingPolicy (customer discount, then
    // Happy Hour) and locked here when the order is taken.
    public Order(Customer customer, MenuItem item, int priceCents){
        if (customer==null||item==null){
            throw new IllegalArgumentException("An order needs a customer and a menu item.");
        }
        if (priceCents<0){
            throw new IllegalArgumentException("An order's price cannot be negative.");
        }
        this.customer=customer;
        this.item=item;
        this.priceCents=priceCents;
        this.remainingUnits=item.getUnits();
        this.status=OrderStatus.PLACED;
    }
    public Customer getCustomer(){
        return this.customer;
    }
    public MenuItem getItem(){
        return this.item;
    }
    public int getPriceCents(){
        return this.priceCents;
    }
    public int getRemainingUnits(){
        return this.remainingUnits;
    }
    public OrderStatus getOrderStatus(){
        return this.status;
    }

    public void prepareOneUnit(){
        if (status!=OrderStatus.PLACED&&status!=OrderStatus.PREPARING){
            throw new IllegalStateException("Order cannot be prepared as it is "+status+".");
        }
        remainingUnits--;
        if (remainingUnits==0){
            status=OrderStatus.READY;
        }else{
            status=OrderStatus.PREPARING;
        }
    }

    public void markServed(){
        if (status!=OrderStatus.READY){
            throw new IllegalStateException("Order cannot be served as it is "+status+".");
        }
        status=OrderStatus.SERVED;
    }

    public void markPaid(){
        if (status!=OrderStatus.SERVED){
            throw new IllegalStateException("Order cannot be paid as it is "+status+".");
        }
        status=OrderStatus.PAID;
    }

    public void cancel(){
        if (status!=OrderStatus.PLACED&&status!=OrderStatus.PREPARING&&status!=OrderStatus.READY){
            throw new IllegalStateException("Order cannot be cancelled as it is "+status+".");
        }
        status=OrderStatus.CANCELLED;
    }
}
