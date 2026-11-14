// The only kind of customer in Stage 1: loses 8 satisfaction points for every turn they spend waiting for a table or for food, and pays the full price.
public class RegularCustomer extends Customer{
    public static final int LOSS_PER_TURN=8;

    public RegularCustomer(int id){
        super(id);
    }
    @Override
    protected int getLossPerTurn(){
        return LOSS_PER_TURN;
    }
    @Override
    public String getTypeName(){
        return "Regular";
    }
}
