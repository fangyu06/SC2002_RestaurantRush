public class VIPCustomer extends Customer{
    public VIPCustomer(int id){
        super(id);
    }
    protected int getLossPerTurn(){
        return 5;
    }
    public String getTypeName(){
        return "VIP";
    }
    public int applyDiscount(int priceCents){
        return (int) Math.round(priceCents*0.9);
    }
}
