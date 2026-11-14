public class Order{
    private final Customer customer;
    private final MenuItem item;
    private final int priceCents;  
    private int remainingUnits;     
    private OrderStatus status;

    public Order(Customer customer, MenuItem item){
        if (customer==null||item==null){
            throw new IllegalArgumentException("An order needs a customer and a menu item.");
        }
        this.customer=customer;
        this.item=item;
        this.priceCents=item.getPriceCents();
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
