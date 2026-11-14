import java.util.List;
import java.util.Random;

public class CriticCustomer extends Customer{
    public CriticCustomer(int id){
        super(id);
    }
    protected int getLossPerTurn(){
        return 12;
    }
    public String getTypeName(){
        return "Critic";
    }
    // Order preference: a Critic always orders the most expensive item on the menu (the first one listed if there is a tie). Deterministic, so rng is unused.
    public MenuItem chooseItem(List<MenuItem> menu, Random rng){
        MenuItem choice=menu.get(0);
        for (MenuItem item:menu){
            if (item.getPriceCents()>choice.getPriceCents()){
                choice=item;
            }
        }
        return choice;
    }
}
